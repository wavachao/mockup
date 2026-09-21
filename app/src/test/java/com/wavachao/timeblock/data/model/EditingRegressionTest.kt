package com.wavachao.timeblock.data.model

import com.wavachao.timeblock.ui.toBlock
import com.wavachao.timeblock.ui.util.buildTimeline
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EditingRegressionTest {
    private val day = LocalDate.of(2026, 9, 19)
    private fun block(start: Int, end: Int) = TimeBlock(
        title = "计划", start = day.atTime(start, 0), end = day.atTime(end, 0),
    )

    @Test fun `editing preserves completion and creation metadata`() {
        val original = block(9, 10).copy(id = 42, done = true, createdAt = 1234)
        val edited = TimeBlockDraft.from(original).copy(title = " 修改 ").toBlock()
        assertEquals(original.copy(title = "修改"), edited)
    }

    @Test fun `moving start preserves duration across midnight`() {
        val draft = TimeBlockDraft.startingAt(day.atTime(9, 0), 90)
            .withStartTime(LocalTime.of(23, 30))
        assertEquals(90, draft.durationMinutes)
        assertEquals(day.plusDays(1).atTime(1, 0), draft.end)
    }

    @Test fun `rounding the last quarter hour advances the date`() {
        val draft = TimeBlockDraft.startingAt(day.atTime(23, 59))
        assertEquals(day.plusDays(1), draft.date)
        assertEquals(LocalTime.MIDNIGHT, draft.startTime)
    }

    @Test fun `nested blocks do not create fake free time`() {
        val timeline = buildTimeline(listOf(block(9, 14), block(10, 11), block(12, 13)))
        assertTrue(timeline.gaps.isEmpty())
    }

    @Test fun `fitted timeline starts at the first scheduled hour`() {
        val timeline = buildTimeline(listOf(block(14, 15)), fitToBlocks = true)
        assertEquals(14, timeline.startHour)
        assertEquals(15, timeline.endHour)
        assertEquals(0, timeline.blocks.single().offsetMinutes)
    }

    @Test fun `overnight timeline extends to midnight and clips visible duration`() {
        val overnight = block(23, 1).copy(end = day.plusDays(1).atTime(1, 0))
        val timeline = buildTimeline(listOf(overnight))
        assertEquals(24, timeline.endHour)
        assertEquals(60, timeline.blocks.single().durationMinutes)
        assertEquals(120, timeline.blocks.single().block.durationMinutes)
    }
}
