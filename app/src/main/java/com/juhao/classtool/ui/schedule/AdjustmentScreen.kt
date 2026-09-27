package com.juhao.classtool.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.activity.compose.BackHandler
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
import com.juhao.classtool.datastore.ScheduleAdjustment
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.Weekday
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.utils.*
import kotlinx.coroutines.launch

private enum class AdjustmentDialogStage { EDIT, START_DATE, END_DATE }

@Composable
fun AdjustmentScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ScheduleDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var refreshKey by remember { mutableIntStateOf(0) }
    val adjustments by produceState(
        initialValue = emptyList<ScheduleAdjustment>(),
        refreshKey
    ) {
        value = store.getSchedule().adjustments
    }

    var showDialog by remember { mutableStateOf(false) }
    var editingAdjustment by remember { mutableStateOf<ScheduleAdjustment?>(null) }
    var deleteAdjustment by remember { mutableStateOf<ScheduleAdjustment?>(null) }

    if (showDialog) {
        AdjustmentEditDialog(
            existing = editingAdjustment,
            onDismiss = { showDialog = false },
            onConfirm = { adjustment ->
                scope.launch {
                    val result = if (editingAdjustment == null) {
                        store.addAdjustment(adjustment)
                    } else {
                        store.updateAdjustment(adjustment)
                    }
                    if (result.valid) {
                        refreshKey++
                        showDialog = false
                        RoundToast.show(context, "操作成功")
                    } else {
                        RoundToast.show(context, result.reason ?: "操作失败")
                    }
                }
            }
        )
        return
    }

    deleteAdjustment?.let { target ->
        AlertDialog(
            visible = true,
            onDismissRequest = { deleteAdjustment = null },
            title = { Text("删除调休") },
            text = { Text("确定要删除这条调休吗？此操作无法撤销。") },
            edgeButton = {
                AlertDialogDefaults.EdgeButton(
                    onClick = { deleteAdjustment = null },
                    content = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Close,
                            contentDescription = null
                        )
                    }
                )
            }
        ) {
            item {
                Button(
                    onClick = {
                        deleteAdjustment = null
                        scope.launch {
                            store.removeAdjustment(target.id)
                            refreshKey++
                            RoundToast.show(context, "已删除")
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    label = { Text("删除") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
                ) { Text("调休") }
            }

            item {
                FilledTonalButton(
                    onClick = {
                        editingAdjustment = null
                        showDialog = true
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .then(
                            if (adjustments.isEmpty()) {
                                Modifier.minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                            } else Modifier
                        ),
                    label = { Text("新增调休") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Add,
                            contentDescription = "新增调休",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                )
            }

            if (adjustments.isEmpty()) {
                item {
                    Text(
                        text = "暂无调休设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            )
                    )
                }
            }

            items(
                count = adjustments.size,
                key = { adjustments[it].id }
            ) { index ->
                val adjustment = adjustments[index]
                val isLast = index == adjustments.lastIndex

                FilledTonalButton(
                    onClick = {
                        editingAdjustment = adjustment
                        showDialog = true
                    },
                    onLongClick = { deleteAdjustment = adjustment },
                    transformation = SurfaceTransformation(transformationSpec),
                    label = {
                        Text(
                            "${weekdayLabel(adjustment.fromWeekday)} 的课调到 ${weekdayLabel(adjustment.toWeekday)}"
                        )
                    },
                    secondaryLabel = {
                        Text("${formatDateWithWeekday(adjustment.startDate)} 至 ${formatDateWithWeekday(adjustment.endDate)}")
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Swap_horiz,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .then(
                            if (isLast) {
                                Modifier.minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                            } else Modifier
                        )
                )
            }
        }
    }
}

@Composable
private fun AdjustmentEditDialog(
    existing: ScheduleAdjustment?,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleAdjustment) -> Unit
) {
    var startDate by remember {
        mutableStateOf(existing?.startDate ?: todayDateString())
    }
    var endDate by remember {
        mutableStateOf(existing?.endDate ?: todayDateString())
    }
    var fromWeekday by remember {
        mutableStateOf(existing?.fromWeekday ?: Weekday.MONDAY)
    }
    var toWeekday by remember {
        mutableStateOf(existing?.toWeekday ?: Weekday.TUESDAY)
    }
    var stage by remember { mutableStateOf(AdjustmentDialogStage.EDIT) }

    if (stage == AdjustmentDialogStage.START_DATE) {
        WearDatePicker(
            initial = startDate,
            onConfirm = {
                startDate = it
                if (endDate < startDate) endDate = startDate
                stage = AdjustmentDialogStage.EDIT
            },
            onCancel = { stage = AdjustmentDialogStage.EDIT }
        )
        return
    }

    if (stage == AdjustmentDialogStage.END_DATE) {
        WearDatePicker(
            initial = endDate,
            onConfirm = {
                endDate = it
                stage = AdjustmentDialogStage.EDIT
            },
            onCancel = { stage = AdjustmentDialogStage.EDIT }
        )
        return
    }

    val invalidRange = endDate < startDate
    val sameWeekday = fromWeekday == toWeekday

    AlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "新增调休" else "编辑调休") },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    if (invalidRange || sameWeekday) return@ConfirmButton
                    val adjustment = (existing ?: ScheduleAdjustment(
                        startDate = startDate,
                        endDate = endDate,
                        fromWeekday = fromWeekday,
                        toWeekday = toWeekday
                    )).copy(
                        startDate = startDate,
                        endDate = endDate,
                        fromWeekday = fromWeekday,
                        toWeekday = toWeekday
                    )
                    onConfirm(adjustment)
                }
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(onClick = onDismiss)
        }
    ) {
        item {
            FilledTonalButton(
                onClick = { stage = AdjustmentDialogStage.START_DATE },
                label = { Text("开始日期") },
                secondaryLabel = { Text(formatDateWithWeekday(startDate)) },
                icon = {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Event,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            FilledTonalButton(
                onClick = { stage = AdjustmentDialogStage.END_DATE },
                label = { Text("结束日期") },
                secondaryLabel = { Text(formatDateWithWeekday(endDate)) },
                icon = {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Date_range,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Column(Modifier.fillMaxWidth()) {
                Text("原星期", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Weekday.entries.forEach { day ->
                        AdjustmentChip(
                            text = weekdayShortLabel(day),
                            selected = fromWeekday == day,
                            onClick = { fromWeekday = day }
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth()) {
                Text("调到星期", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Weekday.entries.forEach { day ->
                        AdjustmentChip(
                            text = weekdayShortLabel(day),
                            selected = toWeekday == day,
                            onClick = { toWeekday = day }
                        )
                    }
                }
            }
        }

        if (invalidRange || sameWeekday) {
            item {
                Text(
                    text = when {
                        invalidRange -> "结束日期需不早于开始日期"
                        else -> "原星期与目标星期不能相同"
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun AdjustmentChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}