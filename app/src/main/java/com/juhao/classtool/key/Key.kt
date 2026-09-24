package com.juhao.classtool.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppKey : NavKey

@Serializable
data object MenuScreen : AppKey

@Serializable
data object EditScheduleNavScreen : AppKey
@Serializable
data object CourseScheduleNavScreen : AppKey

@Serializable
data object CountdownNavScreen : AppKey
@Serializable
data object AddCountdownNavScreen : AppKey
@Serializable
data class EditCountdownNavScreen(val id: Long) : AppKey
@Serializable
data class CountdownDetailNavScreen(val id: Long) : AppKey

@Serializable
data object ToolMenuNavScreen : AppKey
@Serializable
data object TimerNavScreen : AppKey

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
data object BackupRestoreNavScreen : AppKey
@Serializable
data object AboutNavScreen : AppKey