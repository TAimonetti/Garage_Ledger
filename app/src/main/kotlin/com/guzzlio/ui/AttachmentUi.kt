package com.guzzlio.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guzzlio.attachments.LocalAttachmentStore
import com.guzzlio.attachments.PreparedCameraCapture
import com.guzzlio.attachments.StoredAttachment
import com.guzzlio.domain.model.RecordAttachment
import com.guzzlio.domain.model.RecordFamily
import java.time.LocalDateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AttachmentEditorCard(
    vehicleId: Long,
    recordFamily: RecordFamily,
    attachments: List<RecordAttachment>,
    onAttachmentsChange: (List<RecordAttachment>) -> Unit,
    onError: (String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attachmentStore = remember(context) { LocalAttachmentStore(context) }
    var pendingCameraCapture by remember { mutableStateOf<PreparedCameraCapture?>(null) }

    fun addStoredAttachment(stored: StoredAttachment) {
        if (attachments.any { it.uri == stored.uri }) return
        onAttachmentsChange(
            attachments + RecordAttachment(
                vehicleId = vehicleId,
                recordFamily = recordFamily,
                recordId = 0L,
                uri = stored.uri,
                mimeType = stored.mimeType,
                displayName = stored.displayName,
                createdAt = LocalDateTime.now(),
            ),
        )
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                uris.forEach { uri ->
                    addStoredAttachment(attachmentStore.importAttachment(uri))
                }
            }.onSuccess {
                onError(null)
            }.onFailure {
                onError(it.message)
            }
        }
    }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                addStoredAttachment(attachmentStore.importAttachment(uri))
            }.onSuccess {
                onError(null)
            }.onFailure {
                onError(it.message)
            }
        }
    }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capture = pendingCameraCapture
        pendingCameraCapture = null
        if (!success || capture == null) return@rememberLauncherForActivityResult
        addStoredAttachment(
            StoredAttachment(
                uri = capture.uri.toString(),
                mimeType = capture.mimeType,
                displayName = capture.displayName,
            ),
        )
        onError(null)
    }

    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Attachments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Keep local photos and PDFs with the record. Files are copied into app storage for offline use.")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { filePicker.launch(arrayOf("image/*", "application/pdf")) },
                    label = { Text("Files") },
                )
                AssistChip(
                    onClick = {
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    label = { Text("Gallery") },
                )
                AssistChip(
                    onClick = {
                        val prepared = attachmentStore.prepareCameraCapture()
                        pendingCameraCapture = prepared
                        cameraCapture.launch(prepared.uri)
                    },
                    label = { Text("Camera") },
                )
            }
            if (attachments.isEmpty()) {
                Text("No attachments yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                attachments.forEach { attachment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(attachment.displayName.ifBlank { "Attachment" })
                            if (attachment.mimeType.isNotBlank()) {
                                Text(attachment.mimeType, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row {
                            TextButton(onClick = { context.openAttachment(attachment) }) {
                                Text("Open")
                            }
                            TextButton(
                                onClick = {
                                    onAttachmentsChange(attachments.filterNot { it.uri == attachment.uri })
                                },
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun android.content.Context.openAttachment(attachment: RecordAttachment) {
    runCatching {
        startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(Uri.parse(attachment.uri), attachment.mimeType.ifBlank { "*/*" })
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
