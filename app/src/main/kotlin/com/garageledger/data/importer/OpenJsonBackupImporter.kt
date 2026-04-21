package com.garageledger.data.importer

import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.FuelEfficiencyAssignmentMethod
import com.garageledger.core.model.FuelEfficiencyUnit
import com.garageledger.core.model.VolumeUnit
import com.garageledger.data.export.AttachmentPayload
import com.garageledger.data.export.ExpenseRecordPayload
import com.garageledger.data.export.ExpenseTypePayload
import com.garageledger.data.export.FillUpRecordPayload
import com.garageledger.data.export.FuelTypePayload
import com.garageledger.data.export.OpenJsonBackupMetadata
import com.garageledger.data.export.OpenJsonBackupPayload
import com.garageledger.data.export.PreferencesPayload
import com.garageledger.data.export.ServiceRecordPayload
import com.garageledger.data.export.ServiceReminderPayload
import com.garageledger.data.export.ServiceTypePayload
import com.garageledger.data.export.TripRecordPayload
import com.garageledger.data.export.TripTypePayload
import com.garageledger.data.export.VehiclePartPayload
import com.garageledger.data.export.VehiclePayload
import com.garageledger.domain.model.AppPreferenceSnapshot
import com.garageledger.domain.model.ExpenseRecord
import com.garageledger.domain.model.ExpenseType
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.FuelType
import com.garageledger.domain.model.ImportIssue
import com.garageledger.domain.model.ImportedGarageData
import com.garageledger.domain.model.OptionalFieldToggle
import com.garageledger.domain.model.RecordAttachment
import com.garageledger.domain.model.RecordFamily
import com.garageledger.domain.model.ServiceRecord
import com.garageledger.domain.model.ServiceReminder
import com.garageledger.domain.model.ServiceType
import com.garageledger.domain.model.TripRecord
import com.garageledger.domain.model.TripType
import com.garageledger.domain.model.Vehicle
import com.garageledger.domain.model.VehicleLifecycle
import com.garageledger.domain.model.VehiclePart
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipInputStream
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class OpenJsonBackupImporter(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun import(inputStream: InputStream): ImportedGarageData {
        val archiveEntries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                if (!entry.isDirectory) {
                    archiveEntries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
            }
        }

        val payloadBytes = archiveEntries["garage-ledger-backup.json"]
            ?: error("Selected archive is missing garage-ledger-backup.json.")
        val metadata = archiveEntries["metadata.json"]?.decodeAsMetadataOrNull()

        val payload = runCatching {
            json.decodeFromString<OpenJsonBackupPayload>(payloadBytes.toString(Charsets.UTF_8))
        }.getOrElse { error ->
            throw IllegalArgumentException("Selected archive is not a valid Garage Ledger backup.", error)
        }

        val issues = mutableListOf<ImportIssue>()
        val vehicleSourceKeysByBackupId = payload.vehicles.associate { it.id to it.sourceKey() }
        val fuelTypeSourceKeysByBackupId = payload.fuelTypes.associate { it.id to it.sourceKey() }
        val serviceTypeSourceKeysByBackupId = payload.serviceTypes.associate { it.id to it.sourceKey() }
        val expenseTypeSourceKeysByBackupId = payload.expenseTypes.associate { it.id to it.sourceKey() }
        val tripTypeSourceKeysByBackupId = payload.tripTypes.associate { it.id to it.sourceKey() }
        val fillUpSourceKeysByBackupId = payload.fillUpRecords.associate { it.id to it.sourceKey() }
        val serviceRecordSourceKeysByBackupId = payload.serviceRecords.associate { it.id to it.sourceKey() }
        val expenseRecordSourceKeysByBackupId = payload.expenseRecords.associate { it.id to it.sourceKey() }
        val tripRecordSourceKeysByBackupId = payload.tripRecords.associate { it.id to it.sourceKey() }
        val serviceTypeIdsByRecord = payload.serviceRecordTypes.groupBy({ it.serviceRecordId }, { it.serviceTypeId })
        val expenseTypeIdsByRecord = payload.expenseRecordTypes.groupBy({ it.expenseRecordId }, { it.expenseTypeId })

        return ImportedGarageData(
            metadata = payload.metadataMap(metadata),
            preferences = payload.preferences.toDomain(issues),
            vehicles = payload.vehicles.map { it.toDomain() },
            vehicleParts = payload.vehicleParts.mapNotNull { part ->
                val mappedVehicleId = vehicleSourceKeysByBackupId[part.vehicleId]
                if (mappedVehicleId == null) {
                    issues += issue("Skipped vehicle part '${part.name}' because vehicle ${part.vehicleId} is missing.", "garage-ledger-backup.json")
                    null
                } else {
                    part.toDomain(mappedVehicleId)
                }
            },
            fuelTypes = payload.fuelTypes.map(FuelTypePayload::toDomain),
            serviceTypes = payload.serviceTypes.map(ServiceTypePayload::toDomain),
            expenseTypes = payload.expenseTypes.map(ExpenseTypePayload::toDomain),
            tripTypes = payload.tripTypes.map(TripTypePayload::toDomain),
            serviceReminders = payload.serviceReminders.mapNotNull { reminder ->
                reminder.toDomain(
                    vehicleSourceKeysByBackupId = vehicleSourceKeysByBackupId,
                    serviceTypeSourceKeysByBackupId = serviceTypeSourceKeysByBackupId,
                    issues = issues,
                )
            },
            fillUpRecords = payload.fillUpRecords.mapNotNull { record ->
                record.toDomain(
                    vehicleSourceKeysByBackupId = vehicleSourceKeysByBackupId,
                    fuelTypeSourceKeysByBackupId = fuelTypeSourceKeysByBackupId,
                    issues = issues,
                )
            },
            serviceRecords = payload.serviceRecords.mapNotNull { record ->
                record.toDomain(
                    vehicleSourceKeysByBackupId = vehicleSourceKeysByBackupId,
                    serviceTypeIds = serviceTypeIdsByRecord[record.id].orEmpty(),
                    serviceTypeSourceKeysByBackupId = serviceTypeSourceKeysByBackupId,
                    issues = issues,
                )
            },
            expenseRecords = payload.expenseRecords.mapNotNull { record ->
                record.toDomain(
                    vehicleSourceKeysByBackupId = vehicleSourceKeysByBackupId,
                    expenseTypeIds = expenseTypeIdsByRecord[record.id].orEmpty(),
                    expenseTypeSourceKeysByBackupId = expenseTypeSourceKeysByBackupId,
                    issues = issues,
                )
            },
            tripRecords = payload.tripRecords.mapNotNull { record ->
                record.toDomain(
                    vehicleSourceKeysByBackupId = vehicleSourceKeysByBackupId,
                    tripTypeSourceKeysByBackupId = tripTypeSourceKeysByBackupId,
                    issues = issues,
                )
            },
            attachments = payload.attachments.mapNotNull { attachment ->
                attachment.toDomain(
                    vehicleSourceKeysByBackupId = vehicleSourceKeysByBackupId,
                    fillUpSourceKeysByBackupId = fillUpSourceKeysByBackupId,
                    serviceRecordSourceKeysByBackupId = serviceRecordSourceKeysByBackupId,
                    expenseRecordSourceKeysByBackupId = expenseRecordSourceKeysByBackupId,
                    tripRecordSourceKeysByBackupId = tripRecordSourceKeysByBackupId,
                    issues = issues,
                )
            },
            issues = issues,
        )
    }

    private fun ByteArray.decodeAsMetadataOrNull(): OpenJsonBackupMetadata? =
        runCatching { json.decodeFromString<OpenJsonBackupMetadata>(toString(Charsets.UTF_8)) }.getOrNull()

    private fun OpenJsonBackupPayload.metadataMap(metadata: OpenJsonBackupMetadata?): Map<String, String> = buildMap {
        put("formatVersion", formatVersion.toString())
        put("schemaVersion", schemaVersion.toString())
        put("exportedAt", exportedAt)
        put("vehicleCount", (metadata?.vehicleCount ?: vehicles.size).toString())
        put("fillUpCount", (metadata?.fillUpCount ?: fillUpRecords.size).toString())
        put("serviceRecordCount", (metadata?.serviceRecordCount ?: serviceRecords.size).toString())
        put("expenseRecordCount", (metadata?.expenseRecordCount ?: expenseRecords.size).toString())
        put("tripRecordCount", (metadata?.tripRecordCount ?: tripRecords.size).toString())
    }
}

