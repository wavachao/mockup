package com.wavachao.timeblock.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wavachao.timeblock.TimeBlockApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Alarms do not survive a reboot, an app update or a timezone change, so the schedule is
 * rebuilt from the database on every one of those events.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val app = context.applicationContext as? TimeBlockApp ?: return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                app.container.reminderScheduler.rescheduleAll()
            } finally {
                withContext(Dispatchers.Main) { pending.finish() }
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
