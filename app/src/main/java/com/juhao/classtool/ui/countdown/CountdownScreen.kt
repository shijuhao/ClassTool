package com.juhao.classtool.ui.countdown

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.CountdownDataStore
import com.juhao.classtool.key.AddCountdownNavScreen
import com.juhao.classtool.key.CountdownDetailNavScreen
import com.juhao.classtool.ui.schedule.LocalScreenShape
import com.juhao.classtool.ui.schedule.ScreenShape
import com.juhao.classtool.ui.schedule.rememberAdaptiveTransformationSpec

@Composable
fun CountdownScreen(
    onNavigate: (Any) -> Unit
) {
    val context = LocalContext.current
    val store = remember { CountdownDataStore(context) }
    val days by store.daysFlow.collectAsState(initial = emptyList())

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val sorted = remember(days) {
        days.sortedBy { kotlin.math.abs(daysUntil(it.dateMillis)) }
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding
                        ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text("倒计日") }
            }

            item {
                FilledTonalButton(
                    onClick = { onNavigate(AddCountdownNavScreen) },
                    label = { Text("添加倒计日") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            items(sorted.size) { index ->
                val day = sorted[index]
                val remaining = daysUntil(day.dateMillis)
                FilledTonalButton(
                    onClick = { onNavigate(CountdownDetailNavScreen(day.id)) },
                    label = { Text(day.title) },
                    secondaryLabel = {
                        Text("${countdownLabel(remaining)} · ${formatDate(day.dateMillis)}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}