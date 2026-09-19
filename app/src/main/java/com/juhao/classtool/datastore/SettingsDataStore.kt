package com.juhao.classtool.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val showEventOnHomeKey = booleanPreferencesKey("show_event_on_home")
    private val classDurationKey = intPreferencesKey("class_duration_minutes")
    private val breakDurationKey = intPreferencesKey("break_duration_minutes")

    val showEventOnHomeFlow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[showEventOnHomeKey] ?: true
    }

    val classDurationFlow: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[classDurationKey] ?: 40
    }

    val breakDurationFlow: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[breakDurationKey] ?: 10
    }

    suspend fun getShowEventOnHome(): Boolean =
        context.settingsDataStore.data.map { it[showEventOnHomeKey] ?: true }.first()

    suspend fun setShowEventOnHome(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[showEventOnHomeKey] = enabled
        }
    }

    suspend fun getClassDuration(): Int =
        context.settingsDataStore.data.map { it[classDurationKey] ?: 40 }.first()

    suspend fun setClassDuration(minutes: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[classDurationKey] = minutes
        }
    }

    suspend fun getBreakDuration(): Int =
        context.settingsDataStore.data.map { it[breakDurationKey] ?: 10 }.first()

    suspend fun setBreakDuration(minutes: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[breakDurationKey] = minutes
        }
    }
}