# TimeBlock · 时间段待办

把一天切成看得见的时间段。核心交互只有一个：**给事情指定一个开始与结束时间**。

本仓库是 `ui/mockup.html` 高保真设计稿的 Android 实现，使用 **Kotlin + Jetpack Compose + Material 3**，
数据落在 **Room**，提醒走 **AlarmManager 精确闹钟**，全部编译在 **GitHub Actions** 云端完成（本机不需要 Android SDK）。

---

## 1. 设计稿对照

| 设计稿 | 屏幕 | 实现文件 |
| --- | --- | --- |
| ① 今天 · 时间轴主视图 | `TodayScreen` | `app/src/main/java/com/wavachao/timeblock/ui/screens/TodayScreen.kt` |
| ① 快速添加 sheet | `QuickAddSheet` | `app/src/main/java/com/wavachao/timeblock/ui/components/QuickAddSheet.kt` |
| ② 新建 / 编辑 · 滚轮选时 | `BlockEditorScreen` | `app/src/main/java/com/wavachao/timeblock/ui/screens/EditorScreen.kt` |
| ③ 日历 · 月视图 + 负荷标记 | `CalendarScreen` | `app/src/main/java/com/wavachao/timeblock/ui/screens/CalendarScreen.kt` |
| ④ 回顾 · 时长统计与连续打卡 | `InsightsScreen` | `app/src/main/java/com/wavachao/timeblock/ui/screens/InsightsScreen.kt` |
| ⑤ 详情 · 进度与子任务 | `BlockDetailScreen` | `app/src/main/java/com/wavachao/timeblock/ui/screens/BlockDetailScreen.kt` |

设计稿按 `390×844` 逻辑像素绘制，因此 **1 CSS px = 1 dp**，颜色直接一一映射为 `Color(0xFF…)`。
设计令牌集中在两个文件里，改主题只需要改它们：

- `ui/theme/Tokens.kt` — 调色板、品牌渐变、圆角、时间轴度量（46dp/小时）
- `ui/theme/Type.kt` — 字阶（标题 800 / 正文 650 / 弱文 600）

### 设计稿里没有的东西

- **图标**：设计稿的图标是 1.9px 圆头描边的自定义路径。为了不引入 `material-icons-extended`
  （为了 5 个图标背几 MB），`ui/icons/BlockIcons.kt` 内置了一个极小的 SVG path 解析器，
  把设计稿里的路径数据原样渲染成 Canvas 描边。
- **时间轴几何**：`ui/util/TimelineLayout.kt` 负责所有"某个时刻落在第几个像素"的计算，
  包括重叠时间段的列分配（设计稿里没有重叠，但真实排程一定会有）。

---

## 2. 架构

```
data/                    纯 Kotlin/Kotlinx 域层 + Room 持久层
  model/                 TimeBlock · TimeBlockDraft · BlockCategory · RecurrenceRule · SubTask
  local/                 TimeBlockEntity · TimeBlockDao · TimeBlockDatabase · SubTaskCodec
  TimeBlockRepository    接口（屏幕永远不直接碰 Room）
  OfflineTimeBlockRepository
  DayStats / WeekInsight / DayLoad / buildDayLoads / planningStreak
  Stats.kt

reminder/                AlarmManager 精确闹钟 + 通知 + 开机重建
  ReminderScheduler · BlockAlarmReceiver · BootCompletedReceiver · ReminderNotifications

ui/
  MainViewModel          单一状态源：TodayUiState / CalendarUiState / InsightsUiState
  components/            SurfaceCard · Pill · PrimaryButton · MeterBar · TimeBlockIcon 等设计系统组件
  screens/               五个屏幕
  theme/ icons/ util/
```

几个刻意的选择：

- **起止时间同时存 epoch millis 和本地墙钟文本**。范围查询走索引列；显示的永远是你当初排的那个钟点，
  换时区不会把日程悄悄挪走（`TimeBlockEntityTest` 钉住了这一点）。
- **重复日程在保存时展开** 为未来 28 天的具体行（`PlanHorizon.Default`），
  否则日历、统计、提醒三处都要各自实现一次"虚拟实例"。
- **子任务用文本列编码**而不是第二张表：它永远只跟着父块一起读。`SubTaskCodec` 有往返测试。
- **提醒降级**：`SCHEDULE_EXACT_ALARM` 被用户收回时退化为非精确闹钟，而不是直接不提醒。

---

## 3. 云端构建（本机无 SDK）

工作流：

- `.github/workflows/ci.yml` — 每次 push 到 `main` / `feat/**` / `fix/**` / `chore/**`、以及所有 PR：
  跑单元测试、编译 debug + release APK、Lint、上传 APK 产物。
- `.github/workflows/release.yml` — push `v*` 标签时构建 APK 并发布到 GitHub Release。

工作流使用 `gradle/actions/setup-gradle` 提供 Gradle 8.11.1，**不依赖仓库里的 wrapper jar**；
本地已经放了官方 wrapper（`gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar`），
装了 JDK 的机器可以直接 `./gradlew`。

### 取 APK

1. 打开 Actions → 最近一次成功的 `CI` 运行；
2. 页面底部 **Artifacts** 下载 `timeblock-apk-<sha>`；
3. 或者直接打标签，从 Release 页面下载 `timeblock-v1.0.0.apk`。

release 变体目前用 debug keystore 签名（`app/build.gradle.kts` 里有注释标注），
目的是让产物能直接安装评估；正式发布前换成自己的 upload key 即可。

### 本地构建（可选）

本机没有 JDK / Android SDK / Gradle，所以默认走云端。若要本地出包：

```bash
# 需要 JDK 17 与 Android SDK（platform 35 + build-tools）
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

---

## 4. 测试

纯 JVM 单元测试（无需模拟器），覆盖三块最容易出错的逻辑：

| 测试 | 钉住的行为 |
| --- | --- |
| `TimelineLayoutTest` | 时间轴偏移量（`09:00 → 46dp`）、窗口自适应、空档、重叠分列、now 线位置 |
| `TimeFormatTest` | `09:00 – 10:30`、`1h30m`、`6 小时 20 分`、`还有 20 分钟`、`已完成` 等全部文案 |
| `TimeBlockTest` | 时长派生、跨天草稿、重叠判定、进度钳制、重复规则、提醒文案 |
| `StatsTest` | 当日完成率、下一个时间段、周统计与分类占比、日历负荷、连续打卡 |
| `TimeBlockEntityTest` | Room 往返、子任务编解码、时区漂移防护、坏数据兜底 |

```bash
./gradlew :app:testDebugUnitTest      # 或交给 GitHub Actions
```

---

## 5. 开发规范

- `main` 只接受经过 CI 的合并，功能开发在 `feat/*` 分支上进行。
- 提交信息使用 Conventional Commits：`feat:` `fix:` `chore:` `docs:` `test:` `ci:` `refactor:`。
- 合并到 `main` 使用 `--no-ff`，保留分支拓扑，方便回溯某个功能是整块引入的。
- 版本以标签发布：`git tag -a v1.0.0 -m "…" && git push origin v1.0.0`。

```
docs/          # 本文件与设计稿说明
ui/            # 高保真设计稿（mockup.html + preview.png），作为实现对照基准
app/           # Android 应用
.github/       # CI / Release 工作流
```
