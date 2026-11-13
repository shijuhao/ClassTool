package com.juhao.classtool.ui.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Check_circle
import com.composables.icons.materialsymbols.rounded.Chevron_right
import com.composables.icons.materialsymbols.rounded.Checklist
import com.composables.icons.materialsymbols.rounded.Radio_button_unchecked
import com.juhao.classtool.datastore.TodoDataStore
import com.juhao.classtool.datastore.TodoItem
import com.juhao.classtool.utils.LocalScreenShape
import com.juhao.classtool.utils.ScreenShape
import com.juhao.classtool.utils.rememberAdaptiveTransformationSpec
import kotlinx.coroutines.launch

/**
 * 主页第 3 页：待办。
 *
 * - 左侧圆圈：点一下切换「已完成 / 未完成」
 * - 卡片其它区域：进入待办详情
 * - 全部待办都显示，已完成的自动沉到底部
 */
@Composable
fun TodoHomePage(
    onOpenDetail: (Long) -> Unit,
    onOpenAll: () -> Unit,
    onAddTodo: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { TodoDataStore(context) }
    val scope = rememberCoroutineScope()
    val items by store.itemsFlow.collectAsState(initial = emptyList())

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val sorted = remember(items) {
        items.sortedWith(compareBy({ it.done }, { -it.createdMillis }))
    }
    val undoneCount = remember(items) { items.count { !it.done } }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding
                        ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "待办")
                        Text(
                            text = if (items.isEmpty()) {
                                "还没有待办"
                            } else {
                                "$undoneCount 项未完成 · 共 ${items.size} 条"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (sorted.isEmpty()) {
                item {
                    FilledTonalButton(
                        onClick = onAddTodo,
                        label = { Text("添加第一条待办") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
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
            } else {
                items(sorted.size) { index ->
                    val item = sorted[index]
                    TodoHomeRow(
                        item = item,
                        onToggle = { scope.launch { store.toggleDone(item.id) } },
                        onClick = { onOpenDetail(item.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec)
                    )
                }

                item {
                    FilledTonalButton(
                        onClick = onOpenAll,
                        label = { Text("管理待办") },
                        secondaryLabel = { Text("添加 / 编辑 / 删除") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
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
}

@Composable
private fun TodoHomeRow(
    item: TodoItem,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null
) {
    val scheme = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        transformation = transformation,
        modifier = modifier.alpha(if (item.done) 0.55f else 1f),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 左侧圆圈：单独的点击区域，只负责勾选/取消
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.done) {
                        MaterialSymbols.Rounded.Check_circle
                    } else {
                        MaterialSymbols.Rounded.Radio_button_unchecked
                    },
                    contentDescription = if (item.done) "标记为未完成" else "标记为已完成",
                    tint = if (item.done) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }

            Spacer(Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        item.done -> "已完成"
                        item.note.isNotBlank() -> item.note
                        else -> "待完成"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = MaterialSymbols.Rounded.Chevron_right,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(ButtonDefaults.IconSize)
            )
        }
    }
}
