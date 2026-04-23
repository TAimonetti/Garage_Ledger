package com.guzzlio.data.backup

import android.content.Context
import android.os.Environment
import com.guzzlio.data.GarageRepository
import com.guzzlio.domain.model.BackupRunResult
import java.io.File
import java.time.LocalDateTime

class LocalBackupManager(
    context: Context,
    private val repository: GarageRepository,
) {
    private val appContext = context.applicationContext

    suspend fun writeBackupNow(exportedAt: LocalDateTime = LocalDateTime.now()): BackupRunResult {
        val preferences = repository.getPreferenceSnapshot()
        val directory = backupDirectory().apply { mkdirs() }
        val targetFile = BackupFileStore.createBackupFile(directory, exportedAt)
        val tempFile = directory.resolve("${targetFile.name}.tmp")
        tempFile.outputStream().use { outputStream ->
            repository.exportOpenJsonBackup(outputStream)
        }
        if (targetFile.exists()) {
            targetFile.delete()
        }
        val moved = tempFile.renameTo(targetFile)
        if (!moved) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
        val retainedCount = BackupFileStore.pruneOldBackups(directory, preferences.backupHistoryCount)
        return BackupRunResult(
            filePath = targetFile.absolutePath,
            exportedAt = exportedAt,
            retainedBackupCount = retainedCount,
        )
    }

    fun backupDirectoryPath(): String = backupDirectory().absolutePath

    private fun backupDirectory(): File =
        (appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: appContext.filesDir).resolve("backups")
}
