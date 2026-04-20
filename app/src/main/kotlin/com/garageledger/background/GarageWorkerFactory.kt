package com.garageledger.background

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.garageledger.data.GarageRepository
import com.garageledger.data.backup.LocalBackupManager
import com.garageledger.notifications.ReminderNotifier

class GarageWorkerFactory(
    private val repository: GarageRepository,
    private val backupManager: LocalBackupManager,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        LocalBackupWorker::class.qualifiedName -> LocalBackupWorker(appContext, workerParameters, backupManager)
        ReminderCheckWorker::class.qualifiedName -> ReminderCheckWorker(
            appContext = appContext,
            workerParams = workerParameters,
            repository = repository,
            notifier = ReminderNotifier(appContext),
        )

        else -> null
    }
}