private fun VehiclePayload.sourceKey(): Long = legacySourceId ?: id

private fun VehiclePartPayload.sourceKey(): Long = legacySourceId ?: id

private fun FuelTypePayload.sourceKey(): Long = legacySourceId ?: id

private fun ServiceTypePayload.sourceKey(): Long = legacySourceId ?: id

private fun ExpenseTypePayload.sourceKey(): Long = legacySourceId ?: id

private fun TripTypePayload.sourceKey(): Long = legacySourceId ?: id

private fun ServiceReminderPayload.sourceKey(): Long = legacySourceId ?: id

private fun FillUpRecordPayload.sourceKey(): Long = legacySourceId ?: id

private fun ServiceRecordPayload.sourceKey(): Long = legacySourceId ?: id

private fun ExpenseRecordPayload.sourceKey(): Long = legacySourceId ?: id

private fun TripRecordPayload.sourceKey(): Long = legacySourceId ?: id

private fun PreferencesPayload.toDomain(issues: MutableList<ImportIssue>): AppPreferenceSnapshot {
    val visibleFieldSet = buildSet {
        visibleFields.forEach { key ->
            val toggle = OptionalFieldToggle.entries.firstOrNull { it.storageKey == key }
            if (toggle != null) {
                add(toggle)
            } else {
                issues += issue("Ignored unknown visible field key '$key'.", "garage-ledger-backup.json")
            }
        }
    }

    return AppPreferenceSnapshot(
        distanceUnit = DistanceUnit.fromStorage(distanceUnit),
        volumeUnit = VolumeUnit.fromStorage(volumeUnit),
        fuelEfficiencyUnit = FuelEfficiencyUnit.fromStorage(fuelEfficiencyUnit),
        currencySymbol = currencySymbol,
        localeTag = localeTag,
        fullDateFormat = fullDateFormat,
        compactDateFormat = compactDateFormat,
        browseSortDescending = browseSortDescending,
        fuelEfficiencyAssignmentMethod = FuelEfficiencyAssignmentMethod.fromStorage(fuelEfficiencyAssignmentMethod),
        reminderTimeAlertPercent = reminderTimeAlertPercent,
        reminderDistanceAlertPercent = reminderDistanceAlertPercent,
        backupFrequencyHours = backupFrequencyHours,
        backupHistoryCount = backupHistoryCount,
        useLocation = useLocation,
        notificationsEnabled = notificationsEnabled,
        notificationLedEnabled = notificationLedEnabled,
        visibleFields = visibleFieldSet,
    )
}

