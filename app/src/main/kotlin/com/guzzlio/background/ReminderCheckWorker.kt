package com.guzzlio.background

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.guzzlio.data.GarageRepository
import com.guzzlio.notifications.ReminderNotifier

class ReminderCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repository: GarageRepository,
    private val notifier: ReminderNotifier,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = runCatching {
        val preferences = repository.getPreferenceSnapshot()
        if (!preferences.notificationsEnabled || !notificationPermissionGranted()) {
            notifier.clearServiceReminders()
            return Result.success(workDataOf(KEY_ALERT_COUNT to 0))
        }
        val alerts = repository.getDueReminderAlerts()
        if (alerts.isEmpty()) {
            notifier.clearServiceReminders()
            return Result.success(workDataOf(KEY_ALERT_COUNT to 0))
        }
        notifier.showServiceReminders(alerts)
        repository.markReminderAlertsDelivered(alerts)
        Result.success(workDataOf(KEY_ALERT_COUNT to alerts.size))
    }.getOrElse {
        Result.retry()
    }

    private fun notificationPermissionGranted(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val UNIQUE_WORK_NAME: String = "garage_reminder_check"
        const val IMMEDIATE_WORK_NAME: String = "garage_reminder_check_now"
        const val KEY_ALERT_COUNT: String = "alert_count"
    }
}
