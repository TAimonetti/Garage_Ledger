package com.garageledger

import android.content.Context
import androidx.room.Room
import com.garageledger.background.GarageWorkScheduler
import com.garageledger.background.GarageWorkerFactory
import com.garageledger.data.GarageRepository
import com.garageledger.data.backup.LocalBackupManager
import com.garageledger.data.local.GarageDatabase
import com.garageledger.data.preferences.AppPreferencesRepository
import com.garageledger.notifications.NotificationChannels
import com.garageledger.shortcuts.QuickActionShortcutManager
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
        "garage-ledger.db",
    ).build()

    private val preferencesRepository = AppPreferencesRepository(appContext)

    val repository = GarageRepository(
        database = database,
        preferencesRepository = preferencesRepository,
    )

    val backupManager = LocalBackupManager(
        context = appContext,
        repository = repository,
    )

    val workerFactory = GarageWorkerFactory(
        repository = repository,
        backupManager = backupManager,
    )

    private val workScheduler = GarageWorkScheduler(appContext)

    fun start() {
        NotificationChannels.ensureCreated(appContext)
        QuickActionShortcutManager.syncDynamicShortcuts(appContext)
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