private fun VehiclePayload.toDomain(): Vehicle = Vehicle(
    id = id,
    legacySourceId = sourceKey(),
    name = name,
    type = type,
    year = year,
    make = make,
    model = model,
    submodel = submodel,
    lifecycle = runCatching { VehicleLifecycle.valueOf(lifecycle) }.getOrDefault(VehicleLifecycle.ACTIVE),
    country = country,
    licensePlate = licensePlate,
    vin = vin,
    insurancePolicy = insurancePolicy,
    bodyStyle = bodyStyle,
    color = color,
    engineDisplacement = engineDisplacement,
    fuelTankCapacity = fuelTankCapacity,
    purchasePrice = purchasePrice,
    purchaseOdometer = purchaseOdometer,
    purchaseDate = purchaseDate?.let(::parseIsoDateOrNull),
    sellingPrice = sellingPrice,
    sellingOdometer = sellingOdometer,
    sellingDate = sellingDate?.let(::parseIsoDateOrNull),
    notes = notes,
    profilePhotoUri = profilePhotoUri,
    distanceUnitOverride = distanceUnitOverride?.let(DistanceUnit::fromStorage),
    volumeUnitOverride = volumeUnitOverride?.let(VolumeUnit::fromStorage),
    fuelEfficiencyUnitOverride = fuelEfficiencyUnitOverride?.let(FuelEfficiencyUnit::fromStorage),
)

private fun VehiclePartPayload.toDomain(mappedVehicleId: Long): VehiclePart = VehiclePart(
    id = id,
    legacySourceId = sourceKey(),
    vehicleId = mappedVehicleId,
    name = name,
    partNumber = partNumber,
    type = type,
    brand = brand,
    color = color,
    size = size,
    volume = volume,
    pressure = pressure,
    quantity = quantity,
    notes = notes,
)

