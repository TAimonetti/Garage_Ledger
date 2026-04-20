package com.garageledger.data.importer

import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.FuelEfficiencyUnit
import com.garageledger.core.model.VolumeUnit
import com.garageledger.domain.model.ExpenseRecord
import com.garageledger.domain.model.ExpenseType
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.ImportIssue
import com.garageledger.domain.model.ImportedGarageData
import com.garageledger.domain.model.ServiceRecord
import com.garageledger.domain.model.ServiceType
import com.garageledger.domain.model.TripRecord
import com.garageledger.domain.model.TripType
import com.garageledger.domain.model.Vehicle
import com.garageledger.domain.model.VehicleLifecycle
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.time.Duration
import java.util.Locale
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord

class AcarCsvImporter {
    fun import(inputStream: InputStream): ImportedGarageData {
        val lines = BufferedReader(InputStreamReader(inputStream)).readLines()
        val sections = extractSections(lines)
        val issues = mutableListOf<ImportIssue>()

        val metadataRecords = parseSection(sections["Metadata"])
        val metadata = metadataRecords.firstOrNull()?.toMap().orEmpty()
        val datePattern = metadata["Date Format"].orEmpty().ifBlank { "MM/dd/yyyy" }
        val timePattern = metadata["Time Format"].orEmpty().ifBlank { "hh:mm a" }

        val vehicles = parseVehicles(parseSection(sections["Vehicles"]), datePattern, issues)
        val vehicleIdByName = vehicles.associateBy({ it.name }, { it.legacySourceId ?: 0L })

        var dynamicServiceId = -1L
        val serviceTypeByName = linkedMapOf<String, ServiceType>()
        var dynamicExpenseId = -1_000L
        val expenseTypeByName = linkedMapOf<String, ExpenseType>()
        var dynamicTripTypeId = -2_000L
        val tripTypeByName = linkedMapOf<String, TripType>()

        val fillUps = parseSection(sections["Fill-Up Records"]).mapNotNull { record ->
            val row = record.recordNumber.toInt()
            val vehicleName = record["Vehicle"]
            val vehicleId = vehicleIdByName[vehicleName]
            if (vehicleId == null) {
                issues += issue("Unknown vehicle '$vehicleName' in fill-up section.", "Fill-Up Records", row)
                return@mapNotNull null
            }

            val dateTime = parseDateTime(record["Date"], record["Time"], datePattern, timePattern)
            if (dateTime == null) {
                issues += issue("Invalid fill-up date/time.", "Fill-Up Records", row)
                return@mapNotNull null
            }

            FillUpRecord(
                legacySourceId = row.toLong(),
                vehicleId = vehicleId,
                dateTime = dateTime,
                odometerReading = parseLooseDouble(record["Odometer Reading"]) ?: 0.0,
                distanceUnit = DistanceUnit.fromStorage(record["Distance Unit"]),
                volume = parseLooseDouble(record["Volume"]) ?: 0.0,
                volumeUnit = VolumeUnit.fromStorage(record["Volume Unit"]),
                pricePerUnit = parseLooseDouble(record["Price per Unit"]) ?: 0.0,
                totalCost = parseLooseDouble(record["Total Cost"]) ?: 0.0,
                paymentType = record["Payment"].trim(),
                partial = parseYesNo(record["Partial Fill-Up?"]),
                previousMissedFillups = parseYesNo(record["Previously Missed Fill-Ups?"]),
                importedFuelEfficiency = parseLooseDouble(record["Fuel Efficiency"]),
                fuelEfficiencyUnit = FuelEfficiencyUnit.fromStorage(record["Fuel Efficiency Unit"]),
                importedFuelTypeText = record["Fuel Type"].trim().ifBlank { null },
                hasFuelAdditive = parseYesNo(record["Has Fuel Additive?"]),
                fuelAdditiveName = record["Fuel Additive Name"].trim(),
                fuelBrand = record["Fuel Brand"].trim(),
                stationAddress = record["Fueling Station Address"].trim(),
                latitude = parseLooseDouble(record["Latitude"]),
                longitude = parseLooseDouble(record["Longitude"]),
                drivingMode = record["Driving Mode"].trim().lowercase(Locale.US),
                cityDrivingPercentage = parseLooseInt(record["City Driving Percentage"]),
                highwayDrivingPercentage = parseLooseInt(record["Highway Driving Percentage"]),
                averageSpeed = parseLooseDouble(record["Average Speed"]),
                tags = splitCommaList(record["Tags"]),
                notes = record["Notes"],
            )
        }

        val serviceRecords = parseSection(sections["Service Records"]).mapNotNull { record ->
            val row = record.recordNumber.toInt()
            val vehicleName = record["Vehicle"]
            val vehicleId = vehicleIdByName[vehicleName]
            if (vehicleId == null) {
                issues += issue("Unknown vehicle '$vehicleName' in service section.", "Service Records", row)
                return@mapNotNull null
            }

            val dateTime = parseDateTime(record["Date"], record["Time"], datePattern, timePattern)
            if (dateTime == null) {
                issues += issue("Invalid service date/time.", "Service Records", row)
                return@mapNotNull null
            }

            val serviceTypeIds = splitCommaList(record["Services"]).map { name ->
                serviceTypeByName.getOrPut(name) {
                    ServiceType(
                        legacySourceId = dynamicServiceId--,
                        name = name,
                    )
                }.legacySourceId ?: 0L
            }

            ServiceRecord(
                legacySourceId = row.toLong(),
                vehicleId = vehicleId,
                dateTime = dateTime,
                odometerReading = parseLooseDouble(record["Odometer Reading"]) ?: 0.0,
                distanceUnit = DistanceUnit.fromStorage(record["Distance Unit"]),
                totalCost = parseLooseDouble(record["Total Cost"]) ?: 0.0,
                paymentType = record["Payment"].trim(),
                serviceCenterName = record["Service Center Name"].trim(),
                serviceCenterAddress = record["Service Center Address"].trim(),
                latitude = parseLooseDouble(record["Latitude"]),
                longitude = parseLooseDouble(record["Longitude"]),
                tags = splitCommaList(record["Tags"]),
                notes = record["Notes"],
                serviceTypeIds = serviceTypeIds,
            )
        }

        val expenseRecords = parseSection(sections["Expense Records"]).mapNotNull { record ->
            val row = record.recordNumber.toInt()
            val vehicleName = record["Vehicle"]
            val vehicleId = vehicleIdByName[vehicleName]
            if (vehicleId == null) {
                issues += issue("Unknown vehicle '$vehicleName' in expense section.", "Expense Records", row)
                return@mapNotNull null
            }

            val dateTime = parseDateTime(record["Date"], record["Time"], datePattern, timePattern)
            if (dateTime == null) {
                issues += issue("Invalid expense date/time.", "Expense Records", row)
                return@mapNotNull null
            }

            val expenseTypeIds = splitCommaList(record["Expenses"]).map { name ->
                expenseTypeByName.getOrPut(name) {
                    ExpenseType(
                        legacySourceId = dynamicExpenseId--,
                        name = name,
                    )
                }.legacySourceId ?: 0L
            }

            ExpenseRecord(
                legacySourceId = row.toLong(),
                vehicleId = vehicleId,
                dateTime = dateTime,
                odometerReading = parseLooseDouble(record["Odometer Reading"]) ?: 0.0,
                distanceUnit = DistanceUnit.fromStorage(record["Distance Unit"]),
                totalCost = parseLooseDouble(record["Total Cost"]) ?: 0.0,
                paymentType = record["Payment"].trim(),
                expenseCenterName = record["Expense Center Name"].trim(),
                expenseCenterAddress = record["Expense Center Address"].trim(),
                latitude = parseLooseDouble(record["Latitude"]),
                longitude = parseLooseDouble(record["Longitude"]),
                tags = splitCommaList(record["Tags"]),
                notes = record["Notes"],
                expenseTypeIds = expenseTypeIds,
            )
        }

        val tripRecords = parseSection(sections["Trip Records"]).mapNotNull { record ->
            val row = record.recordNumber.toInt()
            val vehicleName = record["Vehicle"]
            val vehicleId = vehicleIdByName[vehicleName]
            if (vehicleId == null) {
                issues += issue("Unknown vehicle '$vehicleName' in trip section.", "Trip Records", row)
                return@mapNotNull null
            }

            val startDateTime = parseDateTime(record["Start Date"], record["Start Time"], datePattern, timePattern)
            if (startDateTime == null) {
                issues += issue("Invalid trip start date/time.", "Trip Records", row)
                return@mapNotNull null
            }

            val endDateTime = parseDateTime(record["End Date"], record["End Time"], datePattern, timePattern)
            val tripTypeId = record["Type"].trim().takeIf { it.isNotEmpty() }?.let { name ->
                tripTypeByName.getOrPut(name) {
                    TripType(
                        legacySourceId = dynamicTripTypeId--,
                        name = name,
                        defaultTaxDeductionRate = parseLooseDouble(record["Tax Deduction Rate"]),
                    )
                }.legacySourceId
            }

            TripRecord(
                legacySourceId = row.toLong(),
                vehicleId = vehicleId,
                startDateTime = startDateTime,
                startOdometerReading = parseLooseDouble(record["Start Odometer Reading"]) ?: 0.0,
                startLocation = record["Start Location"].trim(),
                endDateTime = endDateTime,
                endOdometerReading = parseLooseDouble(record["End Odometer Reading"]),
                endLocation = record["End Location"].trim(),
                startLatitude = parseLooseDouble(record["Start Latitude"]),
                startLongitude = parseLooseDouble(record["Start Longitude"]),
                endLatitude = parseLooseDouble(record["End Latitude"]),
                endLongitude = parseLooseDouble(record["End Longitude"]),
                distanceUnit = DistanceUnit.fromStorage(record["Distance Unit"]),
                distance = parseLooseDouble(record["Distance"]),
                durationMillis = parseDuration(record["Duration"]),
                tripTypeId = tripTypeId,
                purpose = record["Purpose"].trim(),
                client = record["Client"].trim(),
                taxDeductionRate = parseLooseDouble(record["Tax Deduction Rate"]),
                taxDeductionAmount = parseLooseDouble(record["Tax Deduction Amount"]),
                reimbursementRate = parseLooseDouble(record["Reimbursement Rate"]),
                reimbursementAmount = parseLooseDouble(record["Reimbursement Amount"]),
                paid = parseYesNo(record["Paid"]),
                tags = splitCommaList(record["Tags"]),
                notes = record["Notes"],
            )
        }

        return ImportedGarageData(
            metadata = metadata,
            vehicles = vehicles,
            fillUpRecords = fillUps,
            serviceTypes = serviceTypeByName.values.toList(),
            serviceRecords = serviceRecords,
            expenseTypes = expenseTypeByName.values.toList(),
            expenseRecords = expenseRecords,
            tripTypes = tripTypeByName.values.toList(),
            tripRecords = tripRecords,
            issues = issues,
        )
    }

