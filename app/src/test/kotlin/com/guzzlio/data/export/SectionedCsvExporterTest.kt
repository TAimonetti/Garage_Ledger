package com.guzzlio.data.export

import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.core.model.FuelEfficiencyUnit
import com.guzzlio.core.model.VolumeUnit
import com.guzzlio.data.importer.AcarCsvImporter
import com.guzzlio.domain.model.AppPreferenceSnapshot
import com.guzzlio.domain.model.FillUpRecord
import com.guzzlio.domain.model.ServiceRecord
import com.guzzlio.domain.model.ServiceType
import com.guzzlio.domain.model.Vehicle
import com.guzzlio.domain.model.VehicleLifecycle
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import org.junit.Test

class SectionedCsvExporterTest {
    @Test
    fun export_thenImport_roundTripsCoreLedgerFields() {
        val vehicle = Vehicle(
            id = 1,
            legacySourceId = 1,
            name = "Test Car",
            make = "Toyota",
            model = "Corolla",
            year = 2010,
            lifecycle = VehicleLifecycle.ACTIVE,
        )
        val fillUp = FillUpRecord(
            id = 1,
            vehicleId = 1,
            dateTime = LocalDateTime.parse("2024-04-01T09:00:00"),
            odometerReading = 120000.0,
            distanceUnit = DistanceUnit.MILES,
            volume = 10.0,
            volumeUnit = VolumeUnit.GALLONS_US,
            pricePerUnit = 3.5,
            totalCost = 35.0,
            fuelEfficiency = 30.0,
            fuelEfficiencyUnit = FuelEfficiencyUnit.MPG_US,
            importedFuelTypeText = "Gasoline - Regular (87)",
            fuelBrand = "Shell",
            tags = listOf("work"),
            notes = "Round trip",
        )
        val serviceType = ServiceType(id = 11, legacySourceId = 11, name = "Engine Oil")
        val serviceRecord = ServiceRecord(
            id = 2,
            vehicleId = 1,
            dateTime = LocalDateTime.parse("2024-04-02T10:30:00"),
            odometerReading = 120050.0,
            distanceUnit = DistanceUnit.MILES,
            totalCost = 50.0,
            serviceTypeIds = listOf(11),
        )

        val csv = SectionedCsvExporter().export(
            preferences = AppPreferenceSnapshot(),
            vehicles = listOf(vehicle),
            fillUps = listOf(fillUp),
            services = listOf(serviceRecord),
            expenses = emptyList(),
            trips = emptyList(),
            serviceTypes = listOf(serviceType),
        )

        val imported = AcarCsvImporter().import(ByteArrayInputStream(csv.toByteArray()))
        assertThat(imported.vehicles.single().name).isEqualTo("Test Car")
        assertThat(imported.fillUpRecords.single().fuelBrand).isEqualTo("Shell")
        assertThat(imported.fillUpRecords.single().totalCost).isWithin(0.0001).of(35.0)
        assertThat(imported.serviceRecords.single().totalCost).isWithin(0.0001).of(50.0)
        assertThat(imported.serviceTypes.single().name).isEqualTo("Engine Oil")
    }
}
