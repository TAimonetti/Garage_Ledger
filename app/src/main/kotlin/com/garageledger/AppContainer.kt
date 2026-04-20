package com.garageledger

import android.content.Context
import androidx.room.Room
import com.garageledger.data.GarageRepository
import com.garageledger.data.local.GarageDatabase
import com.garageledger.data.preferences.AppPreferencesRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

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
}
