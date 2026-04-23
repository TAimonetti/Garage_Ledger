package com.guzzlio.data.export

import com.guzzlio.domain.model.CostStatisticsSummary
import com.guzzlio.domain.model.FillUpStatisticsSummary
import com.guzzlio.domain.model.OverallStatisticsSummary
import com.guzzlio.domain.model.StatisticsChart
import com.guzzlio.domain.model.StatisticsChartKey
import com.guzzlio.domain.model.StatisticsChartStyle
import com.guzzlio.domain.model.StatisticsDashboard
import com.guzzlio.domain.model.StatisticsPoint
import com.guzzlio.domain.model.TripStatisticsSummary
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.junit.Test

class StatisticsCsvExporterTest {
    @Test
    fun export_includesSummaryAndChartSections() {
        val dashboard = StatisticsDashboard(
            scopeLabel = "Corolla",
            periodDescription = "30 days",
            overall = OverallStatisticsSummary(
                vehicleCount = 1,
                totalRecordCount = 4,
                fillUpCount = 1,
                serviceCount = 1,
                expenseCount = 1,
                tripCount = 1,
                totalOperatingCost = 117.5,
            ),
            fillUps = FillUpStatisticsSummary(
                count = 1,
                totalVolume = 10.0,
                totalCost = 35.0,
                totalDistance = 300.0,
                averageFuelEfficiency = 30.0,
                lastFuelEfficiency = 30.0,
                averagePricePerUnit = 3.5,
                averageCostPerFillUp = 35.0,
            ),
            services = CostStatisticsSummary(count = 1, totalCost = 70.0, averageCost = 70.0),
            expenses = CostStatisticsSummary(count = 1, totalCost = 12.5, averageCost = 12.5),
            trips = TripStatisticsSummary(count = 1, totalDistance = 30.0, averageDistance = 30.0),
            charts = listOf(
                StatisticsChart(
                    key = StatisticsChartKey.FUEL_COST,
                    title = "Fuel Costs",
                    unitLabel = "$",
                    style = StatisticsChartStyle.BAR,
                    points = listOf(
                        StatisticsPoint(
                            label = "Apr 15",
                            value = 35.0,
                            recordedAt = LocalDateTime.parse("2026-04-15T08:00:00"),
                            vehicleId = 1,
                            vehicleName = "Corolla",
                        ),
                    ),
                ),
            ),
        )

        val csv = StatisticsCsvExporter().export(dashboard)

        assertThat(csv).contains("Statistics Metadata")
        assertThat(csv).contains("Overall Summary")
        assertThat(csv).contains("Fill-Up Summary")
        assertThat(csv).contains("Chart - Fuel Costs")
        assertThat(csv).contains("Corolla")
        assertThat(csv).contains("117.5")
    }
}
