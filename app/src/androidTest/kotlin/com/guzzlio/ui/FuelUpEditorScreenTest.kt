package com.guzzlio.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.guzzlio.data.GarageRepository
import com.guzzlio.data.local.GarageDatabase
import com.guzzlio.data.local.toEntity
import com.guzzlio.data.preferences.AppPreferencesRepository
import com.guzzlio.domain.model.Vehicle
import com.guzzlio.ui.theme.GuzzlioTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FuelUpEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: GarageDatabase
    private lateinit var repository: GarageRepository
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
        runBlocking {
            vehicleId = database.garageDao().insertVehicles(
                listOf(
                    Vehicle(
                        name = "Test Vehicle",
                        make = "Toyota",
                        model = "Corolla",
                    ).toEntity(idOverride = 0L),
                ),
            ).single()
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savesNewFuelUpAgainstRealRepository() {
        var backCalls = 0
        composeRule.setContent {
            GuzzlioTheme {
                FuelUpEditorScreen(
                    repository = repository,
                    vehicleId = vehicleId,
                    recordId = -1L,
                    onOpenFuelTypes = {},
                    onBack = { backCalls += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Odometer (mi)").performTextInput("120000")
        composeRule.onNodeWithText("Volume (gal (US))").performTextInput("10")
        composeRule.onNodeWithText("Price Per Unit").performTextInput("3.50")
        composeRule.onNodeWithText("Save Fuel-Up").performClick()

        composeRule.runOnIdle {
            assertEquals(1, backCalls)
        }
        runBlocking {
            assertEquals(1, database.garageDao().getVehicleFillUpsAscending(vehicleId).size)
        }
    }

    @Test
    fun rendersEssentialEditorFields() {
        composeRule.setContent {
            GuzzlioTheme {
                FuelUpEditorScreen(
                    repository = repository,
                    vehicleId = vehicleId,
                    recordId = -1L,
                    onOpenFuelTypes = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Date/Time (yyyy-MM-dd HH:mm)").assertIsDisplayed()
        composeRule.onNodeWithText("Price Per Unit").assertIsDisplayed()
        composeRule.onNodeWithText("Save Fuel-Up").assertIsDisplayed()
    }
}
