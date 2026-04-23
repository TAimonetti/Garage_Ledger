package com.guzzlio.data.export

import com.guzzlio.data.local.GarageDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class OpenJsonBackupExporter(
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
    },
) {
    fun export(snapshot: ExportSnapshot, outputStream: OutputStream, exportedAt: LocalDateTime = LocalDateTime.now()) {
        val payload = OpenJsonBackupPayload.from(snapshot, exportedAt)
        val metadata = OpenJsonBackupMetadata(
            formatVersion = OpenJsonBackupPayload.FORMAT_VERSION,
            schemaVersion = GarageDatabase.SCHEMA_VERSION,
            exportedAt = exportedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            vehicleCount = snapshot.vehicles.size,
            fillUpCount = snapshot.fillUpRecords.size,
            serviceRecordCount = snapshot.serviceRecords.size,
            expenseRecordCount = snapshot.expenseRecords.size,
            tripRecordCount = snapshot.tripRecords.size,
        )
        ZipOutputStream(outputStream.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("metadata.json"))
            zip.write(json.encodeToString(metadata).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("guzzlio-backup.json"))
            zip.write(json.encodeToString(payload).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
}

@Serializable
internal data class OpenJsonBackupMetadata(
    val formatVersion: Int,
    val schemaVersion: Int,
    val exportedAt: String,
    val vehicleCount: Int,
    val fillUpCount: Int,
    val serviceRecordCount: Int,
    val expenseRecordCount: Int,
    val tripRecordCount: Int,
)

@Serializable
internal data class OpenJsonBackupPayload(
    val formatVersion: Int,
    val schemaVersion: Int,
    val exportedAt: String,
    val preferences: PreferencesPayload,
    val vehicles: List<VehiclePayload>,
    val vehicleParts: List<VehiclePartPayload>,
    val fuelTypes: List<FuelTypePayload>,
    val serviceTypes: List<ServiceTypePayload>,
    val expenseTypes: List<ExpenseTypePayload>,
    val tripTypes: List<TripTypePayload>,
    val serviceReminders: List<ServiceReminderPayload>,
    val fillUpRecords: List<FillUpRecordPayload>,
    val serviceRecords: List<ServiceRecordPayload>,
    val serviceRecordTypes: List<ServiceRecordTypePayload>,
    val expenseRecords: List<ExpenseRecordPayload>,
    val expenseRecordTypes: List<ExpenseRecordTypePayload>,
    val tripRecords: List<TripRecordPayload>,
    val attachments: List<AttachmentPayload>,
) {
    companion object {
        const val FORMAT_VERSION: Int = 1

        fun from(snapshot: ExportSnapshot, exportedAt: LocalDateTime): OpenJsonBackupPayload = OpenJsonBackupPayload(
            formatVersion = FORMAT_VERSION,
            schemaVersion = GarageDatabase.SCHEMA_VERSION,
            exportedAt = exportedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            preferences = PreferencesPayload.from(snapshot.preferences),
            vehicles = snapshot.vehicles.map(VehiclePayload::from),
            vehicleParts = snapshot.vehicleParts.map(VehiclePartPayload::from),
            fuelTypes = snapshot.fuelTypes.map(FuelTypePayload::from),
            serviceTypes = snapshot.serviceTypes.map(ServiceTypePayload::from),
            expenseTypes = snapshot.expenseTypes.map(ExpenseTypePayload::from),
            tripTypes = snapshot.tripTypes.map(TripTypePayload::from),
            serviceReminders = snapshot.serviceReminders.map(ServiceReminderPayload::from),
            fillUpRecords = snapshot.fillUpRecords.map(FillUpRecordPayload::from),
            serviceRecords = snapshot.serviceRecords.map(ServiceRecordPayload::from),
            serviceRecordTypes = snapshot.serviceRecordTypes.map { ServiceRecordTypePayload(it.serviceRecordId, it.serviceTypeId) },
            expenseRecords = snapshot.expenseRecords.map(ExpenseRecordPayload::from),
            expenseRecordTypes = snapshot.expenseRecordTypes.map { ExpenseRecordTypePayload(it.expenseRecordId, it.expenseTypeId) },
            tripRecords = snapshot.tripRecords.map(TripRecordPayload::from),
            attachments = snapshot.attachments.map(AttachmentPayload::from),
        )
    }
}

@Serializable
internal data class PreferencesPayload(
    val distanceUnit: String,
    val volumeUnit: String,
    val fuelEfficiencyUnit: String,
    val currencySymbol: String,
    val localeTag: String,
    val fullDateFormat: String,
    val compactDateFormat: String,
    val browseSortDescending: Boolean,
    val fuelEfficiencyAssignmentMethod: String,
    val reminderTimeAlertPercent: Int,
    val reminderDistanceAlertPercent: Int,
    val backupFrequencyHours: Int,
    val backupHistoryCount: Int,
    val useLocation: Boolean,
    val notificationsEnabled: Boolean,
    val notificationLedEnabled: Boolean,
    val visibleFields: List<String>,
    val savedBrowseSearches: List<SavedBrowseSearchPayload> = emptyList(),
) {
    companion object {
        fun from(snapshot: com.guzzlio.domain.model.AppPreferenceSnapshot): PreferencesPayload = PreferencesPayload(
            distanceUnit = snapshot.distanceUnit.storageValue,
            volumeUnit = snapshot.volumeUnit.storageValue,
            fuelEfficiencyUnit = snapshot.fuelEfficiencyUnit.storageValue,
            currencySymbol = snapshot.currencySymbol,
            localeTag = snapshot.localeTag,
            fullDateFormat = snapshot.fullDateFormat,
            compactDateFormat = snapshot.compactDateFormat,
            browseSortDescending = snapshot.browseSortDescending,
            fuelEfficiencyAssignmentMethod = snapshot.fuelEfficiencyAssignmentMethod.storageValue,
            reminderTimeAlertPercent = snapshot.reminderTimeAlertPercent,
            reminderDistanceAlertPercent = snapshot.reminderDistanceAlertPercent,
            backupFrequencyHours = snapshot.backupFrequencyHours,
            backupHistoryCount = snapshot.backupHistoryCount,
            useLocation = snapshot.useLocation,
            notificationsEnabled = snapshot.notificationsEnabled,
            notificationLedEnabled = snapshot.notificationLedEnabled,
            visibleFields = snapshot.visibleFields.map { it.storageKey }.sorted(),
            savedBrowseSearches = snapshot.savedBrowseSearches.map(SavedBrowseSearchPayload::from),
        )
    }
}

@Serializable
internal data class SavedBrowseSearchPayload(
    val name: String,
    val vehicleId: Long? = null,
    val family: String? = null,
    val query: String = "",
    val tag: String = "",
    val fromDateIso: String? = null,
    val toDateIso: String? = null,
    val subtype: String = "",
    val paymentType: String = "",
    val eventPlace: String = "",
    val fuelBrand: String = "",
    val fuelType: String = "",
    val fuelAdditive: String = "",
    val drivingMode: String = "",
    val tripPurpose: String = "",
    val tripClient: String = "",
    val tripLocation: String = "",
    val tripPaidStatus: String? = null,
) {
    companion object {
        fun from(search: com.guzzlio.domain.model.SavedBrowseSearch): SavedBrowseSearchPayload =
            SavedBrowseSearchPayload(
                name = search.name,
                vehicleId = search.vehicleId,
                family = search.family?.name,
                query = search.query,
                tag = search.tag,
                fromDateIso = search.fromDateIso,
                toDateIso = search.toDateIso,
                subtype = search.subtype,
                paymentType = search.paymentType,
                eventPlace = search.eventPlace,
                fuelBrand = search.fuelBrand,
                fuelType = search.fuelType,
                fuelAdditive = search.fuelAdditive,
                drivingMode = search.drivingMode,
                tripPurpose = search.tripPurpose,
                tripClient = search.tripClient,
                tripLocation = search.tripLocation,
                tripPaidStatus = search.tripPaidStatus?.name,
            )
    }
}

@Serializable
internal data class VehiclePayload(
    val id: Long,
    val legacySourceId: Long?,
    val name: String,
    val type: String,
    val year: Int?,
    val make: String,
    val model: String,
    val submodel: String,
    val lifecycle: String,
    val country: String,
    val licensePlate: String,
    val vin: String,
    val insurancePolicy: String,
    val bodyStyle: String,
    val color: String,
    val engineDisplacement: String,
    val fuelTankCapacity: Double?,
    val purchasePrice: Double?,
    val purchaseOdometer: Double?,
    val purchaseDate: String?,
    val sellingPrice: Double?,
    val sellingOdometer: Double?,
    val sellingDate: String?,
    val notes: String,
    val profilePhotoUri: String?,
    val distanceUnitOverride: String?,
    val volumeUnitOverride: String?,
    val fuelEfficiencyUnitOverride: String?,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.VehicleEntity): VehiclePayload = VehiclePayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            name = entity.name,
            type = entity.type,
            year = entity.year,
            make = entity.make,
            model = entity.model,
            submodel = entity.submodel,
            lifecycle = entity.lifecycle,
            country = entity.country,
            licensePlate = entity.licensePlate,
            vin = entity.vin,
            insurancePolicy = entity.insurancePolicy,
            bodyStyle = entity.bodyStyle,
            color = entity.color,
            engineDisplacement = entity.engineDisplacement,
            fuelTankCapacity = entity.fuelTankCapacity,
            purchasePrice = entity.purchasePrice,
            purchaseOdometer = entity.purchaseOdometer,
            purchaseDate = entity.purchaseDate?.toString(),
            sellingPrice = entity.sellingPrice,
            sellingOdometer = entity.sellingOdometer,
            sellingDate = entity.sellingDate?.toString(),
            notes = entity.notes,
            profilePhotoUri = entity.profilePhotoUri,
            distanceUnitOverride = entity.distanceUnitOverride,
            volumeUnitOverride = entity.volumeUnitOverride,
            fuelEfficiencyUnitOverride = entity.fuelEfficiencyUnitOverride,
        )
    }
}

