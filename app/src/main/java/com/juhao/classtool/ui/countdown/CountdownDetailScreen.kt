package com.juhao.classtool.ui.countdown

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

@Composable
fun CountdownDetailScreen(
    dayId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { CountdownDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var day by remember { mutableStateOf<CountdownDay?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(dayId) {
        day = store.getDay(dayId)
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
                ) { Text(day?.title ?: "倒计日") }
            }

            val current = day
            if (current != null) {
                item {
                    val remaining = daysUntil(current.dateMillis)
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
                            Text(
                                text = formatDate(current.dateMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (current.note.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = current.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = countdownLabel(remaining),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    FilledTonalButton(
                        onClick = onEdit,
                        label = { Text("编辑") },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.edit),
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
                                painter = painterResource(R.drawable.delete),
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
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            visible = showDeleteConfirm,
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除倒计日") },
            text = { Text("确定要删除「${day?.title ?: ""}」吗？此操作无法撤销。") },
            edgeButton = {
                AlertDialogDefaults.EdgeButton(
                    onClick = { showDeleteConfirm = false },
                    content = {
                        Icon(
                            painter = painterResource(R.drawable.close),
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
                            store.delete(dayId)
                            onBack()
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
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
}