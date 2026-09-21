package com.wavachao.timeblock.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * What the editor screens collect before anything is persisted. Keeping it separate from
 * [TimeBlock] means "an unsaved block with an empty title" is representable without
 * weakening the domain model.
 */
data class TimeBlockDraft(
    val id: Long = 0L,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val category: BlockCategory = BlockCategory.WORK,
    val reminderMinutes: Int = 10,
    val recurrence: RecurrenceRule = RecurrenceRule.NONE,
    val notes: String? = null,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val allDay: Boolean = false,
    val endDate: LocalDate? = null,
) {
    /** End after start, spilling into the next day when the user picks e.g. 23:30-00:30. */
    val start: LocalDateTime get() = if (allDay) date.atStartOfDay() else date.atTime(startTime)

    val end: LocalDateTime
        get() {
            if (allDay) return (endDate ?: date).plusDays(1).atStartOfDay()
            if (endDate != null) return endDate.atTime(endTime)
            val sameDay = date.atTime(endTime)
            return if (sameDay.isAfter(start)) sameDay else sameDay.plusDays(1)
        }

    val durationMinutes: Int
        get() = java.time.Duration.between(start, end).toMinutes().toInt().coerceAtLeast(0)

    fun withDuration(minutes: Int): TimeBlockDraft {
        val safe = minutes.coerceIn(5, 24 * 60)
        val target = date.atTime(startTime).plusMinutes(safe.toLong())
        return copy(endTime = target.toLocalTime(), endDate = target.toLocalDate())
    }

    fun withStartTime(time: LocalTime): TimeBlockDraft = withStart(date, time)

    fun withStart(day: LocalDate, time: LocalTime = startTime): TimeBlockDraft {
        val target = day.atTime(time).plusMinutes(durationMinutes.toLong())
        return copy(date = day, startTime = time, endTime = target.toLocalTime(), endDate = target.toLocalDate())
    }

    val validationError: String? get() = when {
        title.isBlank() -> "请填写日程名称"
        !end.isAfter(start) -> "结束时间必须晚于开始时间"
        else -> null
    }

    companion object {
        fun from(block: TimeBlock): TimeBlockDraft = TimeBlockDraft(
            id = block.id,
            title = block.title,
            date = block.start.toLocalDate(),
            startTime = block.start.toLocalTime(),
            endTime = block.end.toLocalTime(),
            category = block.category,
            reminderMinutes = block.reminderMinutes,
            recurrence = block.recurrence,
            notes = block.notes,
            done = block.done,
            createdAt = block.createdAt,
            allDay = block.allDay,
            endDate = if (block.allDay) block.end.toLocalDate().minusDays(1) else block.end.toLocalDate(),
        )

        /** A sensible block starting at the next quarter hour, like the quick-add sheet. */
        fun startingAt(moment: LocalDateTime, minutes: Int = 60): TimeBlockDraft {
            val rounded = moment.withSecond(0).withNano(0)
            val add = (15 - rounded.minute % 15) % 15
            val start = rounded.plusMinutes(add.toLong())
            return TimeBlockDraft(
                date = start.toLocalDate(),
                startTime = start.toLocalTime(),
                endTime = start.plusMinutes(minutes.toLong()).toLocalTime(),
            )
        }
    }
}
