package com.guzzlio.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.guzzlio.domain.model.AppPreferenceSnapshot
import java.util.concurrent.TimeUnit

class GarageWorkScheduler(
    context: Context,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun sync(preferences: AppPreferenceSnapshot) {
        syncBackups(preferences)
        syncReminderChecks(preferences)
    }

    fun enqueueImmediateBackup() {
        workManager.enqueueUniqueWork(
            LocalBackupWorker.IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<LocalBackupWorker>().build(),
        )
    }

    private fun syncBackups(preferences: AppPreferenceSnapshot) {
        if (preferences.backupFrequencyHours <= 0) {
            workManager.cancelUniqueWork(LocalBackupWorker.UNIQUE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<LocalBackupWorker>(
            preferences.backupFrequencyHours.coerceAtLeast(1).toLong(),
            TimeUnit.HOURS,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresStorageNotLow(true)
                    .build(),
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            LocalBackupWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun syncReminderChecks(preferences: AppPreferenceSnapshot) {
        if (!preferences.notificationsEnabled) {
            workManager.cancelUniqueWork(ReminderCheckWorker.UNIQUE_WORK_NAME)
            workManager.cancelUniqueWork(ReminderCheckWorker.IMMEDIATE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(
            REMINDER_CHECK_INTERVAL_HOURS,
            TimeUnit.HOURS,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            ReminderCheckWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        workManager.enqueueUniqueWork(
            ReminderCheckWorker.IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ReminderCheckWorker>().build(),
        )
    }

    private companion object {
        const val REMINDER_CHECK_INTERVAL_HOURS: Long = 12
    }
}
