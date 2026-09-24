package com.juhao.classtool.ui.countdown

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.R
import com.juhao.classtool.datastore.CountdownDataStore
import com.juhao.classtool.datastore.CountdownDay
import com.juhao.classtool.ui.schedule.LocalScreenShape
import com.juhao.classtool.ui.schedule.ScreenShape
import com.juhao.classtool.ui.schedule.rememberAdaptiveTransformationSpec
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CountdownEditScreen(
    dayId: Long?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { CountdownDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var loaded by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(dayId) {
        if (dayId != null) {
            store.getDay(dayId)?.let {
                title = it.title
                note = it.note
                date = Instant.ofEpochMilli(it.dateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        }
        loaded = true
    }

    if (showDatePicker) {
        DatePicker(
            initialDate = date,
            onDatePicked = {
                date = it
                showDatePicker = false
            },
            datePickerType = DatePickerType.YearMonthDay,
            minValidDate = LocalDate.now()
        )
        return
    }

    val formatter = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(LocalConfiguration.current.locales[0])

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
                ) { Text(if (dayId == null) "添加倒计日" else "编辑倒计日") }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("名称")
                        Spacer(Modifier.height(4.dp))
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(
                                MaterialTheme.colorScheme.primary
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("备注（可选）")
                        Spacer(Modifier.height(4.dp))
                        BasicTextField(
                            value = note,
                            onValueChange = { note = it },
                            cursorBrush = SolidColor(
                                MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = { showDatePicker = true },
                    label = { Text("目标日期") },
                    secondaryLabel = { Text(date.format(formatter)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.date_range),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                Button(
                    onClick = {
                        if (title.isBlank()) return@Button
                        scope.launch {
                            store.upsert(
                                CountdownDay(
                                    id = dayId ?: System.currentTimeMillis(),
                                    title = title.trim(),
                                    dateMillis = date
                                        .atStartOfDay(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli(),
                                    note = note.trim()
                                )
                            )
                            onBack()
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.save),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    label = { Text("保存") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}