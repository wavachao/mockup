package com.wavachao.timeblock.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TimeBlockTest {

    private val day = LocalDate.of(2025, 10, 24)

    private fun block(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) = TimeBlock(
        id = 1,
        title = "深度工作",
        start = day.atTime(LocalTime.of(startHour, startMinute)),
        end = day.atTime(LocalTime.of(endHour, endMinute)),
    )

    @Test
    fun `duration is derived from the range`() {
        assertEquals(90, block(9, 0, 10, 30).durationMinutes)
        assertEquals(60, block(11, 0, 12, 0).durationMinutes)
        assertEquals(45, block(16, 15, 17, 0).durationMinutes)
    }

    @Test
    fun `date comes from the start instant`() {
        assertEquals(day, block(9, 0, 10, 0).date)
    }

    @Test
    fun `overlap follows half open intervals`() {
        val a = block(9, 0, 10, 30)
        val touching = block(10, 30, 11, 0)
        val inside = block(10, 0, 10, 15)

        assertFalse("end-to-start is not an overlap", a.overlaps(touching))
        assertTrue(inside.overlaps(a))
        assertTrue(a.overlaps(inside))
    }

    @Test
    fun `progress tracks the wall clock and clamps`() {
        val target = block(14, 0, 15, 30)

        assertEquals(0f, target.progressAt(day.atTime(13, 0)), 0.001f)
        assertEquals(0.5f, target.progressAt(day.atTime(14, 45)), 0.001f)
        assertEquals(1f, target.progressAt(day.atTime(18, 0)), 0.001f)
    }

    @Test
    fun `a finished block reports full progress regardless of the clock`() {
        val done = block(9, 0, 10, 0).copy(done = true)
        assertEquals(1f, done.progressAt(day.atTime(8, 0)), 0.001f)
    }

    @Test
    fun `remaining time never goes negative`() {
        val target = block(14, 0, 15, 30)
        assertEquals(45L, target.remainingAt(day.atTime(14, 45)).toMinutes())
        assertEquals(0L, target.remainingAt(day.atTime(20, 0)).toMinutes())
        assertEquals(90L, target.remainingAt(day.atTime(9, 0)).toMinutes())
    }

    @Test
    fun `draft rolls the end time into the next day`() {
        val draft = TimeBlockDraft(
            date = day,
            startTime = LocalTime.of(23, 30),
            endTime = LocalTime.of(0, 30),
        )
        assertEquals(day.plusDays(1), draft.end.toLocalDate())
        assertEquals(60, draft.durationMinutes)
    }

    @Test
    fun `draft duration chips move the end time only`() {
        val draft = TimeBlockDraft(date = day, startTime = LocalTime.of(14, 30), endTime = LocalTime.of(16, 0))
        assertEquals(90, draft.durationMinutes)

        val twoHours = draft.withDuration(120)
        assertEquals(LocalTime.of(14, 30), twoHours.startTime)
        assertEquals(LocalTime.of(16, 30), twoHours.endTime)
        assertEquals(120, twoHours.durationMinutes)
    }

    @Test
    fun `draft round trips through an existing block`() {
        val original = block(14, 0, 15, 30).copy(category = BlockCategory.STUDY, reminderMinutes = 30)
        val draft = TimeBlockDraft.from(original)

        assertEquals(original.id, draft.id)
        assertEquals(original.title, draft.title)
        assertEquals(original.start, draft.start)
        assertEquals(original.end, draft.end)
        assertEquals(BlockCategory.STUDY, draft.category)
        assertEquals(30, draft.reminderMinutes)
    }

    @Test
    fun `startingAt rounds up to the next quarter hour`() {
        val draft = TimeBlockDraft.startingAt(day.atTime(14, 7), minutes = 60)
        assertEquals(LocalTime.of(14, 15), draft.startTime)
        assertEquals(LocalTime.of(15, 15), draft.endTime)

        val exact = TimeBlockDraft.startingAt(day.atTime(14, 15), minutes = 30)
        assertEquals(LocalTime.of(14, 15), exact.startTime)
        assertEquals(LocalTime.of(14, 45), exact.endTime)
    }

    @Test
    fun `recurrence rules match the right weekdays`() {
        val friday = LocalDate.of(2025, 10, 24).dayOfWeek
        val saturday = LocalDate.of(2025, 10, 25).dayOfWeek

        assertFalse(RecurrenceRule.NONE.matches(friday))
        assertTrue(RecurrenceRule.DAILY.matches(saturday))
        assertTrue(RecurrenceRule.WEEKDAYS.matches(friday))
        assertFalse(RecurrenceRule.WEEKDAYS.matches(saturday))
        assertTrue(RecurrenceRule.WEEKLY.matches(friday))
    }

    @Test
    fun `reminder labels read naturally in chinese`() {
        assertEquals("开始时", ReminderLead.label(0))
        assertEquals("开始前 10 分钟", ReminderLead.label(10))
        assertEquals("开始前 1 小时", ReminderLead.label(60))
        assertEquals("开始前 30 分钟", ReminderLead.fromMinutes(30).displayName)
        assertEquals(ReminderLead.NONE, ReminderLead.fromMinutes(7))
    }
}
