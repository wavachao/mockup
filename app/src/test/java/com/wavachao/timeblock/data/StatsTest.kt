package com.wavachao.timeblock.data

import com.wavachao.timeblock.data.local.BlockRange
import com.wavachao.timeblock.data.model.BlockCategory
import com.wavachao.timeblock.data.model.TimeBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class StatsTest {

    private val monday = LocalDate.of(2025, 10, 20) // Monday
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun block(
        date: LocalDate,
        startHour: Int,
        startMinute: Int = 0,
        minutes: Int,
        category: BlockCategory = BlockCategory.WORK,
        done: Boolean = false,
    ): TimeBlock {
        val start = date.atTime(LocalTime.of(startHour, startMinute))
        return TimeBlock(
            id = date.toEpochDay() * 100 + startHour,
            title = "$date $startHour:$startMinute",
            start = start,
            end = start.plusMinutes(minutes.toLong()),
            category = category,
            done = done,
        )
    }

    @Test
    fun `day stats sum planned and completed minutes`() {
        val blocks = listOf(
            block(monday, 9, minutes = 90, done = true),
            block(monday, 11, minutes = 60, category = BlockCategory.STUDY),
            block(monday, 14, minutes = 90, category = BlockCategory.SPORT, done = true),
        )

        val stats = DayStats.of(monday, blocks, monday.atTime(13, 0))

        assertEquals(3, stats.blockCount)
        assertEquals(2, stats.doneCount)
        assertEquals(180, stats.doneMinutes)
        assertEquals(240, stats.plannedMinutes)
        assertEquals(66, stats.completionPercent)
    }

    @Test
    fun `next skips finished and already ended blocks`() {
        val blocks = listOf(
            block(monday, 8, minutes = 60, done = true),   // finished
            block(monday, 9, minutes = 60),                // already over at 12:05
            block(monday, 14, minutes = 60),               // the only candidate
        )

        val stats = DayStats.of(monday, blocks, monday.atTime(12, 5))

        assertEquals(monday.atTime(14, 0), stats.next?.start)
        assertFalse(stats.isLive)
    }

    @Test
    fun `next prefers the block that owns this moment`() {
        val blocks = listOf(
            block(monday, 11, minutes = 60),  // 11:00-12:00, running at 11:30
            block(monday, 14, minutes = 60),
        )

        val stats = DayStats.of(monday, blocks, monday.atTime(11, 30))

        assertEquals(monday.atTime(11, 0), stats.next?.start)
        assertTrue("an ongoing block is what the summary card should highlight", stats.isLive)
    }

    @Test
    fun `an empty day reports zero instead of dividing by zero`() {
        val stats = DayStats.of(monday, emptyList(), monday.atTime(9, 0))
        assertEquals(0f, stats.completion, 0.001f)
        assertEquals(0, stats.completionPercent)
        assertEquals(null, stats.next)
        assertFalse(stats.isLive)
    }

    @Test
    fun `week insight groups minutes per day and by category`() {
        val friday = monday.plusDays(4)
        val blocks = listOf(
            block(monday, 9, minutes = 120, category = BlockCategory.WORK),
            block(monday, 14, minutes = 60, category = BlockCategory.STUDY),
            block(friday, 18, minutes = 30, category = BlockCategory.SPORT),
        )

        val insight = WeekInsight.of(
            weekStart = monday,
            weekEnd = monday.plusDays(6),
            blocks = blocks,
            previousTotalMinutes = 100,
            streakDays = 4,
        )

        assertEquals(210, insight.totalMinutes)
        assertEquals(110, insight.deltaMinutes)
        assertEquals(180, insight.minutesOn(monday))
        assertEquals(0, insight.minutesOn(monday.plusDays(2)))
        assertEquals(30, insight.minutesOn(friday))
        assertEquals(monday, insight.peakDay)
        assertEquals(listOf(BlockCategory.WORK, BlockCategory.STUDY, BlockCategory.SPORT), insight.slices.map { it.category })
        assertEquals(120, insight.slices.first().minutes)
        assertEquals(120f / 210f, insight.slices.first().share, 0.001f)
        assertEquals(7, insight.perDay.size)
    }

    @Test
    fun `day loads fold rows into per day intensity`() {
        val rows = listOf(
            range(monday, 9, 90, BlockCategory.WORK),
            range(monday, 11, 30, BlockCategory.STUDY),
            range(monday.plusDays(1), 8, 45, BlockCategory.LIFE),
        )

        val loads = buildDayLoads(
            ranges = rows,
            days = listOf(monday, monday.plusDays(1), monday.plusDays(2)),
            zone = zone,
        )

        assertEquals(3, loads.size)
        assertEquals(120, loads[0].minutes)
        assertEquals(listOf(BlockCategory.WORK, BlockCategory.STUDY), loads[0].categories)
        assertEquals(45, loads[1].minutes)
        assertTrue(loads[2].isEmpty)
        assertFalse(loads[0].isEmpty)
        assertEquals(1f, loads[0].intensity(maxMinutes = 120), 0.001f)
    }

    @Test
    fun `streak counts back from today and falls back to yesterday`() {
        val days = setOf(
            monday,
            monday.plusDays(1),
            monday.plusDays(2),
            monday.plusDays(4),
        )

        // today (Thursday) has no plan yet, so the streak still counts back from yesterday
        assertEquals(3, planningStreak(days, today = monday.plusDays(3)))
        // today itself is planned
        assertEquals(1, planningStreak(days, today = monday.plusDays(4)))
        // the most recent plan is five days stale, so nothing is consecutive any more
        assertEquals(0, planningStreak(days, today = monday.plusDays(9)))
        assertEquals(0, planningStreak(emptySet(), today = monday))
    }

    private fun range(date: LocalDate, hour: Int, minutes: Int, category: BlockCategory): BlockRange {
        val start = date.atTime(LocalTime.of(hour, 0))
        return BlockRange(
            id = hour.toLong(),
            startEpochMillis = start.atZone(zone).toInstant().toEpochMilli(),
            endEpochMillis = start.plusMinutes(minutes.toLong()).atZone(zone).toInstant().toEpochMilli(),
            category = category.name,
            done = false,
        )
    }
}
