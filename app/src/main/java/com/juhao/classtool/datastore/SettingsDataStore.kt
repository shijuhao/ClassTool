package com.juhao.classtool.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
enum class ScreenShapeMode {
    AUTO,
    FORCE_SQUARE,
    FORCE_ROUND
}

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val testModeKey = booleanPreferencesKey("test_mode")
    private val classDurationKey = intPreferencesKey("class_duration_minutes")
    private val breakDurationKey = intPreferencesKey("break_duration_minutes")
    private val prepBellKey = booleanPreferencesKey("prep_bell")
    private val globalEventReminderKey = booleanPreferencesKey("global_event_reminder")
    private val screenShapeModeKey = stringPreferencesKey("screen_shape_mode")
    private val keepScreenOnKey = booleanPreferencesKey("keep_screen_on")
    private val uiScaleKey = floatPreferencesKey("ui_scale")
    private val dynamicThemeKey = booleanPreferencesKey("dynamic_theme")

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

    val screenShapeModeFlow: Flow<ScreenShapeMode> = context.settingsDataStore.data.map { preferences ->
        val raw = preferences[screenShapeModeKey]
        if (raw == null) {
            ScreenShapeMode.AUTO
        } else {
            runCatching { ScreenShapeMode.valueOf(raw) }.getOrDefault(ScreenShapeMode.AUTO)
        }
    }

    suspend fun getScreenShapeMode(): ScreenShapeMode =
        context.settingsDataStore.data.map { preferences ->
            val raw = preferences[screenShapeModeKey]
            if (raw == null) {
                ScreenShapeMode.AUTO
            } else {
                runCatching { ScreenShapeMode.valueOf(raw) }.getOrDefault(ScreenShapeMode.AUTO)
            }
        }.first()

    suspend fun setScreenShapeMode(mode: ScreenShapeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[screenShapeModeKey] = mode.name
        }
    }

    val keepScreenOnFlow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[keepScreenOnKey] ?: false
    }

    suspend fun getKeepScreenOn(): Boolean =
        context.settingsDataStore.data.map { it[keepScreenOnKey] ?: false }.first()

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[keepScreenOnKey] = enabled
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

    val uiScaleFlow: Flow<Float> = context.settingsDataStore.data.map { preferences ->
        preferences[uiScaleKey] ?: 1.0f
    }

    suspend fun getUiScale(): Float =
        context.settingsDataStore.data.map { it[uiScaleKey] ?: 1.0f }.first()

    suspend fun setUiScale(scale: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[uiScaleKey] = scale.coerceIn(0.5f, 1.5f)
        }
    }

    val dynamicThemeFlow: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[dynamicThemeKey] ?: true
    }

    suspend fun getDynamicTheme(): Boolean =
        context.settingsDataStore.data.map { it[dynamicThemeKey] ?: true }.first()

    suspend fun setDynamicTheme(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[dynamicThemeKey] = enabled
        }
    }
}