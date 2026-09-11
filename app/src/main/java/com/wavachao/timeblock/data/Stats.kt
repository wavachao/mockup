package com.wavachao.timeblock.data

import com.wavachao.timeblock.data.local.BlockRange
import com.wavachao.timeblock.data.model.BlockCategory
import java.time.LocalDate
import java.time.ZoneId

/** Groups raw rows into one [DayLoad] per calendar day. */
fun buildDayLoads(
    ranges: List<BlockRange>,
    days: List<LocalDate>,
    zone: ZoneId = ZoneId.systemDefault(),
): List<DayLoad> {
    val grouped = ranges.groupBy { range ->
        java.time.Instant.ofEpochMilli(range.startEpochMillis).atZone(zone).toLocalDate()
    }
    return days.map { day ->
        val rows = grouped[day].orEmpty()
        DayLoad(
            date = day,
            minutes = rows.sumOf { ((it.endEpochMillis - it.startEpochMillis) / 60_000L).toInt() }
                .coerceAtLeast(0),
            categories = rows
                .map { BlockCategory.fromStorage(it.category) }
                .distinct()
                .take(3),
        )
    }
}

/** Consecutive days ending at [today] that have at least one block planned. */
fun planningStreak(
    daysWithBlocks: Set<LocalDate>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): Int {
    var cursor = if (daysWithBlocks.contains(today)) today else today.minusDays(1)
    var streak = 0
    while (daysWithBlocks.contains(cursor)) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
