package com.garageledger.data.importer

import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.FuelEfficiencyUnit
import com.garageledger.core.model.VolumeUnit
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.FuellyCsvImportConfig
import com.garageledger.domain.model.FuellyCsvImportResult
import com.garageledger.domain.model.FuellyCsvPreview
import com.garageledger.domain.model.FuellyCsvSampleRow
import com.garageledger.domain.model.FuellyImportField
import com.garageledger.domain.model.ImportIssue
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord

class FuellyCsvImporter {
    fun preview(inputStream: InputStream): FuellyCsvPreview {
        val parser = csvFormat().parse(StringReader(InputStreamReader(inputStream).readText()))
        val headers = parser.headerMap.keys.filter { it.isNotBlank() }
        val records = parser.records.toList()
        val suggestedMapping = suggestMapping(headers)
        val issues = mutableListOf<ImportIssue>()

        if (headers.isEmpty()) {
            issues += issue(
                message = "No CSV headers were detected. Choose a file with a header row before importing.",
                section = FUELLY_SECTION,
                severity = ImportIssue.Severity.ERROR,
            )
        }
        if (records.isEmpty()) {
            issues += issue(
                message = "No Fuelly rows were detected in the selected file.",
                section = FUELLY_SECTION,
                severity = ImportIssue.Severity.WARNING,
            )
        }

        val distanceUnit = guessDistanceUnit(suggestedMapping[FuellyImportField.ODOMETER])
        if (distanceUnit == null && headers.isNotEmpty()) {
            issues += issue(
                message = "Distance unit could not be inferred from the CSV headers. Review the unit before importing.",
                section = FUELLY_SECTION,
            )
        }

        val volumeUnit = guessVolumeUnit(suggestedMapping[FuellyImportField.VOLUME])
        if (volumeUnit == null && headers.isNotEmpty()) {
            issues += issue(
                message = "Fuel volume unit could not be inferred from the CSV headers. Review the unit before importing.",
                section = FUELLY_SECTION,
            )
        }

        return FuellyCsvPreview(
            headers = headers,
            sampleRows = records.take(SAMPLE_PREVIEW_ROW_COUNT).map { record ->
                FuellyCsvSampleRow(
                    rowNumber = record.recordNumber.toInt(),
                    values = headers.associateWith { header ->
                        record.valueForHeader(header).orEmpty()
                    },
                )
            },
            suggestedMapping = suggestedMapping,
            suggestedDistanceUnit = distanceUnit,
            suggestedVolumeUnit = volumeUnit,
            issues = issues,
        )
    }

    fun importFillUps(
        inputStream: InputStream,
        config: FuellyCsvImportConfig,
    ): FuellyCsvImportResult {
        val parser = csvFormat().parse(StringReader(InputStreamReader(inputStream).readText()))
        val records = parser.records.toList()
        val issues = mutableListOf<ImportIssue>()
        val fillUps = records.mapNotNull { record ->
            val odometer = parseLooseDouble(value(record, config, FuellyImportField.ODOMETER)) ?: run {
                issues += rowError(record, "Skipped row because odometer could not be parsed.")
                return@mapNotNull null
            }
            val volume = parseLooseDouble(value(record, config, FuellyImportField.VOLUME)) ?: run {
                issues += rowError(record, "Skipped row because fuel volume could not be parsed.")
                return@mapNotNull null
            }
            val totalCost = parseLooseDouble(value(record, config, FuellyImportField.TOTAL_COST)) ?: run {
                issues += rowError(record, "Skipped row because total price could not be parsed.")
                return@mapNotNull null
            }
            val localDate = parseFuellyDate(value(record, config, FuellyImportField.FUEL_UP_DATE)) ?: run {
                issues += rowError(record, "Skipped row because the fuel-up date could not be parsed.")
                return@mapNotNull null
            }

            FillUpRecord(
                legacySourceId = record.recordNumber.toLong(),
                vehicleId = config.vehicleId,
                dateTime = LocalDateTime.of(localDate, LocalTime.NOON),
                odometerReading = odometer,
                distanceUnit = config.distanceUnit,
                volume = volume,
                volumeUnit = config.volumeUnit,
                pricePerUnit = if (volume > 0.0) totalCost / volume else 0.0,
                totalCost = totalCost,
                partial = parseYesNo(value(record, config, FuellyImportField.PARTIAL_FILL_UP)),
                previousMissedFillups = parseYesNo(value(record, config, FuellyImportField.MISSED_FILL_UP)),
                fuelEfficiencyUnit = defaultFuelEfficiencyUnit(config.distanceUnit, config.volumeUnit),
                fuelBrand = value(record, config, FuellyImportField.BRAND).orEmpty(),
                tags = splitCommaList(value(record, config, FuellyImportField.TAGS)),
                notes = value(record, config, FuellyImportField.NOTES).orEmpty(),
                cityDrivingPercentage = parseLooseInt(value(record, config, FuellyImportField.CITY_PERCENTAGE)),
            )
        }

        return FuellyCsvImportResult(
            fillUpRecords = fillUps,
            issues = issues,
        )
    }

