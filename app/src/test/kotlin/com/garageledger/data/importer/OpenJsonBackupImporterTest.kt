package com.garageledger.data.importer

import com.garageledger.data.export.ExportSnapshot
import com.garageledger.data.export.OpenJsonBackupExporter
import com.garageledger.data.local.ExpenseRecordEntity
import com.garageledger.data.local.ExpenseRecordTypeCrossRef
import com.garageledger.data.local.ExpenseTypeEntity
import com.garageledger.data.local.FillUpRecordEntity
import com.garageledger.data.local.FuelTypeEntity
import com.garageledger.data.local.RecordAttachmentEntity
import com.garageledger.data.local.ServiceRecordEntity
import com.garageledger.data.local.ServiceRecordTypeCrossRef
import com.garageledger.data.local.ServiceReminderEntity
import com.garageledger.data.local.ServiceTypeEntity
import com.garageledger.data.local.TripRecordEntity
import com.garageledger.data.local.TripTypeEntity
import com.garageledger.data.local.VehicleEntity
import com.garageledger.data.local.VehiclePartEntity
import com.garageledger.domain.model.AppPreferenceSnapshot
import com.garageledger.domain.model.OptionalFieldToggle
import com.garageledger.domain.model.RecordFamily
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Test

class OpenJsonBackupImporterTest {
    @Test
    fun import_roundTripsExporterPayloadAndMapsReferencesToSourceKeys() {
        val snapshot = ExportSnapshot(
            preferences = AppPreferenceSnapshot(
                currencySymbol = "$",
                localeTag = "en-US",
                backupHistoryCount = 7,
                visibleFields = setOf(OptionalFieldToggle.NOTES, OptionalFieldToggle.FUEL_TYPE),
            ),
            vehicles = listOf(
                VehicleEntity(
                    id = 101L,
                    legacySourceId = 501L,
                    name = "Corolla",
                    type = "Car",
                    year = 2010,
                    make = "Toyota",
                    model = "Corolla",
                    submodel = "LE",
                    lifecycle = "ACTIVE",
                    country = "US",
                    licensePlate = "ABC123",
                    vin = "VIN123",
                    insurancePolicy = "POLICY",
                    bodyStyle = "Sedan",
                    color = "Blue",
                    engineDisplacement = "1.8L",
                    fuelTankCapacity = 13.2,
                    purchasePrice = 18000.0,
                    purchaseOdometer = 10.0,
                    purchaseDate = LocalDate.parse("2010-01-10"),
                    sellingPrice = null,
                    sellingOdometer = null,
                    sellingDate = null,
                    notes = "Primary car",
                    profilePhotoUri = "content://vehicle-photo",
                    distanceUnitOverride = "mi",
                    volumeUnitOverride = "gal (US)",
                    fuelEfficiencyUnitOverride = "MPG (US)",
                ),
            ),
            vehicleParts = listOf(
                VehiclePartEntity(
                    id = 102L,
                    legacySourceId = 502L,
                    vehicleId = 101L,
                    name = "Tire",
                    partNumber = "P123",
                    type = "Wheel",
                    brand = "Brand",
                    color = "Black",
                    size = "16",
                    volume = null,
                    pressure = 32.0,
                    quantity = 4,
                    notes = "All season",
                ),
            ),
            fuelTypes = listOf(
                FuelTypeEntity(
                    id = 201L,
                    legacySourceId = 601L,
                    category = "Gasoline",
                    grade = "Regular",
                    octane = 87,
                    cetane = null,
                    notes = "",
                ),
            ),
            serviceTypes = listOf(
                ServiceTypeEntity(
                    id = 301L,
                    legacySourceId = 701L,
                    name = "Oil Change",
                    notes = "",
                    defaultTimeReminderMonths = 6,
                    defaultDistanceReminder = 5000.0,
                ),
            ),
            expenseTypes = listOf(
                ExpenseTypeEntity(
                    id = 401L,
                    legacySourceId = 801L,
                    name = "Registration",
                    notes = "",
                ),
            ),
            tripTypes = listOf(
                TripTypeEntity(
                    id = 501L,
                    legacySourceId = 901L,
                    name = "Business",
                    defaultTaxDeductionRate = 0.67,
                    notes = "",
                ),
            ),
            serviceReminders = listOf(
                ServiceReminderEntity(
                    id = 601L,
                    legacySourceId = 1001L,
                    vehicleId = 101L,
                    serviceTypeId = 301L,
                    intervalTimeMonths = 6,
                    intervalDistance = 5000.0,
                    dueDate = LocalDate.parse("2026-10-01"),
                    dueDistance = 130000.0,
                    timeAlertSilent = false,
                    distanceAlertSilent = false,
                    lastTimeAlert = LocalDateTime.parse("2026-04-01T08:00:00"),
                    lastDistanceAlert = null,
                ),
            ),
            fillUpRecords = listOf(
                FillUpRecordEntity(
                    id = 701L,
                    legacySourceId = 1101L,
                    vehicleId = 101L,
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
                    fuelTypeId = 201L,
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
                    id = 801L,
                    legacySourceId = 1201L,
                    vehicleId = 101L,
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
            serviceRecordTypes = listOf(ServiceRecordTypeCrossRef(serviceRecordId = 801L, serviceTypeId = 301L)),
            expenseRecords = listOf(
                ExpenseRecordEntity(
                    id = 901L,
                    legacySourceId = 1301L,
                    vehicleId = 101L,
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
            expenseRecordTypes = listOf(ExpenseRecordTypeCrossRef(expenseRecordId = 901L, expenseTypeId = 401L)),
            tripRecords = listOf(
                TripRecordEntity(
                    id = 1001L,
                    legacySourceId = 1401L,
                    vehicleId = 101L,
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
                    tripTypeId = 501L,
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
                    id = 1101L,
                    vehicleId = 101L,
                    recordFamily = RecordFamily.SERVICE,
                    recordId = 801L,
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

        val imported = OpenJsonBackupImporter().import(bytes.toByteArray().inputStream())

        assertThat(imported.metadata["formatVersion"]).isEqualTo("1")
        assertThat(imported.metadata["vehicleCount"]).isEqualTo("1")
        assertThat(imported.preferences?.backupHistoryCount).isEqualTo(7)
        assertThat(imported.preferences?.visibleFields).containsExactly(
            OptionalFieldToggle.FUEL_TYPE,
            OptionalFieldToggle.NOTES,
        )
        assertThat(imported.vehicles).hasSize(1)
        assertThat(imported.vehicles.single().legacySourceId).isEqualTo(501L)
        assertThat(imported.vehicleParts.single().vehicleId).isEqualTo(501L)
        assertThat(imported.serviceReminders.single().vehicleId).isEqualTo(501L)
        assertThat(imported.serviceReminders.single().serviceTypeId).isEqualTo(701L)
        assertThat(imported.fillUpRecords.single().vehicleId).isEqualTo(501L)
        assertThat(imported.fillUpRecords.single().fuelTypeId).isEqualTo(601L)
        assertThat(imported.serviceRecords.single().serviceTypeIds).containsExactly(701L)
        assertThat(imported.expenseRecords.single().expenseTypeIds).containsExactly(801L)
        assertThat(imported.tripRecords.single().tripTypeId).isEqualTo(901L)
        assertThat(imported.attachments.single().vehicleId).isEqualTo(501L)
        assertThat(imported.attachments.single().recordFamily).isEqualTo(RecordFamily.SERVICE)
        assertThat(imported.attachments.single().recordId).isEqualTo(1201L)
        assertThat(imported.issues).isEmpty()
    }

    @Test
    fun import_requiresBackupPayloadEntry() {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write("""{"formatVersion":1}""".toByteArray())
            zip.closeEntry()
        }

        val error = runCatching {
            OpenJsonBackupImporter().import(bytes.toByteArray().inputStream())
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error?.message).contains("garage-ledger-backup.json")
    }
}
