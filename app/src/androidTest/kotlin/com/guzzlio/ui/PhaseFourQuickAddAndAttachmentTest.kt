package com.guzzlio.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.guzzlio.data.GarageRepository
import com.guzzlio.data.backup.LocalBackupManager
import com.guzzlio.data.local.GarageDatabase
import com.guzzlio.data.local.toEntity
import com.guzzlio.data.preferences.AppPreferencesRepository
import com.guzzlio.domain.model.Vehicle
import com.guzzlio.shortcuts.LaunchRequest
import com.guzzlio.shortcuts.QuickActionTarget
import com.guzzlio.ui.theme.GuzzlioTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PhaseFourQuickAddAndAttachmentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: GarageDatabase
    private lateinit var repository: GarageRepository
    private lateinit var backupManager: LocalBackupManager

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
            database.garageDao().insertVehicles(
                listOf(
                    Vehicle(name = "Corolla", make = "Toyota", model = "Corolla").toEntity(idOverride = 0L),
                    Vehicle(name = "Tundra", make = "Toyota", model = "Tundra").toEntity(idOverride = 0L),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun launchRequestOpensQuickAddChooserAndEditorShowsAttachments() {
        composeRule.setContent {
            GuzzlioTheme {
                GuzzlioApp(
                    repository = repository,
                    backupManager = backupManager,
                    launchRequest = LaunchRequest(QuickActionTarget.SERVICE.route()),
                )
            }
        }

        composeRule.onNodeWithText("Choose Vehicle").assertIsDisplayed()
        composeRule.onNodeWithText("Tundra").performClick()
        composeRule.onNodeWithText("New Service").assertIsDisplayed()
        composeRule.onNodeWithText("Attachments").assertIsDisplayed()
    }
}