private fun FuelTypePayload.toDomain(): FuelType = FuelType(
    id = id,
    legacySourceId = sourceKey(),
    category = category,
    grade = grade,
    octane = octane,
    cetane = cetane,
    notes = notes,
)

private fun ServiceTypePayload.toDomain(): ServiceType = ServiceType(
    id = id,
    legacySourceId = sourceKey(),
    name = name,
    notes = notes,
    defaultTimeReminderMonths = defaultTimeReminderMonths,
    defaultDistanceReminder = defaultDistanceReminder,
)

private fun ExpenseTypePayload.toDomain(): ExpenseType = ExpenseType(
    id = id,
    legacySourceId = sourceKey(),
    name = name,
    notes = notes,
)

private fun TripTypePayload.toDomain(): TripType = TripType(
    id = id,
    legacySourceId = sourceKey(),
    name = name,
    defaultTaxDeductionRate = defaultTaxDeductionRate,
    notes = notes,
)

private fun ServiceReminderPayload.toDomain(
    vehicleSourceKeysByBackupId: Map<Long, Long>,
    serviceTypeSourceKeysByBackupId: Map<Long, Long>,
    issues: MutableList<ImportIssue>,
): ServiceReminder? {
    val mappedVehicleId = vehicleSourceKeysByBackupId[vehicleId]
    if (mappedVehicleId == null) {
        issues += issue("Skipped reminder $id because vehicle $vehicleId is missing.", "garage-ledger-backup.json")
        return null
    }
    val mappedServiceTypeId = serviceTypeSourceKeysByBackupId[serviceTypeId]
    if (mappedServiceTypeId == null) {
        issues += issue("Skipped reminder $id because service type $serviceTypeId is missing.", "garage-ledger-backup.json")
        return null
    }
    return ServiceReminder(
        id = id,
        legacySourceId = sourceKey(),
        vehicleId = mappedVehicleId,
        serviceTypeId = mappedServiceTypeId,
        intervalTimeMonths = intervalTimeMonths,
        intervalDistance = intervalDistance,
        dueDate = dueDate?.let(::parseIsoDateOrNull),
        dueDistance = dueDistance,
        timeAlertSilent = timeAlertSilent,
        distanceAlertSilent = distanceAlertSilent,
        lastTimeAlert = lastTimeAlert?.let(::parseIsoDateTimeOrNull),
        lastDistanceAlert = lastDistanceAlert?.let(::parseIsoDateTimeOrNull),
    )
}

private fun FillUpRecordPayload.toDomain(
    vehicleSourceKeysByBackupId: Map<Long, Long>,
    fuelTypeSourceKeysByBackupId: Map<Long, Long>,
    issues: MutableList<ImportIssue>,
): FillUpRecord? {
    val mappedVehicleId = vehicleSourceKeysByBackupId[vehicleId]
    if (mappedVehicleId == null) {
        issues += issue("Skipped fill-up $id because vehicle $vehicleId is missing.", "garage-ledger-backup.json")
        return null
    }
    return FillUpRecord(
        id = id,
        legacySourceId = sourceKey(),
        vehicleId = mappedVehicleId,
        dateTime = parseIsoDateTimeOrNull(dateTime)
            ?: return issueAndSkipFillUp(id, dateTime, issues),
        odometerReading = odometerReading,
        distanceUnit = DistanceUnit.fromStorage(distanceUnit),
        volume = volume,
        volumeUnit = VolumeUnit.fromStorage(volumeUnit),
        pricePerUnit = pricePerUnit,
        totalCost = totalCost,
        paymentType = paymentType,
        partial = partial,
        previousMissedFillups = previousMissedFillups,
        fuelEfficiency = fuelEfficiency,
        fuelEfficiencyUnit = fuelEfficiencyUnit?.let(FuelEfficiencyUnit::fromStorage),
        importedFuelEfficiency = importedFuelEfficiency,
        fuelTypeId = fuelTypeId?.let(fuelTypeSourceKeysByBackupId::get),
        importedFuelTypeText = importedFuelTypeText,
        hasFuelAdditive = hasFuelAdditive,
        fuelAdditiveName = fuelAdditiveName,
        fuelBrand = fuelBrand,
        stationAddress = stationAddress,
        latitude = latitude,
        longitude = longitude,
        drivingMode = drivingMode,
        cityDrivingPercentage = cityDrivingPercentage,
        highwayDrivingPercentage = highwayDrivingPercentage,
        averageSpeed = averageSpeed,
        tags = tags,
        notes = notes,
        distanceSincePrevious = distanceSincePrevious,
        distanceTillNextFillUp = distanceTillNextFillUp,
        timeSincePreviousMillis = timeSincePreviousMillis,
        timeTillNextFillUpMillis = timeTillNextFillUpMillis,
        distanceForFuelEfficiency = distanceForFuelEfficiency,
        volumeForFuelEfficiency = volumeForFuelEfficiency,
    )
}

