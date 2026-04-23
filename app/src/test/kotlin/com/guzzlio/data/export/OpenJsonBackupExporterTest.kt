package com.guzzlio.data.export

import com.guzzlio.domain.model.AppPreferenceSnapshot
import com.guzzlio.data.local.ExpenseRecordEntity
import com.guzzlio.data.local.ExpenseRecordTypeCrossRef
import com.guzzlio.data.local.ExpenseTypeEntity
import com.guzzlio.data.local.FillUpRecordEntity
import com.guzzlio.data.local.FuelTypeEntity
import com.guzzlio.data.local.RecordAttachmentEntity
import com.guzzlio.data.local.ServiceRecordEntity
import com.guzzlio.data.local.ServiceRecordTypeCrossRef
import com.guzzlio.data.local.ServiceReminderEntity
import com.guzzlio.data.local.ServiceTypeEntity
import com.guzzlio.data.local.TripRecordEntity
import com.guzzlio.data.local.TripTypeEntity
import com.guzzlio.data.local.VehicleEntity
import com.guzzlio.data.local.VehiclePartEntity
import com.guzzlio.domain.model.BrowseTripPaidStatus
import com.guzzlio.domain.model.RecordFamily
import com.guzzlio.domain.model.SavedBrowseSearch
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipInputStream
import org.junit.Test

