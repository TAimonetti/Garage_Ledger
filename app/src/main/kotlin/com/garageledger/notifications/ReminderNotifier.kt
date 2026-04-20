package com.garageledger.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.garageledger.MainActivity
import com.garageledger.domain.model.ReminderAlert

class ReminderNotifier(
    private val context: Context,
) {
    fun showServiceReminders(alerts: List<ReminderAlert>) {
        if (alerts.isEmpty()) {
            clearServiceReminders()
            return
        }
        val lines = alerts.take(5).map { alert ->
            buildString {
                append(alert.vehicleName.ifBlank { "Vehicle" })
                append(": ")
                append(alert.serviceTypeName)
                when {
                    alert.dueDate != null -> append(" due ${alert.dueDate}")
                    alert.dueDistance != null -> append(" due at ${alert.dueDistance.toInt()}")
                }
            }
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (alerts.size == 1) {
            "${alerts.first().serviceTypeName} reminder due"
        } else {
            "${alerts.size} service reminders due"
        }
        val notification = NotificationCompat.Builder(context, NotificationChannels.SERVICE_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(lines.firstOrNull() ?: "Service reminder due")
            .setStyle(NotificationCompat.InboxStyle().also { style ->
                lines.forEach(style::addLine)
            })
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(SERVICE_REMINDER_NOTIFICATION_ID, notification)
    }

    fun clearServiceReminders() {
        NotificationManagerCompat.from(context).cancel(SERVICE_REMINDER_NOTIFICATION_ID)
    }

    private companion object {
        const val SERVICE_REMINDER_NOTIFICATION_ID: Int = 4200
    }
}
