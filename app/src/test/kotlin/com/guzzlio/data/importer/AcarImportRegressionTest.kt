package com.guzzlio.data.importer

import com.guzzlio.LocalFixturePaths
import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

class AcarImportRegressionTest {
    private val abpImporter = AcarAbpImporter()
    private val csvImporter = AcarCsvImporter()

    @Test
    fun abpSample_matchesExpectedCountsAndPreferences() {
        val fixturePath = LocalFixturePaths.acarAbp
        assumeNotNull(
            LocalFixturePaths.describeMissingFixture(
                name = "aCar ABP",
                propertyName = "guzzlio.fixture.abp",
                envName = "GARAGE_LEDGER_FIXTURE_ABP",
            ),
            fixturePath,
        )
        assumeTrue(Files.exists(fixturePath))
        Files.newInputStream(fixturePath).use { stream ->
            val imported = abpImporter.import(stream)
            assertThat(imported.vehicles).hasSize(4)
            assertThat(imported.fillUpRecords).hasSize(328)
            assertThat(imported.serviceRecords).hasSize(19)
            assertThat(imported.expenseRecords).isEmpty()
            assertThat(imported.tripRecords).isEmpty()
            assertThat(imported.vehicleParts).hasSize(1)
            assertThat(imported.serviceTypes).hasSize(43)
            assertThat(imported.expenseTypes).hasSize(9)
            assertThat(imported.tripTypes).hasSize(6)
            assertThat(imported.fuelTypes).hasSize(28)

            val vehicleNameByLegacyId = imported.vehicles.associate { it.legacySourceId to it.name }
            assertThat(imported.fillUpRecords.count { vehicleNameByLegacyId[it.vehicleId] == "Corolla" }).isEqualTo(264)
            assertThat(imported.fillUpRecords.count { vehicleNameByLegacyId[it.vehicleId] == "Tundra" }).isEqualTo(64)
            assertThat(imported.serviceRecords.count { vehicleNameByLegacyId[it.vehicleId] == "Corolla" }).isEqualTo(8)
            assertThat(imported.serviceRecords.count { vehicleNameByLegacyId[it.vehicleId] == "Tundra" }).isEqualTo(4)
            assertThat(imported.serviceRecords.count { vehicleNameByLegacyId[it.vehicleId] == "Acadia" }).isEqualTo(6)
            assertThat(imported.serviceRecords.count { vehicleNameByLegacyId[it.vehicleId] == "Altima" }).isEqualTo(1)

            val preferences = imported.preferences!!
            assertThat(preferences.distanceUnit.storageValue).isEqualTo("mi")
            assertThat(preferences.volumeUnit.storageValue).isEqualTo("gal (US)")
            assertThat(preferences.fuelEfficiencyUnit.storageValue).isEqualTo("MPG (US)")
            assertThat(preferences.currencySymbol).isEqualTo("$")
            assertThat(preferences.fuelEfficiencyAssignmentMethod.storageValue).isEqualTo("previous-record")
            assertThat(preferences.browseSortDescending).isTrue()
            assertThat(preferences.backupHistoryCount).isEqualTo(10)
        }
    }

    @Test
    fun csvSample_matchesExpectedCounts() {
        val fixturePath = LocalFixturePaths.acarCsv
        assumeNotNull(
            LocalFixturePaths.describeMissingFixture(
                name = "aCar CSV",
                propertyName = "guzzlio.fixture.csv",
                envName = "GARAGE_LEDGER_FIXTURE_CSV",
            ),
            fixturePath,
        )
        assumeTrue(Files.exists(fixturePath))
        Files.newInputStream(fixturePath).use { stream ->
            val imported = csvImporter.import(stream)
            assertThat(imported.vehicles).hasSize(4)
            assertThat(imported.fillUpRecords).hasSize(328)
            assertThat(imported.serviceRecords).hasSize(19)
            assertThat(imported.expenseRecords).isEmpty()
            assertThat(imported.tripRecords).isEmpty()

            val vehicleNameByLegacyId = imported.vehicles.associate { it.legacySourceId to it.name }
            assertThat(imported.fillUpRecords.count { vehicleNameByLegacyId[it.vehicleId] == "Corolla" }).isEqualTo(264)
            assertThat(imported.fillUpRecords.count { vehicleNameByLegacyId[it.vehicleId] == "Tundra" }).isEqualTo(64)
        }
    }
}