    private fun parseVehicles(
        records: List<CSVRecord>,
        datePattern: String,
        issues: MutableList<ImportIssue>,
    ): List<Vehicle> = records.mapNotNull { record ->
        val row = record.recordNumber.toInt()
        val name = record["Name"].trim()
        if (name.isEmpty()) {
            issues += issue("Vehicle row without a name was skipped.", "Vehicles", row)
            return@mapNotNull null
        }
        Vehicle(
            legacySourceId = row.toLong(),
            name = name,
            make = record["Make"].trim(),
            model = record["Model"].trim(),
            lifecycle = if (parseYesNo(record["Active"])) VehicleLifecycle.ACTIVE else VehicleLifecycle.RETIRED,
            year = record["Year"].trim().toIntOrNull(),
            licensePlate = record["License Plate"].trim(),
            vin = record["VIN"].trim(),
            insurancePolicy = record["Insurance Policy"].trim(),
            bodyStyle = record["Body Style"].trim(),
            color = record["Color"].trim(),
            engineDisplacement = record["Engine Displacement"].trim(),
            fuelTankCapacity = parseLooseDouble(record["Fuel Tank Capacity"]),
            purchasePrice = parseLooseDouble(record["Purchase Price"]),
            purchaseOdometer = parseLooseDouble(record["Purchase Odometer Reading"]),
            purchaseDate = parseDate(record["Purchase Date"], datePattern),
            sellingPrice = parseLooseDouble(record["Selling Price"]),
            sellingOdometer = parseLooseDouble(record["Selling Odometer Reading"]),
            sellingDate = parseDate(record["Selling Date"], datePattern),
            notes = record["Notes"],
        )
    }

