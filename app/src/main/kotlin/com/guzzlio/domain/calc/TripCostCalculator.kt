package com.guzzlio.domain.calc

import com.guzzlio.domain.model.ExpenseRecord
import com.guzzlio.domain.model.FillUpRecord
import com.guzzlio.domain.model.ServiceRecord
import com.guzzlio.domain.model.TripRecord

data class TripCostBreakdown(
    val directExpenseCost: Double,
    val estimatedFuelCost: Double,
    val estimatedServiceCost: Double,
) {
    val totalCost: Double = directExpenseCost + estimatedFuelCost + estimatedServiceCost
}

object TripCostCalculator {
    fun deriveFuelCostPerDistance(fillUps: List<FillUpRecord>): Double {
        val usable = fillUps.filter { (it.distanceSincePrevious ?: 0.0) > 0.0 }
        val distance = usable.sumOf { it.distanceSincePrevious ?: 0.0 }
        if (distance <= 0.0) return 0.0
        return usable.sumOf { it.totalCost } / distance
    }

    fun deriveServiceCostPerDistance(serviceRecords: List<ServiceRecord>): Double {
        if (serviceRecords.size < 2) return 0.0
        val sorted = serviceRecords.sortedBy { it.odometerReading }
        val span = sorted.last().odometerReading - sorted.first().odometerReading
        if (span <= 0.0) return 0.0
        return sorted.sumOf { it.totalCost } / span
    }

    fun calculate(
        trip: TripRecord,
        directExpenses: List<ExpenseRecord>,
        fuelCostPerDistance: Double,
        serviceCostPerDistance: Double,
    ): TripCostBreakdown {
        val tripDistance = trip.distance
            ?: trip.endOdometerReading?.let { it - trip.startOdometerReading }
            ?: 0.0

        val directExpenseCost = directExpenses.sumOf { it.totalCost }
        return TripCostBreakdown(
            directExpenseCost = directExpenseCost,
            estimatedFuelCost = tripDistance * fuelCostPerDistance,
            estimatedServiceCost = tripDistance * serviceCostPerDistance,
        )
    }
}
