package com.garageledger.domain.calc

import com.garageledger.core.model.DistanceUnit
import com.garageledger.core.model.FuelEfficiencyAssignmentMethod
import com.garageledger.core.model.FuelEfficiencyUnit
import com.garageledger.core.model.VolumeUnit
import com.garageledger.domain.model.FillUpRecord
import java.time.Duration

object FuelEfficiencyCalculator {
    fun recalculate(
        records: List<FillUpRecord>,
        assignmentMethod: FuelEfficiencyAssignmentMethod,
        fuelEfficiencyUnit: FuelEfficiencyUnit,
    ): List<FillUpRecord> {
        if (records.isEmpty()) return emptyList()

        val odometerSorted = records.sortedWith(
            compareBy<FillUpRecord> { it.odometerReading }
                .thenBy { it.dateTime }
                .thenBy { it.id },
        ).map { record ->
            record.copy(
                fuelEfficiency = null,
                fuelEfficiencyUnit = fuelEfficiencyUnit,
                distanceForFuelEfficiency = null,
                volumeForFuelEfficiency = null,
                distanceSincePrevious = null,
                distanceTillNextFillUp = null,
                timeSincePreviousMillis = null,
                timeTillNextFillUpMillis = null,
            )
        }.toMutableList()

        // Old aCar derives distance intervals by odometer order and time intervals by date order.
        for (index in odometerSorted.indices) {
            val current = odometerSorted[index]
            val previous = odometerSorted.getOrNull(index - 1)
            val next = odometerSorted.getOrNull(index + 1)

            val distanceSincePrevious = if (previous != null && !current.previousMissedFillups) {
                current.odometerReading - previous.odometerReading
            } else {
                null
            }
            val distanceTillNext = if (next != null && !next.previousMissedFillups) {
                next.odometerReading - current.odometerReading
            } else {
                null
            }

            odometerSorted[index] = current.copy(
                distanceSincePrevious = distanceSincePrevious,
                distanceTillNextFillUp = distanceTillNext,
            )
        }

        val dateSortedIndexes = odometerSorted.indices.sortedWith(
            compareBy<Int> { odometerSorted[it].dateTime }
                .thenBy { odometerSorted[it].odometerReading }
                .thenBy { odometerSorted[it].id },
        )
        dateSortedIndexes.forEachIndexed { position, currentIndex ->
            val current = odometerSorted[currentIndex]
            val previous = dateSortedIndexes.getOrNull(position - 1)?.let(odometerSorted::get)
            val next = dateSortedIndexes.getOrNull(position + 1)?.let(odometerSorted::get)
            val timeSincePrevious = if (previous != null && !current.previousMissedFillups) {
                Duration.between(previous.dateTime, current.dateTime).toMillis()
            } else {
                null
            }
            val timeTillNext = if (next != null && !next.previousMissedFillups) {
                Duration.between(current.dateTime, next.dateTime).toMillis()
            } else {
                null
            }

            odometerSorted[currentIndex] = current.copy(
                timeSincePreviousMillis = timeSincePrevious,
                timeTillNextFillUpMillis = timeTillNext,
            )
        }

        var anchorIndex: Int? = null
        var accumulatedPartialVolume = 0.0

        for (index in odometerSorted.indices) {
            val current = odometerSorted[index]

            if (current.previousMissedFillups) {
                anchorIndex = if (!current.partial) index else null
                accumulatedPartialVolume = 0.0
                continue
            }

            if (current.partial) {
                if (anchorIndex != null) {
                    accumulatedPartialVolume += convertVolume(
                        value = current.volume,
                        from = current.volumeUnit,
                        to = targetVolumeUnit(fuelEfficiencyUnit),
                    )
                }
                continue
            }

            val anchor = anchorIndex?.let(odometerSorted::get)
            if (anchor != null) {
                val rawDistance = current.odometerReading - anchor.odometerReading
                val distance = convertDistance(
                    value = rawDistance,
                    from = current.distanceUnit,
                    to = targetDistanceUnit(fuelEfficiencyUnit),
                )
                val volume = accumulatedPartialVolume + convertVolume(
                    value = current.volume,
                    from = current.volumeUnit,
                    to = targetVolumeUnit(fuelEfficiencyUnit),
                )
                if (rawDistance > 0.0 && distance > 0.0 && volume > 0.0) {
                    val targetIndex = when (assignmentMethod) {
                        FuelEfficiencyAssignmentMethod.PREVIOUS_RECORD -> anchorIndex
                        FuelEfficiencyAssignmentMethod.CURRENT_RECORD -> index
                    } ?: index

                    val target = odometerSorted[targetIndex]
                    odometerSorted[targetIndex] = target.copy(
                        fuelEfficiency = calculateFuelEfficiency(distance, volume, fuelEfficiencyUnit),
                        fuelEfficiencyUnit = fuelEfficiencyUnit,
                        distanceForFuelEfficiency = distance,
                        volumeForFuelEfficiency = volume,
                    )
                }
            }

            anchorIndex = index
            accumulatedPartialVolume = 0.0
        }

        return odometerSorted
    }

