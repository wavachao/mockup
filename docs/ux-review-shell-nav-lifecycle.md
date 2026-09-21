# TimeBlock — UX / behavioral review: app shell, navigation, state, lifecycle

Scope: `MainActivity.kt`, `ui/MainViewModel.kt`, `TimeBlockApp.kt`, `ui/screens/ProfileScreen.kt`,
`AndroidManifest.xml`, `res/values*/themes.xml`, `strings.xml`, `colors.xml` (+ transitive deps:
`EditorScreen`, `QuickAddSheet`, `TodayScreen`, `CalendarScreen`, `InsightsScreen`,
`BlockDetailScreen`, `Components.kt`, `TimeBlockTheme.kt`, `TimelineLayout.kt`, `TimeFormat.kt`,
`TimeBlockDao/Database`, `OfflineTimeBlockRepository`, `ReminderScheduler`, `ReminderNotifications`).
No files were modified. Line numbers are exact as of this reading.

---

## Top 5 things to fix first

1. **No `BackHandler` anywhere in the app** → back closes the whole app instead of dismissing the
   quick-add sheet / option sheets. `MainActivity.kt:118`, `MainActivity.kt:289`, `EditorScreen.kt:104`.
2. **Notification taps do nothing while the app is alive** — `MainActivity` never overrides
   `onNewIntent`; the deep-link effect only runs from `onCreate`. `MainActivity.kt:82`, `:132`.
3. **The app is frozen in yesterday after midnight** — `today`/`selectedDate`/`weekStart` are
   computed once at `StateFlow` construction; the 30 s tick only refreshes `now`. `MainViewModel.kt:98`,
   `:105`, `CalendarScreen.kt:77`, `InsightsScreen.kt:105`, `:458`.
4. **Rotation silently destroys the saved-once state**: an unsaved editor draft, the open sheet, the
   calendar month, the selected day. `MainActivity.kt:118-119`, `EditorScreen.kt:98`, `MainViewModel.kt:98-105`.
5. **`values-night/themes.xml` uses the *Light* platform parent** — backwards for a permanently dark
   app, and the file also drops the status/nav-bar contrast flags the day theme sets.
   `values-night/themes.xml:3`.

---

## P0 — blocks a core task

### P0-1 · System/gesture back exits the app instead of dismissing the quick-add sheet
`MainActivity.kt:118`, `MainActivity.kt:289-303`, `ui/components/QuickAddSheet.kt:57-192`

```kotlin
118:    var sheetOpen by remember { mutableStateOf(false) }
...
289:        QuickAddSheet(
290:            visible = sheetOpen,
...
302:            onDismiss = { sheetOpen = false },
```

`QuickAddSheet` is a hand-rolled overlay (documented at `QuickAddSheet.kt:50-56`), not a
`ModalBottomSheet`/`Dialog`, and there is **no `BackHandler` in the entire `app/src/main` tree**
(verified by grep for `BackHandler|rememberSaveable|SavedStateHandle|imePadding|navigationBars`:
zero matches). The scrim and the sheet still consume no back event, so pressing system back while
the sheet is open pops the *NavHost* / finishes the activity.

User impact: the user taps the FAB, starts "快速添加时间段", changes their mind, presses back — the
app disappears. On re-launch the sheet is gone and their typed title is lost. On the editor route the
same missing handler is a behaviour mismatch: gesture back discards silently (see P1-1) while the
`✕` also discards, with no confirmation either way.

Fix: add `BackHandler(enabled = sheetOpen) { sheetOpen = false }` inside `TimeBlockRoot` (before or
after the sheet), and `BackHandler(enabled = showReminderSheet) { showReminderSheet = false }` +
the recurrence twin inside `BlockEditorScreen` (`EditorScreen.kt:104-105`, `:159`, `:171`).

### P0-2 · A reminder tap does not open the block when the app is already running
`MainActivity.kt:27` (`android:launchMode="singleTop"`), `MainActivity.kt:82-91`, `:132-134`;
`reminder/ReminderNotifications.kt:45-55`

```xml
27:            android:launchMode="singleTop"
```
```kotlin
82:    override fun onCreate(savedInstanceState: Bundle?) {
85:        val initialBlockId = intent?.getLongExtra(EXTRA_BLOCK_ID, -1L) ?: -1L
...
132:    LaunchedEffect(initialBlockId) {
133:        if (initialBlockId > 0L) navController.navigate(Routes.detail(initialBlockId))
134:    }
```

With `singleTop`, re-launching the running activity goes to **`onNewIntent`**, which `MainActivity`
does not override (grep: `onNewIntent` → no match). The `EXTRA_BLOCK_ID` extra is never re-read and
`setIntent` is never called, so tapping "09:00 开始，准备好进入这段时间"
(`ReminderNotifications.kt:60`) just brings the last screen to the front. The whole point of the
notification — "tap to see this block" — silently fails, and the deep link only works for a cold start.