    private fun value(
        record: CSVRecord,
        config: FuellyCsvImportConfig,
        field: FuellyImportField,
    ): String? = config.fieldMapping[field]
        ?.takeIf(record::isMapped)
        ?.let(record::get)

    private fun suggestMapping(headers: List<String>): Map<FuellyImportField, String> {
        val matches = mutableMapOf<FuellyImportField, String>()
        val usedHeaders = mutableSetOf<String>()

        FuellyImportField.entries.forEach { field ->
            val directMatch = headers.firstOrNull { header ->
                header !in usedHeaders && normalizeHeader(header) in field.aliases.map(::normalizeHeader)
            }
            val fuzzyMatch = headers.firstOrNull { header ->
                header !in usedHeaders && field.aliases.any { alias ->
                    val normalizedHeader = normalizeHeader(header)
                    val normalizedAlias = normalizeHeader(alias)
                    normalizedHeader.contains(normalizedAlias) || normalizedAlias.contains(normalizedHeader)
                }
            }
            val match = directMatch ?: fuzzyMatch
            if (match != null) {
                matches[field] = match
                usedHeaders += match
            }
        }

        return matches
    }

    private fun guessDistanceUnit(mappedHeader: String?): DistanceUnit? {
        val normalized = normalizeHeader(mappedHeader.orEmpty())
        return when {
            normalized.contains("km") || normalized.contains("kilometer") -> DistanceUnit.KILOMETERS
            normalized.contains("mile") -> DistanceUnit.MILES
            else -> null
        }
    }

    private fun guessVolumeUnit(mappedHeader: String?): VolumeUnit? {
        val normalized = normalizeHeader(mappedHeader.orEmpty())
        return when {
            normalized.contains("liter") || normalized.contains("litre") -> VolumeUnit.LITERS
            normalized.contains("gallon") || normalized == "gal" -> VolumeUnit.GALLONS_US
            else -> null
        }
    }

    private fun parseFuellyDate(raw: String?): LocalDate? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        return FUELLY_DATE_FORMATTERS.firstNotNullOfOrNull { formatter ->
            try {
                LocalDate.parse(value, formatter)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun defaultFuelEfficiencyUnit(
        distanceUnit: DistanceUnit,
        volumeUnit: VolumeUnit,
    ): FuelEfficiencyUnit = when {
        distanceUnit == DistanceUnit.KILOMETERS && volumeUnit == VolumeUnit.LITERS -> FuelEfficiencyUnit.KILOMETERS_PER_LITER
        volumeUnit == VolumeUnit.GALLONS_UK -> FuelEfficiencyUnit.MPG_UK
        else -> FuelEfficiencyUnit.MPG_US
    }

    private fun rowError(record: CSVRecord, message: String): ImportIssue = issue(
        message = message,
        section = FUELLY_SECTION,
        rowNumber = record.recordNumber.toInt(),
        severity = ImportIssue.Severity.ERROR,
    )

    private fun normalizeHeader(raw: String): String = raw
        .trim()
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]"), "")

    private fun CSVRecord.valueForHeader(header: String): String? = if (isMapped(header)) get(header) else null

    private fun csvFormat(): CSVFormat = CSVFormat.DEFAULT.builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setIgnoreEmptyLines(true)
        .build()

    private companion object {
        private const val FUELLY_SECTION = "Fuelly CSV"
        private const val SAMPLE_PREVIEW_ROW_COUNT = 4
        private val FUELLY_DATE_FORMATTERS = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        )
    }
}
