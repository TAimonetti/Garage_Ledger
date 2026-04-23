package com.guzzlio.domain.model

import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.core.model.VolumeUnit

enum class FuellyImportField(
    val label: String,
    val required: Boolean,
    val aliases: Set<String>,
) {
    ODOMETER(
        label = "Odometer / Distance",
        required = true,
        aliases = setOf("odometer", "miles", "km", "mileage", "distance"),
    ),
    VOLUME(
        label = "Fuel Volume",
        required = true,
        aliases = setOf("gallons", "litres", "liters", "volume", "fuel_volume"),
    ),
    TOTAL_COST(
        label = "Total Price",
        required = true,
        aliases = setOf("price", "total_price", "cost", "amount", "total"),
    ),
    FUEL_UP_DATE(
        label = "Fuel-Up Date",
        required = true,
        aliases = setOf("fuelup_date", "date", "fillup_date", "fuel_date"),
    ),
    CITY_PERCENTAGE(
        label = "City Driving %",
        required = false,
        aliases = setOf("city_percentage", "city_percent", "city"),
    ),
    TAGS(
        label = "Tags",
        required = false,
        aliases = setOf("tags", "tag_list"),
    ),
    NOTES(
        label = "Notes",
        required = false,
        aliases = setOf("notes", "comment", "comments"),
    ),
    BRAND(
        label = "Fuel Brand",
        required = false,
        aliases = setOf("brand", "station_brand"),
    ),
    MISSED_FILL_UP(
        label = "Missed Fill-Up",
        required = false,
        aliases = setOf("missed_fuelup", "missed_fillup", "missed"),
    ),
    PARTIAL_FILL_UP(
        label = "Partial Fill-Up",
        required = false,
        aliases = setOf("partial_fuelup", "partial_fillup", "partial"),
    ),
}

data class FuellyCsvSampleRow(
    val rowNumber: Int,
    val values: Map<String, String>,
)

data class FuellyCsvPreview(
    val headers: List<String>,
    val sampleRows: List<FuellyCsvSampleRow>,
    val suggestedMapping: Map<FuellyImportField, String>,
    val suggestedDistanceUnit: DistanceUnit? = null,
    val suggestedVolumeUnit: VolumeUnit? = null,
    val issues: List<ImportIssue> = emptyList(),
)

data class FuellyCsvImportConfig(
    val vehicleId: Long,
    val fieldMapping: Map<FuellyImportField, String>,
    val distanceUnit: DistanceUnit,
    val volumeUnit: VolumeUnit,
) {
    fun validationErrors(headers: List<String>): List<String> {
        val errors = mutableListOf<String>()
        FuellyImportField.entries.filter(FuellyImportField::required).forEach { field ->
            if (fieldMapping[field].isNullOrBlank()) {
                errors += "${field.label} is required."
            }
        }

        val unknownHeaders = fieldMapping.values.filterNot(headers::contains).distinct()
        if (unknownHeaders.isNotEmpty()) {
            errors += "Some mapped columns are no longer present in the selected file."
        }

        val duplicateHeaders = fieldMapping.values
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicateHeaders.isNotEmpty()) {
            errors += "Each CSV column can only be assigned once."
        }

        return errors
    }
}

data class FuellyCsvImportResult(
    val fillUpRecords: List<FillUpRecord>,
    val issues: List<ImportIssue> = emptyList(),
)
