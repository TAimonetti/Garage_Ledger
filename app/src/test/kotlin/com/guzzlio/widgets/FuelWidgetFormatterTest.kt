package com.guzzlio.widgets

import com.guzzlio.domain.model.FuelWidgetItem
import com.guzzlio.domain.model.FuelWidgetMetric
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FuelWidgetFormatterTest {
    @Test
    fun formatsFuelEfficiencySnapshot() {
        val item = FuelWidgetItem(
            vehicleId = 1L,
            vehicleName = "Corolla",
            metric = FuelWidgetMetric.FUEL_EFFICIENCY,
            latestValue = 30.4,
            averageValue = 29.2,
            unitLabel = "MPG (US)",
        )

        assertThat(FuelWidgetFormatter.title(item)).isEqualTo("Corolla")
        assertThat(FuelWidgetFormatter.subtitle(item)).isEqualTo("Last 30.40 MPG (US) | Avg 29.20 MPG (US)")
    }

    @Test
    fun formatsFuelPriceSnapshot() {
        val item = FuelWidgetItem(
            vehicleId = 2L,
            vehicleName = "Tundra",
            metric = FuelWidgetMetric.FUEL_PRICE,
            latestValue = 3.87,
            averageValue = 3.54,
            unitLabel = "$/gal (US)",
        )

        assertThat(FuelWidgetFormatter.subtitle(item)).isEqualTo("Latest 3.87 $/gal (US) | Avg 3.54 $/gal (US)")
    }
}
