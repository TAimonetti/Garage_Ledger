package com.garageledger.domain.calc

import com.garageledger.domain.model.ServiceReminder
import java.time.LocalDateTime

object ReminderAlertEvaluator {
    data class Trigger(
        val byTime: Boolean,
        val byDistance: Boolean,
    )

    fun evaluate(
        reminder: ServiceReminder,
        now: LocalDateTime,
        currentOdometer: Double?,
        timeThresholdPercent: Int,
        distanceThresholdPercent: Int,
    ): Trigger? {
        val byTime = reminder.dueDate != null &&
            reminder.lastTimeAlert?.toLocalDate() != now.toLocalDate() &&
            ReminderScheduler.shouldAlertByTime(
                reminder = reminder,
                now = now,
                thresholdPercent = timeThresholdPercent,
                createdAt = reminder.dueDate.minusMonths(reminder.intervalTimeMonths.toLong()),
            )

        val byDistance = reminder.dueDistance != null &&
            reminder.lastDistanceAlert?.toLocalDate() != now.toLocalDate() &&
            ReminderScheduler.shouldAlertByDistance(
                reminder = reminder,
                currentOdometer = currentOdometer,
                thresholdPercent = distanceThresholdPercent,
                createdOdometer = reminder.intervalDistance?.let { reminder.dueDistance - it },
            )

        return if (byTime || byDistance) Trigger(byTime = byTime, byDistance = byDistance) else null
    }
}
