package com.garageledger.domain.calc

import com.garageledger.domain.model.CostStatisticsSummary
import com.garageledger.domain.model.FillUpRecord
import com.garageledger.domain.model.FillUpStatisticsSummary
import com.garageledger.domain.model.OverallStatisticsSummary
import com.garageledger.domain.model.StatisticsChart
import com.garageledger.domain.model.StatisticsChartKey
import com.garageledger.domain.model.StatisticsChartStyle
import com.garageledger.domain.model.StatisticsDashboard
import com.garageledger.domain.model.StatisticsFilter
import com.garageledger.domain.model.StatisticsPoint
import com.garageledger.domain.model.StatisticsSource
import com.garageledger.domain.model.StatisticsTimeframe
import com.garageledger.domain.model.TripRecord
import com.garageledger.domain.model.TripStatisticsSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class StatisticsReportBuilder(
    private val labelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US),
) {
    fun build(
        source: StatisticsSource,
        filter: StatisticsFilter,
        today: LocalDate = LocalDate.now(),
    ): StatisticsDashboard {
        val scopedVehicles = if (filter.vehicleId == null) {
            source.vehicles
        } else {
            source.vehicles.filter { it.id == filter.vehicleId }
        }
        val scopedVehicleIds = scopedVehicles.mapTo(linkedSetOf<Long>()) { it.id }.ifEmpty {
            filter.vehicleId?.let { linkedSetOf(it) } ?: linkedSetOf()
        }
        val vehicleNamesById = source.vehicles.associate { it.id to it.name }
        val startDate = filter.timeframe.startDate(today)
        val includeVehicleInLabels = filter.vehicleId == null && scopedVehicles.size > 1

        fun withinScope(vehicleId: Long, occurredAt: LocalDateTime): Boolean {
            val inVehicleScope = scopedVehicleIds.isEmpty() || vehicleId in scopedVehicleIds
            val inDateScope = startDate == null || !occurredAt.toLocalDate().isBefore(startDate)
            return inVehicleScope && inDateScope
        }

        val fillUps = source.fillUps
            .filter { withinScope(it.vehicleId, it.dateTime) }
            .sortedBy(FillUpRecord::dateTime)
        val services = source.services
            .filter { withinScope(it.vehicleId, it.dateTime) }
            .sortedBy { it.dateTime }
        val expenses = source.expenses
            .filter { withinScope(it.vehicleId, it.dateTime) }
            .sortedBy { it.dateTime }
        val trips = source.trips
            .filter { withinScope(it.vehicleId, it.startDateTime) }
            .sortedBy(TripRecord::startDateTime)

        val efficiencyValues = fillUps.mapNotNull { it.fuelEfficiency ?: it.importedFuelEfficiency }
        val scopeLabel = scopedVehicles.singleOrNull()?.name
            ?: if (filter.vehicleId != null) "Selected Vehicle" else "All Vehicles"
        val periodDescription = startDate?.let { "${filter.timeframe.label} (since $it)" } ?: filter.timeframe.label

        return StatisticsDashboard(
            preferences = source.preferences,
            filter = filter,
            scopeLabel = scopeLabel,
            periodDescription = periodDescription,
            overall = OverallStatisticsSummary(
                vehicleCount = when {
                    filter.vehicleId != null -> 1
                    scopedVehicles.isNotEmpty() -> scopedVehicles.size
                    else -> source.vehicles.size
                },
                totalRecordCount = fillUps.size + services.size + expenses.size + trips.size,
                fillUpCount = fillUps.size,
                serviceCount = services.size,
                expenseCount = expenses.size,
                tripCount = trips.size,
                totalOperatingCost = fillUps.sumOf { it.totalCost } + services.sumOf { it.totalCost } + expenses.sumOf { it.totalCost },
            ),
            fillUps = FillUpStatisticsSummary(
                count = fillUps.size,
                totalVolume = fillUps.sumOf { it.volume },
                totalCost = fillUps.sumOf { it.totalCost },
                totalDistance = fillUps.sumOf { it.distanceSincePrevious ?: 0.0 },
                averageFuelEfficiency = efficiencyValues.averageOrNull(),
                lastFuelEfficiency = fillUps.asReversed().firstNotNullOfOrNull { it.fuelEfficiency ?: it.importedFuelEfficiency },
                averagePricePerUnit = fillUps.map(FillUpRecord::pricePerUnit).averageOrNull(),
                averageCostPerFillUp = fillUps.map(FillUpRecord::totalCost).averageOrNull(),
            ),
            services = CostStatisticsSummary(
                count = services.size,
                totalCost = services.sumOf { it.totalCost },
                averageCost = services.map { it.totalCost }.averageOrNull(),
            ),
            expenses = CostStatisticsSummary(
                count = expenses.size,
                totalCost = expenses.sumOf { it.totalCost },
                averageCost = expenses.map { it.totalCost }.averageOrNull(),
            ),
            trips = TripStatisticsSummary(
                count = trips.size,
                totalDistance = trips.sumOf { it.derivedDistance() },
                averageDistance = trips.map { it.derivedDistance() }.averageOrNull(),
                totalTaxDeduction = trips.sumOf { it.taxDeductionAmount ?: 0.0 },
                totalReimbursement = trips.sumOf { it.reimbursementAmount ?: 0.0 },
                paidCount = trips.count(TripRecord::paid),
                openCount = trips.count { it.endDateTime == null || it.endOdometerReading == null },
            ),
            charts = listOf(
                StatisticsChart(
                    key = StatisticsChartKey.FUEL_EFFICIENCY,
                    title = "Fuel Efficiency",
                    unitLabel = source.preferences.fuelEfficiencyUnit.storageValue,
                    style = StatisticsChartStyle.LINE,
                    points = fillUps.mapNotNull { record ->
                        val value = record.fuelEfficiency ?: record.importedFuelEfficiency ?: return@mapNotNull null
                        record.toPoint(value, vehicleNamesById, includeVehicleInLabels)
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.FUEL_PRICE,
                    title = "Fuel Price",
                    unitLabel = "${source.preferences.currencySymbol}/${source.preferences.volumeUnit.storageValue}",
                    style = StatisticsChartStyle.LINE,
                    points = fillUps.map { it.toPoint(it.pricePerUnit, vehicleNamesById, includeVehicleInLabels) },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.FUEL_COST,
                    title = "Fuel Costs",
                    unitLabel = source.preferences.currencySymbol,
                    style = StatisticsChartStyle.BAR,
                    points = fillUps.map { it.toPoint(it.totalCost, vehicleNamesById, includeVehicleInLabels) },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.SERVICE_COST,
                    title = "Service Costs",
                    unitLabel = source.preferences.currencySymbol,
                    style = StatisticsChartStyle.BAR,
                    points = services.map { record ->
                        StatisticsPoint(
                            label = chartLabel(record.dateTime, vehicleNamesById[record.vehicleId], includeVehicleInLabels),
                            value = record.totalCost,
                            recordedAt = record.dateTime,
                            vehicleId = record.vehicleId,
                            vehicleName = vehicleNamesById[record.vehicleId].orEmpty(),
                        )
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.EXPENSE_COST,
                    title = "Expense Costs",
                    unitLabel = source.preferences.currencySymbol,
                    style = StatisticsChartStyle.BAR,
                    points = expenses.map { record ->
                        StatisticsPoint(
                            label = chartLabel(record.dateTime, vehicleNamesById[record.vehicleId], includeVehicleInLabels),
                            value = record.totalCost,
                            recordedAt = record.dateTime,
                            vehicleId = record.vehicleId,
                            vehicleName = vehicleNamesById[record.vehicleId].orEmpty(),
                        )
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.DISTANCE_BETWEEN_FILLUPS,
                    title = "Distance Between Fill-Ups",
                    unitLabel = source.preferences.distanceUnit.storageValue,
                    style = StatisticsChartStyle.LINE,
                    points = fillUps.mapNotNull { record ->
                        val value = record.distanceSincePrevious ?: return@mapNotNull null
                        record.toPoint(value, vehicleNamesById, includeVehicleInLabels)
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.PRICE_PER_UNIT,
                    title = "Price per Unit",
                    unitLabel = "${source.preferences.currencySymbol}/${source.preferences.volumeUnit.storageValue}",
                    style = StatisticsChartStyle.BAR,
                    points = fillUps.map { it.toPoint(it.pricePerUnit, vehicleNamesById, includeVehicleInLabels) },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.VOLUME_PER_FILLUP,
                    title = "Volume per Fill-Up",
                    unitLabel = source.preferences.volumeUnit.storageValue,
                    style = StatisticsChartStyle.BAR,
                    points = fillUps.map { it.toPoint(it.volume, vehicleNamesById, includeVehicleInLabels) },
                ),
            ),
        )
    }

    private fun FillUpRecord.toPoint(
        value: Double,
        vehicleNamesById: Map<Long, String>,
        includeVehicleInLabels: Boolean,
    ): StatisticsPoint = StatisticsPoint(
        label = chartLabel(dateTime, vehicleNamesById[vehicleId], includeVehicleInLabels),
        value = value,
        recordedAt = dateTime,
        vehicleId = vehicleId,
        vehicleName = vehicleNamesById[vehicleId].orEmpty(),
    )

    private fun chartLabel(
        occurredAt: LocalDateTime,
        vehicleName: String?,
        includeVehicleName: Boolean,
    ): String {
        val base = occurredAt.format(labelFormatter)
        return if (includeVehicleName && !vehicleName.isNullOrBlank()) "$base ${vehicleName.trim()}" else base
    }

    private fun StatisticsTimeframe.startDate(today: LocalDate): LocalDate? = when (this) {
        StatisticsTimeframe.ALL_TIME -> null
        StatisticsTimeframe.LAST_30_DAYS -> today.minusDays(29)
        StatisticsTimeframe.LAST_90_DAYS -> today.minusDays(89)
        StatisticsTimeframe.LAST_365_DAYS -> today.minusDays(364)
    }

    private fun TripRecord.derivedDistance(): Double = distance ?: when {
        endOdometerReading != null -> endOdometerReading - startOdometerReading
        else -> 0.0
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