    private fun extractSections(lines: List<String>): Map<String, String> {
        val sections = linkedMapOf<String, String>()
        var index = 0
        while (index < lines.size) {
            val line = lines[index].trim()
            if (line.isEmpty()) {
                index++
                continue
            }
            if (!line.startsWith("\"")) {
                val sectionName = line
                index++
                while (index < lines.size && lines[index].isBlank()) {
                    index++
                }
                if (index >= lines.size || !lines[index].trimStart().startsWith("\"")) {
                    continue
                }
                val buffer = mutableListOf(lines[index])
                index++
                while (index < lines.size && lines[index].trimStart().startsWith("\"")) {
                    buffer += lines[index]
                    index++
                }
                sections[sectionName] = buffer.joinToString("\n")
                continue
            }
            index++
        }
        return sections
    }

    private fun parseSection(rawSection: String?): List<CSVRecord> {
        if (rawSection.isNullOrBlank()) return emptyList()
        val format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .build()
        return format.parse(StringReader(rawSection)).records
    }

    private fun parseDuration(raw: String?): Long? {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return null
        val parts = text.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            3 -> Duration.ofHours(parts[0]).plusMinutes(parts[1]).plusSeconds(parts[2]).toMillis()
            2 -> Duration.ofMinutes(parts[0]).plusSeconds(parts[1]).toMillis()
            else -> null
        }
    }
}
