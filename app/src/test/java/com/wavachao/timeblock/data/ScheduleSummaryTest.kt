package com.wavachao.timeblock.data

import com.wavachao.timeblock.data.model.TimeBlock
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ScheduleSummaryTest {
    private val day=LocalDate.of(2026,9,21)
    private fun event(start:Int,end:Int,done:Boolean=false)=TimeBlock(title="安排",start=day.atTime(start,0),end=day.atTime(end,0),done=done)
    @Test fun emptyPeriodHasZeroRate() {
        val stats=summarizeSchedules(emptyList(),day,day.plusDays(6))
        assertEquals(0,stats.total)
        assertEquals(0f,stats.completionRate)
        assertEquals(List(7){0},stats.dailyCounts)
    }
    @Test fun allDayCountsButDoesNotBecomeTwentyFourHours() {
        val allDay=TimeBlock(title="事项",start=day.atStartOfDay(),end=day.plusDays(1).atStartOfDay(),allDay=true,done=true)
        val stats=summarizeSchedules(listOf(allDay,event(9,10)),day,day)
        assertEquals(2,stats.total)
        assertEquals(1,stats.allDay)
        assertEquals(60L,stats.plannedMinutes)
        assertEquals(0L,stats.completedMinutes)
        assertEquals(.5f,stats.completionRate)
    }
    @Test fun crossBoundaryOnlyCountsMinutesInsidePeriod() {
        val overnight=TimeBlock(title="夜间",start=day.minusDays(1).atTime(23,0),end=day.atTime(1,0),done=true)
        val stats=summarizeSchedules(listOf(overnight),day,day)
        assertEquals(60L,stats.plannedMinutes)
        assertEquals(60L,stats.completedMinutes)
        assertEquals(listOf(1),stats.dailyCounts)
    }
    @Test fun exactMidnightBoundaryDoesNotCountNextDay() {
        val previous=TimeBlock(title="昨天",start=day.minusDays(1).atTime(23,0),end=day.atStartOfDay())
        assertEquals(0,summarizeSchedules(listOf(previous),day,day).total)
    }
    @Test fun multipleDaysCountUniqueRecordsAndDailyPresence() {
        val spanning=TimeBlock(title="旅行",start=day.atStartOfDay(),end=day.plusDays(3).atStartOfDay(),allDay=true)
        val stats=summarizeSchedules(listOf(spanning),day,day.plusDays(3))
        assertEquals(1,stats.total)
        assertEquals(listOf(1,1,1,0),stats.dailyCounts)
    }
    @Test fun overlappingSchedulesAreSummedAsDocumented() {
        val stats=summarizeSchedules(listOf(event(9,11),event(10,12,true)),day,day)
        assertEquals(240L,stats.plannedMinutes)
        assertEquals(120L,stats.completedMinutes)
        assertEquals(2,stats.categories.values.sum())
    }
}
