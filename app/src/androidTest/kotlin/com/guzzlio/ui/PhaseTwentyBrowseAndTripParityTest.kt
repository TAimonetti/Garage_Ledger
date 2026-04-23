package com.guzzlio.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.core.model.VolumeUnit
import com.guzzlio.data.GarageRepository
import com.guzzlio.data.backup.LocalBackupManager
import com.guzzlio.data.local.GarageDatabase
import com.guzzlio.data.local.toEntity
import com.guzzlio.data.preferences.AppPreferencesRepository
import com.guzzlio.domain.model.FillUpRecord
import com.guzzlio.domain.model.TripRecord
import com.guzzlio.domain.model.Vehicle
import com.guzzlio.ui.theme.GuzzlioTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

class PhaseTwentyBrowseAndTripParityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: GarageDatabase
    private lateinit var repository: GarageRepository
    private lateinit var backupManager: LocalBackupManager
    private var vehicleId: Long = 0L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GarageDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GarageRepository(
            database = database,
            preferencesRepository = AppPreferencesRepository(context),
        )
        backupManager = LocalBackupManager(context, repository)
        runBlocking {
            vehicleId = database.garageDao().insertVehicles(
                listOf(
                    Vehicle(name = "Corolla", make = "Toyota", model = "Corolla").toEntity(idOverride = 0L),
                ),
            ).single()
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun browseOptions_toggleSortOrderAndExposeExport() {
        runBlocking {
            repository.saveFillUp(
                FillUpRecord(
                    vehicleId = vehicleId,
                    dateTime = LocalDateTime.parse("2026-04-01T08:00:00"),
                    odometerReading = 120000.0,
                    distanceUnit = DistanceUnit.MILES,
                    volume = 10.0,
                    volumeUnit = VolumeUnit.GALLONS_US,
                    pricePerUnit = 3.20,
                    totalCost = 32.0,
                    fuelBrand = "Shell",
                ),
            )
            repository.saveFillUp(
                FillUpRecord(
                    vehicleId = vehicleId,
                    dateTime = LocalDateTime.parse("2026-04-10T08:00:00"),
                    odometerReading = 120300.0,
                    distanceUnit = DistanceUnit.MILES,
                    volume = 11.0,
                    volumeUnit = VolumeUnit.GALLONS_US,
                    pricePerUnit = 3.30,
                    totalCost = 36.3,
                    fuelBrand = "Chevron",
                ),
            )
        }

        composeRule.setContent {
            GuzzlioTheme {
                GuzzlioApp(
                    repository = repository,
                    backupManager = backupManager,
                )
            }
        }

        composeRule.onNodeWithText("Browse Records").performClick()
        composeRule.onNodeWithText("2 matching records | Newest First").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Browse options").performClick()
        composeRule.onNodeWithText("Export Current Results").assertIsDisplayed()
        composeRule.onNodeWithText("Sort Oldest First").performClick()
        composeRule.onNodeWithText("2 matching records | Oldest First").assertIsDisplayed()

        runBlocking {
            assertFalse(repository.getPreferenceSnapshot().browseSortDescending)
        }
    }

    @Test
    fun browseTripActions_openCopyTripFlow() {
        runBlocking {
            repository.saveTrip(
                TripRecord(
                    vehicleId = vehicleId,
                    startDateTime = LocalDateTime.parse("2026-04-05T09:00:00"),
                    startOdometerReading = 120500.0,
                    startLocation = "Phoenix",
                    endDateTime = LocalDateTime.parse("2026-04-05T09:45:00"),
                    endOdometerReading = 120530.0,
                    endLocation = "Tempe",
                    distanceUnit = DistanceUnit.MILES,
                    purpose = "Client Visit",
                    client = "ACME",
                    tags = listOf("client"),
                    notes = "Bring paperwork",
                ),
            )
        }

        composeRule.setContent {
            GuzzlioTheme {
                GuzzlioApp(
                    repository = repository,
                    backupManager = backupManager,
                )
            }
        }

        composeRule.onNodeWithText("Browse Records").performClick()
        composeRule.onNodeWithText("Trips").performClick()
        composeRule.onAllNodesWithContentDescription("Record actions")[0].performClick()
        composeRule.onNodeWithText("Copy Trip").performClick()
        composeRule.onNodeWithText("Copy Trip").assertIsDisplayed()
        composeRule.onNodeWithText("Start odometer and all end-trip values are reset for the new trip.").assertIsDisplayed()
    }

    @Test
    fun browseTripActions_openFinishTripFlow() {
        runBlocking {
            repository.saveTrip(
                TripRecord(
                    vehicleId = vehicleId,
                    startDateTime = LocalDateTime.parse("2026-04-06T10:00:00"),
                    startOdometerReading = 120600.0,
                    startLocation = "Phoenix",
                    distanceUnit = DistanceUnit.MILES,
                    purpose = "Delivery",
                ),
            )
        }

        composeRule.setContent {
            GuzzlioTheme {
                GuzzlioApp(
                    repository = repository,
                    backupManager = backupManager,
                )
            }
        }

        composeRule.onNodeWithText("Browse Records").performClick()
        composeRule.onNodeWithText("Trips").performClick()
        composeRule.onAllNodesWithContentDescription("Record actions")[0].performClick()
        composeRule.onNodeWithText("Finish Trip").performClick()
        composeRule.onNodeWithText("Finish Trip").assertIsDisplayed()
        composeRule.onNodeWithText("Finish this open trip by entering the arrival details and final odometer.").assertIsDisplayed()
    }
}
