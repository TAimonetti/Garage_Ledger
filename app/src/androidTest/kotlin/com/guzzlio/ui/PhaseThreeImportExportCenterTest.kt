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
import com.guzzlio.data.preferences.AppPreferencesRepository
import com.guzzlio.ui.theme.GuzzlioTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PhaseThreeImportExportCenterTest {
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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun consoleNavigatesToImportExportCenter() {
        composeRule.setContent {
            GuzzlioTheme {
                GuzzlioApp(
                    repository = repository,
                    backupManager = backupManager,
                )
            }
        }

        composeRule.onNodeWithText("Import & Export").performClick()
        composeRule.onNodeWithText("Automatic Local Backups").assertIsDisplayed()
        composeRule.onNodeWithText("Fuelly CSV").assertIsDisplayed()
        composeRule.onNodeWithText("Open Zipped Backup").assertIsDisplayed()
        composeRule.onNodeWithText("Restore Backup Zip").assertIsDisplayed()
    }
}