Fix: override `onNewIntent(intent)` → `setIntent(intent)`, read the extra there and either
`navController.navigate(Routes.detail(id))` directly (hoist the NavController or use a
`MutableStateFlow<Long?>` in the ViewModel) or expose it as an event the root consumes once.

### P0-3 · `加入今天` adds to 9 AM *today* even when "today" is almost over, and the reminder is then discarded
`MainActivity.kt:154-157`, `:166-169`; `EditorScreen.kt:98-103`, `:151`; `MainViewModel.kt:227-234`;
`reminder/ReminderScheduler.kt:40-43`

```kotlin
154:                        onAddForDate = { date ->
155:                            draft = TimeBlockDraft.startingAt(date.atTime(9, 0))
156:                            sheetOpen = true
157:                        },
```
```kotlin
40:        if (triggerAt <= System.currentTimeMillis()) {
41:            cancel(block.id)
42:            return
43:        }
```

Two concrete user-visible failures:

* **Wrong time.** Tap `+` in the header at 17:30 → the sheet opens pre-filled **09:00–10:00**, a
  block that is already over. The FAB path (`MainActivity.kt:266`) correctly uses `todayState.now`,
  so the same action has two different defaults depending on which button you press.
* **Silent reminder loss.** `TimeBlockDraft.reminderMinutes` defaults to `10`
  (`data/model/TimeBlockDraft.kt:19`). For a block at 09:00 created at 17:30, `triggerAt` is in the
  past, so `schedule()` calls `cancel()` and returns. The editor's 提醒 row still reads
  `提前 10 分钟` (`EditorScreen.kt:438`), and the profile screen shows no reminder status at all —
  the user believes a reminder exists. Nothing anywhere tells them it cannot fire.

Fix: derive the default start from `max(selectedDate, today)` — use `now` when the target date is
today and 09:00 only for future dates (`TimeBlockDraft.startingAt(date.atTime(9,0))` → a helper that
takes "now" into account). Separately, disable/annotate the reminder option when the trigger instant
is already past, and surface reminder scheduling failure.

### P0-4 · Any Room write failure is unhandled: the tap appears dead, then the process dies with the draft
`MainViewModel.kt:227-251`

```kotlin
227:    fun createBlock(draft: TimeBlockDraft, onSaved: (Long) -> Unit = {}) {
228:        viewModelScope.launch {
229:            val block = draft.toBlock()
230:            val id = repository.save(block)
231:            reminderScheduler.schedule(block.copy(id = id))
232:            onSaved(id)
233:        }
234:    }
```

No `try/catch`, no `Result`, no error state, and the UI reads no error channel: `grep` for
`Snackbar|Toast|onFailure` across `app/src/main/java` returns only `TimeBlockApp.kt:39` and
`ReminderNotifications.kt:68`. `viewModelScope` uses the default `SupervisorJob` + `Dispatchers.Main.immediate`,
so a `SQLiteFullException`/`DiskFullException`/`IllegalStateException` from Room is rethrown to the
thread's uncaught handler → **crash**. The user's experience is: press 保存 / 创建时间段, nothing
happens for a moment, app vanishes, draft gone, `onSaved` never ran so the screen never pops.

The same pattern covers `updateBlock` (`:236`), `deleteBlock` (`:246`), `toggleDone` (`:215`) and
`markDone` (`:253`). `TimeBlockApp.kt:39` shows the author knows the pattern:
`scope.launch { runCatching { reminderScheduler.rescheduleAll() } }` — swallowing there hides a
completely silent failure of *all* reminders on boot, while crashing here. Both extremes are wrong.

Fix: wrap each write in `runCatching`, expose a `Channel`/`StateFlow<String?>` of errors in
`MainViewModel`, and render a `Snackbar` in `TimeBlockRoot` (or an inline error on the editor). For
`rescheduleReminders`, at least log and, if it fails repeatedly, surface a "提醒不可用" state.

---

## P1 — significant friction

### P1-1 · Back from the editor discards unsaved work with no confirmation
`EditorScreen.kt:98-105`, `:112-116`, `:192-212`; `MainActivity.kt:233`, `:256`

```kotlin
 98:    var draft by remember(initial?.id, dateHint) {
 99:        mutableStateOf(
100:            initial?.let { TimeBlockDraft.from(it) }
101:                ?: TimeBlockDraft.startingAt(dateHint.atTime(defaultStartTime(now)), minutes = 60),
102:        )
103:    }
...
112:        EditorNavBar(
113:            isNew = initial == null,
114:            onBack = onBack,
...
233:                        onBack = { navController.popBackStack() },
```

An edit-session draft lives only in a plain `remember` inside the composable. The `✕`
(`EditorScreen.kt:208`) and system back both call `popBackStack()` immediately. Retitling a block,
picking a different category and a 90-minute duration, then brushing back, loses all of it with no
"放弃修改？" prompt. This is the single most likely daily annoyance in the app.

