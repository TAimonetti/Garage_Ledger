package com.garageledger.domain.calc

import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.FuelEfficiencyAssignmentMethod
import com.garageledger.core.model.FuelEfficiencyUnit
import com.garageledger.core.model.VolumeUnit
import com.garageledger.domain.model.FillUpRecord
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.junit.Test

class FuelEfficiencyCalculatorTest {
    @Test
    fun previousRecord_assignmentUsesNextFullTank() {
        val records = listOf(
            fuelUp(id = 1, odometer = 100.0, volume = 10.0, dateTime = LocalDateTime.parse("2024-01-01T08:00:00")),
            fuelUp(id = 2, odometer = 300.0, volume = 8.0, dateTime = LocalDateTime.parse("2024-01-05T08:00:00")),
        )

        val result = FuelEfficiencyCalculator.recalculate(
            records,
            FuelEfficiencyAssignmentMethod.PREVIOUS_RECORD,
            FuelEfficiencyUnit.MPG_US,
        )
        assertThat(result.first().fuelEfficiency).isWithin(0.0001).of(25.0)
        assertThat(result.last().fuelEfficiency).isNull()
        assertThat(result.first().fuelEfficiencyUnit).isEqualTo(FuelEfficiencyUnit.MPG_US)
    }

    @Test
    fun partialSequence_accumulatesVolumeUntilNextFullTank() {
        val records = listOf(
            fuelUp(id = 1, odometer = 100.0, volume = 10.0, dateTime = LocalDateTime.parse("2024-01-01T08:00:00")),
            fuelUp(id = 2, odometer = 180.0, volume = 3.0, partial = true, dateTime = LocalDateTime.parse("2024-01-03T08:00:00")),
            fuelUp(id = 3, odometer = 300.0, volume = 9.0, dateTime = LocalDateTime.parse("2024-01-05T08:00:00")),
        )

        val result = FuelEfficiencyCalculator.recalculate(
            records,
            FuelEfficiencyAssignmentMethod.PREVIOUS_RECORD,
            FuelEfficiencyUnit.MPG_US,
        )
        assertThat(result[0].fuelEfficiency).isWithin(0.0001).of(200.0 / 12.0)
        assertThat(result[1].fuelEfficiency).isNull()
        assertThat(result[2].fuelEfficiency).isNull()
    }

    @Test
    fun missedFillUp_breaksPreviousIntervalButAllowsFutureInterval() {
        val records = listOf(
            fuelUp(id = 1, odometer = 100.0, volume = 10.0, dateTime = LocalDateTime.parse("2024-01-01T08:00:00")),
            fuelUp(id = 2, odometer = 210.0, volume = 5.0, previousMissed = true, dateTime = LocalDateTime.parse("2024-01-02T08:00:00")),
            fuelUp(id = 3, odometer = 330.0, volume = 6.0, dateTime = LocalDateTime.parse("2024-01-03T08:00:00")),
        )

        val result = FuelEfficiencyCalculator.recalculate(
            records,
            FuelEfficiencyAssignmentMethod.PREVIOUS_RECORD,
            FuelEfficiencyUnit.MPG_US,
        )
        assertThat(result[0].fuelEfficiency).isNull()
        assertThat(result[1].fuelEfficiency).isWithin(0.0001).of(20.0)
    }

    @Test
    fun currentRecord_assignmentUsesCurrentFullTank() {
        val records = listOf(
            fuelUp(id = 1, odometer = 100.0, volume = 10.0, dateTime = LocalDateTime.parse("2024-01-01T08:00:00")),
            fuelUp(id = 2, odometer = 300.0, volume = 8.0, dateTime = LocalDateTime.parse("2024-01-05T08:00:00")),
        )

        val result = FuelEfficiencyCalculator.recalculate(
            records,
            FuelEfficiencyAssignmentMethod.CURRENT_RECORD,
            FuelEfficiencyUnit.MPG_US,
        )

        assertThat(result[0].fuelEfficiency).isNull()
        assertThat(result[1].fuelEfficiency).isWithin(0.0001).of(25.0)
    }

