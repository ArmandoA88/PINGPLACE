package com.pingplace.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pingplace.PingPlaceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val reminderIds = intent.getLongArrayExtra(EXTRA_REMINDER_IDS)?.toList().orEmpty()
        if (reminderIds.isEmpty()) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (appContext as PingPlaceApplication).container
                when (action) {
                    ACTION_COMPLETE -> reminderIds.forEach { id ->
                        container.repository.setReminderCompleted(id, true)
                    }

                    ACTION_SNOOZE -> {
                        val until = System.currentTimeMillis() + 30 * 60 * 1000
                        reminderIds.forEach { id ->
                            container.repository.snoozeReminder(id, until)
                        }
                    }
                }
                container.monitorScheduler.triggerImmediateRefresh()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.pingplace.action.COMPLETE_REMINDERS"
        const val ACTION_SNOOZE = "com.pingplace.action.SNOOZE_REMINDERS"
        const val EXTRA_REMINDER_IDS = "reminder_ids"
    }
}
