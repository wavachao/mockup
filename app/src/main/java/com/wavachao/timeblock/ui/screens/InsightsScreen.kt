package com.wavachao.timeblock.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavachao.timeblock.data.CategorySlice
import com.wavachao.timeblock.data.WeekInsight
import com.wavachao.timeblock.data.weekStart
import com.wavachao.timeblock.ui.InsightsUiState
import com.wavachao.timeblock.ui.components.CategoryDot
import com.wavachao.timeblock.ui.components.FieldLabel
import com.wavachao.timeblock.ui.components.MeterBar
import com.wavachao.timeblock.ui.components.NumberText
import com.wavachao.timeblock.ui.components.Pill
import com.wavachao.timeblock.ui.components.SurfaceCard
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.TimeBlockIcon
import com.wavachao.timeblock.ui.theme.AppTokens
import com.wavachao.timeblock.ui.theme.BrandColors
import com.wavachao.timeblock.ui.theme.Radius
import com.wavachao.timeblock.ui.util.TimeFormat
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Screen 4 `回顾`: the weekly roll-up — planned hours, the seven-day distribution, the
 * category split and the planning streak.
 *
 * The screen paints no background of its own: it is expected to sit on top of
 * `ScreenBackground`, and the bottom bar (110dp tall) is drawn by the host scaffold,
 * which is why the list simply ends in a 110dp spacer.
 */
@Composable
fun InsightsScreen(
    state: InsightsUiState,
    onStepWeek: (Long) -> Unit,
    onCurrentWeek: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val insight = state.insight
    // `WeekInsight.perDay` is documented as Monday..Sunday, but the state default is an
    // empty list, so every read below is index-safe and falls back to the week start.
    val days: List<Pair<LocalDate, Int>> = (0..6).map { index ->
        insight.perDay.getOrNull(index) ?: (state.weekStart.plusDays(index.toLong()) to 0)
    }
    val maxMinutes = days.maxOfOrNull { it.second } ?: 0
    val peakDate = if (maxMinutes <= 0) null else insight.peakDay

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        InsightsHeader(
            weekStart = state.weekStart,
            onStepWeek = onStepWeek,
            onCurrentWeek = onCurrentWeek,
        )
        WeeklyHeroCard(insight = insight, values = days.map { it.second })
        DailyDistributionCard(days = days, peakDate = peakDate, maxMinutes = maxMinutes)
        AllocationCard(slices = insight.slices)
        StreakCard(streakDays = insight.streakDays)
        Spacer(Modifier.height(110.dp))
    }
}

// ---------------------------------------------------------------------------- header

@Composable
private fun InsightsHeader(
    weekStart: LocalDate,
    onStepWeek: (Long) -> Unit,
    onCurrentWeek: () -> Unit,
) {
    val label = if (weekStart == LocalDate.now().weekStart()) {
        "本周"
    } else {
        "${weekStart.monthValue}/${weekStart.dayOfMonth} 起"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "回顾",
            color = AppTokens.palette.text,
            style = AppTokens.type.screenTitle,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeekStepButton(icon = BlockIcons.ChevronLeft) { onStepWeek(-1) }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .clickable { onCurrentWeek() },
            ) {
                Pill(
                    text = label,
                    style = AppTokens.type.caption.copy(fontWeight = FontWeight(650)),
                )
            }
            WeekStepButton(icon = BlockIcons.ChevronRight) { onStepWeek(1) }
        }
    }
}

/** Same 28dp chevron affordance the calendar header and the today hero use. */
@Composable
private fun WeekStepButton(icon: com.wavachao.timeblock.ui.icons.IconSpec, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(Radius.pill))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TimeBlockIcon(
            icon = icon,
            size = 14.dp,
            tint = AppTokens.palette.muted,
            strokeWidth = 2.2f,
        )
    }
}

// ---------------------------------------------------------------------------- hero

