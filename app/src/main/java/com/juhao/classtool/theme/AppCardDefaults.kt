package com.juhao.classtool.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.CardColors
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme

object AppCardDefaults {
    @Composable
    fun cardColors(): CardColors =
        CardDefaults.cardColors(
            titleColor = MaterialTheme.colorScheme.tertiary
        )
}