Fix: track `dirty = draft != original` in the editor, and gate `onBack` (and a `BackHandler`) behind
a small confirm dialog. Cheap alternative: a `rememberSaveable`-backed draft so at least a rotation
or accidental backgrounding does not throw it away (see P1-3).

### P1-2 · Notification permission is requested silently on very first frame, and denial is a dead end
`MainActivity.kt:121-129`

```kotlin
121:    val permissionLauncher = rememberLauncherForActivityResult(
122:        contract = ActivityResultContracts.RequestPermission(),
123:    ) { /* reminders simply stay silent if the user declines */ }
124:
125:    LaunchedEffect(Unit) {
126:        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
127:            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
128:        }
129:    }
```

Two problems: (a) the dialog appears instantly on the app's very first composition, before the user
has any idea what the app does — the worst moment to ask, and the most likely to be denied; (b) the
result callback is an empty comment. There is no re-request, no `shouldShowRequestPermissionRationale`
handling, no notification-status row on `ProfileScreen` (whose header comment at
`ProfileScreen.kt:41-44` claims it "carries … the reminder status" — it does not; the screen only
renders 时间段 stats, 分类, 设计稿对照 and a "回到今天的时间轴" row). `SCHEDULE_EXACT_ALARM` /
`USE_EXACT_ALARM` (`AndroidManifest.xml:6-7`) are covered by neither the manifest-only path nor any
settings prompt: `AndroidReminderScheduler.canScheduleExact` (`ReminderScheduler.kt:68-73`) silently
degrades to `setAndAllowWhileIdle`, which can be minutes late — plausible for `提前 10 分钟`, so
notifications that should land before a block may not. "提醒降级而不是不提醒" (README:65) is a
reasonable engineering decision that is invisible to the user.

Fix: ask for POST_NOTIFICATIONS after the user creates their first block with a reminder; add a real
"提醒" section to `ProfileScreen` showing notifications enabled + exact-alarm allowed, with a
button that opens `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` / app notification settings.

### P1-3 · Rotation or "don't keep activities" wipes the open sheet, the editor draft, the selected day and the calendar month
`MainActivity.kt:118-119`; `EditorScreen.kt:98`; `MainViewModel.kt:98-105`;
`AndroidManifest.xml:23-34`

```kotlin
118:    var sheetOpen by remember { mutableStateOf(false) }
119:    var draft by remember { mutableStateOf(TimeBlockDraft.startingAt(LocalDateTime.now())) }
```

```kotlin
 98:    private val _todayState = MutableStateFlow(TodayUiState())
...
105:    private val _calendarState = MutableStateFlow(CalendarUiState())
```

Nothing in the state plane is `SavedStateHandle`-backed (`grep` → zero matches) and the activity
declares no `android:configChanges`, so every rotation recreates the whole tree. Because
`_todayState.selectedDate` resets to `LocalDate.now()` (`MainViewModel.kt:54`) and
`_calendarState.month` to `YearMonth.now()` (`:78`), rotating on 回顾 for a past week, or on 日历 in
next month, silently teleports the user back to today — the ViewModel survives, but its *position*
does not. Rotating with the quick-add sheet open loses the typed title; rotating in the editor loses
the whole draft while the NavHost correctly restores the *route*, so the user lands on a
freshly-reset editor.

Note the inconsistency: `rememberNavController()` does survive (Navigation-Compose uses
`rememberSaveable` internally), so the app looks half-stateful.

Fix: `MainViewModel(repository, scheduler, savedStateHandle)` storing
`selectedDate`/`calendarMonth`/`weekStart`/`selectedCalendarDate`; `rememberSaveable` for
`sheetOpen`, the sheet draft fields and the editor draft (a `TimeBlockDraft` is flat and parcelable
as a few strings/ints). Add `draft` autosave if you want to survive process death.

### P1-4 · Process death / backgrounding wipes the tab position (and the draft) with no autosave
`MainActivity.kt:109-119`, `EditorScreen.kt:98-105`

Same root cause as P1-3, worse consequence: after Android kills the app in the background, the user
returns via Recents to the start destination. Nothing is persisted mid-edit — there is no
`onSaveInstanceState` override either (`grep` on `MainActivity.kt`: only `onCreate` matches), so the
in-flight `draft` in `MainActivity.kt:119` and the editor draft are gone. There is no draft autosave
anywhere (no DataStore, no SharedPreferences — the only prefs reference in the repo is
`backup_rules.xml:4`).

Fix: shortest path is `rememberSaveable` for the two drafts (survives process death via the activity
bundle). A stronger version writes the editor draft to a `draft` row / DataStore on every change.

### P1-5 · Content is drawn under the system navigation bar and under the IME; only the status bar is inset
`MainActivity.kt:10` (`enableEdgeToEdge()`), `:136-142`; `MainActivity.kt:315-323`;
`QuickAddSheet.kt:86-92`; `EditorScreen.kt:107-111`

