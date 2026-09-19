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
data object ViewScheduleNavScreen : AppKey

@Serializable
data object GameMenuNavScreen : AppKey

@Serializable
data object CoinNavScreen : AppKey