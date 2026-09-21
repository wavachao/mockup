package com.wavachao.timeblock

import android.app.Application
import android.content.Context
import com.wavachao.timeblock.data.OfflineTimeBlockRepository
import com.wavachao.timeblock.data.PlanHorizon
import com.wavachao.timeblock.data.TimeBlockRepository
import com.wavachao.timeblock.data.local.TimeBlockDatabase
import com.wavachao.timeblock.data.local.startOfDayMillis
import com.wavachao.timeblock.reminder.AndroidReminderScheduler
import com.wavachao.timeblock.reminder.ReminderNotifications
import com.wavachao.timeblock.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Hand-rolled dependency container. Two collaborators do not justify a DI framework,
 * and keeping it explicit means the whole object graph is readable in one screen.
 */
class AppContainer(context: Context) {

    private val database: TimeBlockDatabase = TimeBlockDatabase.get(context)

    val repository: TimeBlockRepository = OfflineTimeBlockRepository(database.timeBlockDao())

    val reminderScheduler: ReminderScheduler = AndroidReminderScheduler(context) {
        val today = LocalDate.now()
        repository.observeAll().first()
            .filter { it.reminderMinutes >= 0 && !it.done }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Rebuilds alarms for everything still ahead of us; safe to call on every start. */
    fun rescheduleReminders() {
        scope.launch { runCatching { reminderScheduler.rescheduleAll() } }
    }

    fun todayStartMillis(): Long = LocalDate.now().startOfDayMillis()
}

class TimeBlockApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderNotifications.ensureChannel(this)
        container.rescheduleReminders()
    }
}
