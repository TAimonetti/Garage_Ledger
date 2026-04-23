package com.guzzlio.attachments

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class LocalAttachmentStore(
    private val context: Context,
) {
    fun importAttachment(sourceUri: Uri): StoredAttachment {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(sourceUri).orEmpty()
        val originalName = queryDisplayName(sourceUri) ?: defaultNameForMimeType(mimeType)
        val targetFile = createTargetFile(
            baseName = originalName.substringBeforeLast('.', originalName),
            extension = originalName.substringAfterLast('.', "").ifBlank { extensionForMimeType(mimeType) },
        )
        contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to read the selected attachment.")
        return StoredAttachment(
            uri = contentUriFor(targetFile).toString(),
            mimeType = mimeType.ifBlank { fallbackMimeTypeForExtension(targetFile.extension) },
            displayName = originalName,
        )
    }

    fun prepareCameraCapture(): PreparedCameraCapture {
        val timestamp = LocalDateTime.now().format(FileStampFormatter)
        val file = createTargetFile(baseName = "camera-$timestamp", extension = "jpg")
        return PreparedCameraCapture(
            uri = contentUriFor(file),
            displayName = file.name,
            mimeType = "image/jpeg",
        )
    }

    private fun createTargetFile(baseName: String, extension: String): File {
        val safeBase = baseName.ifBlank { "attachment" }
            .replace(UnsafeFileCharacters, "-")
            .trim('-')
            .ifBlank { "attachment" }
        val timestamp = LocalDateTime.now().format(FileStampFormatter)
        val suffix = extension.lowercase(Locale.US).takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
        return attachmentsDirectory().resolve("$safeBase-$timestamp$suffix")
    }

    private fun attachmentsDirectory(): File = context.filesDir.resolve("attachments").apply { mkdirs() }

    private fun contentUriFor(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    private fun queryDisplayName(sourceUri: Uri): String? = context.contentResolver.query(
        sourceUri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0) cursor.getString(index) else null
    }

    private fun defaultNameForMimeType(mimeType: String): String = when {
        mimeType.equals("application/pdf", ignoreCase = true) -> "attachment.pdf"
        mimeType.startsWith("image/", ignoreCase = true) -> "attachment.${extensionForMimeType(mimeType)}"
        else -> "attachment.bin"
    }

    private fun extensionForMimeType(mimeType: String): String = when {
        mimeType.equals("application/pdf", ignoreCase = true) -> "pdf"
        mimeType.equals("image/png", ignoreCase = true) -> "png"
        mimeType.equals("image/webp", ignoreCase = true) -> "webp"
        else -> "jpg"
    }

    private fun fallbackMimeTypeForExtension(extension: String): String = when (extension.lowercase(Locale.US)) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "application/octet-stream"
    }

    companion object {
        private val UnsafeFileCharacters = Regex("[^A-Za-z0-9._-]+")
        private val FileStampFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS")
    }
}

data class StoredAttachment(
    val uri: String,
    val mimeType: String,
    val displayName: String,
)

data class PreparedCameraCapture(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
)
