package com.wavachao.timeblock.ui.util

import androidx.compose.runtime.saveable.listSaver
import com.wavachao.timeblock.data.model.BlockCategory
import com.wavachao.timeblock.data.model.RecurrenceRule
import com.wavachao.timeblock.data.model.TimeBlockDraft
import java.time.LocalDate
import java.time.LocalTime

/** Primitive values survive activity recreation and process state restoration. */
val DraftSaver = listSaver<TimeBlockDraft, Any>(
    save = { listOf(it.id, it.title, it.date.toString(), it.startTime.toString(),
        it.endTime.toString(), it.category.name, it.reminderMinutes, it.recurrence.name,
        it.notes.orEmpty(), it.done, it.createdAt, it.allDay, it.endDate?.toString().orEmpty()) },
    restore = { TimeBlockDraft(
        id = it[0] as Long, title = it[1] as String, date = LocalDate.parse(it[2] as String),
        startTime = LocalTime.parse(it[3] as String), endTime = LocalTime.parse(it[4] as String),
        category = BlockCategory.valueOf(it[5] as String), reminderMinutes = it[6] as Int,
        recurrence = RecurrenceRule.valueOf(it[7] as String), notes = (it[8] as String).ifBlank { null },
        done = it[9] as Boolean, createdAt = it[10] as Long,
        allDay = it.getOrNull(11) as? Boolean ?: false,
        endDate = (it.getOrNull(12) as? String)?.takeIf { value -> value.isNotEmpty() }?.let(LocalDate::parse),
    ) },
)
