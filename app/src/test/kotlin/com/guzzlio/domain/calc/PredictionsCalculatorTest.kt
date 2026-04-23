package com.guzzlio.domain.calc

import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.core.model.VolumeUnit
import com.guzzlio.domain.model.ExpenseRecord
import com.guzzlio.domain.model.FillUpRecord
import com.guzzlio.domain.model.ServiceRecord
import com.guzzlio.domain.model.TripRecord
import com.guzzlio.domain.model.Vehicle
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.junit.Test

class PredictionsCalculatorTest {
    @Test
    fun buildsPredictionsFromFillUpsAndCosts() {
        val vehicle = Vehicle(
            id = 7L,
            name = "Corolla",
            fuelTankCapacity = 10.0,
            distanceUnitOverride = DistanceUnit.MILES,
        )
        val fillUps = listOf(
            FillUpRecord(
                id = 1L,
                vehicleId = vehicle.id,
                dateTime = LocalDateTime.parse("2026-01-01T08:00:00"),
                odometerReading = 1000.0,
                distanceUnit = DistanceUnit.MILES,
                volume = 8.0,
                volumeUnit = VolumeUnit.GALLONS_US,
                pricePerUnit = 3.0,
                totalCost = 24.0,
            ),
            FillUpRecord(
                id = 2L,
                vehicleId = vehicle.id,
                dateTime = LocalDateTime.parse("2026-01-06T08:00:00"),
                odometerReading = 1300.0,
                distanceUnit = DistanceUnit.MILES,
                volume = 8.0,
                volumeUnit = VolumeUnit.GALLONS_US,
                pricePerUnit = 3.1,
                totalCost = 24.8,
                distanceSincePrevious = 300.0,
            ),
            FillUpRecord(
                id = 3L,
                vehicleId = vehicle.id,
                dateTime = LocalDateTime.parse("2026-01-11T08:00:00"),
                odometerReading = 1600.0,
                distanceUnit = DistanceUnit.MILES,
                volume = 8.0,
                volumeUnit = VolumeUnit.GALLONS_US,
                pricePerUnit = 3.2,
                totalCost = 25.6,
                distanceSincePrevious = 300.0,
            ),
        )
        val services = listOf(
            ServiceRecord(
                id = 10L,
                vehicleId = vehicle.id,
                dateTime = LocalDateTime.parse("2026-01-03T09:00:00"),
                odometerReading = 1100.0,
                distanceUnit = DistanceUnit.MILES,
                totalCost = 100.0,
            ),
        )
        val expenses = listOf(
            ExpenseRecord(
                id = 11L,
                vehicleId = vehicle.id,
                dateTime = LocalDateTime.parse("2026-01-04T09:00:00"),
                odometerReading = 1150.0,
                distanceUnit = DistanceUnit.MILES,
                totalCost = 40.0,
            ),
        )
        val trips = listOf(
            TripRecord(
                id = 12L,
                vehicleId = vehicle.id,
                startDateTime = LocalDateTime.parse("2026-01-02T09:00:00"),
                startOdometerReading = 1025.0,
                endDateTime = LocalDateTime.parse("2026-01-10T10:00:00"),
                endOdometerReading = 1500.0,
                distanceUnit = DistanceUnit.MILES,
            ),
        )

        val prediction = PredictionsCalculator.build(
            vehicle = vehicle,
            fillUps = fillUps,
            services = services,
            expenses = expenses,
            trips = trips,
            now = LocalDateTime.parse("2026-01-12T08:00:00"),
            currencySymbol = "$",
        )

        assertThat(prediction.nextFillUpDateTime).isEqualTo(LocalDateTime.parse("2026-01-16T08:00:00"))
        assertThat(prediction.nextFillUpOdometerReading).isEqualTo(1900.0)
        assertThat(prediction.carRange).isWithin(0.01).of(375.0)
        assertThat(prediction.tripCostPerDay).isWithin(0.01).of(21.44)
        assertThat(prediction.tripCostPerDistanceUnit).isWithin(0.0001).of(0.3573333333)
        assertThat(prediction.tripCostPer100DistanceUnit).isWithin(0.01).of(35.73333333)
    }
}
