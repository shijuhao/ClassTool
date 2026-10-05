package com.juhao.classtool.datastore

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.juhao.classtool.theme.AppTheme
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

    private val dataStore: DataStore<Preferences>
        get() = context.settingsDataStore

    private val testModeKey = booleanPreferencesKey("test_mode")
    private val classDurationKey = intPreferencesKey("class_duration_minutes")
    private val breakDurationKey = intPreferencesKey("break_duration_minutes")
    private val prepBellKey = booleanPreferencesKey("prep_bell")
    private val globalEventReminderKey = booleanPreferencesKey("global_event_reminder")
    private val screenShapeModeKey = stringPreferencesKey("screen_shape_mode")
    private val keepScreenOnKey = booleanPreferencesKey("keep_screen_on")
    private val uiScaleKey = floatPreferencesKey("ui_scale")
    private val dynamicThemeKey = booleanPreferencesKey("dynamic_theme")
    private val appThemeKey = stringPreferencesKey("app_theme")
    private val useSystemColorKey = booleanPreferencesKey("use_system_color")
    private val customColorKey = longPreferencesKey("custom_color")
    private val pureBlackBackgroundKey = booleanPreferencesKey("pure_black_background")
    private val dayModeKey = booleanPreferencesKey("day_mode")

    private fun <T> preferenceFlow(key: Preferences.Key<T>, default: T): Flow<T> =
        dataStore.data.map { it[key] ?: default }

    private suspend fun <T> getPreference(key: Preferences.Key<T>, default: T): T =
        dataStore.data.first()[key] ?: default

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    val testModeFlow = preferenceFlow(testModeKey, false)
    suspend fun getTestMode() = getPreference(testModeKey, false)
    suspend fun setTestMode(enabled: Boolean) = setPreference(testModeKey, enabled)

    val prepBellFlow = preferenceFlow(prepBellKey, true)
    suspend fun getPrepBell() = getPreference(prepBellKey, true)
    suspend fun setPrepBell(enabled: Boolean) = setPreference(prepBellKey, enabled)

    val globalEventReminderFlow = preferenceFlow(globalEventReminderKey, true)
    suspend fun getGlobalEventReminder() = getPreference(globalEventReminderKey, true)
    suspend fun setGlobalEventReminder(enabled: Boolean) =
        setPreference(globalEventReminderKey, enabled)

    val screenShapeModeFlow: Flow<ScreenShapeMode> = dataStore.data.map { preferences ->
        preferences[screenShapeModeKey]
            ?.let { runCatching { ScreenShapeMode.valueOf(it) }.getOrDefault(ScreenShapeMode.AUTO) }
            ?: ScreenShapeMode.AUTO
    }

    suspend fun getScreenShapeMode(): ScreenShapeMode = screenShapeModeFlow.first()

    suspend fun setScreenShapeMode(mode: ScreenShapeMode) =
        setPreference(screenShapeModeKey, mode.name)

    val keepScreenOnFlow = preferenceFlow(keepScreenOnKey, false)
    suspend fun getKeepScreenOn() = getPreference(keepScreenOnKey, false)
    suspend fun setKeepScreenOn(enabled: Boolean) = setPreference(keepScreenOnKey, enabled)

    val classDurationFlow = preferenceFlow(classDurationKey, 40)
    val breakDurationFlow = preferenceFlow(breakDurationKey, 10)

    suspend fun getClassDuration() = getPreference(classDurationKey, 40)
    suspend fun setClassDuration(minutes: Int) = setPreference(classDurationKey, minutes)

    suspend fun getBreakDuration() = getPreference(breakDurationKey, 10)
    suspend fun setBreakDuration(minutes: Int) = setPreference(breakDurationKey, minutes)

    val uiScaleFlow = preferenceFlow(uiScaleKey, 1.0f)
    suspend fun getUiScale() = getPreference(uiScaleKey, 1.0f)
    suspend fun setUiScale(scale: Float) =
        setPreference(uiScaleKey, scale.coerceIn(0.5f, 1.5f))

    val dynamicThemeFlow = preferenceFlow(dynamicThemeKey, true)
    suspend fun getDynamicTheme() = getPreference(dynamicThemeKey, true)
    suspend fun setDynamicTheme(enabled: Boolean) = setPreference(dynamicThemeKey, enabled)

    val appThemeFlow: Flow<AppTheme?> = dataStore.data.map { preferences ->
        preferences[appThemeKey]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
    }

    suspend fun getAppTheme(): AppTheme? = appThemeFlow.first()

    suspend fun setAppTheme(theme: AppTheme?) {
        dataStore.edit { preferences ->
            if (theme == null) preferences.remove(appThemeKey)
            else preferences[appThemeKey] = theme.name
        }
    }

    val useSystemColorFlow = preferenceFlow(useSystemColorKey, true)
    suspend fun getUseSystemColor() = getPreference(useSystemColorKey, true)
    suspend fun setUseSystemColor(enabled: Boolean) = setPreference(useSystemColorKey, enabled)

    val customColorFlow: Flow<Color> = dataStore.data.map { preferences ->
        preferences[customColorKey]?.let { Color(it.toULong()) } ?: Color(0xFF9BD7FF)
    }

    suspend fun getCustomColor(): Color = customColorFlow.first()

    suspend fun setCustomColor(color: Color) =
        setPreference(customColorKey, color.value.toLong())

    val pureBlackBackgroundFlow = preferenceFlow(pureBlackBackgroundKey, false)
    suspend fun getPureBlackBackground() = getPreference(pureBlackBackgroundKey, false)
    suspend fun setPureBlackBackground(enabled: Boolean) =
        setPreference(pureBlackBackgroundKey, enabled)

    val dayModeFlow = preferenceFlow(dayModeKey, false)
    suspend fun getDayMode() = getPreference(dayModeKey, false)
    suspend fun setDayMode(enabled: Boolean) = setPreference(dayModeKey, enabled)
}