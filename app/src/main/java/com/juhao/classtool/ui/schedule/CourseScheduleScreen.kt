package com.juhao.classtool.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.Weekday
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.utils.*
import kotlinx.coroutines.launch

private val presetCourses = listOf(
    "语文" to "#FF7043",
    "数学" to "#66BB6A",
    "英语" to "#42A5F5",
    "物理" to "#AB47BC",
    "化学" to "#FFCA28",
    "生物" to "#78909C",
    "历史" to "#A1887F",
    "地理" to "#26A69A",
    "政治" to "#EC407A",
    "体育" to "#FFA726",
    "音乐" to "#F06292",
    "美术" to "#FF80AB",
    "信息" to "#26C6DA",
    "心理" to "#9CCC65",
    "通用技术" to "#D4E157",
    "自习" to "#00897B"
)

@Composable
fun CourseScheduleScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ScheduleDataStore(context) }

    val weekdays = Weekday.entries.toList()
    val today = todayWeekday()
    val pagerState = rememberPagerState(
        initialPage = weekdays.indexOf(today).coerceAtLeast(0),
        pageCount = { weekdays.size }
    )

    var refreshKey by remember { mutableIntStateOf(0) }
    val schedule by produceState(initialValue = emptyList(), refreshKey) {
        value = store.getSchedule().events
    }

    var editingEvent by remember { mutableStateOf<ScheduleEvent?>(null) }

    editingEvent?.let { target ->
        CourseEditDialog(
            event = target,
            onDismiss = { editingEvent = null },
            onConfirm = { name, color ->
                scope.launch {
                    if (name == null) store.clearCourse(target.id)
                    else store.setCourse(target.id, name, color)
                    refreshKey++
                }
                editingEvent = null
                RoundToast.show(context, "设置成功")
            }
        )
    }

    HorizontalPagerScaffold(pagerState = pagerState, modifier = modifier) {
        HorizontalPager(state = pagerState) { page ->
            val weekday = weekdays[page]
            val listState = rememberTransformingLazyColumnState()
            val square = LocalScreenShape.current == ScreenShape.SQUARE
            val transformationSpec = rememberAdaptiveTransformationSpec(square)

            val dayClasses = schedule
                .filter {
                    it.enabled &&
                        it.type == ScheduleEventType.CLASS &&
                        weekday in it.weekdays
                }
                .sortedBy { it.startTime }

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
                        ) { Text("${weekdayLabel(weekday)} 课程") }
                    }

                    if (dayClasses.isEmpty()) {
                        item {
                            Text(
                                text = "这一天没有上课事件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec)
                                    .graphicsLayer {
                                        with(transformationSpec) {
                                            applyContainerTransformation(scrollProgress)
                                        }
                                    }
                            )
                        }
                    }

                    items(count = dayClasses.size, key = { dayClasses[it].id }) { index ->
                        val event = dayClasses[index]
                        val isLast = index == dayClasses.lastIndex
                        CourseEditButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .then(
                                    if (isLast) {
                                        Modifier.minimumVerticalContentPadding(
                                            ButtonDefaults.minimumVerticalListContentPadding
                                        )
                                    } else Modifier
                                ),
                            transformation = SurfaceTransformation(transformationSpec),
                            event = event,
                            onClick = { editingEvent = event }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseEditButton(
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    event: ScheduleEvent,
    onClick: () -> Unit
) {
    val color = event.courseColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant

    FilledTonalButton(
        onClick = onClick,
        transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        label = { Text(event.courseName ?: "点击设置课程") },
        secondaryLabel = { Text("${event.startTime} - ${event.endTime}") },
        icon = {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun CourseEditDialog(
    event: ScheduleEvent,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?) -> Unit
) {
    var selectedName by remember { mutableStateOf(event.courseName) }
    var selectedColor by remember { mutableStateOf(event.courseColor) }
    var isCustom by remember {
        mutableStateOf(
            event.courseName != null && presetCourses.none { it.first == event.courseName }
        )
    }
    var showCustomDialog by remember { mutableStateOf(false) }

    if (showCustomDialog) {
        CustomPresetDialog(
            title = "自定义课程",
            placeholder = "输入名称",
            initialName = if (isCustom) event.courseName ?: "" else "",
            initialColor = selectedColor,
            onDismiss = { showCustomDialog = false },
            onConfirm = { name, color ->
                selectedName = name
                selectedColor = color
                isCustom = true
                showCustomDialog = false
            }
        )
        return
    }

    AlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = { Text("设置课程") },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    if (selectedName == null) onConfirm(null, null)
                    else onConfirm(selectedName, selectedColor)
                }
            )
        },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = onDismiss) }
    ) {
        item {
            Text(
                text = "${event.startTime} - ${event.endTime}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presetCourses.forEach { (presetName, presetColor) ->
                    val selected = !isCustom && selectedName == presetName
                    PresetChip(
                        text = presetName,
                        colorHex = presetColor,
                        selected = selected,
                        onClick = {
                            if (selected) {
                                selectedName = null
                                selectedColor = null
                                isCustom = false
                            } else {
                                selectedName = presetName
                                selectedColor = presetColor
                                isCustom = false
                            }
                        }
                    )
                }

                val customSelected = isCustom && selectedName != null
                PresetChip(
                    text = "自定义",
                    colorHex = null,
                    selected = customSelected,
                    leadingIcon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    onClick = { showCustomDialog = true }
                )
            }
        }

        if (isCustom && selectedName != null) {
            item {
                Text(
                    text = "当前：${selectedName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (event.courseName != null) {
            item {
                FilledTonalButton(
                    onClick = {
                        selectedName = null
                        selectedColor = null
                        isCustom = false
                    },
                    label = { Text("清除课程") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    text: String,
    colorHex: String?,
    selected: Boolean,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (leadingIcon != null) {
            CompositionLocalProvider(LocalContentColor provides tint) { leadingIcon() }
        } else if (colorHex != null) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(parseColor(colorHex))
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = tint,
            style = MaterialTheme.typography.labelMedium
        )
    }
}