@Composable
private fun WeeklyHeroCard(insight: WeekInsight, values: List<Int>) {
    val parts = TimeFormat.metricParts(insight.totalMinutes)
    SurfaceCard(
        modifier = Modifier.padding(horizontal = 22.dp),
        shape = RoundedCornerShape(Radius.hero),
        brush = Brush.linearGradient(
            listOf(Color(0x3D6D5EF8), Color(0x1A22D3EE), Color(0x00FFFFFF)),
        ),
        contentPadding = 22.dp,
    ) {
        Text(
            text = "本周已规划时长",
            color = AppTokens.palette.textSecondary,
            style = AppTokens.type.caption.copy(fontSize = 11.5.sp, fontWeight = FontWeight(600)),
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            NumberText(
                text = parts.first,
                style = AppTokens.type.metric,
                color = AppTokens.palette.text,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "h",
                style = AppTokens.type.metricUnit,
                color = AppTokens.palette.muted,
            )
            Spacer(Modifier.width(8.dp))
            NumberText(
                text = parts.second,
                style = AppTokens.type.metric,
                color = AppTokens.palette.text,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "m",
                style = AppTokens.type.metricUnit,
                color = AppTokens.palette.muted,
            )
        }
        Spacer(Modifier.height(12.dp))
        WeekDeltaPill(delta = insight.deltaMinutes)
        Spacer(Modifier.height(16.dp))
        Sparkline(
            values = values,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )
    }
}

@Composable
private fun WeekDeltaPill(delta: Int) {
    if (delta == 0) {
        Pill(
            text = "与上周持平",
            fill = AppTokens.palette.panelStrongest,
            border = AppTokens.palette.stroke,
            contentColor = AppTokens.palette.muted,
        )
        return
    }
    val trendColor = Color(0xFF34D399)
    val label = if (delta > 0) {
        "比上周多 ${TimeFormat.longDuration(delta)}"
    } else {
        "比上周少 ${TimeFormat.longDuration(-delta)}"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TimeBlockIcon(
            icon = BlockIcons.Trend,
            size = 12.dp,
            tint = trendColor,
            strokeWidth = 2.4f,
        )
        Spacer(Modifier.width(6.dp))
        Pill(
            text = label,
            fill = Color(0x2434D399),
            border = Color(0x4D34D399),
            contentColor = trendColor,
        )
    }
}

/** Smooth line + soft area under it; falls back to a flat line when the week is empty. */
@Composable
private fun Sparkline(values: List<Int>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        if (values.isEmpty() || canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        val maxValue = values.maxOrNull() ?: 0
        val left = 1.5f
        val right = (canvasWidth - 1.5f).coerceAtLeast(left + 1f)
        val span = (right - left).coerceAtLeast(1f)
        val baseline = (canvasHeight - 2f).coerceAtLeast(1f)
        val topEdge = 4f
        val plot = (baseline - topEdge).coerceAtLeast(1f)
        val step = if (values.size > 1) span / (values.size - 1).toFloat() else 0f

        val points = values.mapIndexed { index, value ->
            val ratio = if (maxValue <= 0) {
                0f
            } else {
                (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
            }
            Offset(x = left + step * index.toFloat(), y = baseline - plot * ratio)
        }

        val linePath = Path().apply { appendSmoothLine(points) }
        val areaPath = Path().apply {
            appendSmoothLine(points)
            lineTo(points.last().x, baseline)
            lineTo(points.first().x, baseline)
            close()
        }

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x476D5EF8), Color(0x006D5EF8)),
                startY = topEdge,
                endY = baseline,
            ),
        )
        drawPath(
            path = linePath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF6D5EF8), Color(0xFF22D3EE)),
                start = Offset(left, 0f),
                end = Offset(right, 0f),
            ),
            style = Stroke(width = 2.4f, cap = StrokeCap.Round),
        )
    }
}

/** Plain cubic smoothing: both control points sit at half the horizontal distance. */
private fun Path.appendSmoothLine(points: List<Offset>) {
    if (points.isEmpty()) return
    moveTo(points[0].x, points[0].y)
    for (index in 1 until points.size) {
        val previous = points[index - 1]
        val current = points[index]
        val midX = (previous.x + current.x) / 2f
        cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
    }
}

// ---------------------------------------------------------------------------- daily bars