    private fun calculateFuelEfficiency(
        distance: Double,
        volume: Double,
        unit: FuelEfficiencyUnit,
    ): Double = when (unit) {
        FuelEfficiencyUnit.LITERS_PER_100_KM -> (volume * 100.0) / distance
        FuelEfficiencyUnit.MPG_US,
        FuelEfficiencyUnit.MPG_UK,
        FuelEfficiencyUnit.KILOMETERS_PER_LITER,
        -> distance / volume
    }

    private fun targetDistanceUnit(unit: FuelEfficiencyUnit): DistanceUnit = when (unit) {
        FuelEfficiencyUnit.MPG_US,
        FuelEfficiencyUnit.MPG_UK,
        -> DistanceUnit.MILES
        FuelEfficiencyUnit.KILOMETERS_PER_LITER,
        FuelEfficiencyUnit.LITERS_PER_100_KM,
        -> DistanceUnit.KILOMETERS
    }

    private fun targetVolumeUnit(unit: FuelEfficiencyUnit): VolumeUnit = when (unit) {
        FuelEfficiencyUnit.MPG_US -> VolumeUnit.GALLONS_US
        FuelEfficiencyUnit.MPG_UK -> VolumeUnit.GALLONS_UK
        FuelEfficiencyUnit.KILOMETERS_PER_LITER,
        FuelEfficiencyUnit.LITERS_PER_100_KM,
        -> VolumeUnit.LITERS
    }

    private fun convertDistance(
        value: Double,
        from: DistanceUnit,
        to: DistanceUnit,
    ): Double = when {
        from == to -> value
        from == DistanceUnit.MILES && to == DistanceUnit.KILOMETERS -> value * MILES_TO_KILOMETERS
        from == DistanceUnit.KILOMETERS && to == DistanceUnit.MILES -> value / MILES_TO_KILOMETERS
        else -> value
    }

    private fun convertVolume(
        value: Double,
        from: VolumeUnit,
        to: VolumeUnit,
    ): Double {
        if (from == to) return value
        val liters = when (from) {
            VolumeUnit.GALLONS_US -> value * US_GALLON_TO_LITERS
            VolumeUnit.GALLONS_UK -> value * UK_GALLON_TO_LITERS
            VolumeUnit.LITERS -> value
        }
        return when (to) {
            VolumeUnit.GALLONS_US -> liters / US_GALLON_TO_LITERS
            VolumeUnit.GALLONS_UK -> liters / UK_GALLON_TO_LITERS
            VolumeUnit.LITERS -> liters
        }
    }

    private const val MILES_TO_KILOMETERS = 1.609344
    private const val US_GALLON_TO_LITERS = 3.785411784
    private const val UK_GALLON_TO_LITERS = 4.54609
}
