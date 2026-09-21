package com.wavachao.timeblock.data

import com.wavachao.timeblock.data.model.BlockCategory
import com.wavachao.timeblock.data.model.TimeBlock
import java.time.Duration
import java.time.LocalDate

data class ScheduleSummary(
    val total: Int,
    val completed: Int,
    val allDay: Int,
    val plannedMinutes: Long,
    val completedMinutes: Long,
    val dailyCounts: List<Int>,
    val categories: Map<BlockCategory, Int>,
) {
    val completionRate: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

/** Unique schedules overlapping the period. All-day records do not imply working hours. */
fun summarizeSchedules(blocks: List<TimeBlock>, from: LocalDate, through: LocalDate): ScheduleSummary {
    require(!through.isBefore(from))
    val start = from.atStartOfDay()
    val end = through.plusDays(1).atStartOfDay()
    val included = blocks.filter { it.start < end && it.end > start }
    fun minutes(block: TimeBlock): Long = if (block.allDay) 0 else
        Duration.between(maxOf(start, block.start), minOf(end, block.end)).toMinutes().coerceAtLeast(0)
    val days = java.time.temporal.ChronoUnit.DAYS.between(from, through).toInt() + 1
    return ScheduleSummary(
        total = included.size,
        completed = included.count { it.done },
        allDay = included.count { it.allDay },
        plannedMinutes = included.sumOf(::minutes),
        completedMinutes = included.filter { it.done }.sumOf(::minutes),
        dailyCounts = (0 until days).map { index ->
            val day = from.plusDays(index.toLong())
            included.count { it.start < day.plusDays(1).atStartOfDay() && it.end > day.atStartOfDay() }
        },
        categories = included.groupingBy { it.category }.eachCount(),
    )
}
