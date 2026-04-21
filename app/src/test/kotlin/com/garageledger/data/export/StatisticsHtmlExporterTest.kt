package com.garageledger.data.export

import com.garageledger.domain.model.CostStatisticsSummary
import com.garageledger.domain.model.FillUpStatisticsSummary
import com.garageledger.domain.model.OverallStatisticsSummary
import com.garageledger.domain.model.StatisticsChart
import com.garageledger.domain.model.StatisticsChartKey
import com.garageledger.domain.model.StatisticsChartStyle
import com.garageledger.domain.model.StatisticsDashboard
import com.garageledger.domain.model.StatisticsPoint
import com.garageledger.domain.model.TripStatisticsSummary
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.junit.Test

class StatisticsHtmlExporterTest {
    @Test
    fun export_includesSummarySectionsAndChartSvg() {
        val dashboard = StatisticsDashboard(
            scopeLabel = "Corolla",
            periodDescription = "All time",
            overall = OverallStatisticsSummary(
                vehicleCount = 1,
                totalRecordCount = 4,
                fillUpCount = 2,
                serviceCount = 1,
                expenseCount = 1,
                tripCount = 0,
                totalOperatingCost = 147.5,
            ),
            fillUps = FillUpStatisticsSummary(
                count = 2,
                totalVolume = 21.0,
                totalCost = 65.0,
                totalDistance = 610.0,
                averageFuelEfficiency = 29.2,
                lastFuelEfficiency = 30.4,
                averagePricePerUnit = 3.22,
                lastPricePerUnit = 3.31,
                averageCostPerFillUp = 32.5,
            ),
            services = CostStatisticsSummary(count = 1, totalCost = 70.0, averageCost = 70.0),
            expenses = CostStatisticsSummary(count = 1, totalCost = 12.5, averageCost = 12.5),
            trips = TripStatisticsSummary(count = 0),
            charts = listOf(
                StatisticsChart(
                    key = StatisticsChartKey.FUEL_EFFICIENCY,
                    title = "Fuel Efficiency",
                    unitLabel = "MPG (US)",
                    style = StatisticsChartStyle.LINE,
                    points = listOf(
                        StatisticsPoint(
                            label = "Apr 01",
                            value = 28.0,
                            recordedAt = LocalDateTime.parse("2026-04-01T08:00:00"),
                            vehicleId = 1,
                            vehicleName = "Corolla",
                        ),
                        StatisticsPoint(
                            label = "Apr 15",
                            value = 30.4,
                            recordedAt = LocalDateTime.parse("2026-04-15T08:00:00"),
                            vehicleId = 1,
                            vehicleName = "Corolla",
                        ),
                    ),
                ),
            ),
        )

        val html = StatisticsHtmlExporter().export(dashboard)

        assertThat(html).contains("<!DOCTYPE html>")
        assertThat(html).contains("Corolla Statistics &amp; Charts")
        assertThat(html).contains("Fill-Up Summary")
        assertThat(html).contains("Last Price")
        assertThat(html).contains("<svg")
        assertThat(html).contains("Fuel Efficiency")
        assertThat(html).contains("Apr 15")
    }
}