@Serializable
internal data class VehiclePartPayload(
    val id: Long,
    val legacySourceId: Long?,
    val vehicleId: Long,
    val name: String,
    val partNumber: String,
    val type: String,
    val brand: String,
    val color: String,
    val size: String,
    val volume: Double?,
    val pressure: Double?,
    val quantity: Int?,
    val notes: String,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.VehiclePartEntity): VehiclePartPayload = VehiclePartPayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            vehicleId = entity.vehicleId,
            name = entity.name,
            partNumber = entity.partNumber,
            type = entity.type,
            brand = entity.brand,
            color = entity.color,
            size = entity.size,
            volume = entity.volume,
            pressure = entity.pressure,
            quantity = entity.quantity,
            notes = entity.notes,
        )
    }
}

@Serializable
internal data class FuelTypePayload(
    val id: Long,
    val legacySourceId: Long?,
    val category: String,
    val grade: String,
    val octane: Int?,
    val cetane: Int?,
    val notes: String,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.FuelTypeEntity): FuelTypePayload = FuelTypePayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            category = entity.category,
            grade = entity.grade,
            octane = entity.octane,
            cetane = entity.cetane,
            notes = entity.notes,
        )
    }
}

@Serializable
internal data class ServiceTypePayload(
    val id: Long,
    val legacySourceId: Long?,
    val name: String,
    val notes: String,
    val defaultTimeReminderMonths: Int,
    val defaultDistanceReminder: Double?,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.ServiceTypeEntity): ServiceTypePayload = ServiceTypePayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            name = entity.name,
            notes = entity.notes,
            defaultTimeReminderMonths = entity.defaultTimeReminderMonths,
            defaultDistanceReminder = entity.defaultDistanceReminder,
        )
    }
}

