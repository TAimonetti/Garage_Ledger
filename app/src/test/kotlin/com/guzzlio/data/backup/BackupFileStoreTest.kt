package com.guzzlio.data.backup

import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import org.junit.Test

class BackupFileStoreTest {
    @Test
    fun pruneOldBackups_keepsNewestZipFilesOnly() {
        val directory = Files.createTempDirectory("guzzlio-backups").toFile()
        try {
            val oldest = directory.resolve("guzzlio-backup-1.zip").apply {
                writeText("oldest")
                setLastModified(1_000L)
            }
            val middle = directory.resolve("guzzlio-backup-2.zip").apply {
                writeText("middle")
                setLastModified(2_000L)
            }
            val newest = directory.resolve("guzzlio-backup-3.zip").apply {
                writeText("newest")
                setLastModified(3_000L)
            }
            directory.resolve("notes.txt").writeText("ignore")

            val retainedCount = BackupFileStore.pruneOldBackups(directory, historyCount = 2)

            assertThat(retainedCount).isEqualTo(2)
            assertThat(oldest.exists()).isFalse()
            assertThat(middle.exists()).isTrue()
            assertThat(newest.exists()).isTrue()
        } finally {
            directory.deleteRecursively()
        }
    }
}
