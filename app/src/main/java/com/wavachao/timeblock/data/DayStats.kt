package com.wavachao.timeblock.data

import com.wavachao.timeblock.data.model.BlockCategory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Per-category slice of the insights screen. */
data class CategorySlice(
    val category: BlockCategory,
    val minutes: Int,
    val blockCount: Int,
    val share: Float,
)

/** Everything screen 1 needs to render its summary card. */
data class DayStats(
    val date: LocalDate,
    val blockCount: Int,
    val doneCount: Int,
    val doneMinutes: Int,
    val plannedMinutes: Int,
    val next: com.wavachao.timeblock.data.model.TimeBlock?,
) {
    val completion: Float
        get() = if (blockCount == 0) 0f else doneCount.toFloat() / blockCount.toFloat()

    val completionPercent: Int get() = (completion * 100f).toInt()

    companion object {
        fun of(
            date: LocalDate,
            blocks: List<com.wavachao.timeblock.data.model.TimeBlock>,
            now: LocalDateTime,
        ): DayStats {
            val planned = blocks.sumOf { it.durationMinutes.coerceAtLeast(0) }
            val doneMinutes = blocks.filter { it.done }.sumOf { it.durationMinutes.coerceAtLeast(0) }
            val next = blocks
                .filter { !it.done && it.end.isAfter(now) }
                .minByOrNull { it.start }
            return DayStats(
                date = date,
                blockCount = blocks.size,
                doneCount = blocks.count { it.done },
                doneMinutes = doneMinutes,
                plannedMinutes = planned,
                next = next,
            )
        }
    }
}

/** Screen 4 roll-up. */
data class WeekInsight(
    val start: LocalDate,
    val end: LocalDate,
    val totalMinutes: Int,
    val previousTotalMinutes: Int,
    val perDay: List<Pair<LocalDate, Int>>,
    val slices: List<CategorySlice>,
    val streakDays: Int,
) {
    val deltaMinutes: Int get() = totalMinutes - previousTotalMinutes

    val peakDay: LocalDate? get() = perDay.maxByOrNull { it.second }?.first

    fun minutesOn(date: LocalDate): Int = perDay.firstOrNull { it.first == date }?.second ?: 0

    companion object {
        fun of(
            weekStart: LocalDate,
            weekEnd: LocalDate,
            blocks: List<com.wavachao.timeblock.data.model.TimeBlock>,
            previousTotalMinutes: Int,
            streakDays: Int,
        ): WeekInsight {
            val perDay = (0..6).map { offset ->
                val day = weekStart.plusDays(offset.toLong())
                day to blocks.filter { it.date == day }.sumOf { it.durationMinutes.coerceAtLeast(0) }
            }
            val total = perDay.sumOf { it.second }
            val byCategory = blocks.groupBy { it.category }
            val slices = BlockCategory.entries.map { category ->
                val minutes = byCategory[category].orEmpty().sumOf { it.durationMinutes.coerceAtLeast(0) }
                CategorySlice(
                    category = category,
                    minutes = minutes,
                    blockCount = byCategory[category].orEmpty().size,
                    share = if (total == 0) 0f else minutes.toFloat() / total.toFloat(),
                )
            }.filter { it.minutes > 0 }
            return WeekInsight(
                start = weekStart,
                end = weekEnd,
                totalMinutes = total,
                previousTotalMinutes = previousTotalMinutes,
                perDay = perDay,
                slices = slices,
                streakDays = streakDays,
            )
        }
    }
}

/** Calendar day cell: which categories are planned and how heavy the day is. */
data class DayLoad(
    val date: LocalDate,
    val minutes: Int,
    val categories: List<BlockCategory>,
) {
    val isEmpty: Boolean get() = minutes == 0

    /** Mirrors the mockup's up-to-three dots + 4px load bar. */
    fun intensity(maxMinutes: Int): Float =
        if (maxMinutes <= 0) 0f else (minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.15f, 1f)
}

internal fun Duration.minutesOrZero(): Int = toMinutes().toInt().coerceAtLeast(0)
