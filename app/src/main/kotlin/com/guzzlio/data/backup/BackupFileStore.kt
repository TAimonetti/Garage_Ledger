package com.guzzlio.data.backup

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object BackupFileStore {
    private val fileStampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun createBackupFile(directory: File, exportedAt: LocalDateTime): File =
        directory.resolve("guzzlio-backup-${exportedAt.format(fileStampFormatter)}.zip")

    fun pruneOldBackups(directory: File, historyCount: Int): Int {
        val keepCount = historyCount.coerceAtLeast(1)
        val backups = directory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.equals("zip", ignoreCase = true) }
            .sortedByDescending(File::lastModified)
        backups.drop(keepCount).forEach(File::delete)
        return backups.take(keepCount).count { it.exists() }
    }
}
