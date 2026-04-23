package com.guzzlio.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guzzlio.attachments.LocalAttachmentStore
import com.guzzlio.attachments.PreparedCameraCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VehiclePhotoEditorCard(
    photoUri: String?,
    onPhotoChanged: (String?) -> Unit,
    onError: (String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attachmentStore = remember(context) { LocalAttachmentStore(context) }
    var pendingCameraCapture by remember { mutableStateOf<PreparedCameraCapture?>(null) }

    fun importPhoto(uri: Uri) {
        scope.launch {
            runCatching {
                attachmentStore.importAttachment(uri).uri
            }.onSuccess { storedUri ->
                onPhotoChanged(storedUri)
                onError(null)
            }.onFailure { error ->
                onError(error.message)
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importPhoto(uri)
    }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importPhoto(uri)
    }
    val cameraCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capture = pendingCameraCapture
        pendingCameraCapture = null
        if (!success || capture == null) return@rememberLauncherForActivityResult
        onPhotoChanged(capture.uri.toString())
        onError(null)
    }

    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Profile Photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VehiclePhotoThumbnail(
                    photoUri = photoUri,
                    modifier = Modifier.size(96.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (photoUri.isNullOrBlank()) {
                            "No local profile photo selected."
                        } else {
                            "The selected photo stays local with the rest of the vehicle data."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!photoUri.isNullOrBlank()) {
                        TextButton(onClick = { onPhotoChanged(null) }) {
                            Text("Remove Photo")
                        }
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { filePicker.launch(arrayOf("image/*")) },
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
        }
    }
}

@Composable
internal fun VehiclePhotoThumbnail(
    photoUri: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val photoBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = photoUri) {
        value = withContext(Dispatchers.IO) {
            photoUri?.takeIf(String::isNotBlank)?.let { uriString ->
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriString))
                        ?.use(BitmapFactory::decodeStream)
                        ?.asImageBitmap()
                }.getOrNull()
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        if (photoBitmap != null) {
            Image(
                bitmap = photoBitmap!!,
                contentDescription = "Vehicle photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