```kotlin
 83:        enableEdgeToEdge()
...
136:    Box(Modifier.fillMaxSize()) {
...
138:        Box(
139:            Modifier
140:                .fillMaxSize()
141:                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
142:        ) {
```

`enableEdgeToEdge()` is called, but the app only compensates for `statusBars`. There is **no**
`navigationBars`, no `imePadding`, no `safeDrawing`/`windowInsetsPadding` anywhere in
`app/src/main` (grep → zero matches). Consequences:

* `BottomTabBar` ends with `.padding(top = 12.dp, bottom = 18.dp)` (`MainActivity.kt:323`). On a
  gesture-nav device that leaves the tab labels inside the ~24–48 dp system gesture area, i.e. the
  "今天/日历/回顾/我的" row is partially under the system bar and the up-swipe region.
* `QuickAddSheet` ends with `bottom = 30.dp` (`QuickAddSheet.kt:92`), so 创建时间段 sits on the
  gesture bar.
* With the keyboard up, `windowSoftInputMode="adjustResize"` (`AndroidManifest.xml:29`) plus
  edge-to-edge means the sheet/editor get no IME padding; the QuickAdd title field
  (`QuickAddSheet.kt:126-150`) and the editor's bottom 保存 button (`EditorScreen.kt:150-155`) can be
  covered by the keyboard with nothing scrolling them into view (the editor is a
  `verticalScroll` column, but the IME overlays the bottom of the window rather than resizing the
  Compose content unless insets are consumed).

Fix: put `.windowInsetsPadding(WindowInsets.navigationBars)` (or
`.safeDrawingPadding()`/`Modifier.imePadding()`) on the sheet and the tab bar container instead of
the hardcoded `bottom = 18.dp` / `bottom = 30.dp`, and add `imePadding()` to the editor's column.

### P1-6 · After midnight the whole app is still showing yesterday
`MainViewModel.kt:98`, `:105`, `:53-55`, `:96`, `:195-202`; `CalendarScreen.kt:77`;
`InsightsScreen.kt:105`, `:458`

```kotlin
 96:    private val clock = MutableStateFlow(LocalDateTime.now())
...
195:    private fun tickClock() {
196:        viewModelScope.launch {
197:            while (true) {
198:                clock.value = LocalDateTime.now()
199:                delay(CLOCK_TICK_MILLIS)
200:            }
201:        }
202:    }
```

```kotlin
 53:    val today: LocalDate = LocalDate.now(),
 54:    val selectedDate: LocalDate = LocalDate.now(),
```

`today` and `selectedDate` are evaluated **once**, when `MutableStateFlow(TodayUiState())` is
constructed (`:98`) — i.e. when the ViewModel is created — and the tick loop only writes `now`
(`clock`). The `combine` at `:118` therefore refreshes `now`/`blocks`/`stats` but never re-evaluates
`today` or `selectedDate`. `_calendarState.month`/`selectedDate` (`:78-79`) and
`_insightsState.weekStart` (`:65`) are frozen the same way, and `CalendarScreen.kt:77`
(`val today = LocalDate.now()`) plus `InsightsScreen.kt:105`/`:458` re-read the clock only when
recomposition happens to run.

So: the app sits in the background (or the phone is on a charger) across midnight → on resume the
header still says 今天 for the previous date, `isToday` is false so the now-line is hidden
(`TodayScreen.kt:255`), the stats describe yesterday, `今天`'s date stepping is off by one, and the
calendar's today-ring sits on the wrong cell. Nothing in `MainViewModel` or `MainActivity` listens
for `ACTION_DATE_CHANGED`/`ACTION_TIMEZONE_CHANGED`/`ACTION_TIME_SET` (the receivers for those
actions only rebuild alarms: `BootCompletedReceiver.kt:33-39`), and there is no `Lifecycle`
observer anywhere (`grep` for `LocalLifecycleOwner|LifecycleEventObserver|repeatOnLifecycle|ON_RESUME`
→ zero matches).

Fix: recompute the day when the date actually changes — keep the tick but have it push a
`LocalDate` into a `todayFlow` and derive `selectedDate`/`today` from it, or add an
`ON_RESUME`/date-change listener (`LocalLifecycleOwner` + `repeatOnLifecycle(STARTED)`, or a
`BroadcastReceiver` for `Intent.ACTION_DATE_CHANGED`). On rollover, move `selectedDate` to the new
day if the user was sitting on "today".

### P1-7 · Timezone change leaves stale UI, and blocks silently shift their stored wall-clock range
`reminder/BootCompletedReceiver.kt:33-39`; `MainViewModel.kt:317-325`;
`data/local/TimeBlockEntity.kt:70-80`