@Composable
private fun DailyDistributionCard(
    days: List<Pair<LocalDate, Int>>,
    peakDate: LocalDate?,
    maxMinutes: Int,
) {
    SurfaceCard(
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
        contentPadding = 20.dp,
    ) {
        FieldLabel(text = "每日分布", modifier = Modifier.padding(bottom = 16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { (date, minutes) ->
                val isPeak = peakDate != null && date == peakDate
                val fraction = if (maxMinutes <= 0) {
                    0.06f
                } else {
                    (minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.06f, 1f)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // The weighted wrapper reserves the room taken by the 9dp gap and the
                    // weekday label, so a full-height bar can never push the label out.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(11.dp, 11.dp, 7.dp, 7.dp))
                                .background(
                                    if (isPeak) {
                                        BrandColors.brandGradient
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color(0x8C6D5EF8), Color(0x296D5EF8)),
                                        )
                                    },
                                ),
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = TimeFormat.shortWeekday(date),
                        color = if (isPeak) {
                            BrandColors.accent(AppTokens.isDark)
                        } else {
                            AppTokens.palette.muted
                        },
                        style = AppTokens.type.micro.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight(650),
                        ),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------- allocation

@Composable
private fun AllocationCard(slices: List<CategorySlice>) {
    SurfaceCard(
        modifier = Modifier.padding(horizontal = 22.dp),
        contentPadding = 20.dp,
    ) {
        FieldLabel(text = "时间分配")
        if (slices.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "本周还没有安排",
                color = AppTokens.palette.mutedDim,
                style = AppTokens.type.caption.copy(fontSize = 12.sp),
            )
            Spacer(Modifier.height(12.dp))
        } else {
            slices.forEach { slice -> AllocationRow(slice = slice) }
        }
    }
}

@Composable
private fun AllocationRow(slice: CategorySlice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryDot(category = slice.category, size = 9.dp)
        Spacer(Modifier.width(13.dp))
        Text(
            text = slice.category.displayName,
            modifier = Modifier.width(52.dp),
            color = AppTokens.palette.text,
            style = AppTokens.type.caption.copy(fontSize = 12.5.sp, fontWeight = FontWeight(650)),
        )
        Spacer(Modifier.width(12.dp))
        MeterBar(
            progress = slice.share,
            modifier = Modifier.weight(1f),
            height = 7.dp,
            brush = Brush.linearGradient(listOf(slice.category.color, slice.category.color)),
            track = AppTokens.palette.hairline,
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.width(60.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            NumberText(
                text = TimeFormat.duration(slice.minutes),
                style = AppTokens.type.caption,
                color = AppTokens.palette.textSecondary,
            )
        }
    }
}

// ---------------------------------------------------------------------------- streak

@Composable
private fun StreakCard(streakDays: Int) {
    val streak = streakDays.coerceAtLeast(0)
    val today = LocalDate.now()
    val streakWeekdays: Set<DayOfWeek> =
        (0 until streak).map { offset -> today.minusDays(offset.toLong()).dayOfWeek }.toSet()
    val hint = if (streakDays > 0) "保持下去，今天也安排一段时间" else "从今天开始记录你的时间段"

    SurfaceCard(
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
        brush = Brush.linearGradient(listOf(Color(0x29FBBF24), Color(0x1AF472B6))),
        border = Color(0x38FBBF24),
        contentPadding = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🔥",
                color = AppTokens.palette.text,
                style = AppTokens.type.hero.copy(fontSize = 26.sp, lineHeight = 30.sp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "连续规划 $streakDays 天",
                    color = AppTokens.palette.text,
                    style = AppTokens.type.bodyStrong.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight(750),
                    ),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = hint,
                    color = AppTokens.palette.textSecondary,
                    style = AppTokens.type.caption.copy(fontSize = 11.5.sp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                weekdayLabels.forEachIndexed { index, label ->
                    StreakDaySquare(
                        label = label,
                        active = streakWeekdays.contains(DayOfWeek.of(index + 1)),
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakDaySquare(label: String, active: Boolean) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .size(20.dp)
            .then(
                if (active) {
                    Modifier.background(BrandColors.warmGradient, shape)
                } else {
                    Modifier.background(AppTokens.palette.panelStrong, shape)
                },
            )
            .border(
                width = 1.dp,
                color = if (active) Color.Transparent else AppTokens.palette.stroke,
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) Color.White else AppTokens.palette.muted,
            style = AppTokens.type.micro.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
        )
    }
}

private val weekdayLabels: List<String> = listOf("一", "二", "三", "四", "五", "六", "日")
