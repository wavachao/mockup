package com.wavachao.timeblock.ui.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.ui.TodayUiState
import com.wavachao.timeblock.ui.components.Eyebrow
import com.wavachao.timeblock.ui.components.GapHint
import com.wavachao.timeblock.ui.components.HourLabel
import com.wavachao.timeblock.ui.components.MeterBar
import com.wavachao.timeblock.ui.components.NowLine
import com.wavachao.timeblock.ui.components.NumberText
import com.wavachao.timeblock.ui.components.ProgressRing
import com.wavachao.timeblock.ui.components.SurfaceCard
import com.wavachao.timeblock.ui.components.TimelineBlockCard
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.TimeBlockIcon
import com.wavachao.timeblock.ui.theme.AppTokens
import com.wavachao.timeblock.ui.theme.BrandColors
import com.wavachao.timeblock.ui.theme.Radius
import com.wavachao.timeblock.ui.theme.TimelineMetrics
import com.wavachao.timeblock.ui.util.LaidOutBlock
import com.wavachao.timeblock.ui.util.TimeFormat
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

private val ScreenPadding = 22.dp

/**
 * Screen 1 · 今天.
 *
 * The 24-hour timeline is the product, so the screen is a scrollable column containing a
 * fixed-height, absolutely positioned timeline canvas. All geometry comes from
 * [TodayUiState.timeline] (a fixed 46dp hour row), never from a layout pass — that is what
 * makes `14:00` always land exactly on the `14` hour rule.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    onToggleDone: (TimeBlock) -> Unit,
    onOpenBlock: (Long) -> Unit,
    onStepDate: (Long) -> Unit,
    onAddForDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TodayHeader(
            state = state,
            onStepDate = onStepDate,
            onAddForDate = onAddForDate,
        )
        SummaryCard(state)
        TimelineCanvas(
            state = state,
            onToggleDone = onToggleDone,
            onOpenBlock = onOpenBlock,
            modifier = Modifier.padding(top = 20.dp, bottom = 132.dp),
        )
    }
}

@Composable
private fun TodayHeader(
    state: TodayUiState,
    onStepDate: (Long) -> Unit,
    onAddForDate: (LocalDate) -> Unit,
) {
    val palette = AppTokens.palette
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Eyebrow(TimeFormat.dateWithWeekday(state.selectedDate))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = state.title, color = palette.text, style = AppTokens.type.hero)
                Spacer(Modifier.width(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DateStepButton(icon = BlockIcons.ChevronLeft) { onStepDate(-1) }
                    DateStepButton(icon = BlockIcons.ChevronRight) { onStepDate(1) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!state.isToday) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(palette.panel)
                        .border(1.dp, palette.stroke, RoundedCornerShape(Radius.pill))
                        .clickable { onStepDate(state.today.toEpochDay() - state.selectedDate.toEpochDay()) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text("回到今天", color = palette.textSecondary, style = AppTokens.type.micro)
                }
            }
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(palette.panel)
                    .border(1.dp, palette.stroke, CircleShape)
                    .clickable { onAddForDate(state.selectedDate) },
                contentAlignment = Alignment.Center,
            ) {
                TimeBlockIcon(
                    icon = BlockIcons.Bell,
                    size = 19.dp,
                    tint = palette.textSecondary,
                )
                if (state.stats.blockCount > state.stats.doneCount) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-9).dp, y = 9.dp)
                            .size(7.dp)
                            .background(palette.danger, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun DateStepButton(icon: com.wavachao.timeblock.ui.icons.IconSpec, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TimeBlockIcon(
            icon = icon,
            size = 15.dp,
            tint = AppTokens.palette.muted,
            strokeWidth = 2.2f,
        )
    }
}

@Composable
private fun SummaryCard(state: TodayUiState) {
    val palette = AppTokens.palette
    val stats = state.stats
    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 20.dp),
        brush = Brush.linearGradient(listOf(Color(0x296D5EF8), Color(0x0DFFFFFF), Color(0x05FFFFFF))),
        contentPadding = 18.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                percent = stats.completion,
                label = "${stats.completionPercent}%",
                caption = "已完成",
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                SummaryLine(key = "已规划时长", value = TimeFormat.duration(stats.plannedMinutes))
                Spacer(Modifier.height(9.dp))
                MeterBar(progress = stats.completion, height = 4.dp)
                Spacer(Modifier.height(11.dp))
                SummaryLine(key = "今日时段", value = "${stats.blockCount} 个")
                Spacer(Modifier.height(9.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("下一个", color = palette.muted, style = AppTokens.type.caption.copy(fontSize = 12.5.sp))
                    val next = stats.next
                    Text(
                        text = next?.let { "${TimeFormat.time(it.start)} ${it.title}" } ?: "没有更多安排",
                        color = if (next != null) palette.text else palette.muted,
                        style = AppTokens.type.caption.copy(fontSize = 12.5.sp, fontWeight = FontWeight(700)),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(key: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(key, color = AppTokens.palette.muted, style = AppTokens.type.caption.copy(fontSize = 12.5.sp))
        NumberText(
            text = value,
            style = AppTokens.type.body.copy(fontSize = 14.5.sp, fontWeight = FontWeight(750)),
        )
    }
}

@Composable
private fun TimelineCanvas(
    state: TodayUiState,
    onToggleDone: (TimeBlock) -> Unit,
    onOpenBlock: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AppTokens.palette
    val layout = state.timeline
    val height = TimelineMetrics.hourHeight * layout.totalHours
    val minuteOfDay = state.now.hour * 60 + state.now.minute
    val showNow = state.isToday && layout.containsMinute(minuteOfDay)
    val nowFraction = layout.fractionFor(minuteOfDay)
    val railPadding = ScreenPadding - TimelineMetrics.gutterWidth

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = ScreenPadding),
    ) {
        Column(Modifier.fillMaxSize()) {
            for (hour in layout.startHour until layout.endHour) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(TimelineMetrics.hourHeight),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(start = TimelineMetrics.gutterWidth + 4.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(palette.hairline),
                    )
                    HourLabel(
                        hour = hour,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = (-7).dp),
                    )
                }
            }
        }

        Box(
            Modifier
                .offset(x = TimelineMetrics.gutterWidth)
                .width(2.dp)
                .fillMaxHeight()
                .background(palette.hairline),
        )

        layout.blocks.forEach { laid ->
            val blockHeight = TimelineMetrics.hourHeight * (laid.durationMinutes / 60f)
            val laneFraction = 1f / laid.laneCount
            Box(
                Modifier
                    .offset(
                        x = TimelineMetrics.blockInset,
                        y = TimelineMetrics.hourHeight * (laid.offsetMinutes / 60f),
                    )
                    .fillMaxWidth(laneFraction)
                    .height(blockHeight.coerceAtLeast(44.dp))
                    .padding(end = if (laid.laneCount > 1) 6.dp else 0.dp),
            ) {
                TimelineBlockCard(
                    block = laid.block,
                    live = isLive(laid, state.now),
                    timingLabel = timingLabel(laid.block, state.now),
                    onToggleDone = { onToggleDone(laid.block) },
                    onClick = { onOpenBlock(laid.block.id) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        layout.gaps.forEach { gap ->
            val gapHeight = TimelineMetrics.hourHeight * (gap.minutes / 60f)
            val hintHeight = gapHeight - 4.dp
            if (hintHeight >= 18.dp) {
                GapHint(
                    label = "空档 ${TimeFormat.longDuration(gap.minutes)}",
                    height = hintHeight,
                    modifier = Modifier
                        .offset(
                            x = TimelineMetrics.blockInset,
                            y = TimelineMetrics.hourHeight * (gap.startMinutes / 60f) + 2.dp,
                        )
                        .padding(end = railPadding),
                )
            }
        }

        if (showNow) {
            NowLine(
                clockLabel = TimeFormat.clock(state.now),
                modifier = Modifier
                    .offset(
                        x = railPadding,
                        y = (height * nowFraction) - 6.dp,
                    )
                    .padding(end = railPadding),
            )
        }
    }
}

private fun isLive(laid: LaidOutBlock, now: LocalDateTime): Boolean =
    !laid.block.done && !now.isBefore(laid.block.start) && now.isBefore(laid.block.end)

/** The coloured caption inside a block: remaining, upcoming, or the final duration. */
private fun timingLabel(block: TimeBlock, now: LocalDateTime): String = when {
    block.done -> TimeFormat.duration(block.durationMinutes)
    now.isBefore(block.start) -> TimeFormat.untilLabel(TimeFormat.minutesBetween(now, block.start))
    now.isAfter(block.end) -> "未完成 · ${TimeFormat.duration(block.durationMinutes)}"
    else -> TimeFormat.remainingLabel(TimeFormat.minutesBetween(now, block.end))
}

/** Height of one timeline lane in whole dp, used by previews and the empty state. */
internal fun laneHeight(minutes: Int): Dp =
    (TimelineMetrics.hourHeight.value * (minutes / 60f)).roundToInt().dp

/** Accent used by the empty state's illustration. */
internal val EmptyStateAccent: Color = BrandColors.Primary