`TIMEZONE_CHANGED` only rebuilds alarms; nothing re-queries the day. Worse, the day query is a
*range in epoch millis computed with `ZoneId.systemDefault()` at call time*
(`OfflineTimeBlockRepository.kt:20-22` → `startOfDayMillis`, `endOfDayMillisExclusive`) while the
displayed times come from the stored wall-clock string (`TimeFormat.time(block.start)`). After a
timezone change the same rows are bucketed into different days: a 09:00 block can appear on the
neighbouring day or vanish from today's timeline until some later event re-subscribes the flow, and
`MainViewModel.kt:317-325` (`toRangeRow`) re-derives the calendar's loads with the new zone, so
日历 and 今天 can disagree. The dataset is small but the inconsistency is user-visible.

Fix: on `ACTION_TIMEZONE_CHANGED`/`ACTION_DATE_CHANGED`, force a refresh of all three flows (e.g.
a `refreshSignal: MutableStateFlow<Int>` folded into each `flatMapLatest`), and re-render the
calendar/insights from the same "day" definition the timeline uses.

### P1-8 · The 30 s tick keeps running while the app is in the background, and it re-subscribes the DB query every time
`MainViewModel.kt:114-133`, `:194-202`, `:293`

```kotlin
114:    /** Screen 1: re-query whenever the selected day changes, refresh on every tick. */
115:    private fun observeSelectedDay() {
116:        _todayState
117:            .flatMapLatest { state -> repository.observeDay(state.selectedDate) }
118:            .combine(clock) { blocks, now -> now to blocks }
```

The comment says "re-query whenever the selected day changes", but `flatMapLatest` is keyed on the
**whole state object**, and the tick calls `_todayState.update { it.copy(now = now, ...) }`
(`:122-130`). Every `now` change therefore cancels and restarts `dao.observeBetween(...)`, i.e. a
fresh Room query + invalidation-tracker registration **every 30 seconds**, forever — even while the
screen is off / another app is foregrounded (`viewModelScope` is not lifecycle-aware, and nothing
uses `collectAsStateWithLifecycle`). On a long background session that is ~2 880 redundant queries
per day plus a wakeup every 30 s.

Fix: key the re-query on the date only —
`_todayState.map { it.selectedDate }.distinctUntilChanged().flatMapLatest { repository.observeDay(it) }`
— and gate the tick (and the collectors) behind `repeatOnLifecycle(STARTED)`
(`collectAsStateWithLifecycle` is already on the classpath: `app/build.gradle.kts:120`,
`libs.versions.toml:21`, currently unused).

### P1-9 · Editor and sheets can render a completely blank frame
`MainActivity.kt:242-259`

```kotlin
242:                    var block by remember(blockId) { mutableStateOf<TimeBlock?>(null) }
243:                    LaunchedEffect(blockId) { block = viewModel.blockById(blockId) }
244:                    val loaded = block
245:                    if (loaded != null) {
246:                        BlockEditorScreen(
```

Until the suspend `blockById` returns, the EDIT route draws **nothing at all** — no nav bar, no
spinner, no back affordance. The detail route at least shows 正在读取时间段…
(`BlockDetailScreen.kt:105`); the editor does not. If the id no longer resolves (see P1-10) the
screen stays permanently blank and the user must use system back to escape a white/transparent void.

Fix: render the editor's chrome (nav bar + a progress indicator, or a skeleton of the fields)
unconditionally, and if `block == null` after the load resolves, show "这个时间段已被删除" with a
back action.

### P1-10 · Deleting a block can leave the Detail screen showing a block that no longer exists
`MainActivity.kt:206-209`, `:252-255`

```kotlin
206:                        onDelete = { id ->
207:                            viewModel.deleteBlock(id)
208:                            navController.popBackStack()
209:                        },
```
```kotlin
252:                            onDelete = { id ->
253:                                viewModel.deleteBlock(id)
254:                                navController.popBackStack()
255:                            },
```

`deleteBlock` only cancels the alarm and deletes the row (`MainViewModel.kt:246-251`). Popping
**before** the delete coroutine completes leaves the Detail route's `block` state
(`MainActivity.kt:197`, loaded once into a `remember`) holding the deleted object, so Detail keeps
rendering it; pressing 编辑 then lands on the blank editor from P1-9, and 提前完成 calls
`dao.setDone` on a missing row (affected rows = 0, no feedback). The detail `block` is also never
refreshed when the underlying row changes — it is read once in `LaunchedEffect(blockId, todayState.blocks)`
(`:198`) and only for the *detail* route, not EDIT.

Fix: make these `suspend`/callback-driven — `viewModel.deleteBlock(id) { navController.popBackStack() }`
— and have the detail route observe `repository.observeBlock(id)` (the DAO query already exists,
`TimeBlockDao.kt:48-49`, and is currently unused by the UI) so a stale object can never be rendered.

---

## P2 — polish / correctness

### P2-1 · Destructive delete has no confirmation on the editor; the detail screen's is a two-tap-with-hint
`EditorScreen.kt:130-147`; `BlockDetailScreen.kt:122-134`, `:180-189`

```kotlin
130:        if (initial != null) {
...
137:                    .clickable { onDelete(initial.id) }
```
```kotlin
127:        onMore = {
128:            if (confirming) {
129:                onDelete(block.id)
130:            } else {
131:                confirming = true
132:            }
133:        },
```

