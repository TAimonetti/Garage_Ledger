package com.garageledger.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.garageledger.data.backup.LocalBackupManager

class LocalBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val backupManager: LocalBackupManager,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = runCatching {
        val result = backupManager.writeBackupNow()
        Result.success(
            workDataOf(
                KEY_FILE_PATH to result.filePath,
                KEY_RETAINED_COUNT to result.retainedBackupCount,
            ),
        )
    }.getOrElse {
        Result.retry()
    }

    companion object {
        const val UNIQUE_WORK_NAME: String = "garage_local_backup"
        const val IMMEDIATE_WORK_NAME: String = "garage_local_backup_now"
        const val KEY_FILE_PATH: String = "file_path"
        const val KEY_RETAINED_COUNT: String = "retained_count"
    }
}
