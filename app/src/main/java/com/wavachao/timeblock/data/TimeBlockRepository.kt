package com.wavachao.timeblock.data

import com.wavachao.timeblock.data.model.TimeBlock
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Domain-facing data access. Screens and the ViewModel never touch Room directly, which
 * keeps the storage choice swappable and the stats logic unit-testable on the JVM.
 */
interface TimeBlockRepository {

    /** Blocks whose start falls on [date], ordered by start time. */
    fun observeDay(date: LocalDate): Flow<List<TimeBlock>>

    fun observeBlock(id: Long): Flow<TimeBlock?>

    suspend fun blockById(id: Long): TimeBlock?

    suspend fun dayBlocks(date: LocalDate): List<TimeBlock>

    suspend fun rangeBlocks(from: LocalDate, toInclusive: LocalDate): List<TimeBlock>

    fun observeRange(from: LocalDate, toInclusive: LocalDate): Flow<List<TimeBlock>>

    /** Blocks of one calendar month, for the calendar's load dots. */
    fun observeMonth(month: java.time.YearMonth): Flow<List<TimeBlock>>

    /**
     * Persists [block]. Recurring rules materialise concrete rows for the matching days
     * inside [horizon], so the overview screens never need virtual occurrences.
     * Returns the id of the (first) stored row.
     */
    suspend fun save(block: TimeBlock, horizon: PlanHorizon = PlanHorizon.Default): Long

    suspend fun setDone(id: Long, done: Boolean)

    suspend fun delete(id: Long)

    fun observeTotalCount(): Flow<Int>

    suspend fun count(): Int
}

/**
 * How far ahead recurring blocks are materialised. 28 days keeps the calendar honest
 * without turning one tap into hundreds of rows.
 */
data class PlanHorizon(val days: Int) {
    val dates: List<LocalDate> get() = (0 until days).map { LocalDate.now().plusDays(it.toLong()) }

    companion object {
        val Default = PlanHorizon(28)
        val None = PlanHorizon(0)
    }
}

fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun LocalDate.weekEnd(): LocalDate = weekStart().plusDays(6)

fun LocalDate.monthStart(): LocalDate = withDayOfMonth(1)

fun LocalDate.monthEnd(): LocalDate = with(TemporalAdjusters.lastDayOfMonth())

/** Convenience used by the timeline: the current wall clock, second precision dropped. */
fun nowMinute(clock: LocalDateTime = LocalDateTime.now()): LocalDateTime =
    clock.withSecond(0).withNano(0)
