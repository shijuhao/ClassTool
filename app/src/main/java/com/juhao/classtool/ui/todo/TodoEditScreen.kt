package com.juhao.classtool.ui.todo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
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
fun TodoEditScreen(
    itemId: Long?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { TodoDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(itemId) {
        if (itemId != null) {
            store.getItem(itemId)?.let {
                title = it.title
                note = it.note
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
                ) { Text(if (itemId == null) "添加待办" else "编辑待办") }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text("标题")
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
                Button(
                    onClick = {
                        if (title.isBlank()) return@Button
                        scope.launch {
                            store.upsert(
                                TodoItem(
                                    id = itemId ?: System.currentTimeMillis(),
                                    title = title.trim(),
                                    note = note.trim()
                                )
                            )
                            onBack()
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    label = { Text("保存") },
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