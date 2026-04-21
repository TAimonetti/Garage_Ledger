package com.garageledger.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class ReminderDisplayItem(
    val reminder: ServiceReminder,
    val serviceTypeName: String,
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