@Serializable
internal data class ExpenseTypePayload(
    val id: Long,
    val legacySourceId: Long?,
    val name: String,
    val notes: String,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.ExpenseTypeEntity): ExpenseTypePayload = ExpenseTypePayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            name = entity.name,
            notes = entity.notes,
        )
    }
}

@Serializable
internal data class TripTypePayload(
    val id: Long,
    val legacySourceId: Long?,
    val name: String,
    val defaultTaxDeductionRate: Double?,
    val notes: String,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.TripTypeEntity): TripTypePayload = TripTypePayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            name = entity.name,
            defaultTaxDeductionRate = entity.defaultTaxDeductionRate,
            notes = entity.notes,
        )
    }
}

@Serializable
internal data class ServiceReminderPayload(
    val id: Long,
    val legacySourceId: Long?,
    val vehicleId: Long,
    val serviceTypeId: Long,
    val intervalTimeMonths: Int,
    val intervalDistance: Double?,
    val dueDate: String?,
    val dueDistance: Double?,
    val timeAlertSilent: Boolean,
    val distanceAlertSilent: Boolean,
    val lastTimeAlert: String?,
    val lastDistanceAlert: String?,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.ServiceReminderEntity): ServiceReminderPayload = ServiceReminderPayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            vehicleId = entity.vehicleId,
            serviceTypeId = entity.serviceTypeId,
            intervalTimeMonths = entity.intervalTimeMonths,
            intervalDistance = entity.intervalDistance,
            dueDate = entity.dueDate?.toString(),
            dueDistance = entity.dueDistance,
            timeAlertSilent = entity.timeAlertSilent,
            distanceAlertSilent = entity.distanceAlertSilent,
            lastTimeAlert = entity.lastTimeAlert?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            lastDistanceAlert = entity.lastDistanceAlert?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        )
    }
}

