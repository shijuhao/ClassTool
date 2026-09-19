package com.juhao.classtool.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.R
import com.juhao.classtool.datastore.SettingsDataStore
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    val classDuration by store.classDurationFlow.collectAsState(initial = 40)
    val breakDuration by store.breakDurationFlow.collectAsState(initial = 10)

    ScreenScaffold(
        scrollState = listState
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ListHeaderDefaults.minimumTopListContentPadding
                            ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(text = "设置") }
            }

            item {
                DurationSettingCard(
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    title = "上课默认时长（分钟）",
                    transformation = SurfaceTransformation(transformationSpec),
                    minutes = classDuration,
                    range = 5..120,
                    step = 5,
                    onChange = { v ->
                        scope.launch { store.setClassDuration(v) }
                    }
                )
            }
            
            item {
                DurationSettingCard(
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    title = "课间默认时长（分钟）",
                    transformation = SurfaceTransformation(transformationSpec),
                    minutes = breakDuration,
                    range = 1..60,
                    step = 5,
                    onChange = { v ->
                        scope.launch { store.setBreakDuration(v) }
                    }
                )
            }
        }
    }
}

@Composable
private fun DurationSettingCard(
    modifier: Modifier = Modifier,
    title: String,
    minutes: Int,
    range: IntRange,
    step: Int,
    onChange: (Int) -> Unit,
    transformation: SurfaceTransformation? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = title)
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        val next = (minutes - step).coerceAtLeast(range.first)
                        if (next != minutes) onChange(next)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.remove),
                        contentDescription = "减少"
                    )
                }
                Text(
                    text = "$minutes",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick = {
                        val next = (minutes + step).coerceAtMost(range.last)
                        if (next != minutes) onChange(next)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = "增加"
                    )
                }
            }
        }
    }
}