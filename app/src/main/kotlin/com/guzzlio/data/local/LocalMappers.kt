package com.guzzlio.data.local

import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.core.model.FuelEfficiencyUnit
import com.guzzlio.core.model.VolumeUnit
import com.guzzlio.domain.model.ExpenseRecord
import com.guzzlio.domain.model.ExpenseType
import com.guzzlio.domain.model.FillUpRecord
import com.guzzlio.domain.model.FuelType
import com.guzzlio.domain.model.RecordAttachment
import com.guzzlio.domain.model.ServiceRecord
import com.guzzlio.domain.model.ServiceReminder
import com.guzzlio.domain.model.ServiceType
import com.guzzlio.domain.model.TripRecord
import com.guzzlio.domain.model.TripType
import com.guzzlio.domain.model.Vehicle
import com.guzzlio.domain.model.VehicleLifecycle
import com.guzzlio.domain.model.VehiclePart

fun VehicleEntity.toDomain(): Vehicle = Vehicle(
    id = id,
    legacySourceId = legacySourceId,
    name = name,
    type = type,
    year = year,
    make = make,
    model = model,
    submodel = submodel,
    lifecycle = VehicleLifecycle.valueOf(lifecycle),
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
    purchaseDate = purchaseDate,
    sellingPrice = sellingPrice,
    sellingOdometer = sellingOdometer,
    sellingDate = sellingDate,
    notes = notes,
    profilePhotoUri = profilePhotoUri,
    distanceUnitOverride = distanceUnitOverride?.let(DistanceUnit::fromStorage),
    volumeUnitOverride = volumeUnitOverride?.let(VolumeUnit::fromStorage),
    fuelEfficiencyUnitOverride = fuelEfficiencyUnitOverride?.let(FuelEfficiencyUnit::fromStorage),
)

fun Vehicle.toEntity(idOverride: Long = id): VehicleEntity = VehicleEntity(
    id = idOverride,
    legacySourceId = legacySourceId,
    name = name,
    type = type,
    year = year,
    make = make,
    model = model,
    submodel = submodel,
    lifecycle = lifecycle.name,
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
    purchaseDate = purchaseDate,
    sellingPrice = sellingPrice,
    sellingOdometer = sellingOdometer,
    sellingDate = sellingDate,
    notes = notes,
    profilePhotoUri = profilePhotoUri,
    distanceUnitOverride = distanceUnitOverride?.storageValue,
    volumeUnitOverride = volumeUnitOverride?.storageValue,
    fuelEfficiencyUnitOverride = fuelEfficiencyUnitOverride?.storageValue,
)

