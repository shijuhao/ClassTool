package com.juhao.classtool.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppKey : NavKey

@Serializable
data object MenuScreen : AppKey

@Serializable
data object TimeTableNavScreen : AppKey
@Serializable
data object CourseTableNavScreen : AppKey


@Serializable
data class AddScheduleEventNavScreen(
    val draft: com.juhao.classtool.ui.schedule.ScheduleEventDraft
) : AppKey

@Serializable
data class EditScheduleEventNavScreen(
    val draft: com.juhao.classtool.ui.schedule.ScheduleEventDraft
) : AppKey

@Serializable
data class CourseEditNavScreen(
    val draft: com.juhao.classtool.ui.schedule.CourseEditDraft
) : AppKey

@Serializable
data class ActivityPresetPickerNavScreen(
    val draft: com.juhao.classtool.ui.schedule.ScheduleEventDraft
) : AppKey

@Serializable
data class CoursePresetPickerNavScreen(
    val draft: com.juhao.classtool.ui.schedule.CourseEditDraft,
    val directSave: Boolean = false
) : AppKey

@Serializable
data class CustomPresetNavScreen(
    val target: com.juhao.classtool.ui.schedule.PresetTarget,
    val initialName: String,
    val initialColor: String? = null,
    val directSave: Boolean = false
) : AppKey

@Serializable
data object CountdownNavScreen : AppKey
@Serializable
data object AddCountdownNavScreen : AppKey
@Serializable
data class EditCountdownNavScreen(val id: Long) : AppKey
@Serializable
data class CountdownDetailNavScreen(val id: Long) : AppKey

@Serializable
data object TodoNavScreen : AppKey
@Serializable
data object AddTodoNavScreen : AppKey
@Serializable
data class EditTodoNavScreen(val id: Long) : AppKey
@Serializable
data class TodoDetailNavScreen(val id: Long) : AppKey

@Serializable
data object ToolMenuNavScreen : AppKey
@Serializable
data object TimerNavScreen : AppKey
@Serializable
data object ToolCountdownNavScreen : AppKey
@Serializable
data object ClockNavScreen : AppKey

@Serializable
data object GameMenuNavScreen : AppKey
@Serializable
data object CoinNavScreen : AppKey
@Serializable
data object DiceNavScreen : AppKey
@Serializable
data object ReactionNavScreen : AppKey

@Serializable
data object SettingsNavScreen : AppKey
@Serializable
data object ThemeNavScreen : AppKey
@Serializable
data object CustomThemeNavScreen : AppKey
@Serializable
data object DeveloperNavScreen : AppKey
@Serializable
data object AdjustmentNavScreen : AppKey
@Serializable
data object BackupRestoreNavScreen : AppKey
@Serializable
data object ScheduleTableNavScreen : AppKey
@Serializable
data object AboutNavScreen : AppKey