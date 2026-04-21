package com.garageledger.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TripEditorSupportTest {
    @Test
    fun resolveTripEndOdometer_supportsDistanceMode() {
        val resolved = resolveTripEndOdometer(
            startOdometer = 1200.0,
            rawInput = "45.5",
            mode = TripEndOdometerMode.DISTANCE_FROM_START,
        )

        assertThat(resolved).isEqualTo(1245.5)
    }

    @Test
    fun translateTripEndOdometerInput_convertsBetweenModes() {
        val absolute = translateTripEndOdometerInput(
            rawInput = "45",
            startOdometer = 1200.0,
            fromMode = TripEndOdometerMode.DISTANCE_FROM_START,
            toMode = TripEndOdometerMode.ABSOLUTE,
        )
        val distance = translateTripEndOdometerInput(
            rawInput = "1245",
            startOdometer = 1200.0,
            fromMode = TripEndOdometerMode.ABSOLUTE,
            toMode = TripEndOdometerMode.DISTANCE_FROM_START,
        )

        assertThat(absolute).isEqualTo("1245")
        assertThat(distance).isEqualTo("45")
    }

    @Test
    fun buildReturnTripPurpose_prefixesExistingPurpose() {
        assertThat(buildReturnTripPurpose("Client Visit")).isEqualTo("Return: Client Visit")
        assertThat(buildReturnTripPurpose("  ")).isEmpty()
    }
}
