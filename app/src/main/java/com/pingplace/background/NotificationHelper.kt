package com.pingplace.background

import android.Manifest
import android.app.NotificationChannel
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pingplace.MainActivity
import com.pingplace.R
import com.pingplace.domain.BrandReminderMatch

class NotificationHelper(
    private val context: Context
) {

    fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ERRANDS,
                "Errand reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when you are near places tied to reminders."
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_MONITORING, "Nearby monitoring", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows when PingPlace is checking for nearby errands." })
    }

    fun monitoringNotification(): Notification {
        ensureChannels()
        val pendingIntent = PendingIntent.getActivity(context, 7401,
            Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_stat_pingplace)
            .setContentTitle("PingPlace is watching for your places")
            .setContentText("Faster checks while moving. Manage monitoring in Settings.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun alertsAvailable(): Boolean {
        ensureChannels()
        return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL_ERRANDS)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun showBrandReminder(match: BrandReminderMatch, soundEnabled: Boolean = true): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (!alertsAvailable()) return false
        val content = match.reminders.joinToString("\n") { "- ${it.title}" }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_BRAND_QUERY, match.brandQuery)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            match.brandQuery.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val reminderIds = match.reminders.map { it.id }.toLongArray()
        val title = when {
            match.nearestPlace?.estimatedTravelMinutes != null ->
                "You are ${match.nearestPlace.estimatedTravelMinutes} min from ${match.brandName}"

            else -> "You are near ${match.brandName}"
        }
        val body = if (match.reminders.size == 1) {
            match.reminders.first().title
        } else {
            "${match.reminders.size} things to do"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ERRANDS)
            .setSmallIcon(R.drawable.ic_stat_pingplace)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .addAction(
                0,
                "Done",
                actionPendingIntent(
                    action = NotificationActionReceiver.ACTION_COMPLETE,
                    requestCode = match.brandQuery.hashCode() + 1,
                    reminderIds = reminderIds
                )
            )
            .addAction(
                0,
                "Snooze 30m",
                actionPendingIntent(
                    action = NotificationActionReceiver.ACTION_SNOOZE,
                    requestCode = match.brandQuery.hashCode() + 2,
                    reminderIds = reminderIds
                )
            )
            .setSilent(!soundEnabled)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(match.brandQuery.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val CHANNEL_ERRANDS = "errand_reminders"
        const val CHANNEL_MONITORING = "nearby_monitoring"
    }

    private fun actionPendingIntent(
        action: String,
        requestCode: Int,
        reminderIds: LongArray
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_REMINDER_IDS, reminderIds)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
