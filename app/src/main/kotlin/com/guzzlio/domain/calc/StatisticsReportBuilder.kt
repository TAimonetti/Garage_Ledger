package com.guzzlio.domain.calc

import com.guzzlio.domain.model.CostStatisticsSummary
import com.guzzlio.domain.model.ExpenseRecord
import com.guzzlio.domain.model.FillUpRecord
import com.guzzlio.domain.model.FillUpStatisticsSummary
import com.guzzlio.domain.model.OverallStatisticsSummary
import com.guzzlio.domain.model.ServiceRecord
import com.guzzlio.domain.model.StatisticsChart
import com.guzzlio.domain.model.StatisticsChartKey
import com.guzzlio.domain.model.StatisticsChartStyle
import com.guzzlio.domain.model.StatisticsDashboard
import com.guzzlio.domain.model.StatisticsFilter
import com.guzzlio.domain.model.StatisticsPoint
import com.guzzlio.domain.model.StatisticsSource
import com.guzzlio.domain.model.StatisticsTimeframe
import com.guzzlio.domain.model.TripRecord
import com.guzzlio.domain.model.TripStatisticsSummary
import java.time.Duration
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
        val fuelTypeLabelsById = source.fuelTypes.associate { it.id to it.displayName }
        val fuelTypeSeries = fillUps
            .groupBy { record -> record.fuelTypeDisplayLabel(fuelTypeLabelsById) }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        val tripTypeLabelsById = source.tripTypes.associate { it.id to it.name }
        val tripTypeSeries = trips
            .groupBy { record -> record.tripTypeDisplayLabel(tripTypeLabelsById) }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        val comparisonVehicleIds = scopedVehicles.map { it.id }.ifEmpty {
            linkedSetOf<Long>().apply {
                addAll(fillUps.map(FillUpRecord::vehicleId))
                addAll(services.map(ServiceRecord::vehicleId))
                addAll(expenses.map(ExpenseRecord::vehicleId))
                addAll(trips.map(TripRecord::vehicleId))
            }.toList()
        }
        val odometerTimeline = buildList {
            fillUps.forEach { record ->
                add(record.toPoint(record.odometerReading, vehicleNamesById, includeVehicleInLabels))
            }
            services.forEach { record ->
                add(
                    StatisticsPoint(
                        label = chartLabel(record.dateTime, vehicleNamesById[record.vehicleId], includeVehicleInLabels),
                        value = record.odometerReading,
                        recordedAt = record.dateTime,
                        vehicleId = record.vehicleId,
                        vehicleName = vehicleNamesById[record.vehicleId].orEmpty(),
                    ),
                )
            }
            expenses.forEach { record ->
                add(
                    StatisticsPoint(
                        label = chartLabel(record.dateTime, vehicleNamesById[record.vehicleId], includeVehicleInLabels),
                        value = record.odometerReading,
                        recordedAt = record.dateTime,
                        vehicleId = record.vehicleId,
                        vehicleName = vehicleNamesById[record.vehicleId].orEmpty(),
                    ),
                )
            }
            trips.forEach { record ->
                add(
                    StatisticsPoint(
                        label = chartLabel(record.startDateTime, vehicleNamesById[record.vehicleId], includeVehicleInLabels),
                        value = record.startOdometerReading,
                        recordedAt = record.startDateTime,
                        vehicleId = record.vehicleId,
                        vehicleName = vehicleNamesById[record.vehicleId].orEmpty(),
                    ),
                )
                if (record.endDateTime != null && record.endOdometerReading != null) {
                    add(
                        StatisticsPoint(
                            label = chartLabel(record.endDateTime, vehicleNamesById[record.vehicleId], includeVehicleInLabels),
                            value = record.endOdometerReading,
                            recordedAt = record.endDateTime,
                            vehicleId = record.vehicleId,
                            vehicleName = vehicleNamesById[record.vehicleId].orEmpty(),
                        ),
                    )
                }
            }
        }.sortedBy(StatisticsPoint::recordedAt)
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
                lastPricePerUnit = fillUps.lastOrNull()?.pricePerUnit,
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
                    key = StatisticsChartKey.TIME_BETWEEN_FILLUPS,
                    title = "Time Between Fill-Ups",
                    unitLabel = "days",
                    style = StatisticsChartStyle.LINE,
                    points = fillUps.mapNotNull { record ->
                        val value = record.timeBetweenFillUpsDays() ?: return@mapNotNull null
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
                StatisticsChart(
                    key = StatisticsChartKey.AVG_FUEL_EFFICIENCY_BY_FUEL,
                    title = "Avg Fuel Efficiency by Fuel",
                    unitLabel = source.preferences.fuelEfficiencyUnit.storageValue,
                    style = StatisticsChartStyle.BAR,
                    points = fuelTypeSeries.entries.mapNotNullIndexed { index, (label, records) ->
                        val value = records.mapNotNull { it.fuelEfficiency ?: it.importedFuelEfficiency }.averageOrNull()
                            ?: return@mapNotNullIndexed null
                        categoryPoint(label, value, records.first().dateTime, index)
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.AVG_PRICE_PER_UNIT_BY_FUEL,
                    title = "Avg Price per Unit by Fuel",
                    unitLabel = "${source.preferences.currencySymbol}/${source.preferences.volumeUnit.storageValue}",
                    style = StatisticsChartStyle.BAR,
                    points = fuelTypeSeries.entries.mapNotNullIndexed { index, (label, records) ->
                        val value = records.map { it.pricePerUnit }.averageOrNull() ?: return@mapNotNullIndexed null
                        categoryPoint(label, value, records.first().dateTime, index)
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.AVG_COST_PER_FILLUP_BY_FUEL,
                    title = "Avg Cost per Fill-Up by Fuel",
                    unitLabel = source.preferences.currencySymbol,
                    style = StatisticsChartStyle.BAR,
                    points = fuelTypeSeries.entries.mapNotNullIndexed { index, (label, records) ->
                        val value = records.map { it.totalCost }.averageOrNull() ?: return@mapNotNullIndexed null
                        categoryPoint(label, value, records.first().dateTime, index)
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.TOTAL_FUEL_COST_BY_FUEL,
                    title = "Total Fuel Cost by Fuel",
                    unitLabel = source.preferences.currencySymbol,
                    style = StatisticsChartStyle.BAR,
                    points = fuelTypeSeries.entries.mapIndexed { index, (label, records) ->
                        categoryPoint(label, records.sumOf { it.totalCost }, records.first().dateTime, index)
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.TRIP_DISTANCE,
                    title = "Distance per Trip",
                    unitLabel = source.preferences.distanceUnit.storageValue,
                    style = StatisticsChartStyle.BAR,
                    points = trips.mapNotNull { record ->
                        val value = record.derivedDistance().takeIf { it > 0.0 } ?: return@mapNotNull null
                        StatisticsPoint(
                            label = chartLabel(record.startDateTime, vehicleNamesById[record.vehicleId], includeVehicleInLabels),
                            value = value,
                            recordedAt = record.startDateTime,
                            vehicleId = record.vehicleId,
                            vehicleName = vehicleNamesById[record.vehicleId].orEmpty(),
                        )
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.TRIP_DURATION,
                    title = "Duration per Trip",
                    unitLabel = "hours",
                    style = StatisticsChartStyle.BAR,
                    points = trips.mapNotNull { record ->
                        val value = record.durationHours() ?: return@mapNotNull null
                        StatisticsPoint(
                            label = chartLabel(record.startDateTime, vehicleNamesById[record.vehicleId], includeVehicleInLabels),
                            value = value,
                            recordedAt = record.startDateTime,
                            vehicleId = record.vehicleId,
                            vehicleName = vehicleNamesById[record.vehicleId].orEmpty(),
                        )
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.TOTAL_TRIP_DISTANCE_BY_TYPE,
                    title = "Total Trip Distance by Type",
                    unitLabel = source.preferences.distanceUnit.storageValue,
                    style = StatisticsChartStyle.BAR,
                    points = tripTypeSeries.entries.mapIndexed { index, (label, records) ->
                        categoryPoint(label, records.sumOf { it.derivedDistance() }, records.first().startDateTime, index)
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.TOTAL_TRIP_DURATION_BY_TYPE,
                    title = "Total Trip Duration by Type",
                    unitLabel = "hours",
                    style = StatisticsChartStyle.BAR,
                    points = tripTypeSeries.entries.mapNotNullIndexed { index, (label, records) ->
                        val value = records.mapNotNull { it.durationHours() }.sum().takeIf { it > 0.0 }
                            ?: return@mapNotNullIndexed null
                        categoryPoint(label, value, records.first().startDateTime, index)
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.ODOMETER_OVER_TIME,
                    title = "Odometer Over Time",
                    unitLabel = source.preferences.distanceUnit.storageValue,
                    style = StatisticsChartStyle.LINE,
                    points = odometerTimeline,
                ),
                StatisticsChart(
                    key = StatisticsChartKey.TOTAL_DISTANCE_BY_VEHICLE,
                    title = "Total Distance by Vehicle",
                    unitLabel = source.preferences.distanceUnit.storageValue,
                    style = StatisticsChartStyle.BAR,
                    points = comparisonVehicleIds.mapIndexed { index, vehicleId ->
                        val vehicleName = vehicleNamesById[vehicleId] ?: "Vehicle $vehicleId"
                        categoryPoint(
                            label = vehicleName,
                            value = vehicleTotalDistance(vehicleId, fillUps, services, expenses, trips),
                            recordedAt = overallReferenceTime(fillUps, services, expenses, trips),
                            index = index,
                        )
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.TOTAL_COST_BY_VEHICLE,
                    title = "Total Costs by Vehicle",
                    unitLabel = source.preferences.currencySymbol,
                    style = StatisticsChartStyle.BAR,
                    points = comparisonVehicleIds.mapIndexed { index, vehicleId ->
                        val vehicleName = vehicleNamesById[vehicleId] ?: "Vehicle $vehicleId"
                        categoryPoint(
                            label = vehicleName,
                            value = vehicleOperatingCost(vehicleId, fillUps, services, expenses),
                            recordedAt = overallReferenceTime(fillUps, services, expenses, trips),
                            index = index,
                        )
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.COST_PER_DAY_BY_VEHICLE,
                    title = "Cost per Day by Vehicle",
                    unitLabel = "${source.preferences.currencySymbol}/day",
                    style = StatisticsChartStyle.BAR,
                    points = comparisonVehicleIds.mapNotNullIndexed { index, vehicleId ->
                        val dates = vehicleRecordDates(vehicleId, fillUps, services, expenses, trips)
                        val cost = vehicleOperatingCost(vehicleId, fillUps, services, expenses)
                        val value = when {
                            dates.isEmpty() -> null
                            dates.size == 1 -> cost
                            else -> {
                                val firstDate = dates.first()
                                val lastDate = dates.last()
                                val days = Duration.between(firstDate, lastDate).toHours().toDouble() / 24.0
                                cost / days.coerceAtLeast(1.0)
                            }
                        } ?: return@mapNotNullIndexed null
                        categoryPoint(
                            label = vehicleNamesById[vehicleId] ?: "Vehicle $vehicleId",
                            value = value,
                            recordedAt = overallReferenceTime(fillUps, services, expenses, trips),
                            index = index,
                        )
                    },
                ),
                StatisticsChart(
                    key = StatisticsChartKey.COST_PER_DISTANCE_BY_VEHICLE,
                    title = "Cost per Distance by Vehicle",
                    unitLabel = "${source.preferences.currencySymbol}/${source.preferences.distanceUnit.storageValue}",
                    style = StatisticsChartStyle.BAR,
                    points = comparisonVehicleIds.mapNotNullIndexed { index, vehicleId ->
                        val totalDistance = vehicleTotalDistance(vehicleId, fillUps, services, expenses, trips)
                        val totalCost = vehicleOperatingCost(vehicleId, fillUps, services, expenses)
                        if (totalDistance <= 0.0) return@mapNotNullIndexed null
                        categoryPoint(
                            label = vehicleNamesById[vehicleId] ?: "Vehicle $vehicleId",
                            value = totalCost / totalDistance,
                            recordedAt = overallReferenceTime(fillUps, services, expenses, trips),
                            index = index,
                        )
                    },
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

    private fun TripRecord.durationHours(): Double? = durationMillis?.let { Duration.ofMillis(it).toMinutes().toDouble() / 60.0 }

    private fun FillUpRecord.timeBetweenFillUpsDays(): Double? {
        val durationMillis = timeSincePreviousMillis ?: return null
        return Duration.ofMillis(durationMillis).toHours().toDouble() / 24.0
    }

    private fun FillUpRecord.fuelTypeDisplayLabel(fuelTypeLabelsById: Map<Long, String>): String = when {
        fuelTypeId != null -> fuelTypeLabelsById[fuelTypeId].orEmpty()
        !importedFuelTypeText.isNullOrBlank() -> importedFuelTypeText
        else -> fuelBrand.takeIf { it.isNotBlank() } ?: "Unspecified Fuel"
    }.trim().ifBlank { "Unspecified Fuel" }

    private fun TripRecord.tripTypeDisplayLabel(tripTypeLabelsById: Map<Long, String>): String = when {
        tripTypeId != null -> tripTypeLabelsById[tripTypeId].orEmpty()
        purpose.isNotBlank() -> purpose
        else -> "Unspecified Trip Type"
    }.trim().ifBlank { "Unspecified Trip Type" }

    private fun vehicleTotalDistance(
        vehicleId: Long,
        fillUps: List<FillUpRecord>,
        services: List<ServiceRecord>,
        expenses: List<ExpenseRecord>,
        trips: List<TripRecord>,
    ): Double {
        val odometerValues = buildList {
            fillUps.filter { it.vehicleId == vehicleId }.mapTo(this, FillUpRecord::odometerReading)
            services.filter { it.vehicleId == vehicleId }.mapTo(this, ServiceRecord::odometerReading)
            expenses.filter { it.vehicleId == vehicleId }.mapTo(this, ExpenseRecord::odometerReading)
            trips.filter { it.vehicleId == vehicleId }.forEach { trip ->
                add(trip.startOdometerReading)
                trip.endOdometerReading?.let(::add)
            }
        }
        return if (odometerValues.size >= 2) {
            odometerValues.maxOrNull().orEmptyDouble() - odometerValues.minOrNull().orEmptyDouble()
        } else {
            fillUps.filter { it.vehicleId == vehicleId }.sumOf { it.distanceSincePrevious ?: 0.0 } +
                trips.filter { it.vehicleId == vehicleId }.sumOf { it.derivedDistance() }
        }
    }

    private fun vehicleOperatingCost(
        vehicleId: Long,
        fillUps: List<FillUpRecord>,
        services: List<ServiceRecord>,
        expenses: List<ExpenseRecord>,
    ): Double = fillUps.filter { it.vehicleId == vehicleId }.sumOf(FillUpRecord::totalCost) +
        services.filter { it.vehicleId == vehicleId }.sumOf(ServiceRecord::totalCost) +
        expenses.filter { it.vehicleId == vehicleId }.sumOf(ExpenseRecord::totalCost)

    private fun vehicleRecordDates(
        vehicleId: Long,
        fillUps: List<FillUpRecord>,
        services: List<ServiceRecord>,
        expenses: List<ExpenseRecord>,
        trips: List<TripRecord>,
    ): List<LocalDateTime> = buildList {
        fillUps.filter { it.vehicleId == vehicleId }.mapTo(this, FillUpRecord::dateTime)
        services.filter { it.vehicleId == vehicleId }.mapTo(this, ServiceRecord::dateTime)
        expenses.filter { it.vehicleId == vehicleId }.mapTo(this, ExpenseRecord::dateTime)
        trips.filter { it.vehicleId == vehicleId }.forEach { trip ->
            add(trip.startDateTime)
            trip.endDateTime?.let(::add)
        }
    }.sorted()

    private fun overallReferenceTime(
        fillUps: List<FillUpRecord>,
        services: List<ServiceRecord>,
        expenses: List<ExpenseRecord>,
        trips: List<TripRecord>,
    ): LocalDateTime = listOfNotNull(
        fillUps.maxOfOrNull(FillUpRecord::dateTime),
        services.maxOfOrNull(ServiceRecord::dateTime),
        expenses.maxOfOrNull(ExpenseRecord::dateTime),
        trips.maxOfOrNull(TripRecord::startDateTime),
    ).maxOrNull() ?: LocalDateTime.now()

    private fun categoryPoint(
        label: String,
        value: Double,
        recordedAt: LocalDateTime,
        index: Int,
    ): StatisticsPoint = StatisticsPoint(
        label = label,
        value = value,
        recordedAt = recordedAt.plusMinutes(index.toLong()),
        vehicleId = 0L,
        vehicleName = "",
    )

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
    private fun Double?.orEmptyDouble(): Double = this ?: 0.0

    private inline fun <T, R : Any> Iterable<T>.mapNotNullIndexed(transform: (index: Int, T) -> R?): List<R> {
        val destination = ArrayList<R>()
        forEachIndexed { index, item ->
            transform(index, item)?.let(destination::add)
        }
        return destination
    }
}
