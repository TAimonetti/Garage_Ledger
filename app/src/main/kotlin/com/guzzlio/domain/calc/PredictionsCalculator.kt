package com.guzzlio.domain.calc

import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.domain.model.ExpenseRecord
import com.guzzlio.domain.model.FillUpRecord
import com.guzzlio.domain.model.ServiceRecord
import com.guzzlio.domain.model.TripRecord
import com.guzzlio.domain.model.Vehicle
import com.guzzlio.domain.model.VehiclePredictionSummary
import java.time.Duration
import java.time.LocalDateTime

object PredictionsCalculator {
    fun build(
        vehicle: Vehicle,
        fillUps: List<FillUpRecord>,
        services: List<ServiceRecord>,
        expenses: List<ExpenseRecord>,
        trips: List<TripRecord>,
        now: LocalDateTime,
        currencySymbol: String,
    ): VehiclePredictionSummary {
        val sortedFillUps = fillUps.sortedBy(FillUpRecord::dateTime)
        val averageDistance = averageDistanceBetweenFillUps(sortedFillUps)
        val averageTime = averageTimeBetweenFillUps(sortedFillUps)
        val lastFillUp = sortedFillUps.lastOrNull()

        val predictedDate = when {
            lastFillUp == null || averageTime == null -> null
            else -> maxOf(lastFillUp.dateTime.plus(averageTime), now)
        }
        val predictedOdometer = when {
            lastFillUp == null || averageDistance == null -> null
            else -> lastFillUp.odometerReading + averageDistance
        }

        val totalCost = fillUps.sumOf(FillUpRecord::totalCost) +
            services.sumOf(ServiceRecord::totalCost) +
            expenses.sumOf(ExpenseRecord::totalCost)

        val minimumDate = listOfNotNull(
            fillUps.minOfOrNull(FillUpRecord::dateTime),
            services.minOfOrNull(ServiceRecord::dateTime),
            expenses.minOfOrNull(ExpenseRecord::dateTime),
            trips.minOfOrNull(TripRecord::startDateTime),
        ).minOrNull()
        val maximumDate = listOfNotNull(
            fillUps.maxOfOrNull(FillUpRecord::dateTime),
            services.maxOfOrNull(ServiceRecord::dateTime),
            expenses.maxOfOrNull(ExpenseRecord::dateTime),
            trips.maxOfOrNull { it.endDateTime ?: it.startDateTime },
        ).maxOrNull()
        val totalDays = if (minimumDate != null && maximumDate != null) {
            Duration.between(minimumDate, maximumDate).toDays().takeIf { it > 0L }?.toDouble()
        } else {
            null
        }

        val minimumOdometer = listOfNotNull(
            fillUps.minOfOrNull(FillUpRecord::odometerReading),
            services.minOfOrNull(ServiceRecord::odometerReading),
            expenses.minOfOrNull(ExpenseRecord::odometerReading),
            trips.minOfOrNull(TripRecord::startOdometerReading),
        ).minOrNull()
        val maximumOdometer = listOfNotNull(
            fillUps.maxOfOrNull(FillUpRecord::odometerReading),
            services.maxOfOrNull(ServiceRecord::odometerReading),
            expenses.maxOfOrNull(ExpenseRecord::odometerReading),
            trips.maxOfOrNull { it.endOdometerReading ?: it.startOdometerReading },
        ).maxOrNull()
        val totalDistance = if (minimumOdometer != null && maximumOdometer != null) {
            (maximumOdometer - minimumOdometer).takeIf { it > 0.0 }
        } else {
            null
        }

        val tripCostPerDistance = totalDistance?.takeIf { it > 0.0 }?.let { totalCost / it }
        val tripCostPer100Distance = tripCostPerDistance?.let { it * 100.0 }

        return VehiclePredictionSummary(
            vehicleId = vehicle.id,
            vehicleName = vehicle.name,
            nextFillUpDateTime = predictedDate,
            nextFillUpOdometerReading = predictedOdometer,
            carRange = calculateCarRange(sortedFillUps, vehicle.fuelTankCapacity),
            tripCostPerDay = totalDays?.takeIf { it > 0.0 }?.let { totalCost / it },
            tripCostPerDistanceUnit = tripCostPerDistance,
            tripCostPer100DistanceUnit = tripCostPer100Distance,
            distanceUnitLabel = vehicle.distanceUnitOverride?.storageValue ?: DistanceUnit.MILES.storageValue,
            currencySymbol = currencySymbol,
        )
    }

    internal fun averageDistanceBetweenFillUps(fillUps: List<FillUpRecord>): Double? {
        val distances = fillUps.mapNotNull(FillUpRecord::distanceSincePrevious).filter { it > 0.0 }
        return distances.takeIf { it.isNotEmpty() }?.average()
    }

    internal fun averageTimeBetweenFillUps(fillUps: List<FillUpRecord>): Duration? {
        val intervals = fillUps.zipWithNext()
            .map { (previous, current) -> Duration.between(previous.dateTime, current.dateTime) }
            .filter { !it.isNegative && !it.isZero }
        return intervals.takeIf { it.isNotEmpty() }?.let { entries ->
            Duration.ofMillis(entries.sumOf(Duration::toMillis) / entries.size)
        }
    }

    internal fun calculateCarRange(fillUps: List<FillUpRecord>, fuelTankCapacity: Double?): Double? {
        val tankCapacity = fuelTankCapacity?.takeIf { it > 0.0 } ?: return null
        val usable = fillUps.filter { record ->
            val distance = record.distanceSincePrevious ?: 0.0
            distance > 0.0 && record.volume > 0.0 && (!record.partial || record.previousMissedFillups)
        }
        val totalDistance = usable.sumOf { it.distanceSincePrevious ?: 0.0 }
        val totalVolume = usable.sumOf(FillUpRecord::volume)
        if (totalDistance <= 0.0 || totalVolume <= 0.0) return null
        return (totalDistance * tankCapacity) / totalVolume
    }
}