删除这个时间段 (`EditorScreen.kt:142`) deletes permanently on a single tap, with no undo
(`deleteBlock` is a hard `DELETE`, `TimeBlockDao.kt:78-79`). The detail screen's 再次点击删除 hint is
better but the "armed" state is a plain `remember` (`BlockDetailScreen.kt:122`) that never resets on
timeout, so the ⋮ button stays in delete mode indefinitely. Fix: unified confirm dialog (or
`Snackbar` + undo via an "undelete" that re-inserts the row), and clear `confirming` after a few
seconds or on any other interaction.

### P2-2 · Deep-link intent is consumed twice, producing a duplicated back-stack entry
`MainActivity.kt:132-134`

`LaunchedEffect(initialBlockId)` re-runs on every activity recreation, `initialBlockId` is read from
`intent` in `onCreate` (`:85`) and the extra is never removed or marked consumed, while Navigation
Compose restores its own back stack from the saved bundle. After a rotation on the detail screen
opened from a notification, the restored detail entry plus a second `navigate(Routes.detail(id))`
puts the same block on the stack twice — one back press appears to do nothing (to the user, "back is
broken"). Fix: only honour the extra for a fresh launch (`savedInstanceState == null`), then
`intent.removeExtra(EXTRA_BLOCK_ID)`, or route it through a one-shot event.

### P2-3 · "回到今天的时间轴" pushes an extra "today" on top of the back stack
`MainActivity.kt:186-188`

```kotlin
186:                        onOpenToday = {
187:                            navController.navigate(Routes.TODAY) { launchSingleTop = true }
188:                        },
```

No `popUpTo(startDestination)`/`restoreState` (compare the tab bar's builder at `:279-283`). From
我的 (or 回顾) the stack becomes `today, calendar, insights, profile, today`; pressing back returns
to 我的 instead of leaving the app, and the tab bar now highlights 今天 while an identical 今天 entry
sits underneath. Fix: reuse the tab-bar navigation options.

### P2-4 · Tab switches lose scroll position and the calendar's scroll page
`TodayScreen.kt:79`, `CalendarScreen.kt:81`, `InsightsScreen.kt:82`, `MainActivity.kt:279-283`

```kotlin
 79:            .verticalScroll(rememberScrollState()),
```

Every tab body scrolls with a plain `rememberScrollState()` (none of the four screens uses
`rememberSaveable` — grep confirms zero matches app-wide). Navigation-Compose's
`popUpTo(...){saveState=true}` / `restoreState=true` (`MainActivity.kt:281-282`) faithfully restores
the entry, but there is no saveable state to restore, so switching 今天 → 日历 → 今天 drops you back
at the top of a 24-hour timeline (1 104 dp of content at `hourHeight = 46.dp`). Day/month/week
selection *does* survive (it lives in the ViewModel), which makes the reset more disorienting, not
less. Fix: `rememberSaveable(saver = ScrollState.Saver)` for each list.

### P2-5 · `values-night` parent theme is inverted, and the two theme files disagree
`values/themes.xml:3-11`; `values-night/themes.xml:3-8`

```xml
<!-- values/themes.xml -->
 3:    <style name="Theme.TimeBlock" parent="android:Theme.Material.NoActionBar">
 7:        <item name="android:windowLightStatusBar">false</item>
 8:        <item name="android:windowLightNavigationBar">false</item>
```
```xml
<!-- values-night/themes.xml -->
 3:    <style name="Theme.TimeBlock" parent="android:Theme.Material.Light.NoActionBar">
```

The app is permanently dark (`TimeBlockTheme.kt:74` `const val USE_LIGHT_PALETTE = false`, and
`values/colors.xml:5` pins `window_background` to `#FF07080C` for both configurations), so the
*-night* resource should not switch the platform theme to the Light variant — that flips the
inherited `windowLightStatusBar`/`windowLightNavigationBar` defaults in the opposite direction from
the file that handles the common case. The night file also omits `enforceStatusBarContrast` /
`enforceNavigationBarContrast` (which the day file sets at `:10-11` — note these are not valid
attribute names; the real ones are `android:enforceStatusBarContrast` under
`Theme.MaterialComponents`/values-v29 style hierarchies, so this is likely being ignored). Net effect
on a light-mode device vs a dark-mode device: an inconsistent status-bar icon treatment on a UI that
is dark in both. Fix: make both files identical dark parents with explicit
`windowLightStatusBar=false` / `windowLightNavigationBar=false`, delete the `-night` override, and
let `TimeBlockTheme.kt:130-136` (which does set the appearance flags correctly via
`WindowCompat.getInsetsController`) be the single source of truth. **Not device-verified** — see the
verification section.

### P2-6 · Almost every string is a hardcoded Chinese literal; a non-Chinese user gets a mixed-language app
`res/values/strings.xml:2` (`app_name` only); examples: `MainActivity.kt:101-104`,
`EditorScreen.kt:76`, `:142`, `:151`, `:214`, `:551`, `TodayScreen.kt:132`, `:204`, `:218`,
`QuickAddSheet.kt:108`, `ProfileScreen.kt:66`, `:95`, `:103`, `BlockDetailScreen.kt:105`, `:169`,
`ReminderNotifications.kt:27`, `:30`, `:60`

`strings.xml` contains exactly one string. Everything the user reads is a Kotlin literal. On an
English phone the app name, notification title/channel name ("时间段提醒", "在时间段开始前提醒你"),
permission prompts and every button stay Chinese while the OS around them is English — the app looks
broken rather than localized. Because `android:supportsRtl="true"` is declared
(`AndroidManifest.xml:19`) with essentially no `start/end`-aware padding (the code uses
`padding(horizontal = …)` and `padding(start = …)` inconsistently), an RTL locale also mirrors
layout around LTR Chinese text. Also `TimeFormat.weekday` hardcodes `Locale.SIMPLIFIED_CHINESE`
(`TimeFormat.kt:16`, `:62`, `:66`) and manual string templates replace what should be
`DateTimeFormatter.ofLocalizedDate`/plurals (e.g. `"${hours} 小时 ${rest} 分"`, `TimeFormat.kt:44`),
so there is no path to per-locale formatting at all. Fix: move UI copy into `values/strings.xml` +
`values-zh-rCN/strings.xml`, use `pluralStringResource` where counts appear, and pass
`Locale.getDefault()` (with a Chinese default) into the formatters.

### P2-7 · No empty-state guidance on the main screen
`TodayScreen.kt:76-93`, `TimelineLayout.kt:67-69`

For a brand-new user the 今天 tab renders a bare 08:00–20:00 ruler: `buildTimeline(emptyList())`
returns an empty block list, and `TodayScreen` has no branch for it (`TimelineCanvas` at `:245-351`
draws only hour rules, the rail and gaps). The only cue is the unlabelled FAB
(`QuickAddSheet.kt:307-320`, icon-only, no content description). `CalendarScreen` does have
"这一天还没有安排" (`:118`) and `TodayScreen.kt:369` even reserves `EmptyStateAccent` for an
illustration that is never rendered. Fix: render an empty-state card with the same accent, e.g.
"今天还没有安排 · 点右下角 + 添加第一段时间", and give the FAB a `contentDescription`.

### P2-8 · The "详细设置" expand resets the quick-add time to 09:00
`MainActivity.kt:298-301`

```kotlin
298:            onExpand = {
299:                sheetOpen = false
300:                navController.navigate(Routes.addFor(draft.date))
301:            },
```

The sheet holds the user's chosen start time (`draft.startTime`, edited via `DurationField`),
but Expanding discards it: the ADD route recomputes from `dateHint.atTime(defaultStartTime(now))`
(`EditorScreen.kt:101`), where `now` defaults to `LocalDateTime.now()`. So 15:45 → 详细设置 silently
becomes 15:45-or-09:00 depending on the path, and any +1h/+2h choice is lost. Fix: pass the draft
into the editor (e.g. `Routes.addFor(date)` plus the start minute, or hoist the draft into the
ViewModel) instead of rebuilding it from `dateHint`.

### P2-9 · `ProfileScreen` renders Jetpack/kotlin defaults and a designer-facing token table
`ProfileScreen.kt:46-51`, `:158-164`, `:103-108`

```kotlin
 46: fun ProfileScreen(
 47:     totalBlocks: Int = 0,
 48:     selectedDate: java.time.LocalDate = java.time.LocalDate.now(),
 49:     onOpenToday: () -> Unit = {},
```
```kotlin
158:            FieldLabel("设计稿对照")
160:            TokenRow(name = "主色", value = "#6D5EF8", color = BrandColors.Primary)
```

"设计稿对照" with four hardcoded hex chips is implementation scaffolding surfaced to end users (the
same category of leak as the dead `AvatarPlaceholder`, `ProfileScreen.kt:245`). Meanwhile the card
labelled 今天 shows `TimeFormat.dateWithWeekday(selectedDate)` (`:106`) — the *selected* date, not
today — so navigating the 今天 tab to next Wednesday and opening 我的 shows a 今天 tile reading
"周三". And 已记录时间段 counts only the selected day's blocks (`MainActivity.kt:184`
`totalBlocks = todayState.blocks.size`) while `repository.observeTotalCount()`
(`TimeBlockDao.kt:87-88`) exists unused — the label reads like an all-time total. Fix: replace the
token table with the reminder/permission status this screen was meant to carry, use
`observeTotalCount()` for the stat, and label the date tile with the actual today.

### P2-10 · Time-wheel selection works by rewriting state from list geometry
`EditorScreen.kt:346-368`

```kotlin
358:    val centeredIndex by remember {
359:        derivedStateOf {
360:            (listState.firstVisibleItemIndex + 1).coerceIn(items.first(), items.last())
361:        }
362:    }
364:    LaunchedEffect(listState) {
365:        snapshotFlow { centeredIndex }.collect { index ->
366:            if (index != value) onValueChange(index)
367:        }
368:    }
```

The wheel infers the user's chosen hour/minute from `firstVisibleItemIndex + 1` and writes it back
into the draft, while the highlighted row is chosen by `value` (`:390`). The list has a 22 dp top
spacer and 44 dp rows (`:388-408`) under a 46 dp highlight band (`:376`), so the index formula and
the band do not line up exactly; the effects are a highlight that drifts from the band and a value
that can be rewritten by a fling. It is fragile by construction: any change to spacer/row/band
heights silently changes the saved time. Fix: compute the centred index from
`listState.firstVisibleItemIndex + listState.firstVisibleItemScrollOffset / rowHeight` (rounded), or
use a `Picker`-style implementation driven by a single source of truth, and unit-test "scroll to row
N → draft hour == N". **Interactive behaviour not device-verified.**

### P2-11 · Cold start does extra main-thread work before the first frame; no splash
`TimeBlockApp.kt:50-55`, `:38-40`

```kotlin
50:    override fun onCreate() {
51:        super.onCreate()
52:        container = AppContainer(this)
53:        ReminderNotifications.ensureChannel(this)
54:        container.rescheduleReminders()
55:    }
```
```kotlin
 25:    private val database: TimeBlockDatabase = TimeBlockDatabase.get(context)
```

`Room.databaseBuilder(...).build()` runs on the main thread inside `AppContainer.init`
(`TimeBlockDatabase.kt:23-32`) — only a handle, but it forces the DB config/`openHelper` path before
the first frame. `rescheduleReminders()` (`:38-40`) then opens the database for real and issues a
synchronous `between()` query (`ReminderScheduler.kt:61-66` → `rangeBlocks`) on a `Dispatchers.Default`
scope started from `Application.onCreate`; on a cold start that query contends with the first
composition, and Room's first open does file I/O plus (on a first-ever launch) `CREATE TABLE`. There
is no splash screen dependency (`grep` for `core-splashscreen|SplashScreen` → no match), so the
window background is the only thing covering that work; `window_background` is a solid dark color
(`values/colors.xml:5`), so the StartWindow itself is fine on Android 12+. Fix: defer
`rescheduleReminders()` until after the first frame (e.g. from the UI's first `ON_START`, or a
`LaunchedEffect`), and avoid touching the DB in `Application.onCreate`.

### P2-12 · Notification text/title fallback and result handling swallow everything
`ReminderNotifications.kt:68`, `TimeBlockApp.kt:39`, `BlockAlarmReceiver.kt:21-24`

`runCatching { manager.notify(...) }` (`:68`) discards both the failure and the reason; combined
with the empty permission callback (`MainActivity.kt:123`) there is no point in the app at which a
user can discover that reminders are not firing. `runCatching { reminderScheduler.rescheduleAll() }`
(`TimeBlockApp.kt:39`) does the same for the whole boot path. Fix: log at minimum, and add the
reminder-status UI from P1-2.

---

## What I could not verify

* **No build/run.** I did not compile or install the app and there is no emulator in this session
  (`app/build.gradle.kts` targets `minSdk 26 / targetSdk 35`, AGP 8.9.2, Compose BOM 2024.12.01,
  Navigation 2.8.5, activity-compose 1.9.3). Everything below is static reading of the source.
* **Runtime back-stack behaviour of P0-2 / P2-2.** That `onNewIntent` is not called is certain from
  the manifest + source; the *exact* duplicate-entry symptom in P2-2 depends on Navigation-Compose's
  state restoration after `onCreate` re-entry and should be confirmed with `adb shell am start` +
  `adb shell input keyevent KEYCODE_BACK`.
* **Theme/status-bar rendering (P2-5).** Whether the inverted `-night` parent actually produces
  wrong-coloured status-bar icons depends on the platform default resolution of
  `windowLightStatusBar` for `Theme.Material.Light.NoActionBar` on the device in question; I did not
  confirm it on a device. The inversion itself and the missing flags are visible in the files.
* **Keyboard/IME and gesture-bar overlap (P1-5).** The absence of `navigationBars`/`imePadding`
  insets is certain from the source; the exact dp of overlap depends on the device's nav mode and
  gesture inset — worth a screenshot on a gesture-nav device and one 3-button device.
* **The time wheel (P2-10).** I traced the arithmetic by hand but could not fling it; the
  index-vs-band misalignment needs an interactive check (scroll to a known hour and compare the
  highlighted row with the saved value).
* **`ProfileScreen` against the mockup.** `ui/mockup.html` has a 我的 tab label (`mockup.html:783`)
  but no profile screen body, so I could not compare the 设计稿对照 card or empty-state treatment
  against a reference; `ui/preview.png` was not inspected image-wise. I did not open the mockup's
  screen list beyond grepping for the tab bar.