private fun ServiceRecordPayload.toDomain(
    vehicleSourceKeysByBackupId: Map<Long, Long>,
    serviceTypeIds: List<Long>,
    serviceTypeSourceKeysByBackupId: Map<Long, Long>,
    issues: MutableList<ImportIssue>,
): ServiceRecord? {
    val mappedVehicleId = vehicleSourceKeysByBackupId[vehicleId]
    if (mappedVehicleId == null) {
        issues += issue("Skipped service record $id because vehicle $vehicleId is missing.", "garage-ledger-backup.json")
        return null
    }
    return ServiceRecord(
        id = id,
        legacySourceId = sourceKey(),
        vehicleId = mappedVehicleId,
        dateTime = parseIsoDateTimeOrNull(dateTime)
            ?: return issueAndSkipService(id, dateTime, issues),
        odometerReading = odometerReading,
        distanceUnit = DistanceUnit.fromStorage(distanceUnit),
        totalCost = totalCost,
        paymentType = paymentType,
        serviceCenterName = serviceCenterName,
        serviceCenterAddress = serviceCenterAddress,
        latitude = latitude,
        longitude = longitude,
        tags = tags,
        notes = notes,
        serviceTypeIds = serviceTypeIds.mapNotNull { serviceTypeId ->
            serviceTypeSourceKeysByBackupId[serviceTypeId] ?: run {
                issues += issue(
                    "Ignored missing service type $serviceTypeId for service record $id.",
                    "garage-ledger-backup.json",
                )
                null
            }
        },
    )
}

private fun ExpenseRecordPayload.toDomain(
    vehicleSourceKeysByBackupId: Map<Long, Long>,
    expenseTypeIds: List<Long>,
    expenseTypeSourceKeysByBackupId: Map<Long, Long>,
    issues: MutableList<ImportIssue>,
): ExpenseRecord? {
    val mappedVehicleId = vehicleSourceKeysByBackupId[vehicleId]
    if (mappedVehicleId == null) {
        issues += issue("Skipped expense record $id because vehicle $vehicleId is missing.", "garage-ledger-backup.json")
        return null
    }
    return ExpenseRecord(
        id = id,
        legacySourceId = sourceKey(),
        vehicleId = mappedVehicleId,
        dateTime = parseIsoDateTimeOrNull(dateTime)
            ?: return issueAndSkipExpense(id, dateTime, issues),
        odometerReading = odometerReading,
        distanceUnit = DistanceUnit.fromStorage(distanceUnit),
        totalCost = totalCost,
        paymentType = paymentType,
        expenseCenterName = expenseCenterName,
        expenseCenterAddress = expenseCenterAddress,
        latitude = latitude,
        longitude = longitude,
        tags = tags,
        notes = notes,
        expenseTypeIds = expenseTypeIds.mapNotNull { expenseTypeId ->
            expenseTypeSourceKeysByBackupId[expenseTypeId] ?: run {
                issues += issue(
                    "Ignored missing expense type $expenseTypeId for expense record $id.",
                    "garage-ledger-backup.json",
                )
                null
            }
        },
    )
}

