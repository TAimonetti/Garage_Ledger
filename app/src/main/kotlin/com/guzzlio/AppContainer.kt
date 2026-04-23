package com.guzzlio

import android.content.Context
import androidx.room.Room
import com.guzzlio.background.GarageWorkScheduler
import com.guzzlio.background.GarageWorkerFactory
import com.guzzlio.data.GarageRepository
import com.guzzlio.data.backup.LocalBackupManager
import com.guzzlio.data.local.GarageDatabase
import com.guzzlio.data.preferences.AppPreferencesRepository
import com.guzzlio.notifications.NotificationChannels
import com.guzzlio.shortcuts.QuickActionShortcutManager
import com.guzzlio.widgets.GarageWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: GarageDatabase = Room.databaseBuilder(
        appContext,
        GarageDatabase::class.java,
        "guzzlio.db",
    ).build()

    private val preferencesRepository = AppPreferencesRepository(appContext)

    val widgetUpdater = GarageWidgetUpdater(appContext)

    val repository: GarageRepository = GarageRepository(
        database = database,
        preferencesRepository = preferencesRepository,
        onLedgerChanged = {
            widgetUpdater.refreshAll()
        },
    )

    val backupManager = LocalBackupManager(
        context = appContext,
        repository = repository,
    )

    val workerFactory = GarageWorkerFactory(
        repository = repository,
        backupManager = backupManager,
    )

    private val workScheduler by lazy(LazyThreadSafetyMode.NONE) {
        GarageWorkScheduler(appContext)
    }

    fun start() {
        NotificationChannels.ensureCreated(appContext)
        QuickActionShortcutManager.syncDynamicShortcuts(appContext)
        applicationScope.launch {
            widgetUpdater.refreshAll()
        }
        applicationScope.launch {
            preferencesRepository.preferences.collectLatest { snapshot ->
                workScheduler.sync(snapshot)
            }
        }
    }

    fun enqueueImmediateBackup() {
        workScheduler.enqueueImmediateBackup()
    }
}
