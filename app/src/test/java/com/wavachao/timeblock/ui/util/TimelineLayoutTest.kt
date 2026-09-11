package com.wavachao.timeblock.ui.util

import com.wavachao.timeblock.data.model.BlockCategory
import com.wavachao.timeblock.data.model.TimeBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TimelineLayoutTest {

    private val day: LocalDate = LocalDate.of(2025, 10, 24)

    private fun block(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        title: String = "block",
        category: BlockCategory = BlockCategory.WORK,
        done: Boolean = false,
    ): TimeBlock = TimeBlock(
        id = (startHour * 60 + startMinute).toLong(),
        title = title,
        start = day.atTime(LocalTime.of(startHour, startMinute)),
        end = day.atTime(LocalTime.of(endHour, endMinute)),
        category = category,
        done = done,
    )

    @Test
    fun `an empty day keeps the default 08-20 window`() {
        val layout = buildTimeline(emptyList())
        assertEquals(8, layout.startHour)
        assertEquals(20, layout.endHour)
        assertEquals(12 * 60, layout.totalMinutes)
        assertTrue(layout.blocks.isEmpty())
        assertTrue(layout.gaps.isEmpty())
    }

    @Test
    fun `block offsets are minutes from the first hour`() {
        // The mockup's own maths: 09:00 sits at 46px when the day starts at 08:00.
        val layout = buildTimeline(listOf(block(9, 0, 10, 30)))

        val laid = layout.blocks.single()
        assertEquals(60, laid.offsetMinutes)
        assertEquals(90, laid.durationMinutes)
        assertEquals(150, laid.endOffsetMinutes)
    }

    @Test
    fun `the window stretches to contain early and late blocks`() {
        val layout = buildTimeline(listOf(block(6, 30, 7, 30), block(21, 0, 22, 30)))
        assertEquals(6, layout.startHour)
        assertEquals(23, layout.endHour)
    }

    @Test
    fun `gaps are reported only when long enough to be useful`() {
        val layout = buildTimeline(
            listOf(
                block(9, 0, 10, 30),
                block(11, 0, 12, 0),
                block(12, 10, 13, 0),
            ),
            minGapMinutes = 25,
        )

        assertEquals(1, layout.gaps.size)
        val gap = layout.gaps.single()
        assertEquals(150, gap.startMinutes) // 10:30 relative to 08:00
        assertEquals(180, gap.endMinutes)   // 11:00 relative to 08:00
        assertEquals(30, gap.minutes)
    }

    @Test
    fun `overlapping blocks get their own lane`() {
        val layout = buildTimeline(
            listOf(
                block(9, 0, 10, 30, title = "a"),
                block(9, 30, 10, 0, title = "b"),
                block(11, 0, 12, 0, title = "c"),
            ),
        )

        val byTitle = layout.blocks.associateBy { it.block.title }
        assertEquals(2, byTitle.getValue("a").laneCount)
        assertEquals(0, byTitle.getValue("a").lane)
        assertEquals(1, byTitle.getValue("b").lane)
        assertEquals(1, byTitle.getValue("c").laneCount)
    }

    @Test
    fun `the now fraction maps a minute to 0-1`() {
        val layout = buildTimeline(emptyList())

        assertEquals(0f, layout.fractionFor(8 * 60), 0.001f)
        assertEquals(0.5f, layout.fractionFor(14 * 60), 0.001f)
        assertEquals(1f, layout.fractionFor(20 * 60), 0.001f)
        assertEquals(0f, layout.fractionFor(3 * 60), 0.001f)
        assertEquals(1f, layout.fractionFor(23 * 60), 0.001f)

        assertTrue(layout.containsMinute(9 * 60))
        assertFalse(layout.containsMinute(21 * 60))
    }

    @Test
    fun `blocks stay ordered by start time`() {
        val layout = buildTimeline(
            listOf(
                block(16, 0, 17, 0, title = "gym"),
                block(9, 0, 10, 0, title = "deep work"),
                block(12, 30, 13, 30, title = "lunch"),
            ),
        )

        assertEquals(listOf("deep work", "lunch", "gym"), layout.blocks.map { it.block.title })
    }

    @Test
    fun `a zero length block still occupies a visible minute`() {
        val layout = buildTimeline(listOf(block(10, 0, 10, 0)))
        assertEquals(1, layout.blocks.single().durationMinutes)
    }
}

