package com.wavachao.timeblock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavachao.timeblock.data.model.BlockCategory
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.ui.CalendarUiState
import com.wavachao.timeblock.ui.components.DayLoadDots
import com.wavachao.timeblock.ui.components.SurfaceCard
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.IconSpec
import com.wavachao.timeblock.ui.icons.TimeBlockIcon
import com.wavachao.timeblock.ui.theme.AppTokens
import com.wavachao.timeblock.ui.theme.BrandColors
import com.wavachao.timeblock.ui.util.TimeFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Weekday header labels, Monday first (the mockup's 一 … 日 row). */
private val WeekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/** `rgba(140,124,255,.5)` — the inset ring the mockup draws around today's cell. */
private val TodayRing = Color(0xFF8C7CFF).copy(alpha = 0.5f)

/**
 * Screen 3 `日历`: a month grid with per-day load markers, the category legend, a
 * selected-day footer and that day's blocks.
 *
 * `state.loads` holds one [com.wavachao.timeblock.data.DayLoad] per day of the visible
 * month only, so the leading/trailing cells of the neighbouring months simply resolve
 * to `null` and render bare day numbers (exactly like the mockup's `.day.out` cells).
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onStepMonth: (Long) -> Unit,
    onOpenBlock: (Long) -> Unit,
    onCreateForDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val gridStart = remember(state.month) { monthGridStart(state.month) }
    val weekCount = remember(state.month) { monthWeekCount(state.month) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CalendarNavBar(onCreate = { onCreateForDate(state.selectedDate) })

        MonthHeader(month = state.month, onStepMonth = onStepMonth)

        WeekdayHeader()

        Column(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (week in 0 until weekCount) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (day in 0 until 7) {
                        val date = gridStart.plusDays((week * 7 + day).toLong())
                        DayCell(
                            date = date,
                            inMonth = YearMonth.from(date) == state.month,
                            selected = date == state.selectedDate,
                            today = date == today,
                            categories = state.loadFor(date)?.categories.orEmpty(),
                            onSelect = { onSelectDate(date) },
                        )
                    }
                }
            }
        }

        CategoryLegend()

        SelectedDayFooter(
            state = state,
            onCreate = { onCreateForDate(state.selectedDate) },
        )

        if (state.selectedBlocks.isEmpty()) {
            Text(
                text = "这一天还没有安排",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                color = AppTokens.palette.mutedDim,
                style = AppTokens.type.caption.copy(fontSize = 11.5.sp),
                textAlign = TextAlign.Center,
            )
        } else {
            state.selectedBlocks.forEach { block ->
                BlockRow(block = block, onOpen = { onOpenBlock(block.id) })
            }
        }

        // Clearance for the host's floating bottom bar.
        Spacer(Modifier.height(110.dp))
    }
}

// ---------------------------------------------------------------------------- header

@Composable
private fun CalendarNavBar(onCreate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .height(52.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "日历", color = AppTokens.palette.text, style = AppTokens.type.screenTitle)
        RoundPlusButton(onClick = onCreate)
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onStepMonth: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = TimeFormat.monthLabel(month.atDay(1)),
                color = AppTokens.palette.text,
                style = AppTokens.type.hero.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight(800),
                    letterSpacing = (-0.8).sp,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = month.year.toString(),
                color = AppTokens.palette.muted,
                style = AppTokens.type.hero.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight(650),
                    letterSpacing = (-0.1).sp,
                ),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChevronButton(icon = BlockIcons.ChevronLeft, onClick = { onStepMonth(-1) })
            ChevronButton(icon = BlockIcons.ChevronRight, onClick = { onStepMonth(1) })
        }
    }
}

@Composable
private fun RoundPlusButton(onClick: () -> Unit, size: Dp = 34.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(AppTokens.palette.panel)
            .border(1.dp, AppTokens.palette.stroke, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TimeBlockIcon(
            icon = BlockIcons.Plus,
            size = size * 0.47f,
            tint = AppTokens.palette.textSecondary,
            strokeWidth = 1.9f,
        )
    }
}

@Composable
private fun ChevronButton(icon: IconSpec, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(shape)
            .background(AppTokens.palette.panel)
            .border(1.dp, AppTokens.palette.stroke, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TimeBlockIcon(
            icon = icon,
            size = 15.dp,
            tint = AppTokens.palette.textSecondary,
            strokeWidth = 2.2f,
        )
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp, start = 18.dp, end = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WeekdayLabels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = AppTokens.palette.muted,
                style = AppTokens.type.micro.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight(700),
                    letterSpacing = 0.4.sp,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------- grid

@Composable
private fun RowScope.DayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    categories: List<BlockCategory>,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    val numberColor = when {
        selected -> Color.White
        today -> BrandColors.accent(AppTokens.isDark)
        else -> AppTokens.palette.textSecondary
    }
    val numberWeight = if (selected || today) FontWeight(800) else FontWeight(650)
    val numberAlpha = if (inMonth) 1f else 0.55f

    Box(
        modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .then(if (selected) Modifier.background(BrandColors.brandGradient, shape) else Modifier)
            .then(
                if (today && !selected) {
                    Modifier
                        .padding(5.dp)
                        .border(1.4.dp, TodayRing, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = TimeFormat.dayNumber(date),
                color = numberColor,
                style = AppTokens.type.body.copy(
                    fontSize = 13.5.sp,
                    fontWeight = numberWeight,
                    letterSpacing = (-0.2).sp,
                ),
                modifier = Modifier.alpha(numberAlpha),
            )
            if (categories.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                DayLoadDots(categories = categories, selected = selected)
            }
        }
    }
}

// ---------------------------------------------------------------------------- legend

@Composable
private fun CategoryLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BlockCategory.entries.forEach { category ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(category.color, CircleShape),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = category.displayName,
                    color = AppTokens.palette.muted,
                    style = AppTokens.type.caption.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight(600),
                    ),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------- footer

@Composable
private fun SelectedDayFooter(state: CalendarUiState, onCreate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = TimeFormat.dateWithWeekday(state.selectedDate),
                color = AppTokens.palette.text,
                style = AppTokens.type.bodyStrong.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight(750),
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${state.selectedBlocks.size} 个时间段 · 共 " +
                    TimeFormat.longDurationOrDash(state.selectedMinutes),
                color = AppTokens.palette.muted,
                style = AppTokens.type.caption.copy(fontSize = 11.5.sp),
            )
        }
        RoundPlusButton(onClick = onCreate, size = 36.dp)
    }
}

@Composable
private fun BlockRow(block: TimeBlock, onOpen: () -> Unit) {
    Box(Modifier.padding(horizontal = 22.dp)) {
        SurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .alpha(if (block.done) 0.46f else 1f)
                .clickable(onClick = onOpen),
            shape = RoundedCornerShape(20.dp),
            fill = AppTokens.palette.panel,
            border = AppTokens.palette.stroke,
        ) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.fillMaxHeight().width(3.dp)) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(3.dp)
                            .background(block.category.color),
                    )
                }
                Row(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${TimeFormat.time(block.start)}–${TimeFormat.time(block.end)}",
                        modifier = Modifier.width(74.dp),
                        color = block.category.color,
                        style = AppTokens.type.caption.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight(750),
                            letterSpacing = (-0.2).sp,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        text = block.title,
                        modifier = Modifier.weight(1f),
                        color = AppTokens.palette.text,
                        style = AppTokens.type.body.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight(650),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (block.done) TextDecoration.LineThrough else null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = TimeFormat.duration(block.durationMinutes),
                        color = AppTokens.palette.muted,
                        style = AppTokens.type.caption.copy(fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------- month maths

/** The Monday on or before the first of [month]: where the grid's first cell starts. */
private fun monthGridStart(month: YearMonth): LocalDate {
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    return first.minusDays(offset.toLong())
}

/** Whole weeks needed to show every day of [month] with full Monday-first rows. */
private fun monthWeekCount(month: YearMonth): Int {
    val start = monthGridStart(month)
    val end = month.atEndOfMonth()
    val span = end.toEpochDay() - start.toEpochDay() + 1
    return ((span + 6) / 7).toInt()
}
