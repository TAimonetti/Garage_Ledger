package com.guzzlio.core.model

import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
enum class DistanceUnit(val storageValue: String) {
    MILES("mi"),
    KILOMETERS("km");

    companion object {
        fun fromStorage(value: String?): DistanceUnit = when (value?.trim()?.lowercase(Locale.US)) {
            "km", "kilometer", "kilometers" -> KILOMETERS
            else -> MILES
        }
    }
}

@Serializable
enum class VolumeUnit(val storageValue: String) {
    GALLONS_US("gal (US)"),
    GALLONS_UK("gal (UK)"),
    LITERS("L");

    companion object {
        fun fromStorage(value: String?): VolumeUnit = when (value?.trim()?.lowercase(Locale.US)) {
            "l", "liter", "liters", "litre", "litres" -> LITERS
            "gal (uk)" -> GALLONS_UK
            else -> GALLONS_US
        }
    }
}

@Serializable
enum class FuelEfficiencyUnit(val storageValue: String) {
    MPG_US("MPG (US)"),
    MPG_UK("MPG (UK)"),
    KILOMETERS_PER_LITER("km/L"),
    LITERS_PER_100_KM("L/100km");

    companion object {
        fun fromStorage(value: String?): FuelEfficiencyUnit = when (value?.trim()?.lowercase(Locale.US)) {
            "mpg (uk)" -> MPG_UK
            "km/l" -> KILOMETERS_PER_LITER
            "l/100km" -> LITERS_PER_100_KM
            else -> MPG_US
        }
    }
}

@Serializable
enum class FuelEfficiencyAssignmentMethod(val storageValue: String) {
    PREVIOUS_RECORD("previous-record"),
    CURRENT_RECORD("current-record");

    companion object {
        fun fromStorage(value: String?): FuelEfficiencyAssignmentMethod = when (value?.trim()?.lowercase(Locale.US)) {
            "current-record" -> CURRENT_RECORD
            else -> PREVIOUS_RECORD
        }
    }
}
