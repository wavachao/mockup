package com.wavachao.timeblock.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wavachao.timeblock.data.model.BlockCategory
import com.wavachao.timeblock.data.model.RecurrenceRule
import com.wavachao.timeblock.data.model.SubTask
import com.wavachao.timeblock.data.model.TimeBlock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Room row for a planned time range.
 *
 * `startEpochMillis`/`endEpochMillis` drive the range queries (day list, month load),
 * while the `startLocal`/`endLocal` columns keep the exact wall-clock time the block
 * was planned for, so a timezone change cannot silently shift someone's schedule.
 */
@Entity(tableName = "time_blocks")
data class TimeBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val startLocal: String,
    val endLocal: String,
    val category: String,
    val done: Boolean = false,
    val notes: String? = null,
    val reminderMinutes: Int = 10,
    val recurrence: String = RecurrenceRule.NONE.name,
    @ColumnInfo(defaultValue = "") val subtasks: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** `yyyy-MM-ddTHH:mm` — minute precision is enough for a planning app. */
private val StorageFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

/**
 * Plain-text sub-task encoding: one item per line, an `x ` prefix marks it complete.
 * A separate table would be overkill — sub-tasks are only ever read with their parent.
 */
object SubTaskCodec {
    fun encode(items: List<SubTask>): String = items.joinToString("\n") { item ->
        if (item.done) "x ${item.title}" else item.title
    }

    fun decode(raw: String?): List<SubTask> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split('\n').mapNotNull { line ->
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> null
                trimmed.startsWith("x ") -> SubTask(trimmed.removePrefix("x ").trim(), true)
                else -> SubTask(trimmed, false)
            }
        }
    }
}

fun LocalDateTime.toStorage(): String = format(StorageFormatter)

fun String.toLocalDateTimeOrNull(): LocalDateTime? =
    runCatching { LocalDateTime.parse(this, StorageFormatter) }.getOrNull()

fun LocalDate.startOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun LocalDate.endOfDayMillisExclusive(zone: ZoneId = ZoneId.systemDefault()): Long =
    plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

fun LocalDateTime.toEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atZone(zone).toInstant().toEpochMilli()

fun TimeBlockEntity.toModel(fallbackCategory: BlockCategory = BlockCategory.WORK): TimeBlock {
    val zone = ZoneId.systemDefault()
    val start = startLocal.toLocalDateTimeOrNull()
        ?: Instant.ofEpochMilli(startEpochMillis).atZone(zone).toLocalDateTime()
    val end = endLocal.toLocalDateTimeOrNull()
        ?: Instant.ofEpochMilli(endEpochMillis).atZone(zone).toLocalDateTime()
    val resolvedCategory = when {
        category.isBlank() -> fallbackCategory
        else -> BlockCategory.fromStorage(category)
    }
    return TimeBlock(
        id = id,
        title = title,
        start = start,
        end = end,
        category = resolvedCategory,
        done = done,
        notes = notes,
        reminderMinutes = reminderMinutes,
        recurrence = RecurrenceRule.fromStorage(recurrence),
        createdAt = createdAt,
    )
}

fun TimeBlock.toEntity(): TimeBlockEntity = TimeBlockEntity(
    id = id,
    title = title,
    startEpochMillis = start.toEpochMillis(),
    endEpochMillis = end.toEpochMillis(),
    startLocal = start.toStorage(),
    endLocal = end.toStorage(),
    category = category.name,
    done = done,
    notes = notes,
    reminderMinutes = reminderMinutes,
    recurrence = recurrence.name,
    createdAt = createdAt,
)
