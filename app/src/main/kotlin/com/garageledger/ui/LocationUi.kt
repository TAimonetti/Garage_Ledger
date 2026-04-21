package com.garageledger.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.garageledger.location.CoordinateSnapshot
import com.garageledger.location.CurrentLocationProvider
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlinx.coroutines.launch

internal fun formatCoordinates(
    latitude: Double,
    longitude: Double,
): String = String.format(Locale.US, "%.6f, %.6f", latitude, longitude)

internal fun buildGeoMapUriString(
    latitude: Double,
    longitude: Double,
    label: String,
): String {
    val encodedLabel = URLEncoder.encode(label.ifBlank { "Location" }, StandardCharsets.UTF_8.name())
        .replace("+", "%20")
    return "geo:0,0?q=$latitude,$longitude($encodedLabel)"
}

internal fun buildGeoMapUri(
    latitude: Double,
    longitude: Double,
    label: String,
): Uri = Uri.parse(buildGeoMapUriString(latitude, longitude, label))

internal fun Context.openCoordinatesOnMap(
    latitude: Double,
    longitude: Double,
    label: String,
) {
    runCatching {
        startActivity(
            Intent(Intent.ACTION_VIEW, buildGeoMapUri(latitude, longitude, label))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LocationActionSection(
    title: String,
    locationEnabled: Boolean,
    latitude: Double?,
    longitude: Double?,
    mapLabel: String,
    captureLabel: String,
    onCaptured: (CoordinateSnapshot) -> Unit,
    onCleared: () -> Unit,
    onError: (String?) -> Unit,
) {
    if (!locationEnabled && (latitude == null || longitude == null)) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationProvider = remember(context) { CurrentLocationProvider(context) }

    fun captureLocation() {
        scope.launch {
            runCatching { locationProvider.captureCurrentLocation() }
                .onSuccess {
                    onCaptured(it)
                    onError(null)
                }
                .onFailure { error ->
                    onError(error.message)
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) {
            captureLocation()
        } else {
            onError("Location permission is required to capture coordinates.")
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        if (latitude != null && longitude != null) {
            Text(
                "Coordinates: ${formatCoordinates(latitude, longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (locationEnabled) {
            Text(
                "No coordinates captured yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (locationEnabled) {
                AssistChip(
                    onClick = {
                        if (CurrentLocationProvider.hasLocationPermission(context)) {
                            captureLocation()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                    },
                    label = { Text(captureLabel) },
                )
            }
            if (latitude != null && longitude != null) {
                AssistChip(
                    onClick = { context.openCoordinatesOnMap(latitude, longitude, mapLabel) },
                    label = { Text("Open Map") },
                )
                AssistChip(
                    onClick = onCleared,
                    label = { Text("Clear") },
                )
            }
        }
    }
}
