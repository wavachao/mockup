package com.wavachao.timeblock.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.wavachao.timeblock.MainActivity
import com.wavachao.timeblock.R

/** Builds the "your block starts now" notification. */
object ReminderNotifications {

    const val CHANNEL_BLOCKS = "block_reminders"
    private const val NOTIFICATION_BASE_ID = 4200

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_BLOCKS) != null) return
        val channel = NotificationChannel(
            CHANNEL_BLOCKS,
            "时间段提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "在时间段开始前提醒你"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, blockId: Long, title: String, timeLabel: String, category: Int) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !manager.areNotificationsEnabled()
        ) {
            return
        }
        ensureChannel(context)

        val open = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_BLOCK_ID, blockId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            blockId.toInt(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_BLOCKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("$timeLabel 开始，准备好进入这段时间")
            .setColor(category)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching { manager.notify(NOTIFICATION_BASE_ID + blockId.toInt(), notification) }
    }
}
