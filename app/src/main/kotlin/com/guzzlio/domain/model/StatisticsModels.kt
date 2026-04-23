package com.guzzlio.domain.model

import java.time.LocalDateTime

enum class StatisticsTimeframe(val label: String) {
    ALL_TIME("All time"),
    LAST_30_DAYS("30 days"),
    LAST_90_DAYS("90 days"),
    LAST_365_DAYS("1 year"),
}

data class StatisticsFilter(
    val vehicleId: Long? = null,
    val timeframe: StatisticsTimeframe = StatisticsTimeframe.ALL_TIME,
)

data class StatisticsSource(
    val preferences: AppPreferenceSnapshot = AppPreferenceSnapshot(),
    val vehicles: List<Vehicle> = emptyList(),
    val fuelTypes: List<FuelType> = emptyList(),
    val tripTypes: List<TripType> = emptyList(),
    val fillUps: List<FillUpRecord> = emptyList(),
    val services: List<ServiceRecord> = emptyList(),
    val expenses: List<ExpenseRecord> = emptyList(),
    val trips: List<TripRecord> = emptyList(),
)

data class StatisticsDashboard(
    val preferences: AppPreferenceSnapshot = AppPreferenceSnapshot(),
    val filter: StatisticsFilter = StatisticsFilter(),
    val scopeLabel: String = "All Vehicles",
    val periodDescription: String = "All time",
    val overall: OverallStatisticsSummary = OverallStatisticsSummary(),
    val fillUps: FillUpStatisticsSummary = FillUpStatisticsSummary(),
    val services: CostStatisticsSummary = CostStatisticsSummary(),
    val expenses: CostStatisticsSummary = CostStatisticsSummary(),
    val trips: TripStatisticsSummary = TripStatisticsSummary(),
    val charts: List<StatisticsChart> = emptyList(),
)

data class OverallStatisticsSummary(
    val vehicleCount: Int = 0,
    val totalRecordCount: Int = 0,
    val fillUpCount: Int = 0,
    val serviceCount: Int = 0,
    val expenseCount: Int = 0,
    val tripCount: Int = 0,
    val totalOperatingCost: Double = 0.0,
)

data class FillUpStatisticsSummary(
    val count: Int = 0,
    val totalVolume: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalDistance: Double = 0.0,
    val averageFuelEfficiency: Double? = null,
    val lastFuelEfficiency: Double? = null,
    val averagePricePerUnit: Double? = null,
    val lastPricePerUnit: Double? = null,
    val averageCostPerFillUp: Double? = null,
)

data class CostStatisticsSummary(
    val count: Int = 0,
    val totalCost: Double = 0.0,
    val averageCost: Double? = null,
)

data class TripStatisticsSummary(
    val count: Int = 0,
    val totalDistance: Double = 0.0,
    val averageDistance: Double? = null,
    val totalTaxDeduction: Double = 0.0,
    val totalReimbursement: Double = 0.0,
    val paidCount: Int = 0,
    val openCount: Int = 0,
)

enum class StatisticsChartStyle {
    LINE,
    BAR,
}

enum class StatisticsChartKey {
    FUEL_EFFICIENCY,
    FUEL_PRICE,
    FUEL_COST,
    SERVICE_COST,
    EXPENSE_COST,
    DISTANCE_BETWEEN_FILLUPS,
    TIME_BETWEEN_FILLUPS,
    PRICE_PER_UNIT,
    VOLUME_PER_FILLUP,
    AVG_FUEL_EFFICIENCY_BY_FUEL,
    AVG_PRICE_PER_UNIT_BY_FUEL,
    AVG_COST_PER_FILLUP_BY_FUEL,
    TOTAL_FUEL_COST_BY_FUEL,
    TRIP_DISTANCE,
    TRIP_DURATION,
    TOTAL_TRIP_DISTANCE_BY_TYPE,
    TOTAL_TRIP_DURATION_BY_TYPE,
    ODOMETER_OVER_TIME,
    TOTAL_DISTANCE_BY_VEHICLE,
    TOTAL_COST_BY_VEHICLE,
    COST_PER_DAY_BY_VEHICLE,
    COST_PER_DISTANCE_BY_VEHICLE,
}

data class StatisticsChart(
    val key: StatisticsChartKey,
    val title: String,
    val unitLabel: String,
    val style: StatisticsChartStyle,
    val points: List<StatisticsPoint> = emptyList(),
)

data class StatisticsPoint(
    val label: String,
    val value: Double,
    val recordedAt: LocalDateTime,
    val vehicleId: Long,
    val vehicleName: String,
)
