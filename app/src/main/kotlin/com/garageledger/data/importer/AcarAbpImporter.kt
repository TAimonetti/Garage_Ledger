package com.garageledger.data.importer

import com.garageledger.domain.model.AppPreferenceSnapshot
import com.garageledger.domain.model.ExpenseRecord
import com.garageledger.domain.model.ExpenseType
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.FuelType
import com.garageledger.domain.model.ImportIssue
import com.garageledger.domain.model.ImportedGarageData
import com.garageledger.domain.model.ServiceRecord
import com.garageledger.domain.model.ServiceReminder
import com.garageledger.domain.model.ServiceType
import com.garageledger.domain.model.TripRecord
import com.garageledger.domain.model.TripType
import com.garageledger.domain.model.Vehicle
import com.garageledger.domain.model.VehicleLifecycle
import com.garageledger.domain.model.VehiclePart
import java.io.InputStream
import java.time.Duration
import java.util.zip.ZipInputStream

class AcarAbpImporter {
    fun import(inputStream: InputStream): ImportedGarageData {
        val archiveEntries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                archiveEntries[entry.name] = zip.readBytes()
            }
        }

        val metadata = archiveEntries["metadata.inf"]?.inputStream()?.use(::readProperties).orEmpty()
        val preferences = archiveEntries["preferences.xml"]?.inputStream()?.use { stream ->
            val document = readXml(stream)
            val values = document.rootElement().childElements("preference").associate { element ->
                element.getAttribute("name") to element.textContent.trim()
            }
            preferencesFromMap(values)
        }

        val serviceTypes = parseServices(archiveEntries["services.xml"]?.inputStream())
        val expenseTypes = parseExpenses(archiveEntries["expenses.xml"]?.inputStream())
        val tripTypes = parseTripTypes(archiveEntries["trip-types.xml"]?.inputStream())
        val fuelTypes = parseFuelTypes(archiveEntries["fuel-types.xml"]?.inputStream())
        val vehicleBundle = parseVehicles(archiveEntries["vehicles.xml"]?.inputStream(), preferences ?: AppPreferenceSnapshot())

        return ImportedGarageData(
            metadata = metadata,
            preferences = preferences,
            vehicles = vehicleBundle.vehicles,
            vehicleParts = vehicleBundle.parts,
            fuelTypes = fuelTypes,
            serviceTypes = serviceTypes,
            expenseTypes = expenseTypes,
            tripTypes = tripTypes,
            serviceReminders = vehicleBundle.reminders,
            fillUpRecords = vehicleBundle.fillUps,
            serviceRecords = vehicleBundle.services,
            expenseRecords = vehicleBundle.expenses,
            tripRecords = vehicleBundle.trips,
            issues = vehicleBundle.issues,
        )
    }

    private fun parseServices(inputStream: InputStream?): List<ServiceType> {
        if (inputStream == null) return emptyList()
        return readXml(inputStream).rootElement().childElements("service").map { element ->
            ServiceType(
                legacySourceId = element.getAttribute("id").toLongOrNull(),
                name = element.childText("name"),
                notes = element.childText("notes"),
                defaultTimeReminderMonths = element.childText("time-reminder").toIntOrNull() ?: 0,
                defaultDistanceReminder = parseLooseDouble(element.childText("distance-reminder")),
            )
        }
    }

    private fun parseExpenses(inputStream: InputStream?): List<ExpenseType> {
        if (inputStream == null) return emptyList()
        return readXml(inputStream).rootElement().childElements("expense").map { element ->
            ExpenseType(
                legacySourceId = element.getAttribute("id").toLongOrNull(),
                name = element.childText("name"),
                notes = element.childText("notes"),
            )
        }
    }

    private fun parseTripTypes(inputStream: InputStream?): List<TripType> {
        if (inputStream == null) return emptyList()
        return readXml(inputStream).rootElement().childElements("trip-type").map { element ->
            TripType(
                legacySourceId = element.getAttribute("id").toLongOrNull(),
                name = element.childText("name"),
                defaultTaxDeductionRate = parseLooseDouble(element.childText("default-tax-deduction-rate")),
                notes = element.childText("notes"),
            )
        }
    }

    private fun parseFuelTypes(inputStream: InputStream?): List<FuelType> {
        if (inputStream == null) return emptyList()
        return readXml(inputStream).rootElement().childElements("fuel-type").map { element ->
            FuelType(
                legacySourceId = element.getAttribute("id").toLongOrNull(),
                category = element.childText("category"),
                grade = element.childText("grade"),
                octane = parseLooseInt(element.childText("octane")),
                cetane = parseLooseInt(element.childText("cetane")),
                notes = element.childText("notes"),
            )
        }
    }

    private fun parseVehicles(
        inputStream: InputStream?,
        preferences: AppPreferenceSnapshot,
    ): VehicleBundle {
        if (inputStream == null) return VehicleBundle()
        val root = readXml(inputStream).rootElement()
        val issues = mutableListOf<ImportIssue>()
        val vehicles = mutableListOf<Vehicle>()
        val parts = mutableListOf<VehiclePart>()
        val reminders = mutableListOf<ServiceReminder>()
        val fillUps = mutableListOf<FillUpRecord>()
        val services = mutableListOf<ServiceRecord>()
        val expenses = mutableListOf<ExpenseRecord>()
        val trips = mutableListOf<TripRecord>()

        root.childElements("vehicle").forEach { vehicleElement ->
            val vehicleLegacyId = vehicleElement.getAttribute("id").toLongOrNull() ?: 0L
            vehicles += Vehicle(
                legacySourceId = vehicleLegacyId,
                name = vehicleElement.childText("name"),
                make = vehicleElement.childText("make"),
                model = vehicleElement.childText("model"),
                year = vehicleElement.childText("year").toIntOrNull(),
                licensePlate = vehicleElement.childText("license-plate"),
                vin = vehicleElement.childText("vin"),
                insurancePolicy = vehicleElement.childText("insurance-policy"),
                bodyStyle = vehicleElement.childText("body-style"),
                color = vehicleElement.childText("color"),
                engineDisplacement = vehicleElement.childText("engine-displacement"),
                fuelTankCapacity = parseLooseDouble(vehicleElement.childText("fuel-tank-capacity")),
                purchasePrice = parseLooseDouble(vehicleElement.childText("purchase-price")),
                purchaseOdometer = parseLooseDouble(vehicleElement.childText("purchase-odometer-reading")),
                purchaseDate = parseDate(vehicleElement.childText("purchase-date"), "MM/dd/yyyy"),
                sellingPrice = parseLooseDouble(vehicleElement.childText("selling-price")),
                sellingOdometer = parseLooseDouble(vehicleElement.childText("selling-odometer-reading")),
                sellingDate = parseDate(vehicleElement.childText("selling-date"), "MM/dd/yyyy"),
                lifecycle = if (vehicleElement.childText("active").equals("true", ignoreCase = true)) VehicleLifecycle.ACTIVE else VehicleLifecycle.RETIRED,
                notes = vehicleElement.childText("notes"),
                distanceUnitOverride = preferences.distanceUnit,
                volumeUnitOverride = preferences.volumeUnit,
                fuelEfficiencyUnitOverride = preferences.fuelEfficiencyUnit,
            )

            vehicleElement.firstChildElement("vehicle-parts")?.childElements("vehicle-part")?.forEach { partElement ->
                parts += VehiclePart(
                    legacySourceId = partElement.getAttribute("id").toLongOrNull(),
                    vehicleId = vehicleLegacyId,
                    name = partElement.childText("name"),
                    partNumber = partElement.childText("part-number"),
                    type = partElement.childText("type"),
                    brand = partElement.childText("brand"),
                    color = partElement.childText("color"),
                    size = partElement.childText("size"),
                    volume = parseLooseDouble(partElement.childText("volume")),
                    pressure = parseLooseDouble(partElement.childText("pressure")),
                    quantity = partElement.childText("quantity").toIntOrNull(),
                    notes = partElement.childText("notes"),
                )
            }

            vehicleElement.firstChildElement("service-reminders")?.childElements("service-reminder")?.forEach { reminderElement ->
                reminders += ServiceReminder(
                    legacySourceId = reminderElement.getAttribute("id").toLongOrNull(),
                    vehicleId = vehicleLegacyId,
                    serviceTypeId = reminderElement.getAttribute("service-id").toLongOrNull() ?: 0L,
                    intervalTimeMonths = reminderElement.childText("time").toIntOrNull() ?: 0,
                    intervalDistance = parseLooseDouble(reminderElement.childText("distance")),
                    timeAlertSilent = reminderElement.childText("time-alert-silent").equals("true", ignoreCase = true),
                    distanceAlertSilent = reminderElement.childText("distance-alert-silent").equals("true", ignoreCase = true),
                    lastTimeAlert = parseBackupDateTime(reminderElement.childText("last-time-alert")),
                    lastDistanceAlert = parseBackupDateTime(reminderElement.childText("last-distance-alert")),
                )
            }

            vehicleElement.firstChildElement("fillup-records")?.childElements("fillup-record")?.forEach { fillupElement ->
                val dateTime = parseBackupDateTime(fillupElement.childText("date"))
                if (dateTime == null) {
                    issues += issue("Skipped fill-up with invalid date in vehicles.xml.", "vehicles.xml")
                    return@forEach
                }
                fillUps += FillUpRecord(
                    legacySourceId = fillupElement.getAttribute("id").toLongOrNull(),
                    vehicleId = vehicleLegacyId,
                    dateTime = dateTime,
                    odometerReading = parseLooseDouble(fillupElement.childText("odometer-reading")) ?: 0.0,
                    distanceUnit = preferences.distanceUnit,
                    volume = parseLooseDouble(fillupElement.childText("volume")) ?: 0.0,
                    volumeUnit = preferences.volumeUnit,
                    pricePerUnit = parseLooseDouble(fillupElement.childText("price-per-volume-unit")) ?: 0.0,
                    totalCost = parseLooseDouble(fillupElement.childText("total-cost")) ?: 0.0,
                    paymentType = fillupElement.childText("payment-type"),
                    partial = fillupElement.childText("partial").equals("true", ignoreCase = true),
                    previousMissedFillups = fillupElement.childText("previous-missed-fillups").equals("true", ignoreCase = true),
                    fuelEfficiency = parseLooseDouble(fillupElement.childText("fuel-efficiency")),
                    importedFuelEfficiency = parseLooseDouble(fillupElement.childText("fuel-efficiency")),
                    fuelEfficiencyUnit = preferences.fuelEfficiencyUnit,
                    fuelTypeId = fillupElement.childText("fuel-type-id").toLongOrNull(),
                    hasFuelAdditive = fillupElement.childText("has-fuel-additive").equals("true", ignoreCase = true),
                    fuelAdditiveName = fillupElement.childText("fuel-additive-name"),
                    fuelBrand = fillupElement.childText("fuel-brand"),
                    stationAddress = fillupElement.childText("fueling-station-address"),
                    latitude = parseLooseDouble(fillupElement.childText("latitude")),
                    longitude = parseLooseDouble(fillupElement.childText("longitude")),
                    drivingMode = fillupElement.childText("driving-mode"),
                    cityDrivingPercentage = parseLooseInt(fillupElement.childText("city-driving-percentage")),
                    highwayDrivingPercentage = parseLooseInt(fillupElement.childText("highway-driving-percentage")),
                    averageSpeed = parseLooseDouble(fillupElement.childText("average-speed")),
                    tags = splitCommaList(fillupElement.childText("tags")),
                    notes = fillupElement.childText("notes"),
                    distanceSincePrevious = parseLooseDouble(fillupElement.childText("driven-distance")),
                    distanceTillNextFillUp = parseLooseDouble(fillupElement.childText("distance-till-next-fillup")),
                    timeSincePreviousMillis = fillupElement.childText("time-since-previous-fillup").toLongOrNull(),
                    timeTillNextFillUpMillis = fillupElement.childText("time-till-next-fillup").toLongOrNull(),
                    distanceForFuelEfficiency = parseLooseDouble(fillupElement.childText("distance-for-fuel-efficiency")),
                    volumeForFuelEfficiency = parseLooseDouble(fillupElement.childText("volume-for-fuel-efficiency")),
                )
            }

            vehicleElement.firstChildElement("service-records")?.childElements("service-record")?.forEach { serviceElement ->
                val dateTime = parseBackupDateTime(serviceElement.childText("date")) ?: return@forEach
                services += ServiceRecord(
                    legacySourceId = serviceElement.getAttribute("id").toLongOrNull(),
                    vehicleId = vehicleLegacyId,
                    dateTime = dateTime,
                    odometerReading = parseLooseDouble(serviceElement.childText("odometer-reading")) ?: 0.0,
                    distanceUnit = preferences.distanceUnit,
                    totalCost = parseLooseDouble(serviceElement.childText("total-cost")) ?: 0.0,
                    paymentType = serviceElement.childText("payment-type"),
                    serviceCenterName = serviceElement.childText("service-center-name"),
                    serviceCenterAddress = serviceElement.childText("service-center-address"),
                    latitude = parseLooseDouble(serviceElement.childText("latitude")),
                    longitude = parseLooseDouble(serviceElement.childText("longitude")),
                    tags = splitCommaList(serviceElement.childText("tags")),
                    notes = serviceElement.childText("notes"),
                    serviceTypeIds = serviceElement.firstChildElement("services")
                        ?.childElements("service")
                        ?.mapNotNull { it.getAttribute("id").toLongOrNull() }
                        .orEmpty(),
                )
            }

            vehicleElement.firstChildElement("expense-records")?.childElements("expense-record")?.forEach { expenseElement ->
                val dateTime = parseBackupDateTime(expenseElement.childText("date")) ?: return@forEach
                expenses += ExpenseRecord(
                    legacySourceId = expenseElement.getAttribute("id").toLongOrNull(),
                    vehicleId = vehicleLegacyId,
                    dateTime = dateTime,
                    odometerReading = parseLooseDouble(expenseElement.childText("odometer-reading")) ?: 0.0,
                    distanceUnit = preferences.distanceUnit,
                    totalCost = parseLooseDouble(expenseElement.childText("total-cost")) ?: 0.0,
                    paymentType = expenseElement.childText("payment-type"),
                    expenseCenterName = expenseElement.childText("expense-center-name"),
                    expenseCenterAddress = expenseElement.childText("expense-center-address"),
                    latitude = parseLooseDouble(expenseElement.childText("latitude")),
                    longitude = parseLooseDouble(expenseElement.childText("longitude")),
                    tags = splitCommaList(expenseElement.childText("tags")),
                    notes = expenseElement.childText("notes"),
                    expenseTypeIds = expenseElement.firstChildElement("expenses")
                        ?.childElements("expense")
                        ?.mapNotNull { it.getAttribute("id").toLongOrNull() }
                        .orEmpty(),
                )
            }

            vehicleElement.firstChildElement("trip-records")?.childElements("trip-record")?.forEach { tripElement ->
                val startDateTime = parseBackupDateTime(tripElement.childText("start-date")) ?: return@forEach
                val endDateTime = parseBackupDateTime(tripElement.childText("end-date"))
                trips += TripRecord(
                    legacySourceId = tripElement.getAttribute("id").toLongOrNull(),
                    vehicleId = vehicleLegacyId,
                    startDateTime = startDateTime,
                    startOdometerReading = parseLooseDouble(tripElement.childText("start-odometer-reading")) ?: 0.0,
                    startLocation = tripElement.childText("start-location"),
                    endDateTime = endDateTime,
                    endOdometerReading = parseLooseDouble(tripElement.childText("end-odometer-reading")),
                    endLocation = tripElement.childText("end-location"),
                    startLatitude = parseLooseDouble(tripElement.childText("start-latitude")),
                    startLongitude = parseLooseDouble(tripElement.childText("start-longitude")),
                    endLatitude = parseLooseDouble(tripElement.childText("end-latitude")),
                    endLongitude = parseLooseDouble(tripElement.childText("end-longitude")),
                    distanceUnit = preferences.distanceUnit,
                    distance = parseLooseDouble(tripElement.childText("distance")),
                    durationMillis = tripElement.childText("duration").toLongOrNull()?.let { Duration.ofMillis(it).toMillis() },
                    tripTypeId = tripElement.childText("trip-type-id").toLongOrNull(),
                    purpose = tripElement.childText("purpose"),
                    client = tripElement.childText("client"),
                    taxDeductionRate = parseLooseDouble(tripElement.childText("tax-deduction-rate")),
                    taxDeductionAmount = parseLooseDouble(tripElement.childText("tax-deduction-amount")),
                    reimbursementRate = parseLooseDouble(tripElement.childText("reimbursement-rate")),
                    reimbursementAmount = parseLooseDouble(tripElement.childText("reimbursement-amount")),
                    paid = tripElement.childText("paid").equals("true", ignoreCase = true),
                    tags = splitCommaList(tripElement.childText("tags")),
                    notes = tripElement.childText("notes"),
                )
            }
        }

        return VehicleBundle(
            vehicles = vehicles,
            parts = parts,
            reminders = reminders,
            fillUps = fillUps,
            services = services,
            expenses = expenses,
            trips = trips,
            issues = issues,
        )
    }

    private data class VehicleBundle(
        val vehicles: List<Vehicle> = emptyList(),
        val parts: List<VehiclePart> = emptyList(),
        val reminders: List<ServiceReminder> = emptyList(),
        val fillUps: List<FillUpRecord> = emptyList(),
        val services: List<ServiceRecord> = emptyList(),
        val expenses: List<ExpenseRecord> = emptyList(),
        val trips: List<TripRecord> = emptyList(),
        val issues: List<ImportIssue> = emptyList(),
    )
}
