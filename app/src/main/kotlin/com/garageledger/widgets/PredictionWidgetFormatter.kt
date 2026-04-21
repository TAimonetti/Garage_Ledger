package com.garageledger.widgets

import com.garageledger.domain.model.PredictionWidgetItem
import java.time.format.DateTimeFormatter

internal object PredictionWidgetFormatter {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd")

    fun vehicleTitle(item: PredictionWidgetItem): String = item.vehicleName

    fun nextFillUpSubtitle(item: PredictionWidgetItem): String = listOfNotNull(
        item.nextFillUpDateTime?.let { "Next ${it.format(dateFormatter)}" },
        item.nextFillUpOdometerReading?.let { "${it.toInt()} ${item.distanceUnitLabel}" },
    ).joinToString(" | ").ifBlank { "Not enough fill-up history yet" }

    fun rangeSubtitle(item: PredictionWidgetItem): String =
        item.carRange?.let { "Range ${"%.1f".format(it)} ${item.distanceUnitLabel}" } ?: "Range unavailable"

    fun tripCostSubtitle(item: PredictionWidgetItem): String =
        item.tripCostPer100DistanceUnit?.let {
            "Trip ${item.currencySymbol}${"%.2f".format(it)}/100${item.distanceUnitLabel}"
        } ?: "Trip cost unavailable"
}
