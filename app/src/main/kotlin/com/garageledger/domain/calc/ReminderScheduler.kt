package com.garageledger.domain.calc

import com.garageledger.domain.model.ServiceRecord
import com.garageledger.domain.model.ServiceReminder
import java.time.LocalDate
import java.time.LocalDateTime

object ReminderScheduler {
    fun schedule(
        reminder: ServiceReminder,
        serviceHistory: List<ServiceRecord>,
        currentDate: LocalDate,
        currentOdometer: Double?,
    ): ServiceReminder {
        val matchingService = serviceHistory
            .filter { reminder.serviceTypeId in it.serviceTypeIds }
            .maxByOrNull { it.dateTime }

        val baseDate = matchingService?.dateTime?.toLocalDate() ?: currentDate
        val baseOdometer = matchingService?.odometerReading ?: currentOdometer

        val dueDate = if (reminder.intervalTimeMonths > 0) {
            baseDate.plusMonths(reminder.intervalTimeMonths.toLong())
        } else {
            null
        }

        val dueDistance = if ((reminder.intervalDistance ?: 0.0) > 0.0 && baseOdometer != null) {
            baseOdometer + (reminder.intervalDistance ?: 0.0)
        } else {
            null
        }

        return reminder.copy(dueDate = dueDate, dueDistance = dueDistance)
    }

    fun shouldAlertByTime(
        reminder: ServiceReminder,
        now: LocalDateTime,
        thresholdPercent: Int,
        createdAt: LocalDate,
    ): Boolean {
        if (reminder.timeAlertSilent) return false
        val dueDate = reminder.dueDate ?: return false
        val totalDays = createdAt.until(dueDate).days.coerceAtLeast(1)
        val remaining = now.toLocalDate().until(dueDate).days
        return remaining <= (totalDays * thresholdPercent / 100.0)
    }

    fun shouldAlertByDistance(
        reminder: ServiceReminder,
        currentOdometer: Double?,
        thresholdPercent: Int,
        createdOdometer: Double?,
    ): Boolean {
        if (reminder.distanceAlertSilent) return false
        val dueDistance = reminder.dueDistance ?: return false
        if (currentOdometer == null || createdOdometer == null) return false
        val totalDistance = (dueDistance - createdOdometer).coerceAtLeast(1.0)
        val remaining = dueDistance - currentOdometer
        return remaining <= totalDistance * thresholdPercent / 100.0
    }
}
