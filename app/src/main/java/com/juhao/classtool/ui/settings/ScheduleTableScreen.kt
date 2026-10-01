package com.juhao.classtool.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.lazy.TransformationSpec
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleTable
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.utils.*
import kotlinx.coroutines.launch

@Composable
fun ScheduleTableScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ScheduleDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var refreshKey by remember { mutableIntStateOf(0) }
    val tables by produceState(initialValue = emptyList<ScheduleTable>(), refreshKey) {
        value = store.getTableCollection().tables
    }
    val activeTableId by produceState(initialValue = "", refreshKey) {
        value = store.getActiveTable()?.id ?: ""
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<ScheduleTable?>(null) }
    var showDeleteDialog by remember { mutableStateOf<ScheduleTable?>(null) }
    var actionTable by remember { mutableStateOf<ScheduleTable?>(null) }

    if (showAddDialog) {
        ScheduleTableNameDialog(
            title = "新建日程表",
            initialName = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                scope.launch {
                    val result = store.addTable(name)
                    if (result.valid) {
                        refreshKey++
                        showAddDialog = false
                        RoundToast.show(context, "已创建")
                    } else {
                        RoundToast.show(context, result.reason ?: "创建失败")
                    }
                }
            }
        )
        return
    }

    showRenameDialog?.let { table ->
        ScheduleTableNameDialog(
            title = "重命名日程表",
            initialName = table.name,
            onDismiss = { showRenameDialog = null },
            onConfirm = { name ->
                scope.launch {
                    val result = store.renameTable(table.id, name)
                    if (result.valid) {
                        refreshKey++
                        showRenameDialog = null
                        RoundToast.show(context, "已重命名")
                    } else {
                        RoundToast.show(context, result.reason ?: "重命名失败")
                    }
                }
            }
        )
        return
    }

    actionTable?.let { target ->
        AlertDialog(
            visible = true,
            onDismissRequest = { actionTable = null },
            title = { Text(target.name) },
            text = {
                Text(
                    text = "${target.schedule.events.size} 个事件 · ${target.schedule.adjustments.size} 条调休",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            edgeButton = {
                AlertDialogDefaults.EdgeButton(
                    onClick = { actionTable = null },
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
                FilledTonalButton(
                    onClick = {
                        val t = target
                        actionTable = null
                        showRenameDialog = t
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    label = { Text("重命名") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = {
                        val t = target
                        actionTable = null
                        showDeleteDialog = t
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

    showDeleteDialog?.let { table ->
        AlertDialog(
            visible = true,
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除日程表") },
            text = { Text("确定要删除「${table.name}」吗？其中的所有事件都将被删除。") },
            edgeButton = {
                AlertDialogDefaults.EdgeButton(
                    onClick = { showDeleteDialog = null },
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
                        showDeleteDialog = null
                        scope.launch {
                            val result = store.removeTable(table.id)
                            if (result.valid) {
                                refreshKey++
                                RoundToast.show(context, "已删除")
                            } else {
                                RoundToast.show(context, result.reason ?: "删除失败")
                            }
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
                ) { Text("日程表管理") }
            }

            item {
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .then(
                            if (tables.isEmpty()) {
                                Modifier.minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                            } else Modifier
                        ),
                    label = { Text("新建日程表") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                )
            }

            items(
                count = tables.size,
                key = { tables[it].id }
            ) { index ->
                val table = tables[index]
                val isActive = table.id == activeTableId
                val isLast = index == tables.lastIndex

                ScheduleTableItem(
                    table = table,
                    isActive = isActive,
                    isLast = isLast,
                    transformationSpec = transformationSpec,
                    onSelect = {
                        scope.launch {
                            store.setActiveTable(table.id)
                            refreshKey++
                            RoundToast.show(context, "已切换到「${table.name}」")
                        }
                    },
                    onLongClick = { actionTable = table }
                )
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.ScheduleTableItem(
    table: ScheduleTable,
    isActive: Boolean,
    isLast: Boolean,
    transformationSpec: TransformationSpec,
    onSelect: () -> Unit,
    onLongClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onSelect,
        onLongClick = onLongClick,
        transformation = SurfaceTransformation(transformationSpec),
        colors = if (isActive) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        label = { Text(table.name) },
        secondaryLabel = {
            val eventCount = table.schedule.events.size
            val adjustCount = table.schedule.adjustments.size
            Text("$eventCount 个事件 · $adjustCount 条调休")
        },
        icon = {
            if (isActive) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Check_circle,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Calendar_month,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }
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

@Composable
private fun ScheduleTableNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                    }
                }
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(onClick = onDismiss)
        }
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "名称")
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(
                        MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (name.isEmpty()) {
                                Text(
                                    text = "输入日程表名称",
                                    color = MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}