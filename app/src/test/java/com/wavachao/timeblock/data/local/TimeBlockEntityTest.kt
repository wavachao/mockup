package com.wavachao.timeblock.data.local

import com.wavachao.timeblock.data.model.BlockCategory
import com.wavachao.timeblock.data.model.RecurrenceRule
import com.wavachao.timeblock.data.model.SubTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class TimeBlockEntityTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `subtask codec round trips done state`() {
        val items = listOf(
            SubTask("完成接口层重构", done = true),
            SubTask("补充单元测试", done = false),
        )

        val encoded = SubTaskCodec.encode(items)
        assertEquals("x 完成接口层重构\n补充单元测试", encoded)
        assertEquals(items, SubTaskCodec.decode(encoded))
    }

    @Test
    fun `subtask codec tolerates blanks and stray whitespace`() {
        assertTrue(SubTaskCodec.decode(null).isEmpty())
        assertTrue(SubTaskCodec.decode("").isEmpty())
        assertTrue(SubTaskCodec.decode("   ").isEmpty())
        assertEquals(listOf(SubTask("a", false)), SubTaskCodec.decode("\n  a  \n\n"))
    }

    @Test
    fun `block survives a storage round trip`() {
        val original = com.wavachao.timeblock.data.model.TimeBlock(
            id = 7,
            title = "重构：数据层",
            start = LocalDateTime.of(2025, 10, 24, 14, 0),
            end = LocalDateTime.of(2025, 10, 24, 15, 30),
            category = BlockCategory.STUDY,
            done = true,
            notes = "拆出 repository",
            reminderMinutes = 30,
            recurrence = RecurrenceRule.WEEKDAYS,
        )

        val restored = original.toEntity().toModel()

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.start, restored.start)
        assertEquals(original.end, restored.end)
        assertEquals(original.category, restored.category)
        assertEquals(original.done, restored.done)
        assertEquals(original.notes, restored.notes)
        assertEquals(original.reminderMinutes, restored.reminderMinutes)
        assertEquals(original.recurrence, restored.recurrence)
        assertEquals(90, restored.durationMinutes)
    }

    @Test
    fun `local text columns win over the epoch fallback`() {
        // A block planned in Shanghai must not shift when the device zone changes:
        // the stored wall clock is the source of truth.
        val entity = TimeBlockEntity(
            id = 1,
            title = "深度工作",
            startEpochMillis = LocalDateTime.of(2025, 10, 24, 1, 0)
                .atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(),
            endEpochMillis = LocalDateTime.of(2025, 10, 24, 2, 30)
                .atZone(ZoneId.of("UTC")).toInstant().toEpochMilli(),
            startLocal = LocalDateTime.of(2025, 10, 24, 9, 0).toStorage(),
            endLocal = LocalDateTime.of(2025, 10, 24, 10, 30).toStorage(),
            category = BlockCategory.WORK.name,
        )

        val model = entity.toModel()
        assertEquals(LocalTime.of(9, 0), model.start.toLocalTime())
        assertEquals(LocalTime.of(10, 30), model.end.toLocalTime())
    }

    @Test
    fun `malformed local columns fall back to the epoch value`() {
        val start = LocalDateTime.of(2025, 10, 24, 9, 0)
        val entity = TimeBlockEntity(
            id = 2,
            title = "兜底",
            startEpochMillis = start.atZone(zone).toInstant().toEpochMilli(),
            endEpochMillis = start.plusHours(1).atZone(zone).toInstant().toEpochMilli(),
            startLocal = "not-a-date",
            endLocal = "",
            category = "unknown-category",
        )

        val model = entity.toModel()
        assertEquals(start, model.start)
        assertEquals(start.plusHours(1), model.end)
        assertEquals(BlockCategory.WORK, model.category)
        assertNull(model.notes)
    }

    @Test
    fun `day boundaries cover the whole local day`() {
        val date = LocalDate.of(2025, 10, 24)
        val start = date.startOfDayMillis(zone)
        val end = date.endOfDayMillisExclusive(zone)

        assertEquals(24 * 60 * 60 * 1000L, end - start)
        assertTrue(start < date.atTime(9, 0).toEpochMillis(zone))
        assertTrue(date.atTime(23, 59).toEpochMillis(zone) < end)
    }

    @Test
    fun `storage format is minute precision`() {
        val value = LocalDateTime.of(2025, 10, 24, 9, 5)
        assertEquals("2025-10-24T09:05", value.toStorage())
        assertEquals(value, "2025-10-24T09:05".toLocalDateTimeOrNull())
        assertNull("garbage".toLocalDateTimeOrNull())
    }
}