@Serializable
internal data class FillUpRecordPayload(
    val id: Long,
    val legacySourceId: Long?,
    val vehicleId: Long,
    val dateTime: String,
    val odometerReading: Double,
    val distanceUnit: String,
    val volume: Double,
    val volumeUnit: String,
    val pricePerUnit: Double,
    val totalCost: Double,
    val paymentType: String,
    val partial: Boolean,
    val previousMissedFillups: Boolean,
    val fuelEfficiency: Double?,
    val fuelEfficiencyUnit: String?,
    val importedFuelEfficiency: Double?,
    val fuelTypeId: Long?,
    val importedFuelTypeText: String?,
    val hasFuelAdditive: Boolean,
    val fuelAdditiveName: String,
    val fuelBrand: String,
    val stationAddress: String,
    val latitude: Double?,
    val longitude: Double?,
    val drivingMode: String,
    val cityDrivingPercentage: Int?,
    val highwayDrivingPercentage: Int?,
    val averageSpeed: Double?,
    val tags: List<String>,
    val notes: String,
    val distanceSincePrevious: Double?,
    val distanceTillNextFillUp: Double?,
    val timeSincePreviousMillis: Long?,
    val timeTillNextFillUpMillis: Long?,
    val distanceForFuelEfficiency: Double?,
    val volumeForFuelEfficiency: Double?,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.FillUpRecordEntity): FillUpRecordPayload = FillUpRecordPayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            vehicleId = entity.vehicleId,
            dateTime = entity.dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            odometerReading = entity.odometerReading,
            distanceUnit = entity.distanceUnit,
            volume = entity.volume,
            volumeUnit = entity.volumeUnit,
            pricePerUnit = entity.pricePerUnit,
            totalCost = entity.totalCost,
            paymentType = entity.paymentType,
            partial = entity.partial,
            previousMissedFillups = entity.previousMissedFillups,
            fuelEfficiency = entity.fuelEfficiency,
            fuelEfficiencyUnit = entity.fuelEfficiencyUnit,
            importedFuelEfficiency = entity.importedFuelEfficiency,
            fuelTypeId = entity.fuelTypeId,
            importedFuelTypeText = entity.importedFuelTypeText,
            hasFuelAdditive = entity.hasFuelAdditive,
            fuelAdditiveName = entity.fuelAdditiveName,
            fuelBrand = entity.fuelBrand,
            stationAddress = entity.stationAddress,
            latitude = entity.latitude,
            longitude = entity.longitude,
            drivingMode = entity.drivingMode,
            cityDrivingPercentage = entity.cityDrivingPercentage,
            highwayDrivingPercentage = entity.highwayDrivingPercentage,
            averageSpeed = entity.averageSpeed,
            tags = entity.tags,
            notes = entity.notes,
            distanceSincePrevious = entity.distanceSincePrevious,
            distanceTillNextFillUp = entity.distanceTillNextFillUp,
            timeSincePreviousMillis = entity.timeSincePreviousMillis,
            timeTillNextFillUpMillis = entity.timeTillNextFillUpMillis,
            distanceForFuelEfficiency = entity.distanceForFuelEfficiency,
            volumeForFuelEfficiency = entity.volumeForFuelEfficiency,
        )
    }
}

@Serializable
internal data class ServiceRecordPayload(
    val id: Long,
    val legacySourceId: Long?,
    val vehicleId: Long,
    val dateTime: String,
    val odometerReading: Double,
    val distanceUnit: String,
    val totalCost: Double,
    val paymentType: String,
    val serviceCenterName: String,
    val serviceCenterAddress: String,
    val latitude: Double?,
    val longitude: Double?,
    val tags: List<String>,
    val notes: String,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.ServiceRecordEntity): ServiceRecordPayload = ServiceRecordPayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            vehicleId = entity.vehicleId,
            dateTime = entity.dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            odometerReading = entity.odometerReading,
            distanceUnit = entity.distanceUnit,
            totalCost = entity.totalCost,
            paymentType = entity.paymentType,
            serviceCenterName = entity.serviceCenterName,
            serviceCenterAddress = entity.serviceCenterAddress,
            latitude = entity.latitude,
            longitude = entity.longitude,
            tags = entity.tags,
            notes = entity.notes,
        )
    }
}