    @Test
    fun missedFillUp_breaksDistanceAndTimeIntervals() {
        val records = listOf(
            fuelUp(id = 1, odometer = 100.0, volume = 10.0, dateTime = LocalDateTime.parse("2024-01-01T08:00:00")),
            fuelUp(id = 2, odometer = 210.0, volume = 5.0, previousMissed = true, dateTime = LocalDateTime.parse("2024-01-02T08:00:00")),
            fuelUp(id = 3, odometer = 330.0, volume = 6.0, dateTime = LocalDateTime.parse("2024-01-04T08:00:00")),
        )

        val result = FuelEfficiencyCalculator.recalculate(
            records,
            FuelEfficiencyAssignmentMethod.PREVIOUS_RECORD,
            FuelEfficiencyUnit.MPG_US,
        )

        assertThat(result[0].distanceTillNextFillUp).isNull()
        assertThat(result[0].timeTillNextFillUpMillis).isNull()
        assertThat(result[1].distanceSincePrevious).isNull()
        assertThat(result[1].timeSincePreviousMillis).isNull()
        assertThat(result[1].distanceTillNextFillUp).isWithin(0.0001).of(120.0)
        assertThat(result[1].timeTillNextFillUpMillis).isEqualTo(172800000L)
    }

    @Test
    fun litersPer100km_conversionUsesMetricFormula() {
        val records = listOf(
            fuelUp(
                id = 1,
                odometer = 1000.0,
                volume = 40.0,
                distanceUnit = DistanceUnit.KILOMETERS,
                volumeUnit = VolumeUnit.LITERS,
                dateTime = LocalDateTime.parse("2024-01-01T08:00:00"),
            ),
            fuelUp(
                id = 2,
                odometer = 1500.0,
                volume = 35.0,
                distanceUnit = DistanceUnit.KILOMETERS,
                volumeUnit = VolumeUnit.LITERS,
                dateTime = LocalDateTime.parse("2024-01-10T08:00:00"),
            ),
        )

        val result = FuelEfficiencyCalculator.recalculate(
            records,
            FuelEfficiencyAssignmentMethod.CURRENT_RECORD,
            FuelEfficiencyUnit.LITERS_PER_100_KM,
        )

        assertThat(result[1].fuelEfficiency).isWithin(0.0001).of(7.0)
    }

    @Test
    fun mpgUk_conversionUsesImperialGallons() {
        val records = listOf(
            fuelUp(id = 1, odometer = 100.0, volume = 12.0, dateTime = LocalDateTime.parse("2024-01-01T08:00:00")),
            fuelUp(id = 2, odometer = 220.0, volume = 4.0, dateTime = LocalDateTime.parse("2024-01-05T08:00:00")),
        )

        val result = FuelEfficiencyCalculator.recalculate(
            records,
            FuelEfficiencyAssignmentMethod.CURRENT_RECORD,
            FuelEfficiencyUnit.MPG_UK,
        )

        assertThat(result[1].fuelEfficiency).isWithin(0.0001).of(36.0285)
    }

    private fun fuelUp(
        id: Long,
        odometer: Double,
        volume: Double,
        dateTime: LocalDateTime,
        distanceUnit: DistanceUnit = DistanceUnit.MILES,
        volumeUnit: VolumeUnit = VolumeUnit.GALLONS_US,
        partial: Boolean = false,
        previousMissed: Boolean = false,
    ) = FillUpRecord(
        id = id,
        vehicleId = 1L,
        dateTime = dateTime,
        odometerReading = odometer,
        distanceUnit = distanceUnit,
        volume = volume,
        volumeUnit = volumeUnit,
        pricePerUnit = 1.0,
        totalCost = volume,
        partial = partial,
        previousMissedFillups = previousMissed,
    )
}