class TimeFormatTest {

    private val day: LocalDate = LocalDate.of(2025, 10, 24) // Friday

    @Test
    fun `compact durations drop empty units`() {
        assertEquals("45m", TimeFormat.compactDuration(45))
        assertEquals("1h", TimeFormat.compactDuration(60))
        assertEquals("1h30m", TimeFormat.compactDuration(90))
        assertEquals("0m", TimeFormat.compactDuration(0))
        assertEquals("0m", TimeFormat.compactDuration(-5))
    }

    @Test
    fun `long durations read as chinese`() {
        assertEquals("45 分钟", TimeFormat.longDuration(45))
        assertEquals("1 小时", TimeFormat.longDuration(60))
        assertEquals("6 小时 20 分", TimeFormat.longDuration(380))
        assertEquals("—", TimeFormat.longDurationOrDash(0))
    }

    @Test
    fun `ranges use an en dash like the design`() {
        val block = TimeBlock(
            title = "深度工作",
            start = day.atTime(9, 0),
            end = day.atTime(10, 30),
        )
        assertEquals("09:00 – 10:30", TimeFormat.range(block))
    }

    @Test
    fun `dates and weekdays are localised`() {
        assertEquals("10月24日 · 星期五", TimeFormat.dateWithWeekday(day))
        assertEquals("10月", TimeFormat.monthLabel(day))
        assertEquals("周五", TimeFormat.shortWeekday(day))
        assertEquals("24", TimeFormat.dayNumber(day))
    }

    @Test
    fun `relative day labels collapse the next three days`() {
        assertEquals("今天", TimeFormat.relativeDay(day, today = day))
        assertEquals("明天", TimeFormat.relativeDay(day.plusDays(1), today = day))
        assertEquals("昨天", TimeFormat.relativeDay(day.minusDays(1), today = day))
        assertEquals("周一", TimeFormat.relativeDay(day.plusDays(3), today = day))
    }

    @Test
    fun `metric parts split hours and padded minutes`() {
        assertEquals("28" to "40", TimeFormat.metricParts(28 * 60 + 40))
        assertEquals("3" to "05", TimeFormat.metricParts(185))
        assertEquals("0" to "00", TimeFormat.metricParts(0))
    }

    @Test
    fun `countdown copy switches at the hour boundary`() {
        assertEquals("即将开始", TimeFormat.untilLabel(0))
        assertEquals("还有 20 分钟", TimeFormat.untilLabel(20))
        assertEquals("还有 1 小时 20 分", TimeFormat.untilLabel(80))
        assertEquals("剩余 34 分钟", TimeFormat.remainingLabel(34))
        assertEquals("已超时 5 分钟", TimeFormat.remainingLabel(-5))
    }

    @Test
    fun `status follows the wall clock and the done flag`() {
        val block = TimeBlock(
            title = "重构",
            start = day.atTime(14, 0),
            end = day.atTime(15, 30),
        )

        assertEquals("计划中", TimeFormat.statusLabel(block, day.atTime(13, 0)))
        assertEquals("进行中", TimeFormat.statusLabel(block, day.atTime(14, 30)))
        assertEquals("未完成", TimeFormat.statusLabel(block, day.atTime(16, 0)))
        assertEquals("已完成", TimeFormat.statusLabel(block.copy(done = true), day.atTime(13, 0)))
    }

    @Test
    fun `minutes between clamps negatives to zero`() {
        assertEquals(90, TimeFormat.minutesBetween(day.atTime(9, 0), day.atTime(10, 30)))
        assertEquals(0, TimeFormat.minutesBetween(day.atTime(10, 0), day.atTime(9, 0)))
    }

    @Test
    fun `clock keeps minute precision`() {
        assertEquals("13:52", TimeFormat.clock(LocalDateTime.of(2025, 10, 24, 13, 52, 41)))
    }
}
