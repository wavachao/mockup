package com.wavachao.timeblock.data

import com.wavachao.timeblock.data.local.TimeBlockDao
import com.wavachao.timeblock.data.local.endOfDayMillisExclusive
import com.wavachao.timeblock.data.local.startOfDayMillis
import com.wavachao.timeblock.data.local.toEntity
import com.wavachao.timeblock.data.local.toModel
import com.wavachao.timeblock.data.model.RecurrenceRule
import com.wavachao.timeblock.data.model.TimeBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth

class OfflineTimeBlockRepository(
    private val dao: TimeBlockDao,
) : TimeBlockRepository {

    override fun observeDay(date: LocalDate): Flow<List<TimeBlock>> =
        dao.observeBetween(date.startOfDayMillis(), date.endOfDayMillisExclusive())
            .map { rows -> rows.map { it.toModel() }.sortedBy { it.start } }

    override fun observeBlock(id: Long): Flow<TimeBlock?> =
        dao.observeById(id).map { it?.toModel() }

    override suspend fun blockById(id: Long): TimeBlock? = dao.findById(id)?.toModel()

    override suspend fun dayBlocks(date: LocalDate): List<TimeBlock> =
        dao.between(date.startOfDayMillis(), date.endOfDayMillisExclusive())
            .map { it.toModel() }
            .sortedBy { it.start }

    override suspend fun rangeBlocks(from: LocalDate, toInclusive: LocalDate): List<TimeBlock> =
        dao.between(from.startOfDayMillis(), toInclusive.endOfDayMillisExclusive())
            .map { it.toModel() }
            .sortedBy { it.start }

    override fun observeRange(from: LocalDate, toInclusive: LocalDate): Flow<List<TimeBlock>> =
        dao.observeBetween(from.startOfDayMillis(), toInclusive.endOfDayMillisExclusive())
            .map { rows -> rows.map { it.toModel() }.sortedBy { it.start } }

    override fun observeMonth(month: YearMonth): Flow<List<TimeBlock>> {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        return observeRange(first, last)
    }

    override suspend fun save(block: TimeBlock, horizon: PlanHorizon): Long {
        val rule = block.recurrence
        if (rule == RecurrenceRule.NONE) {
            val id = dao.upsert(block.toEntity())
            return if (block.id != 0L) block.id else id
        }

        val dates = horizon.dates.filter { it >= block.date && rule.matches(it.dayOfWeek) }
        val rows = dates.map { date ->
            val dayShift = Duration.between(block.date.atStartOfDay(), date.atStartOfDay()).toDays()
            block.copy(
                id = 0L,
                start = block.start.plusDays(dayShift),
                end = block.end.plusDays(dayShift),
                done = false,
            ).toEntity()
        }
        if (rows.isEmpty()) {
            val id = dao.upsert(block.toEntity())
            return if (block.id != 0L) block.id else id
        }
        val ids = dao.upsertAll(rows)
        return ids.firstOrNull() ?: 0L
    }

    override suspend fun setDone(id: Long, done: Boolean) = dao.setDone(id, done)

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override fun observeTotalCount(): Flow<Int> = dao.observeTotalCount()

    override suspend fun count(): Int = dao.count()
}
