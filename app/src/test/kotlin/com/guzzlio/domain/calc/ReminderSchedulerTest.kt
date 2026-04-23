package com.guzzlio.domain.calc

import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.domain.model.ServiceRecord
import com.guzzlio.domain.model.ServiceReminder
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

class ReminderSchedulerTest {
    @Test
    fun schedulesFromLastMatchingService() {
        val reminder = ServiceReminder(vehicleId = 1L, serviceTypeId = 42L, intervalTimeMonths = 6, intervalDistance = 5000.0)
        val history = listOf(
            ServiceRecord(
                id = 10L,
                vehicleId = 1L,
                dateTime = LocalDateTime.parse("2024-01-15T10:00:00"),
                odometerReading = 120000.0,
                distanceUnit = DistanceUnit.MILES,
                totalCost = 100.0,
                serviceTypeIds = listOf(42L),
            ),
        )

        val scheduled = ReminderScheduler.schedule(reminder, history, LocalDate.parse("2024-02-01"), 121000.0)
        assertThat(scheduled.dueDate).isEqualTo(LocalDate.parse("2024-07-15"))
        assertThat(scheduled.dueDistance).isEqualTo(125000.0)
    }
}
