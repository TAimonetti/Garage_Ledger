package com.garageledger.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val SERVICE_REMINDERS: String = "service_reminders"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val reminderChannel = NotificationChannel(
            SERVICE_REMINDERS,
            "Service reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Due and upcoming vehicle service reminders."
        }
        manager.createNotificationChannel(reminderChannel)
    }
}
