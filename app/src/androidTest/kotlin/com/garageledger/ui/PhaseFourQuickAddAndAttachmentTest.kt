package com.garageledger.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.garageledger.data.GarageRepository
import com.garageledger.data.backup.LocalBackupManager
import com.garageledger.data.local.GarageDatabase
import com.garageledger.data.local.toEntity
import com.garageledger.data.preferences.AppPreferencesRepository
import com.garageledger.domain.model.Vehicle
import com.garageledger.shortcuts.LaunchRequest
import com.garageledger.shortcuts.QuickActionTarget
import com.garageledger.ui.theme.GarageLedgerTheme
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
            GarageLedgerTheme {
                GarageLedgerApp(
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
