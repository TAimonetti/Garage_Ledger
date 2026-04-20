package com.garageledger.data.importer

import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.FuelEfficiencyUnit
import com.garageledger.core.model.VolumeUnit
import com.garageledger.domain.model.FillUpRecord
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord

class FuellyCsvImporter {
    fun importFillUps(
        inputStream: InputStream,
        vehicleId: Long,
        fieldMapping: Map<String, String> = emptyMap(),
    ): List<FillUpRecord> {
        val text = InputStreamReader(inputStream).readText()
        val format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .build()

        return format.parse(StringReader(text)).records.mapNotNull { record ->
            val odometer = parseLooseDouble(value(record, fieldMapping, "odometer", "miles", "km")) ?: return@mapNotNull null
            val volume = parseLooseDouble(value(record, fieldMapping, "gallons", "litres", "liters")) ?: return@mapNotNull null
            val totalCost = parseLooseDouble(value(record, fieldMapping, "price")) ?: 0.0
            val localDate = runCatching { LocalDate.parse(value(record, fieldMapping, "fuelup_date").orEmpty()) }
                .getOrDefault(LocalDate.now())

            FillUpRecord(
                legacySourceId = record.recordNumber.toLong(),
                vehicleId = vehicleId,
                dateTime = LocalDateTime.of(localDate, LocalTime.NOON),
                odometerReading = odometer,
                distanceUnit = when {
                    record.isMapped("km") || fieldMapping["km"] != null -> DistanceUnit.KILOMETERS
                    else -> DistanceUnit.MILES
                },
                volume = volume,
                volumeUnit = when {
                    record.isMapped("litres") || record.isMapped("liters") || fieldMapping["litres"] != null || fieldMapping["liters"] != null -> VolumeUnit.LITERS
                    else -> VolumeUnit.GALLONS_US
                },
                pricePerUnit = if (volume > 0.0) totalCost / volume else 0.0,
                totalCost = totalCost,
                partial = parseYesNo(value(record, fieldMapping, "partial_fuelup")),
                previousMissedFillups = parseYesNo(value(record, fieldMapping, "missed_fuelup")),
                fuelEfficiencyUnit = FuelEfficiencyUnit.MPG_US,
                fuelBrand = value(record, fieldMapping, "brand").orEmpty(),
                tags = splitCommaList(value(record, fieldMapping, "tags")),
                notes = value(record, fieldMapping, "notes").orEmpty(),
                cityDrivingPercentage = parseLooseInt(value(record, fieldMapping, "city_percentage")),
            )
        }
    }

    private fun value(
        record: CSVRecord,
        fieldMapping: Map<String, String>,
        vararg aliases: String,
    ): String? {
        val mapped = aliases.firstNotNullOfOrNull { alias ->
            fieldMapping[alias]?.takeIf { record.isMapped(it) }?.let(record::get)
        }
        if (mapped != null) return mapped
        return aliases.firstNotNullOfOrNull { alias ->
            if (record.isMapped(alias)) record.get(alias) else null
        }
    }
}