@Serializable
internal data class ServiceRecordTypePayload(
    val serviceRecordId: Long,
    val serviceTypeId: Long,
)

@Serializable
internal data class ExpenseRecordPayload(
    val id: Long,
    val legacySourceId: Long?,
    val vehicleId: Long,
    val dateTime: String,
    val odometerReading: Double,
    val distanceUnit: String,
    val totalCost: Double,
    val paymentType: String,
    val expenseCenterName: String,
    val expenseCenterAddress: String,
    val latitude: Double?,
    val longitude: Double?,
    val tags: List<String>,
    val notes: String,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.ExpenseRecordEntity): ExpenseRecordPayload = ExpenseRecordPayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            vehicleId = entity.vehicleId,
            dateTime = entity.dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            odometerReading = entity.odometerReading,
            distanceUnit = entity.distanceUnit,
            totalCost = entity.totalCost,
            paymentType = entity.paymentType,
            expenseCenterName = entity.expenseCenterName,
            expenseCenterAddress = entity.expenseCenterAddress,
            latitude = entity.latitude,
            longitude = entity.longitude,
            tags = entity.tags,
            notes = entity.notes,
        )
    }
}

@Serializable
internal data class ExpenseRecordTypePayload(
    val expenseRecordId: Long,
    val expenseTypeId: Long,
)

@Serializable
internal data class TripRecordPayload(
    val id: Long,
    val legacySourceId: Long?,
    val vehicleId: Long,
    val startDateTime: String,
    val startOdometerReading: Double,
    val startLocation: String,
    val endDateTime: String?,
    val endOdometerReading: Double?,
    val endLocation: String,
    val startLatitude: Double?,
    val startLongitude: Double?,
    val endLatitude: Double?,
    val endLongitude: Double?,
    val distanceUnit: String,
    val distance: Double?,
    val durationMillis: Long?,
    val tripTypeId: Long?,
    val purpose: String,
    val client: String,
    val taxDeductionRate: Double?,
    val taxDeductionAmount: Double?,
    val reimbursementRate: Double?,
    val reimbursementAmount: Double?,
    val paid: Boolean,
    val tags: List<String>,
    val notes: String,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.TripRecordEntity): TripRecordPayload = TripRecordPayload(
            id = entity.id,
            legacySourceId = entity.legacySourceId,
            vehicleId = entity.vehicleId,
            startDateTime = entity.startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            startOdometerReading = entity.startOdometerReading,
            startLocation = entity.startLocation,
            endDateTime = entity.endDateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            endOdometerReading = entity.endOdometerReading,
            endLocation = entity.endLocation,
            startLatitude = entity.startLatitude,
            startLongitude = entity.startLongitude,
            endLatitude = entity.endLatitude,
            endLongitude = entity.endLongitude,
            distanceUnit = entity.distanceUnit,
            distance = entity.distance,
            durationMillis = entity.durationMillis,
            tripTypeId = entity.tripTypeId,
            purpose = entity.purpose,
            client = entity.client,
            taxDeductionRate = entity.taxDeductionRate,
            taxDeductionAmount = entity.taxDeductionAmount,
            reimbursementRate = entity.reimbursementRate,
            reimbursementAmount = entity.reimbursementAmount,
            paid = entity.paid,
            tags = entity.tags,
            notes = entity.notes,
        )
    }
}

@Serializable
internal data class AttachmentPayload(
    val id: Long,
    val vehicleId: Long,
    val recordFamily: String,
    val recordId: Long,
    val uri: String,
    val mimeType: String,
    val displayName: String,
    val createdAt: String,
) {
    companion object {
        fun from(entity: com.guzzlio.data.local.RecordAttachmentEntity): AttachmentPayload = AttachmentPayload(
            id = entity.id,
            vehicleId = entity.vehicleId,
            recordFamily = entity.recordFamily.name,
            recordId = entity.recordId,
            uri = entity.uri,
            mimeType = entity.mimeType,
            displayName = entity.displayName,
            createdAt = entity.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        )
    }
}