class OpenJsonBackupExporterTest {
    @Test
    fun export_writesMetadataAndPayloadEntries() {
        val snapshot = ExportSnapshot(
            preferences = AppPreferenceSnapshot(
                currencySymbol = "$",
                backupHistoryCount = 5,
                savedBrowseSearches = listOf(
                    SavedBrowseSearch(
                        name = "Fuel This Year",
                        family = com.guzzlio.domain.model.RecordFamily.FILL_UP,
                        fromDateIso = "2026-01-01",
                        toDateIso = "2026-04-20",
                    ),
                    SavedBrowseSearch(
                        name = "Unpaid Trips",
                        family = com.guzzlio.domain.model.RecordFamily.TRIP,
                        tripPaidStatus = BrowseTripPaidStatus.UNPAID,
                    ),
                ),
            ),
            vehicles = listOf(
                VehicleEntity(
                    id = 1L,
                    name = "Corolla",
                    type = "Car",
                    year = 2010,
                    make = "Toyota",
                    model = "Corolla",
                    submodel = "",
                    lifecycle = "ACTIVE",
                    country = "US",
                    licensePlate = "ABC123",
                    vin = "VIN123",
                    insurancePolicy = "",
                    bodyStyle = "",
                    color = "Blue",
                    engineDisplacement = "1.8L",
                    fuelTankCapacity = 13.2,
                    purchasePrice = null,
                    purchaseOdometer = null,
                    purchaseDate = null,
                    sellingPrice = null,
                    sellingOdometer = null,
                    sellingDate = null,
                    notes = "Primary car",
                    profilePhotoUri = null,
                    distanceUnitOverride = null,
                    volumeUnitOverride = null,
                    fuelEfficiencyUnitOverride = null,
                ),
            ),
            vehicleParts = listOf(
                VehiclePartEntity(
                    id = 4L,
                    vehicleId = 1L,
                    name = "Tire",
                    partNumber = "P123",
                    type = "Wheel",
                    brand = "Brand",
                    color = "Black",
                    size = "16",
                    volume = null,
                    pressure = 32.0,
                    quantity = 4,
                    notes = "",
                ),
            ),
            fuelTypes = listOf(FuelTypeEntity(id = 2L, category = "Gasoline", grade = "Regular", octane = 87, cetane = null, notes = "")),
            serviceTypes = listOf(ServiceTypeEntity(id = 3L, name = "Oil Change", notes = "", defaultTimeReminderMonths = 6, defaultDistanceReminder = 5000.0)),
            expenseTypes = listOf(ExpenseTypeEntity(id = 4L, name = "Registration", notes = "")),
            tripTypes = listOf(TripTypeEntity(id = 5L, name = "Business", defaultTaxDeductionRate = 0.67, notes = "")),
            serviceReminders = listOf(
                ServiceReminderEntity(
                    id = 6L,
                    vehicleId = 1L,
                    serviceTypeId = 3L,
                    intervalTimeMonths = 6,
                    intervalDistance = 5000.0,
                    dueDate = LocalDate.parse("2026-10-01"),
                    dueDistance = 130000.0,
                    timeAlertSilent = false,
                    distanceAlertSilent = false,
                    lastTimeAlert = null,
                    lastDistanceAlert = null,
                ),
            ),
            fillUpRecords = listOf(
                FillUpRecordEntity(
                    id = 7L,
                    vehicleId = 1L,
                    dateTime = LocalDateTime.parse("2026-04-20T09:00:00"),
                    odometerReading = 125000.0,
                    distanceUnit = "mi",
                    volume = 10.0,
                    volumeUnit = "gal (US)",
                    pricePerUnit = 3.5,
                    totalCost = 35.0,
                    paymentType = "Visa",
                    partial = false,
                    previousMissedFillups = false,
                    fuelEfficiency = 32.0,
                    fuelEfficiencyUnit = "MPG (US)",
                    importedFuelEfficiency = null,
                    fuelTypeId = 2L,
                    importedFuelTypeText = "Regular",
                    hasFuelAdditive = false,
                    fuelAdditiveName = "",
                    fuelBrand = "Chevron",
                    stationAddress = "123 Main",
                    latitude = null,
                    longitude = null,
                    drivingMode = "mixed",
                    cityDrivingPercentage = 50,
                    highwayDrivingPercentage = 50,
                    averageSpeed = null,
                    tags = listOf("commute"),
                    notes = "Morning fill",
                    distanceSincePrevious = 320.0,
                    distanceTillNextFillUp = null,
                    timeSincePreviousMillis = null,
                    timeTillNextFillUpMillis = null,
                    distanceForFuelEfficiency = 320.0,
                    volumeForFuelEfficiency = 10.0,
                ),
            ),
            serviceRecords = listOf(
                ServiceRecordEntity(
                    id = 8L,
                    vehicleId = 1L,
                    dateTime = LocalDateTime.parse("2026-04-01T12:00:00"),
                    odometerReading = 124500.0,
                    distanceUnit = "mi",
                    totalCost = 89.0,
                    paymentType = "Cash",
                    serviceCenterName = "Quick Lube",
                    serviceCenterAddress = "456 Service Rd",
                    latitude = null,
                    longitude = null,
                    tags = listOf("oil"),
                    notes = "",
                ),
            ),
            serviceRecordTypes = listOf(ServiceRecordTypeCrossRef(serviceRecordId = 8L, serviceTypeId = 3L)),
            expenseRecords = listOf(
                ExpenseRecordEntity(
                    id = 9L,
                    vehicleId = 1L,
                    dateTime = LocalDateTime.parse("2026-03-01T10:00:00"),
                    odometerReading = 124000.0,
                    distanceUnit = "mi",
                    totalCost = 120.0,
                    paymentType = "Visa",
                    expenseCenterName = "DMV",
                    expenseCenterAddress = "789 State St",
                    latitude = null,
                    longitude = null,
                    tags = listOf("fees"),
                    notes = "",
                ),
            ),
            expenseRecordTypes = listOf(ExpenseRecordTypeCrossRef(expenseRecordId = 9L, expenseTypeId = 4L)),
            tripRecords = listOf(
                TripRecordEntity(
                    id = 10L,
                    vehicleId = 1L,
                    startDateTime = LocalDateTime.parse("2026-04-19T08:00:00"),
                    startOdometerReading = 124900.0,
                    startLocation = "Home",
                    endDateTime = LocalDateTime.parse("2026-04-19T09:00:00"),
                    endOdometerReading = 124930.0,
                    endLocation = "Office",
                    startLatitude = null,
                    startLongitude = null,
                    endLatitude = null,
                    endLongitude = null,
                    distanceUnit = "mi",
                    distance = 30.0,
                    durationMillis = 3_600_000L,
                    tripTypeId = 5L,
                    purpose = "Meeting",
                    client = "Client A",
                    taxDeductionRate = 0.67,
                    taxDeductionAmount = 20.1,
                    reimbursementRate = null,
                    reimbursementAmount = null,
                    paid = false,
                    tags = listOf("work"),
                    notes = "",
                ),
            ),
            attachments = listOf(
                RecordAttachmentEntity(
                    id = 11L,
                    vehicleId = 1L,
                    recordFamily = RecordFamily.SERVICE,
                    recordId = 8L,
                    uri = "content://receipt",
                    mimeType = "application/pdf",
                    displayName = "receipt.pdf",
                    createdAt = LocalDateTime.parse("2026-04-01T12:15:00"),
                ),
            ),
        )

        val bytes = ByteArrayOutputStream()
        OpenJsonBackupExporter().export(
            snapshot = snapshot,
            outputStream = bytes,
            exportedAt = LocalDateTime.parse("2026-04-20T10:15:30"),
        )

        val entries = mutableMapOf<String, String>()
        ZipInputStream(bytes.toByteArray().inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }

        assertThat(entries.keys).containsExactly("metadata.json", "guzzlio-backup.json")
        assertThat(entries["metadata.json"]).contains("\"vehicleCount\": 1")
        assertThat(entries["metadata.json"]).contains("\"fillUpCount\": 1")
        assertThat(entries["guzzlio-backup.json"]).contains("\"name\": \"Corolla\"")
        assertThat(entries["guzzlio-backup.json"]).doesNotContain("\"serviceTypeName\"")
        assertThat(entries["guzzlio-backup.json"]).contains("\"recordFamily\": \"SERVICE\"")
        assertThat(entries["guzzlio-backup.json"]).contains("\"serviceRecordTypes\"")
        assertThat(entries["guzzlio-backup.json"]).contains("\"savedBrowseSearches\"")
        assertThat(entries["guzzlio-backup.json"]).contains("\"name\": \"Fuel This Year\"")
        assertThat(entries["guzzlio-backup.json"]).contains("\"tripPaidStatus\": \"UNPAID\"")
    }
}
