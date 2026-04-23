package com.guzzlio.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.guzzlio.domain.model.AppPreferenceSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "guzzlio_preferences")

class AppPreferencesRepository(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val snapshotKey = stringPreferencesKey("app_preferences_snapshot")

    val preferences: Flow<AppPreferenceSnapshot> = context.dataStore.data.map { preferences ->
        preferences[snapshotKey]
            ?.let { runCatching { json.decodeFromString<AppPreferenceSnapshot>(it) }.getOrNull() }
            ?: AppPreferenceSnapshot()
    }

    suspend fun replace(snapshot: AppPreferenceSnapshot) {
        context.dataStore.edit { preferences ->
            preferences[snapshotKey] = json.encodeToString(snapshot)
        }
    }

    suspend fun update(transform: (AppPreferenceSnapshot) -> AppPreferenceSnapshot) {
        context.dataStore.edit { preferences ->
            val current = preferences[snapshotKey]
                ?.let { runCatching { json.decodeFromString<AppPreferenceSnapshot>(it) }.getOrNull() }
                ?: AppPreferenceSnapshot()
            preferences[snapshotKey] = json.encodeToString(transform(current))
        }
    }

    suspend fun currentSnapshot(): AppPreferenceSnapshot = preferences.first()
}
