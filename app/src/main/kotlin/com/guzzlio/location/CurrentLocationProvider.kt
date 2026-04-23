package com.guzzlio.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class CoordinateSnapshot(
    val latitude: Double,
    val longitude: Double,
)

class CurrentLocationProvider(
    private val context: Context,
) {
    suspend fun captureCurrentLocation(): CoordinateSnapshot {
        if (!hasLocationPermission(context)) {
            error("Location permission is required to capture coordinates.")
        }
        val locationManager = context.getSystemService(LocationManager::class.java)
            ?: error("Location services are unavailable on this device.")
        val enabledProviders = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        val freshestLastKnown = freshestLastKnownLocation(locationManager, enabledProviders)
        if (freshestLastKnown != null) {
            return CoordinateSnapshot(
                latitude = freshestLastKnown.latitude,
                longitude = freshestLastKnown.longitude,
            )
        }
        val provider = enabledProviders.firstOrNull()
            ?: error("Enable device location services to capture coordinates.")
        return requestCurrentLocation(locationManager, provider)
    }

    @SuppressLint("MissingPermission")
    private fun freshestLastKnownLocation(
        locationManager: LocationManager,
        enabledProviders: List<String>,
    ): Location? = enabledProviders.asSequence()
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(
        locationManager: LocationManager,
        provider: String,
    ): CoordinateSnapshot = suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(context),
            ) { location ->
                if (!continuation.isActive) return@getCurrentLocation
                if (location == null) {
                    continuation.resumeWithException(
                        IllegalStateException("Unable to determine the current location right now."),
                    )
                } else {
                    continuation.resume(
                        CoordinateSnapshot(
                            latitude = location.latitude,
                            longitude = location.longitude,
                        ),
                    )
                }
            }
        }

    companion object {
        fun hasLocationPermission(context: Context): Boolean = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ).any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
