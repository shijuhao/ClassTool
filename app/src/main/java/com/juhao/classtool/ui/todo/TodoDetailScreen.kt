package com.juhao.classtool.ui.todo

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.TodoDataStore
import com.juhao.classtool.datastore.TodoItem
import com.juhao.classtool.utils.*
import kotlinx.coroutines.launch

@Composable
fun TodoDetailScreen(
    itemId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { TodoDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var item by remember { mutableStateOf<TodoItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        item = store.getItem(itemId)
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
                ) { Text(item?.title ?: "待办") }
            }

            val current = item
            if (current != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (current.note.isNotBlank()) {
                                Text(
                                    text = current.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            Text(
                                text = if (current.done) "已完成" else "待完成",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (current.done) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                store.toggleDone(itemId)
                                item = store.getItem(itemId)
                            }
                        },
                        label = { Text(if (current.done) "标记未完成" else "标记完成") },
                        icon = {
                            Icon(
                                imageVector = if (current.done) {
                                    MaterialSymbols.Rounded.Radio_button_unchecked
                                } else {
                                    MaterialSymbols.Rounded.Check_circle
                                },
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
                    FilledTonalButton(
                        onClick = onEdit,
                        label = { Text("编辑") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Edit,
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
                    FilledTonalButton(
                        onClick = { showDeleteConfirm = true },
                        label = { Text("删除") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            ),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }
            }
        }
    }

    AlertDialog(
        visible = showDeleteConfirm,
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text("删除待办") },
        text = { Text("确定要删除「${item?.title ?: ""}」吗？此操作无法撤销。") },
        edgeButton = {
            AlertDialogDefaults.EdgeButton(
                onClick = { showDeleteConfirm = false },
                content = {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Close,
                        contentDescription = null
                    )
                },
            )
        },
    ) {
        item {
            Button(
                onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        store.delete(itemId)
                        onBack()
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
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}