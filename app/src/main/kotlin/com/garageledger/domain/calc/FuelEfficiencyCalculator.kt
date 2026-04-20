package com.garageledger.domain.calc

import com.garageledger.core.model.FuelEfficiencyAssignmentMethod
import com.garageledger.domain.model.FillUpRecord
import java.time.Duration

object FuelEfficiencyCalculator {
    fun recalculate(
        records: List<FillUpRecord>,
        assignmentMethod: FuelEfficiencyAssignmentMethod,
    ): List<FillUpRecord> {
        if (records.isEmpty()) return emptyList()

        val sorted = records.sortedBy { it.dateTime }.map { it.copy(
            fuelEfficiency = null,
            distanceForFuelEfficiency = null,
            volumeForFuelEfficiency = null,
            distanceSincePrevious = null,
            distanceTillNextFillUp = null,
            timeSincePreviousMillis = null,
            timeTillNextFillUpMillis = null,
        ) }.toMutableList()

        for (index in sorted.indices) {
            val current = sorted[index]
            val previous = sorted.getOrNull(index - 1)
            val next = sorted.getOrNull(index + 1)

            val distanceSincePrevious = previous?.let { current.odometerReading - it.odometerReading }
            val timeSincePrevious = previous?.let { Duration.between(it.dateTime, current.dateTime).toMillis() }
            val distanceTillNext = next?.let { it.odometerReading - current.odometerReading }
            val timeTillNext = next?.let { Duration.between(current.dateTime, it.dateTime).toMillis() }

            sorted[index] = current.copy(
                distanceSincePrevious = distanceSincePrevious,
                distanceTillNextFillUp = distanceTillNext,
                timeSincePreviousMillis = timeSincePrevious,
                timeTillNextFillUpMillis = timeTillNext,
            )
        }

        var anchorIndex: Int? = null
        var accumulatedPartialVolume = 0.0

        for (index in sorted.indices) {
            val current = sorted[index]

            if (current.previousMissedFillups) {
                anchorIndex = if (!current.partial) index else null
                accumulatedPartialVolume = 0.0
                continue
            }

            if (current.partial) {
                if (anchorIndex != null) {
                    accumulatedPartialVolume += current.volume
                }
                continue
            }

            val anchor = anchorIndex?.let(sorted::get)
            if (anchor != null) {
                val distance = current.odometerReading - anchor.odometerReading
                val volume = accumulatedPartialVolume + current.volume
                if (distance > 0.0 && volume > 0.0) {
                    val targetIndex = when (assignmentMethod) {
                        FuelEfficiencyAssignmentMethod.PREVIOUS_RECORD -> anchorIndex
                        FuelEfficiencyAssignmentMethod.CURRENT_RECORD -> index
                    } ?: index

                    val target = sorted[targetIndex]
                    sorted[targetIndex] = target.copy(
                        fuelEfficiency = distance / volume,
                        distanceForFuelEfficiency = distance,
                        volumeForFuelEfficiency = volume,
                    )
                }
            }

            anchorIndex = index
            accumulatedPartialVolume = 0.0
        }

        return sorted
    }
}
