package com.guzzlio.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class ReminderDisplayItem(
    val reminder: ServiceReminder,
    val serviceTypeName: String,
)

data class ReminderCenterItem(
    val reminder: ServiceReminder,
    val vehicleName: String,
    val serviceTypeName: String,
    val distanceUnitLabel: String,
    val currentOdometer: Double?,
)

data class ReminderWidgetItem(
    val reminderId: Long,
    val vehicleName: String,
    val serviceTypeName: String,
    val dueDate: LocalDate?,
    val dueDistance: Double?,
)

enum class FuelWidgetMetric {
    FUEL_EFFICIENCY,
    FUEL_PRICE,
}

data class FuelWidgetItem(
    val vehicleId: Long,
    val vehicleName: String,
    val metric: FuelWidgetMetric,
    val latestValue: Double?,
    val averageValue: Double?,
    val unitLabel: String,
)

data class VehiclePredictionSummary(
    val vehicleId: Long,
    val vehicleName: String,
    val nextFillUpDateTime: LocalDateTime?,
    val nextFillUpOdometerReading: Double?,
    val carRange: Double?,
    val tripCostPerDay: Double?,
    val tripCostPerDistanceUnit: Double?,
    val tripCostPer100DistanceUnit: Double?,
    val distanceUnitLabel: String,
    val currencySymbol: String,
)

data class PredictionWidgetItem(
    val vehicleId: Long,
    val vehicleName: String,
    val nextFillUpDateTime: LocalDateTime?,
    val nextFillUpOdometerReading: Double?,
    val carRange: Double?,
    val tripCostPer100DistanceUnit: Double?,
    val distanceUnitLabel: String,
    val currencySymbol: String,
)

data class ReminderAlert(
    val reminderId: Long,
    val vehicleId: Long,
    val vehicleName: String,
    val serviceTypeId: Long,
    val serviceTypeName: String,
    val dueDate: LocalDate?,
    val dueDistance: Double?,
    val currentOdometer: Double?,
    val triggeredByTime: Boolean,
    val triggeredByDistance: Boolean,
)

data class BackupRunResult(
    val filePath: String,
    val exportedAt: LocalDateTime,
    val retainedBackupCount: Int,
)
