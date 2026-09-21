package com.wavachao.timeblock.data.model
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import com.wavachao.timeblock.ui.toBlock
class ScheduleDraftTest {
    private val day = LocalDate.of(2027, 1, 3)
    @Test fun dateOnlyRoundTripIncludesFinalDay() {
        val draft = TimeBlockDraft(title="旅行",date=day,endDate=day.plusDays(2),allDay=true)
        val block = draft.toBlock()
        assertEquals(day.plusDays(3).atStartOfDay(),block.end)
        assertTrue(block.allDay)
        assertEquals(draft.endDate,TimeBlockDraft.from(block).endDate)
    }
    @Test fun explicitEndBeforeStartIsRejected() {
        val draft = TimeBlockDraft(title="会议",date=day,endDate=day,startTime=LocalTime.of(12,0),endTime=LocalTime.of(11,0))
        assertNotNull(draft.validationError)
    }
    @Test fun multiDayTimedEventKeepsDatesOnEdit() {
        val draft = TimeBlockDraft(title="出差",date=day,endDate=day.plusDays(3))
        val restored=TimeBlockDraft.from(draft.toBlock())
        assertEquals(draft.start,restored.start)
        assertEquals(draft.end,restored.end)
        assertNull(restored.validationError)
    }
    @Test fun dateOnlyEndBeforeStartIsRejected() {
        assertNotNull(TimeBlockDraft(title="事项",date=day,endDate=day.minusDays(1),allDay=true).validationError)
    }
}
