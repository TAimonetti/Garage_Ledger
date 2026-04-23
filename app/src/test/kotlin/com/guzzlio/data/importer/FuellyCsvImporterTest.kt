package com.guzzlio.data.importer

import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.core.model.VolumeUnit
import com.guzzlio.domain.model.FuellyCsvImportConfig
import com.guzzlio.domain.model.FuellyImportField
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FuellyCsvImporterTest {
    private val importer = FuellyCsvImporter()

    @Test
    fun preview_suggestsMappingsAndMetricUnits() {
        val preview = importer.preview(
            """
            km,litres,price,fuelup_date,brand,tags,notes,city_percentage,missed_fuelup,partial_fuelup
            1200,40,120.50,2024-01-05,Shell,"roadtrip,work","Test note",60,no,yes
            """.trimIndent().byteInputStream(),
        )

        assertThat(preview.headers).containsExactly(
            "km",
            "litres",
            "price",
            "fuelup_date",
            "brand",
            "tags",
            "notes",
            "city_percentage",
            "missed_fuelup",
            "partial_fuelup",
        ).inOrder()
        assertThat(preview.suggestedMapping[FuellyImportField.ODOMETER]).isEqualTo("km")
        assertThat(preview.suggestedMapping[FuellyImportField.VOLUME]).isEqualTo("litres")
        assertThat(preview.suggestedMapping[FuellyImportField.TOTAL_COST]).isEqualTo("price")
        assertThat(preview.suggestedDistanceUnit).isEqualTo(DistanceUnit.KILOMETERS)
        assertThat(preview.suggestedVolumeUnit).isEqualTo(VolumeUnit.LITERS)
        assertThat(preview.sampleRows).hasSize(1)
    }

    @Test
    fun importFillUps_honorsCustomFieldMapping() {
        val config = FuellyCsvImportConfig(
            vehicleId = 44L,
            fieldMapping = mapOf(
                FuellyImportField.ODOMETER to "Distance Reading",
                FuellyImportField.VOLUME to "Fuel Bought",
                FuellyImportField.TOTAL_COST to "Amount Paid",
                FuellyImportField.FUEL_UP_DATE to "Date of Fill",
                FuellyImportField.BRAND to "Station",
                FuellyImportField.NOTES to "Driver Notes",
                FuellyImportField.CITY_PERCENTAGE to "Town Driving",
                FuellyImportField.TAGS to "Labels",
                FuellyImportField.MISSED_FILL_UP to "Missed",
                FuellyImportField.PARTIAL_FILL_UP to "Partial",
            ),
            distanceUnit = DistanceUnit.MILES,
            volumeUnit = VolumeUnit.GALLONS_US,
        )

        val result = importer.importFillUps(
            """
            Distance Reading,Fuel Bought,Amount Paid,Date of Fill,Station,Driver Notes,Town Driving,Labels,Missed,Partial
            15234,12.5,43.75,03/18/2024,Chevron,"Spring break fuel",35,"trip, work",1,0
            """.trimIndent().byteInputStream(),
            config = config,
        )

        assertThat(result.issues).isEmpty()
        assertThat(result.fillUpRecords).hasSize(1)
        val record = result.fillUpRecords.single()
        assertThat(record.vehicleId).isEqualTo(44L)
        assertThat(record.odometerReading).isEqualTo(15234.0)
        assertThat(record.volume).isEqualTo(12.5)
        assertThat(record.totalCost).isEqualTo(43.75)
        assertThat(record.pricePerUnit).isWithin(0.0001).of(3.5)
        assertThat(record.fuelBrand).isEqualTo("Chevron")
        assertThat(record.notes).isEqualTo("Spring break fuel")
        assertThat(record.cityDrivingPercentage).isEqualTo(35)
        assertThat(record.tags).containsExactly("trip", "work").inOrder()
        assertThat(record.previousMissedFillups).isTrue()
        assertThat(record.partial).isFalse()
    }

    @Test
    fun importFillUps_reportsSkippedRows() {
        val config = FuellyCsvImportConfig(
            vehicleId = 12L,
            fieldMapping = mapOf(
                FuellyImportField.ODOMETER to "odometer",
                FuellyImportField.VOLUME to "gallons",
                FuellyImportField.TOTAL_COST to "price",
                FuellyImportField.FUEL_UP_DATE to "fuelup_date",
            ),
            distanceUnit = DistanceUnit.MILES,
            volumeUnit = VolumeUnit.GALLONS_US,
        )

        val result = importer.importFillUps(
            """
            odometer,gallons,price,fuelup_date
            10100,10.0,31.25,2024-04-10
            10200,11.0,not-a-price,2024-04-20
            """.trimIndent().byteInputStream(),
            config = config,
        )

        assertThat(result.fillUpRecords).hasSize(1)
        assertThat(result.issues).hasSize(1)
        assertThat(result.issues.single().message).contains("total price")
    }

    @Test
    fun importConfig_validationFlagsMissingRequiredAndDuplicateHeaders() {
        val config = FuellyCsvImportConfig(
            vehicleId = 99L,
            fieldMapping = mapOf(
                FuellyImportField.ODOMETER to "distance",
                FuellyImportField.VOLUME to "distance",
            ),
            distanceUnit = DistanceUnit.MILES,
            volumeUnit = VolumeUnit.GALLONS_US,
        )

        val errors = config.validationErrors(listOf("distance"))

        assertThat(errors).contains("Total Price is required.")
        assertThat(errors).contains("Fuel-Up Date is required.")
        assertThat(errors).contains("Each CSV column can only be assigned once.")
    }
}
