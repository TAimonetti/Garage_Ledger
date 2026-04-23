package com.guzzlio.widgets

import com.guzzlio.domain.model.FuelWidgetItem
import com.guzzlio.domain.model.FuelWidgetMetric

object FuelWidgetFormatter {
    fun title(item: FuelWidgetItem): String = item.vehicleName.ifBlank { "Vehicle" }

    fun subtitle(item: FuelWidgetItem): String {
        val latestLabel = when (item.metric) {
            FuelWidgetMetric.FUEL_EFFICIENCY -> "Last"
            FuelWidgetMetric.FUEL_PRICE -> "Latest"
        }
        val averageLabel = "Avg"
        return listOfNotNull(
            item.latestValue?.let { "$latestLabel ${it.toDisplay()} ${item.unitLabel}" },
            item.averageValue?.let { "$averageLabel ${it.toDisplay()} ${item.unitLabel}" },
        ).joinToString(" | ").ifBlank { "No fill-up history yet" }
    }

    private fun Double.toDisplay(): String = if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        "%,.2f".format(this).replace(",", "")
    }
}
