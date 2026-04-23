package com.guzzlio.ui

import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.domain.model.TripRecord
import com.guzzlio.domain.model.VehicleLifecycle
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
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

    @Test
    fun buildTripCopySeed_copiesReusableFieldsAndResetsEntryData() {
        val seed = buildTripCopySeed(
            source = TripRecord(
                id = 4L,
                vehicleId = 9L,
                startDateTime = LocalDateTime.parse("2024-04-01T08:30:00"),
                startOdometerReading = 1200.0,
                startLocation = "Phoenix",
                endDateTime = LocalDateTime.parse("2024-04-01T09:15:00"),
                endOdometerReading = 1235.0,
                endLocation = "Tempe",
                distanceUnit = DistanceUnit.MILES,
                purpose = "Client Visit",
                client = "ACME",
                taxDeductionRate = 0.67,
                reimbursementRate = 0.45,
                paid = true,
                tags = listOf("client", "tax"),
                notes = "Bring paperwork",
            ),
            now = LocalDateTime.parse("2024-04-05T10:00:00"),
        )

        assertThat(seed.startDateText).isEqualTo("2024-04-05 10:00")
        assertThat(seed.startLocation).isEqualTo("Phoenix")
        assertThat(seed.endLocation).isEqualTo("Tempe")
        assertThat(seed.purpose).isEqualTo("Client Visit")
        assertThat(seed.client).isEqualTo("ACME")
        assertThat(seed.taxRateText).isEqualTo("0.67")
        assertThat(seed.reimbursementRateText).isEqualTo("0.45")
        assertThat(seed.tagsText).isEqualTo("client, tax")
        assertThat(seed.notesText).isEqualTo("Bring paperwork")
    }

    @Test
    fun allowsRecordModification_onlyAllowsActiveVehicles() {
        assertThat(VehicleLifecycle.ACTIVE.allowsRecordModification()).isTrue()
        assertThat(VehicleLifecycle.RETIRED.allowsRecordModification()).isFalse()
    }
}
