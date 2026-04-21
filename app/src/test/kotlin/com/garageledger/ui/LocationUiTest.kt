package com.garageledger.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocationUiTest {
    @Test
    fun formatCoordinates_usesStableSixDecimalFormat() {
        assertThat(formatCoordinates(33.448376, -112.074036)).isEqualTo("33.448376, -112.074036")
    }

    @Test
    fun buildGeoMapUriString_embedsCoordinatesAndLabel() {
        val uri = buildGeoMapUriString(
            latitude = 33.448376,
            longitude = -112.074036,
            label = "Phoenix Fuel Stop",
        )

        assertThat(uri)
            .isEqualTo("geo:0,0?q=33.448376,-112.074036(Phoenix%20Fuel%20Stop)")
    }
}