fun VehiclePartEntity.toDomain(): VehiclePart = VehiclePart(
    id = id,
    legacySourceId = legacySourceId,
    vehicleId = vehicleId,
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

fun VehiclePart.toEntity(vehicleIdOverride: Long = vehicleId): VehiclePartEntity = VehiclePartEntity(
    id = id,
    legacySourceId = legacySourceId,
    vehicleId = vehicleIdOverride,
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

fun FuelTypeEntity.toDomain(): FuelType = FuelType(id, legacySourceId, category, grade, octane, cetane, notes)

fun FuelType.toEntity(idOverride: Long = id): FuelTypeEntity = FuelTypeEntity(idOverride, legacySourceId, category, grade, octane, cetane, notes)

fun ServiceTypeEntity.toDomain(): ServiceType = ServiceType(id, legacySourceId, name, notes, defaultTimeReminderMonths, defaultDistanceReminder)

fun ServiceType.toEntity(idOverride: Long = id): ServiceTypeEntity = ServiceTypeEntity(idOverride, legacySourceId, name, notes, defaultTimeReminderMonths, defaultDistanceReminder)

fun ExpenseTypeEntity.toDomain(): ExpenseType = ExpenseType(id, legacySourceId, name, notes)

fun ExpenseType.toEntity(idOverride: Long = id): ExpenseTypeEntity = ExpenseTypeEntity(idOverride, legacySourceId, name, notes)

fun TripTypeEntity.toDomain(): TripType = TripType(id, legacySourceId, name, defaultTaxDeductionRate, notes)

fun TripType.toEntity(idOverride: Long = id): TripTypeEntity = TripTypeEntity(idOverride, legacySourceId, name, defaultTaxDeductionRate, notes)

fun ServiceReminderEntity.toDomain(): ServiceReminder = ServiceReminder(
    id,
    legacySourceId,
    vehicleId,
    serviceTypeId,
    intervalTimeMonths,
    intervalDistance,
    dueDate,
    dueDistance,
    timeAlertSilent,
    distanceAlertSilent,
    lastTimeAlert,
    lastDistanceAlert,
)

fun ServiceReminder.toEntity(
    vehicleIdOverride: Long = vehicleId,
    serviceTypeIdOverride: Long = serviceTypeId,
): ServiceReminderEntity = ServiceReminderEntity(
    id,
    legacySourceId,
    vehicleIdOverride,
    serviceTypeIdOverride,
    intervalTimeMonths,
    intervalDistance,
    dueDate,
    dueDistance,
    timeAlertSilent,
    distanceAlertSilent,
    lastTimeAlert,
    lastDistanceAlert,
)

fun FillUpRecordEntity.toDomain(): FillUpRecord = FillUpRecord(
    id = id,
    legacySourceId = legacySourceId,
    vehicleId = vehicleId,
    dateTime = dateTime,
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
    fuelTypeId = fuelTypeId,
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

fun FillUpRecord.toEntity(vehicleIdOverride: Long = vehicleId): FillUpRecordEntity = FillUpRecordEntity(
    id = id,
    legacySourceId = legacySourceId,
    vehicleId = vehicleIdOverride,
    dateTime = dateTime,
    odometerReading = odometerReading,
    distanceUnit = distanceUnit.storageValue,
    volume = volume,
    volumeUnit = volumeUnit.storageValue,
    pricePerUnit = pricePerUnit,
    totalCost = totalCost,
    paymentType = paymentType,
    partial = partial,
    previousMissedFillups = previousMissedFillups,
    fuelEfficiency = fuelEfficiency,
    fuelEfficiencyUnit = fuelEfficiencyUnit?.storageValue,
    importedFuelEfficiency = importedFuelEfficiency,
    fuelTypeId = fuelTypeId,
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

fun ServiceRecordEntity.toDomain(serviceTypeIds: List<Long>): ServiceRecord = ServiceRecord(
    id,
    legacySourceId,
    vehicleId,
    dateTime,
    odometerReading,
    DistanceUnit.fromStorage(distanceUnit),
    totalCost,
    paymentType,
    serviceCenterName,
    serviceCenterAddress,
    latitude,
    longitude,
    tags,
    notes,
    serviceTypeIds,
)

fun ServiceRecord.toEntity(vehicleIdOverride: Long = vehicleId): ServiceRecordEntity = ServiceRecordEntity(
    id,
    legacySourceId,
    vehicleIdOverride,
    dateTime,
    odometerReading,
    distanceUnit.storageValue,
    totalCost,
    paymentType,
    serviceCenterName,
    serviceCenterAddress,
    latitude,
    longitude,
    tags,
    notes,
)

fun ExpenseRecordEntity.toDomain(expenseTypeIds: List<Long>): ExpenseRecord = ExpenseRecord(
    id,
    legacySourceId,
    vehicleId,
    dateTime,
    odometerReading,
    DistanceUnit.fromStorage(distanceUnit),
    totalCost,
    paymentType,
    expenseCenterName,
    expenseCenterAddress,
    latitude,
    longitude,
    tags,
    notes,
    expenseTypeIds,
)

fun ExpenseRecord.toEntity(vehicleIdOverride: Long = vehicleId): ExpenseRecordEntity = ExpenseRecordEntity(
    id,
    legacySourceId,
    vehicleIdOverride,
    dateTime,
    odometerReading,
    distanceUnit.storageValue,
    totalCost,
    paymentType,
    expenseCenterName,
    expenseCenterAddress,
    latitude,
    longitude,
    tags,
    notes,
)

fun TripRecordEntity.toDomain(): TripRecord = TripRecord(
    id,
    legacySourceId,
    vehicleId,
    startDateTime,
    startOdometerReading,
    startLocation,
    endDateTime,
    endOdometerReading,
    endLocation,
    startLatitude,
    startLongitude,
    endLatitude,
    endLongitude,
    DistanceUnit.fromStorage(distanceUnit),
    distance,
    durationMillis,
    tripTypeId,
    purpose,
    client,
    taxDeductionRate,
    taxDeductionAmount,
    reimbursementRate,
    reimbursementAmount,
    paid,
    tags,
    notes,
)

fun TripRecord.toEntity(vehicleIdOverride: Long = vehicleId): TripRecordEntity = TripRecordEntity(
    id,
    legacySourceId,
    vehicleIdOverride,
    startDateTime,
    startOdometerReading,
    startLocation,
    endDateTime,
    endOdometerReading,
    endLocation,
    startLatitude,
    startLongitude,
    endLatitude,
    endLongitude,
    distanceUnit.storageValue,
    distance,
    durationMillis,
    tripTypeId,
    purpose,
    client,
    taxDeductionRate,
    taxDeductionAmount,
    reimbursementRate,
    reimbursementAmount,
    paid,
    tags,
    notes,
)

fun RecordAttachmentEntity.toDomain(): RecordAttachment = RecordAttachment(id, vehicleId, recordFamily, recordId, uri, mimeType, displayName, createdAt)

fun RecordAttachment.toEntity(): RecordAttachmentEntity = RecordAttachmentEntity(id, vehicleId, recordFamily, recordId, uri, mimeType, displayName, createdAt)