private fun TripRecordPayload.toDomain(
    vehicleSourceKeysByBackupId: Map<Long, Long>,
    tripTypeSourceKeysByBackupId: Map<Long, Long>,
    issues: MutableList<ImportIssue>,
): TripRecord? {
    val mappedVehicleId = vehicleSourceKeysByBackupId[vehicleId]
    if (mappedVehicleId == null) {
        issues += issue("Skipped trip record $id because vehicle $vehicleId is missing.", "garage-ledger-backup.json")
        return null
    }
    return TripRecord(
        id = id,
        legacySourceId = sourceKey(),
        vehicleId = mappedVehicleId,
        startDateTime = parseIsoDateTimeOrNull(startDateTime)
            ?: return issueAndSkipTrip(id, startDateTime, issues),
        startOdometerReading = startOdometerReading,
        startLocation = startLocation,
        endDateTime = endDateTime?.let(::parseIsoDateTimeOrNull),
        endOdometerReading = endOdometerReading,
        endLocation = endLocation,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        endLatitude = endLatitude,
        endLongitude = endLongitude,
        distanceUnit = DistanceUnit.fromStorage(distanceUnit),
        distance = distance,
        durationMillis = durationMillis,
        tripTypeId = tripTypeId?.let(tripTypeSourceKeysByBackupId::get),
        purpose = purpose,
        client = client,
        taxDeductionRate = taxDeductionRate,
        taxDeductionAmount = taxDeductionAmount,
        reimbursementRate = reimbursementRate,
        reimbursementAmount = reimbursementAmount,
        paid = paid,
        tags = tags,
        notes = notes,
    )
}

private fun AttachmentPayload.toDomain(
    vehicleSourceKeysByBackupId: Map<Long, Long>,
    fillUpSourceKeysByBackupId: Map<Long, Long>,
    serviceRecordSourceKeysByBackupId: Map<Long, Long>,
    expenseRecordSourceKeysByBackupId: Map<Long, Long>,
    tripRecordSourceKeysByBackupId: Map<Long, Long>,
    issues: MutableList<ImportIssue>,
): RecordAttachment? {
    val mappedVehicleId = vehicleSourceKeysByBackupId[vehicleId]
    if (mappedVehicleId == null) {
        issues += issue("Skipped attachment '$displayName' because vehicle $vehicleId is missing.", "garage-ledger-backup.json")
        return null
    }
    val family = runCatching { RecordFamily.valueOf(recordFamily) }.getOrElse {
        issues += issue("Skipped attachment '$displayName' because record family '$recordFamily' is unknown.", "garage-ledger-backup.json")
        return null
    }
    val mappedRecordId = when (family) {
        RecordFamily.FILL_UP -> fillUpSourceKeysByBackupId[recordId]
        RecordFamily.SERVICE -> serviceRecordSourceKeysByBackupId[recordId]
        RecordFamily.EXPENSE -> expenseRecordSourceKeysByBackupId[recordId]
        RecordFamily.TRIP -> tripRecordSourceKeysByBackupId[recordId]
    }
    if (mappedRecordId == null) {
        issues += issue(
            "Skipped attachment '$displayName' because ${family.name.lowercase()} record $recordId is missing.",
            "garage-ledger-backup.json",
        )
        return null
    }
    return RecordAttachment(
        id = id,
        vehicleId = mappedVehicleId,
        recordFamily = family,
        recordId = mappedRecordId,
        uri = uri,
        mimeType = mimeType,
        displayName = displayName,
        createdAt = parseIsoDateTimeOrNull(createdAt) ?: LocalDateTime.now(),
    )
}

private fun issueAndSkipFillUp(
    id: Long,
    dateTime: String,
    issues: MutableList<ImportIssue>,
): FillUpRecord? {
    issues += issue("Skipped fill-up $id because '$dateTime' is not a valid ISO date-time.", "garage-ledger-backup.json")
    return null
}

private fun issueAndSkipService(
    id: Long,
    dateTime: String,
    issues: MutableList<ImportIssue>,
): ServiceRecord? {
    issues += issue("Skipped service record $id because '$dateTime' is not a valid ISO date-time.", "garage-ledger-backup.json")
    return null
}

private fun issueAndSkipExpense(
    id: Long,
    dateTime: String,
    issues: MutableList<ImportIssue>,
): ExpenseRecord? {
    issues += issue("Skipped expense record $id because '$dateTime' is not a valid ISO date-time.", "garage-ledger-backup.json")
    return null
}

private fun issueAndSkipTrip(
    id: Long,
    startDateTime: String,
    issues: MutableList<ImportIssue>,
): TripRecord? {
    issues += issue("Skipped trip record $id because '$startDateTime' is not a valid ISO date-time.", "garage-ledger-backup.json")
    return null
}

private fun parseIsoDateOrNull(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

private fun parseIsoDateTimeOrNull(value: String): LocalDateTime? = runCatching { LocalDateTime.parse(value) }.getOrNull()
