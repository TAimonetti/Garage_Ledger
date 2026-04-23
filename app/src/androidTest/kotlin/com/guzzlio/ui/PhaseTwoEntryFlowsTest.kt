package com.guzzlio.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.guzzlio.data.GarageRepository
import com.guzzlio.data.local.GarageDatabase
import com.guzzlio.data.local.ServiceTypeEntity
import com.guzzlio.data.local.TripTypeEntity
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

class PhaseTwoEntryFlowsTest {
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
            database.garageDao().insertServiceTypes(
                listOf(
                    ServiceTypeEntity(
                        name = "Oil Change",
                        notes = "",
                        defaultTimeReminderMonths = 6,
                        defaultDistanceReminder = 5000.0,
                    ),
                ),
            )
            database.garageDao().insertTripTypes(
                listOf(
                    TripTypeEntity(
                        name = "Business",
                        defaultTaxDeductionRate = 0.67,
                        notes = "",
                    ),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savesNewServiceWithSelectedSubtype() {
        var backCalls = 0
        composeRule.setContent {
            GuzzlioTheme {
                ServiceEditorScreen(
                    repository = repository,
                    vehicleId = vehicleId,
                    recordId = -1L,
                    onOpenServiceTypes = {},
                    onBack = { backCalls += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Odometer (mi)").performTextInput("120500")
        composeRule.onNodeWithText("Total Cost").performTextInput("65")
        composeRule.onNodeWithText("Oil Change").performClick()
        composeRule.onNodeWithText("Add Service").performClick()

        composeRule.runOnIdle {
            assertEquals(1, backCalls)
        }
        runBlocking {
            val services = database.garageDao().getVehicleServicesAscending(vehicleId)
            assertEquals(1, services.size)
            val crossRefs = database.garageDao().getServiceRecordCrossRefs(listOf(services.single().id))
            assertEquals(1, crossRefs.size)
        }
    }

    @Test
    fun savesTripAndDerivesDistance() {
        var backCalls = 0
        composeRule.setContent {
            GuzzlioTheme {
                TripEditorScreen(
                    repository = repository,
                    vehicleId = vehicleId,
                    recordId = -1L,
                    onOpenTripTypes = {},
                    onBack = { backCalls += 1 },
                )
            }
        }

        composeRule.onNodeWithText("Start (yyyy-MM-dd HH:mm)").performTextClearance()
        composeRule.onNodeWithText("Start (yyyy-MM-dd HH:mm)").performTextInput("2026-04-20 08:00")
        composeRule.onNodeWithText("Start Odometer (mi)").performTextInput("120500")
        composeRule.onNodeWithText("End (optional)").performTextInput("2026-04-20 09:15")
        composeRule.onNodeWithText("End Odometer (optional)").performTextInput("120530")
        composeRule.onNodeWithText("Add Trip").performClick()

        composeRule.runOnIdle {
            assertEquals(1, backCalls)
        }
        runBlocking {
            val trips = database.garageDao().getVehicleTripsAscending(vehicleId)
            assertEquals(1, trips.size)
            assertEquals(30.0, trips.single().distance ?: 0.0, 0.001)
        }
    }
}
