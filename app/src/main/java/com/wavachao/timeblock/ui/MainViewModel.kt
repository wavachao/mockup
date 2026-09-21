package com.wavachao.timeblock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wavachao.timeblock.data.DayLoad
import com.wavachao.timeblock.data.DayStats
import com.wavachao.timeblock.data.PlanHorizon
import com.wavachao.timeblock.data.TimeBlockRepository
import com.wavachao.timeblock.data.WeekInsight
import com.wavachao.timeblock.data.buildDayLoads
import com.wavachao.timeblock.data.local.BlockRange
import com.wavachao.timeblock.data.model.TimeBlock
import com.wavachao.timeblock.data.model.TimeBlockDraft
import com.wavachao.timeblock.data.planningStreak
import com.wavachao.timeblock.data.weekEnd
import com.wavachao.timeblock.data.weekStart
import com.wavachao.timeblock.reminder.ReminderScheduler
import com.wavachao.timeblock.ui.util.TimelineLayout
import com.wavachao.timeblock.ui.util.buildTimeline
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/** Top-level destinations. Screens 1/3/4 are the bottom-bar tabs from the mockup. */
object Routes {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val INSIGHTS = "insights"
    const val PROFILE = "profile"
    const val DETAIL = "block/{blockId}"
    const val ADD = "block/new?date={date}"
    const val EDIT = "block/{blockId}/edit"

    fun detail(blockId: Long): String = "block/$blockId"
    fun edit(blockId: Long): String = "block/$blockId/edit"
    fun addFor(date: LocalDate): String = "block/new?date=$date"
}

data class TodayUiState(
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val now: LocalDateTime = LocalDateTime.now(),
    val blocks: List<TimeBlock> = emptyList(),
    val timeline: TimelineLayout = buildTimeline(emptyList()),
    val stats: DayStats = DayStats(LocalDate.now(), 0, 0, 0, 0, null),
) {
    val isToday: Boolean get() = selectedDate == today
    val title: String get() = if (isToday) "今天" else "计划"
}

