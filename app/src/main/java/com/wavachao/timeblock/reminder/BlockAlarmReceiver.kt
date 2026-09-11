package com.wavachao.timeblock.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wavachao.timeblock.TimeBlockApp
import com.wavachao.timeblock.data.local.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

/**
 * Fires when a block's lead time elapses: announces the block and, for recurring rules,
 * hands the materialisation of the next occurrence to WorkManager.
 */
class BlockAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_BLOCK_REMINDER) return
        val blockId = intent.getLongExtra(EXTRA_BLOCK_ID, -1L)
        if (blockId <= 0L) return
        val fallbackTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        val app = context.applicationContext as? TimeBlockApp ?: return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                val block = app.container.repository.blockById(blockId)
                val title = block?.title ?: fallbackTitle.ifBlank { "时间段开始" }
                val timeLabel = block?.start?.format(TimeFormatter) ?: ""
                val color = block?.category?.color?.value?.toLong()?.toInt() ?: DEFAULT_COLOR
                ReminderNotifications.show(context, blockId, title, timeLabel, color)
            } finally {
                withContext(Dispatchers.Main) { pending.finish() }
            }
        }
    }

    companion object {
        const val ACTION_BLOCK_REMINDER = "com.wavachao.timeblock.action.BLOCK_REMINDER"
        const val EXTRA_BLOCK_ID = "block_id"
        const val EXTRA_TITLE = "block_title"
        private const val DEFAULT_COLOR = 0xFF6D5EF8.toInt()
    }
}

internal val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
