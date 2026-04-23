package com.guzzlio.domain.calc

import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.core.model.VolumeUnit
import com.guzzlio.domain.model.ExpenseRecord
import com.guzzlio.domain.model.FillUpRecord
import com.guzzlio.domain.model.ServiceRecord
import com.guzzlio.domain.model.StatisticsFilter
import com.guzzlio.domain.model.StatisticsSource
import com.guzzlio.domain.model.StatisticsTimeframe
import com.guzzlio.domain.model.TripRecord
import com.guzzlio.domain.model.TripType
import com.guzzlio.domain.model.Vehicle
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

class StatisticsReportBuilderTest {
    private val builder = StatisticsReportBuilder()

    @Test
    fun build_filtersVehicleAndTimeframe_andCreatesExpectedCharts() {
        val source = StatisticsSource(
            vehicles = listOf(
                Vehicle(id = 1, name = "Corolla"),
                Vehicle(id = 2, name = "Tundra"),
            ),
            fillUps = listOf(
                FillUpRecord(
                    id = 1,
                    vehicleId = 1,
                    dateTime = LocalDateTime.parse("2026-04-15T08:00:00"),
                    odometerReading = 1000.0,
                    distanceUnit = DistanceUnit.MILES,
                    volume = 10.0,
                    volumeUnit = VolumeUnit.GALLONS_US,
                    pricePerUnit = 3.5,
                    totalCost = 35.0,
                    fuelEfficiency = 30.0,
                    distanceSincePrevious = 300.0,
                ),
                FillUpRecord(
                    id = 2,
                    vehicleId = 1,
                    dateTime = LocalDateTime.parse("2026-01-10T08:00:00"),
                    odometerReading = 700.0,
                    distanceUnit = DistanceUnit.MILES,
                    volume = 9.0,
                    volumeUnit = VolumeUnit.GALLONS_US,
                    pricePerUnit = 3.0,
                    totalCost = 27.0,
                    fuelEfficiency = 28.0,
                    distanceSincePrevious = 250.0,
                ),
                FillUpRecord(
                    id = 3,
                    vehicleId = 2,
                    dateTime = LocalDateTime.parse("2026-04-12T08:00:00"),
                    odometerReading = 5000.0,
                    distanceUnit = DistanceUnit.MILES,
                    volume = 14.0,
                    volumeUnit = VolumeUnit.GALLONS_US,
                    pricePerUnit = 3.8,
                    totalCost = 53.2,
                    fuelEfficiency = 17.0,
                    distanceSincePrevious = 220.0,
                ),
            ),
            services = listOf(
                ServiceRecord(
                    id = 10,
                    vehicleId = 1,
                    dateTime = LocalDateTime.parse("2026-04-16T10:00:00"),
                    odometerReading = 1010.0,
                    distanceUnit = DistanceUnit.MILES,
                    totalCost = 70.0,
                ),
            ),
            expenses = listOf(
                ExpenseRecord(
                    id = 11,
                    vehicleId = 1,
                    dateTime = LocalDateTime.parse("2026-04-17T10:00:00"),
                    odometerReading = 1015.0,
                    distanceUnit = DistanceUnit.MILES,
                    totalCost = 12.5,
                ),
            ),
            trips = listOf(
                TripRecord(
                    id = 12,
                    vehicleId = 1,
                    startDateTime = LocalDateTime.parse("2026-04-18T09:00:00"),
                    startOdometerReading = 1015.0,
                    endDateTime = LocalDateTime.parse("2026-04-18T11:00:00"),
                    endOdometerReading = 1045.0,
                    distanceUnit = DistanceUnit.MILES,
                    distance = 30.0,
                    reimbursementAmount = 20.0,
                    taxDeductionAmount = 9.0,
                    paid = true,
                ),
            ),
            tripTypes = listOf(
                TripType(id = 5, name = "Business"),
            ),
        )

        val dashboard = builder.build(
            source = source,
            filter = StatisticsFilter(vehicleId = 1L, timeframe = StatisticsTimeframe.LAST_30_DAYS),
            today = LocalDate.parse("2026-04-20"),
        )

        assertThat(dashboard.scopeLabel).isEqualTo("Corolla")
        assertThat(dashboard.periodDescription).contains("30 days")
        assertThat(dashboard.overall.totalRecordCount).isEqualTo(4)
        assertThat(dashboard.fillUps.count).isEqualTo(1)
        assertThat(dashboard.fillUps.totalCost).isWithin(0.0001).of(35.0)
        assertThat(dashboard.fillUps.averageFuelEfficiency).isWithin(0.0001).of(30.0)
        assertThat(dashboard.services.totalCost).isWithin(0.0001).of(70.0)
        assertThat(dashboard.expenses.totalCost).isWithin(0.0001).of(12.5)
        assertThat(dashboard.trips.totalDistance).isWithin(0.0001).of(30.0)
        assertThat(dashboard.trips.paidCount).isEqualTo(1)
        assertThat(dashboard.charts).hasSize(22)
        assertThat(dashboard.charts.first { it.title == "Fuel Costs" }.points).hasSize(1)
        assertThat(dashboard.charts.first { it.title == "Service Costs" }.points.single().value).isWithin(0.0001).of(70.0)
        assertThat(dashboard.charts.first { it.title == "Distance per Trip" }.points.single().value).isWithin(0.0001).of(30.0)
        assertThat(dashboard.charts.first { it.title == "Odometer Over Time" }.points).isNotEmpty()
        assertThat(dashboard.charts.first { it.title == "Total Costs by Vehicle" }.points.single().value).isWithin(0.0001).of(117.5)
    }

    @Test
    fun build_allVehiclesIncludesMultipleVehicleLabelsForCharts() {
        val source = StatisticsSource(
            vehicles = listOf(
                Vehicle(id = 1, name = "Corolla"),
                Vehicle(id = 2, name = "Tundra"),
            ),
            fillUps = listOf(
                FillUpRecord(
                    id = 1,
                    vehicleId = 1,
                    dateTime = LocalDateTime.parse("2026-04-15T08:00:00"),
                    odometerReading = 1000.0,
                    distanceUnit = DistanceUnit.MILES,
                    volume = 10.0,
                    volumeUnit = VolumeUnit.GALLONS_US,
                    pricePerUnit = 3.5,
                    totalCost = 35.0,
                    fuelEfficiency = 30.0,
                ),
                FillUpRecord(
                    id = 2,
                    vehicleId = 2,
                    dateTime = LocalDateTime.parse("2026-04-16T08:00:00"),
                    odometerReading = 5000.0,
                    distanceUnit = DistanceUnit.MILES,
                    volume = 14.0,
                    volumeUnit = VolumeUnit.GALLONS_US,
                    pricePerUnit = 3.8,
                    totalCost = 53.2,
                    fuelEfficiency = 17.0,
                ),
            ),
        )

        val dashboard = builder.build(
            source = source,
            filter = StatisticsFilter(),
            today = LocalDate.parse("2026-04-20"),
        )

        assertThat(dashboard.scopeLabel).isEqualTo("All Vehicles")
        assertThat(dashboard.overall.vehicleCount).isEqualTo(2)
        assertThat(dashboard.charts.first().points.map { it.label }).containsAtLeast("Apr 15 Corolla", "Apr 16 Tundra")
    }
}
