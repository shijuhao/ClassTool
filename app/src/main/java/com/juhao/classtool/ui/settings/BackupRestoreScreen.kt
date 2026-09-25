package com.juhao.classtool.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.Schedule
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleValidator
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.ui.schedule.LocalScreenShape
import com.juhao.classtool.ui.schedule.ScreenShape
import com.juhao.classtool.ui.schedule.rememberAdaptiveTransformationSpec
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val schedule: String = ""
)

private val backupJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Composable
fun BackupRestoreScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheduleStore = remember { ScheduleDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var backupText by remember { mutableStateOf("") }
    var restoreText by remember { mutableStateOf("") }

    fun toast(message: String) {
        RoundToast.show(context, message, RoundToast.LENGTH_SHORT)
    }

    suspend fun generateBackup() {
        val payload = BackupPayload(
            schedule = backupJson.encodeToString(
                Schedule.serializer(),
                scheduleStore.getSchedule()
            )
        )
        backupText = backupJson.encodeToString(BackupPayload.serializer(), payload)
    }

    LaunchedEffect(Unit) {
        generateBackup()
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("ClassTool Backup", text))
        toast("已复制到剪贴板")
    }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            restoreText = clip.getItemAt(0).text?.toString() ?: ""
            toast("已从剪贴板粘贴")
        } else {
            toast("剪贴板为空")
        }
    }

    fun doRestore() {
        val text = restoreText.trim()
        if (text.isEmpty()) {
            toast("请输入备份内容")
            return
        }
        val payload = runCatching {
            backupJson.decodeFromString(BackupPayload.serializer(), text)
        }.getOrElse {
            toast("解析失败")
            return
        }

        val scheduleToRestore: Schedule? = if (payload.schedule.isNotBlank()) {
            runCatching {
                backupJson.decodeFromString(Schedule.serializer(), payload.schedule)
            }.getOrElse {
                toast("日程解析失败")
                return
            }
        } else {
            null
        }

        if (scheduleToRestore != null) {
            val validation = ScheduleValidator.validateSchedule(scheduleToRestore)
            if (!validation.valid) {
                toast("日程校验失败：${validation.reason}")
                return
            }
        }

        scope.launch {
            if (scheduleToRestore != null) {
                val result = scheduleStore.setSchedule(scheduleToRestore)
                if (!result.valid) {
                    toast("日程写入失败：${result.reason}")
                    return@launch
                }
            }

            toast("还原成功")
            generateBackup()
        }
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding
                        ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(text = "备份与还原") }
            }

            item {
                FilledTonalButton(
                    onClick = { copyToClipboard(backupText) },
                    label = { Text("复制备份") },
                    secondaryLabel = { Text("复制 JSON 到剪贴板") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Content_copy,
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

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "备份预览",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = backupText.ifBlank { "（无数据）" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "还原内容",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 120.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (restoreText.isEmpty()) {
                                Text(
                                    text = "在此粘贴 JSON...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            BasicTextField(
                                value = restoreText,
                                onValueChange = { restoreText = it },
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = { pasteFromClipboard() },
                    label = { Text("粘贴备份") },
                    secondaryLabel = { Text("从剪贴板读取 JSON") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Content_paste,
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

            item {
                FilledTonalButton(
                    onClick = { doRestore() },
                    label = { Text("执行还原") },
                    secondaryLabel = { Text("覆盖当前日程") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
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
        }
    }
}