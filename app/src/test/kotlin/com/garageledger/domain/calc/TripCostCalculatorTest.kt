package com.garageledger.domain.calc

import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.VolumeUnit
import com.garageledger.domain.model.ExpenseRecord
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.ServiceRecord
import com.garageledger.domain.model.TripRecord
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.junit.Test

class TripCostCalculatorTest {
    @Test
    fun combinesDirectAndEstimatedCosts() {
        val fillUps = listOf(
            FillUpRecord(
                id = 1,
                vehicleId = 1,
                dateTime = LocalDateTime.parse("2024-01-01T10:00:00"),
                odometerReading = 100.0,
                distanceUnit = DistanceUnit.MILES,
                volume = 10.0,
                volumeUnit = VolumeUnit.GALLONS_US,
                pricePerUnit = 3.0,
                totalCost = 30.0,
                distanceSincePrevious = 100.0,
            ),
            FillUpRecord(
                id = 2,
                vehicleId = 1,
                dateTime = LocalDateTime.parse("2024-01-05T10:00:00"),
                odometerReading = 200.0,
                distanceUnit = DistanceUnit.MILES,
                volume = 10.0,
                volumeUnit = VolumeUnit.GALLONS_US,
                pricePerUnit = 3.0,
                totalCost = 30.0,
                distanceSincePrevious = 100.0,
            ),
        )
        val services = listOf(
            ServiceRecord(
                id = 1,
                vehicleId = 1,
                dateTime = LocalDateTime.parse("2024-01-10T10:00:00"),
                odometerReading = 200.0,
                distanceUnit = DistanceUnit.MILES,
                totalCost = 40.0,
            ),
            ServiceRecord(
                id = 2,
                vehicleId = 1,
                dateTime = LocalDateTime.parse("2024-02-10T10:00:00"),
                odometerReading = 400.0,
                distanceUnit = DistanceUnit.MILES,
                totalCost = 60.0,
            ),
        )
        val trip = TripRecord(
            vehicleId = 1,
            startDateTime = LocalDateTime.parse("2024-02-15T10:00:00"),
            startOdometerReading = 400.0,
            endDateTime = LocalDateTime.parse("2024-02-16T10:00:00"),
            endOdometerReading = 500.0,
            distanceUnit = DistanceUnit.MILES,
            distance = 100.0,
        )
        val directExpenses = listOf(
            ExpenseRecord(
                id = 1,
                vehicleId = 1,
                dateTime = LocalDateTime.parse("2024-02-15T12:00:00"),
                odometerReading = 450.0,
                distanceUnit = DistanceUnit.MILES,
                totalCost = 25.0,
            ),
        )

        val result = TripCostCalculator.calculate(
            trip = trip,
            directExpenses = directExpenses,
            fuelCostPerDistance = TripCostCalculator.deriveFuelCostPerDistance(fillUps),
            serviceCostPerDistance = TripCostCalculator.deriveServiceCostPerDistance(services),
        )

        assertThat(result.directExpenseCost).isEqualTo(25.0)
        assertThat(result.estimatedFuelCost).isWithin(0.0001).of(30.0)
        assertThat(result.estimatedServiceCost).isWithin(0.0001).of(50.0)
        assertThat(result.totalCost).isWithin(0.0001).of(105.0)
    }
}
