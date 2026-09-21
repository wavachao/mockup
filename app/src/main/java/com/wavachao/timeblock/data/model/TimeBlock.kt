package com.wavachao.timeblock.data.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * One planned time range. `end` is always after `start`; the duration is derived, never
 * stored, so a block can be dragged or resized without bookkeeping.
 */
data class TimeBlock(
    val id: Long = 0L,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val category: BlockCategory = BlockCategory.WORK,
    val done: Boolean = false,
    val notes: String? = null,
    val reminderMinutes: Int = 10,
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val createdAt: Long = System.currentTimeMillis(),
    val allDay: Boolean = false,
) {
    val duration: Duration get() = Duration.between(start, end)
    val durationMinutes: Int get() = duration.toMinutes().toInt()
    val date: LocalDate get() = start.toLocalDate()

    fun overlaps(other: TimeBlock): Boolean = start < other.end && other.start < end

    /** Progress in 0..1 for the detail screen's progress bar. */
    fun progressAt(now: LocalDateTime): Float {
        val total = duration.toMinutes().toDouble()
        if (total <= 0.0) return if (done) 1f else 0f
        if (done) return 1f
        val elapsed = Duration.between(start, now).toMinutes().toDouble()
        return (elapsed / total).coerceIn(0.0, 1.0).toFloat()
    }

    fun remainingAt(now: LocalDateTime): Duration =
        if (now.isBefore(start)) duration else Duration.between(now, end).coerceAtLeastZero()

    companion object {
        fun at(date: LocalDate, startTime: LocalTime, endTime: LocalTime): Pair<LocalDateTime, LocalDateTime> =
            date.atTime(startTime) to date.atTime(endTime)
    }
}

private fun Duration.coerceAtLeastZero(): Duration =
    if (isNegative) Duration.ZERO else this
