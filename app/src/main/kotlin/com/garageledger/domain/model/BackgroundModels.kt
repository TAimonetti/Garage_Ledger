package com.garageledger.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class ReminderDisplayItem(
    val reminder: ServiceReminder,
    val serviceTypeName: String,
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
