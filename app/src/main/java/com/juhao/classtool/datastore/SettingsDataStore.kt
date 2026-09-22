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

    private val testModeKey = booleanPreferencesKey("test_mode")
    private val classDurationKey = intPreferencesKey("class_duration_minutes")
    private val breakDurationKey = intPreferencesKey("break_duration_minutes")
    private val prepBellKey = booleanPreferencesKey("prep_bell")
    private val globalEventReminderKey = booleanPreferencesKey("global_event_reminder")
    private val squareScreenModeKey = booleanPreferencesKey("square_screen_mode")

    val testModeFlow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[testModeKey] ?: false
    }

    suspend fun getTestMode(): Boolean =
        context.settingsDataStore.data.map { it[testModeKey] ?: false }.first()

    suspend fun setTestMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[testModeKey] = enabled
        }
    }

    val prepBellFlow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[prepBellKey] ?: true
    }

    suspend fun getPrepBell(): Boolean =
        context.settingsDataStore.data.map { it[prepBellKey] ?: true }.first()

    suspend fun setPrepBell(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[prepBellKey] = enabled
        }
    }

    val globalEventReminderFlow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[globalEventReminderKey] ?: true
    }

    suspend fun getGlobalEventReminder(): Boolean =
        context.settingsDataStore.data.map { it[globalEventReminderKey] ?: true }.first()

    suspend fun setGlobalEventReminder(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[globalEventReminderKey] = enabled
        }
    }

    val squareScreenModeFlow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[squareScreenModeKey] ?: false
    }

    suspend fun getSquareScreenMode(): Boolean =
        context.settingsDataStore.data.map { it[squareScreenModeKey] ?: false }.first()

    suspend fun setSquareScreenMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[squareScreenModeKey] = enabled
        }
    }

    val classDurationFlow: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[classDurationKey] ?: 40
    }

    val breakDurationFlow: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[breakDurationKey] ?: 10
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