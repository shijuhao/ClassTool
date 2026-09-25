package com.juhao.classtool.ui.countdown

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.CountdownDataStore
import com.juhao.classtool.navigation.*
import com.juhao.classtool.utils.*

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
                Card(
                    onClick = { onNavigate(CountdownDetailNavScreen(day.id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = day.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${countdownLabel(remaining)} · ${formatDate(day.dateMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (day.progressEnabled && day.startDateMillis > 0L) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction(day.startDateMillis, day.dateMillis) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}