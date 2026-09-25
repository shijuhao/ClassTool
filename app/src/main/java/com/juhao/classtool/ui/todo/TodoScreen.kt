package com.juhao.classtool.ui.todo

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.TodoDataStore
import com.juhao.classtool.navigation.*
import com.juhao.classtool.utils.*

@Composable
fun TodoScreen(
    onNavigate: (Any) -> Unit
) {
    val context = LocalContext.current
    val store = remember { TodoDataStore(context) }
    val items by store.itemsFlow.collectAsState(initial = emptyList())

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val sorted = remember(items) {
        items.sortedWith(compareBy({ it.done }, { -it.createdMillis }))
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
                ) { Text("TODO") }
            }

            item {
                FilledTonalButton(
                    onClick = { onNavigate(AddTodoNavScreen) },
                    label = { Text("添加待办") },
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
                val item = sorted[index]
                FilledTonalButton(
                    onClick = { onNavigate(TodoDetailNavScreen(item.id)) },
                    label = { Text(item.title) },
                    secondaryLabel = {
                        Text(
                            when {
                                item.done -> "已完成"
                                item.note.isNotBlank() -> item.note
                                else -> "待完成"
                            }
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = if (item.done) {
                                MaterialSymbols.Rounded.Check_circle
                            } else {
                                MaterialSymbols.Rounded.Radio_button_unchecked
                            },
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .alpha(if (item.done) 0.6f else 1f),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}