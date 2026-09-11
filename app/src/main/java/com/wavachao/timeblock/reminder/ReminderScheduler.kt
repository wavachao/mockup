package com.wavachao.timeblock.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.wavachao.timeblock.data.local.toModel
import com.wavachao.timeblock.data.model.TimeBlock
import javax.inject.Inject

/**
 * Schedules (and cancels) the "block is starting" alarm.
 *
 * Exact alarms are the difference between a reminder that lands on the minute and one
 * the OS batches into the next maintenance window, so we ask for
 * `SCHEDULE_EXACT_ALARM` and degrade to an inexact alarm when the user has revoked it
 * instead of dropping the reminder entirely.
 */
interface ReminderScheduler {
    suspend fun schedule(block: TimeBlock)
    suspend fun cancel(blockId: Long)
    suspend fun rescheduleAll()
}

class AndroidReminderScheduler(
    private val context: Context,
    private val blockProvider: suspend () -> List<TimeBlock>,
) : ReminderScheduler {

    private val alarmManager: AlarmManager? get() = context.getSystemService()

    override suspend fun schedule(block: TimeBlock) {
        val manager = alarmManager ?: return
        val triggerAt = block.start
            .minusMinutes(block.reminderMinutes.coerceAtLeast(0).toLong())
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerAt <= System.currentTimeMillis()) {
            cancel(block.id)
            return
        }
        val pending = pendingIntent(block.id, block.title, triggerAt)
        try {
            if (canScheduleExact(manager)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    override suspend fun cancel(blockId: Long) {
        val manager = alarmManager ?: return
        manager.cancel(pendingIntent(blockId, "", 0L))
    }

    override suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        blockProvider()
            .filter { it.start.toEpochMillisCompat() > now }
            .forEach { schedule(it) }
    }

    private fun canScheduleExact(manager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }

    private fun pendingIntent(blockId: Long, title: String, triggerAt: Long): PendingIntent {
        val intent = Intent(context, BlockAlarmReceiver::class.java).apply {
            action = BlockAlarmReceiver.ACTION_BLOCK_REMINDER
            putExtra(BlockAlarmReceiver.EXTRA_BLOCK_ID, blockId)
            putExtra(BlockAlarmReceiver.EXTRA_TITLE, title)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode(blockId), intent, flags)
    }

    private fun requestCode(blockId: Long): Int = (blockId % Int.MAX_VALUE).toInt() + 1000
}

/** Keeps the alarm layer independent of entity mapping details. */
internal fun TimeBlock.toEpochMillisCompat(): Long =
    start.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
