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
val Context.settingsDataStoreTest: DataStore<Preferences> by preferencesDataStore(name = "settings_test")

class SettingsDataStore(private val context: Context) {

    private val testModeKey = booleanPreferencesKey("test_mode")
    private val classDurationKey = intPreferencesKey("class_duration_minutes")
    private val breakDurationKey = intPreferencesKey("break_duration_minutes")
    private val prepBellKey = booleanPreferencesKey("prep_bell")
    private val globalEventReminderKey = booleanPreferencesKey("global_event_reminder")

    private val dataStore: DataStore<Preferences>
        get() = if (TestModeState.enabled) {
            context.settingsDataStoreTest
        } else {
            context.settingsDataStore
        }

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

    val prepBellFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[prepBellKey] ?: true
    }

    suspend fun getPrepBell(): Boolean =
        dataStore.data.map { it[prepBellKey] ?: true }.first()

    suspend fun setPrepBell(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[prepBellKey] = enabled
        }
    }

    val globalEventReminderFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[globalEventReminderKey] ?: true
    }

    suspend fun getGlobalEventReminder(): Boolean =
        dataStore.data.map { it[globalEventReminderKey] ?: true }.first()

    suspend fun setGlobalEventReminder(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[globalEventReminderKey] = enabled
        }
    }

    val classDurationFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[classDurationKey] ?: 40
    }

    val breakDurationFlow: Flow<Int> = dataStore.data.map { preferences ->
        preferences[breakDurationKey] ?: 10
    }

    suspend fun getClassDuration(): Int =
        dataStore.data.map { it[classDurationKey] ?: 40 }.first()

    suspend fun setClassDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[classDurationKey] = minutes
        }
    }

    suspend fun getBreakDuration(): Int =
        dataStore.data.map { it[breakDurationKey] ?: 10 }.first()

    suspend fun setBreakDuration(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[breakDurationKey] = minutes
        }
    }
}