data class InsightsUiState(
    val weekStart: LocalDate = LocalDate.now().weekStart(),
    val insight: WeekInsight = WeekInsight(
        start = LocalDate.now().weekStart(),
        end = LocalDate.now().weekEnd(),
        totalMinutes = 0,
        previousTotalMinutes = 0,
        perDay = emptyList(),
        slices = emptyList(),
        streakDays = 0,
    ),
)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val loads: List<DayLoad> = emptyList(),
    val selectedBlocks: List<TimeBlock> = emptyList(),
) {
    val maxMinutes: Int get() = loads.maxOfOrNull { it.minutes } ?: 0

    val selectedMinutes: Int get() = selectedBlocks.sumOf { it.durationMinutes.coerceAtLeast(0) }

    fun loadFor(date: LocalDate): DayLoad? = loads.firstOrNull { it.date == date }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: TimeBlockRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val clock = MutableStateFlow(LocalDateTime.now())

    private val _todayState = MutableStateFlow(TodayUiState())
    val todayState: StateFlow<TodayUiState> = _todayState.asStateFlow()

    private val _insightsState = MutableStateFlow(InsightsUiState())
    val insightsState: StateFlow<InsightsUiState> = _insightsState.asStateFlow()

    private val _calendarState = MutableStateFlow(CalendarUiState())
    val calendarState: StateFlow<CalendarUiState> = _calendarState.asStateFlow()
    val totalCount = repository.observeTotalCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val allBlocks = repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private var mutating = false

    fun clearMessage() { _message.value = null }

    private fun mutate(action: suspend () -> Unit) {
        if (mutating) return
        mutating = true
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _message.value = "操作未完成，请重试。你的输入已保留。"
            } finally {
                mutating = false
            }
        }
    }

    private suspend fun updateReminder(action: suspend () -> Unit) {
        try { action() } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { _message.value = "日程已保存，但提醒设置失败，请检查系统通知设置。" }
    }

    init {
        observeSelectedDay()
        observeSelectedMonth()
        observeInsightWeek()
        tickClock()
    }

    /** Screen 1: re-query whenever the selected day changes, refresh on every tick. */
    private fun observeSelectedDay() {
        _todayState
            .map { it.selectedDate }
            .distinctUntilChanged()
            .flatMapLatest { date -> repository.observeDay(date) }
            .combine(clock) { blocks, now -> now to blocks }
            .onEach { (now, blocks) ->
                val date = _todayState.value.selectedDate
                val ordered = blocks.filter { it.start.toLocalDate() == date }.sortedBy { it.start }
                _todayState.update { current ->
                    current.copy(
                        today = now.toLocalDate(),
                        now = now,
                        blocks = ordered,
                        timeline = buildTimeline(ordered, fitToBlocks = true),
                        stats = DayStats.of(date, ordered, now),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /** Screen 3: a month of load dots plus the selected day's short list. */
    private fun observeSelectedMonth() {
        _calendarState
            .map { it.month to it.selectedDate }
            .distinctUntilChanged()
            .flatMapLatest { (month, _) ->
                // The visible grid shows days of the neighbouring months too, so the
                // query window is padded by a week on both sides: tapping a leading or
                // trailing cell then has its list ready instead of looking empty.
                repository.observeRange(
                    month.atDay(1).minusDays(7),
                    month.atEndOfMonth().plusDays(7),
                )
            }
            .onEach { blocks ->
                val state = _calendarState.value
                val month = state.month
                val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
                val loads = buildDayLoads(blocks.map { it.toRangeRow() }, days)
                val selected = blocks.filter { it.start.toLocalDate() == state.selectedDate }
                    .sortedBy { it.start }
                _calendarState.update { current ->
                    current.copy(loads = loads, selectedBlocks = selected)
                }
            }
            .launchIn(viewModelScope)
    }

    /** Screen 4: current week plus the previous one for the delta badge. */
    private fun observeInsightWeek() {
        _insightsState
            .map { it.weekStart }
            .distinctUntilChanged()
            .flatMapLatest { start ->
                repository.observeRange(start.minusDays(7), start.plusDays(13))
            }
            .onEach { blocks ->
                val start = _insightsState.value.weekStart
                val thisWeek = blocks.filter { it.start.toLocalDate() in start..start.plusDays(6) }
                val lastWeek = blocks.filter {
                    it.start.toLocalDate() in start.minusDays(7)..start.minusDays(1)
                }
                val streak = planningStreak(
                    daysWithBlocks = blocks.map { it.start.toLocalDate() }.toSet(),
                    today = LocalDate.now(),
                )
                _insightsState.update { current ->
                    current.copy(
                        insight = WeekInsight.of(
                            weekStart = start,
                            weekEnd = start.plusDays(6),
                            blocks = thisWeek,
                            previousTotalMinutes = lastWeek.sumOf { it.durationMinutes.coerceAtLeast(0) },
                            streakDays = streak,
                        ),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /** A 30s tick keeps the "now" line and the countdowns honest. */
    private fun tickClock() {
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val previousToday = _todayState.value.today
                if (now.toLocalDate() != previousToday && _todayState.value.selectedDate == previousToday) {
                    selectDate(now.toLocalDate())
                }
                clock.value = now
                delay(CLOCK_TICK_MILLIS)
            }
        }
    }

    // ---------------------------------------------------------------- screen 1

    fun selectDate(date: LocalDate) {
        _todayState.update { it.copy(selectedDate = date) }
        _calendarState.update { it.copy(selectedDate = date, month = YearMonth.from(date)) }
    }

    fun stepSelectedDate(days: Long) = selectDate(_todayState.value.selectedDate.plusDays(days))

    fun goToToday() = selectDate(LocalDate.now())

    fun toggleDone(block: TimeBlock) = toggleDone(block) {}

    fun toggleDone(block: TimeBlock, onSaved: () -> Unit) {
        mutate {
            val target = !block.done
            repository.setDone(block.id, target)
            updateReminder { if (target) {
                reminderScheduler.cancel(block.id)
            } else {
                reminderScheduler.schedule(block.copy(done = false))
            } }
            onSaved()
        }
    }

    fun moveBlock(block: TimeBlock, date: LocalDate) {
        val days = java.time.temporal.ChronoUnit.DAYS.between(block.date, date)
        val moved = block.copy(start = block.start.plusDays(days), end = block.end.plusDays(days))
        updateBlock(TimeBlockDraft.from(moved)) { _message.value = "已改期至 $date" }
    }

    fun createBlock(draft: TimeBlockDraft, onSaved: (Long) -> Unit = {}) {
        if (draft.validationError != null) { _message.value = draft.validationError; return }
        mutate {
            val block = draft.toBlock()
            val id = repository.save(block)
            updateReminder {
                repository.rangeBlocks(block.date, block.date.plusDays(PlanHorizon.Default.days.toLong()))
                    .forEach { reminderScheduler.schedule(it) }
            }
            onSaved(id)
        }
    }

    fun updateBlock(draft: TimeBlockDraft, onSaved: (Long) -> Unit = {}) {
        if (draft.validationError != null) { _message.value = draft.validationError; return }
        mutate {
            val block = draft.toBlock()
            repository.save(block, PlanHorizon.None)
            updateReminder {
                reminderScheduler.cancel(block.id)
                reminderScheduler.schedule(block)
            }
            onSaved(block.id)
        }
    }

    fun deleteBlock(blockId: Long, onDeleted: () -> Unit = {}) {
        mutate {
            repository.delete(blockId)
            updateReminder { reminderScheduler.cancel(blockId) }
            onDeleted()
        }
    }

    fun markDone(blockId: Long, done: Boolean = true) {
        viewModelScope.launch {
            repository.setDone(blockId, done)
            if (done) reminderScheduler.cancel(blockId)
        }
    }

    suspend fun blockById(blockId: Long): TimeBlock? = repository.blockById(blockId)

    fun observeBlock(blockId: Long) = repository.observeBlock(blockId)

    // ---------------------------------------------------------------- screen 3

    fun selectMonth(month: YearMonth) {
        _calendarState.update { current ->
            val keepDay = current.selectedDate.year == month.year &&
                current.selectedDate.monthValue == month.monthValue
            current.copy(
                month = month,
                selectedDate = if (keepDay) current.selectedDate else month.atDay(1),
            )
        }
    }

    fun stepMonth(delta: Long) = selectMonth(_calendarState.value.month.plusMonths(delta))

    fun selectCalendarDate(date: LocalDate) {
        _calendarState.update { it.copy(selectedDate = date, month = YearMonth.from(date)) }
        _todayState.update { it.copy(selectedDate = date) }
    }

    // ---------------------------------------------------------------- screen 4

    fun stepWeek(delta: Long) {
        _insightsState.update { it.copy(weekStart = it.weekStart.plusWeeks(delta)) }
    }

    fun currentWeek() {
        _insightsState.update { it.copy(weekStart = LocalDate.now().weekStart()) }
    }

    companion object {
        private const val CLOCK_TICK_MILLIS = 30_000L

        fun factory(
            repository: TimeBlockRepository,
            reminderScheduler: ReminderScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(repository, reminderScheduler) as T
        }
    }
}

fun TimeBlockDraft.toBlock(id: Long = this.id): TimeBlock = TimeBlock(
    id = id,
    title = title.trim().ifBlank { "新时间段" },
    start = start,
    end = end,
    category = category,
    notes = notes,
    reminderMinutes = reminderMinutes,
    recurrence = recurrence,
    done = done,
    createdAt = createdAt,
    allDay = allDay,
)

private fun TimeBlock.toRangeRow(): BlockRange {
    val zone = ZoneId.systemDefault()
    return BlockRange(
        id = id,
        startEpochMillis = start.atZone(zone).toInstant().toEpochMilli(),
        endEpochMillis = end.atZone(zone).toInstant().toEpochMilli(),
        category = category.name,
        done = done,
    )
}
