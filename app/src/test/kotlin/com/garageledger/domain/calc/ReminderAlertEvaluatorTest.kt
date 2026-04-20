package com.garageledger.domain.calc

import com.garageledger.domain.model.ServiceReminder
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

class ReminderAlertEvaluatorTest {
    @Test
    fun triggersTimeAlertInsideThresholdWindow() {
        val reminder = ServiceReminder(
            vehicleId = 1L,
            serviceTypeId = 7L,
            intervalTimeMonths = 6,
            dueDate = LocalDate.parse("2024-07-15"),
        )

        val trigger = ReminderAlertEvaluator.evaluate(
            reminder = reminder,
            now = LocalDateTime.parse("2024-07-01T08:00:00"),
            currentOdometer = 120000.0,
            timeThresholdPercent = 10,
            distanceThresholdPercent = 10,
        )

        assertThat(trigger).isNotNull()
        assertThat(trigger?.byTime).isTrue()
        assertThat(trigger?.byDistance).isFalse()
    }

    @Test
    fun suppressesDistanceAlertWhenAlreadyDeliveredToday() {
        val reminder = ServiceReminder(
            vehicleId = 1L,
            serviceTypeId = 7L,
            intervalDistance = 5000.0,
            dueDistance = 125000.0,
            lastDistanceAlert = LocalDateTime.parse("2024-07-01T07:15:00"),
        )

        val trigger = ReminderAlertEvaluator.evaluate(
            reminder = reminder,
            now = LocalDateTime.parse("2024-07-01T17:45:00"),
            currentOdometer = 124700.0,
            timeThresholdPercent = 10,
            distanceThresholdPercent = 10,
        )

        assertThat(trigger).isNull()
    }
}
