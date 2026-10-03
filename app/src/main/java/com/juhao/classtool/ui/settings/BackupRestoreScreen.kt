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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.Schedule
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleTable
import com.juhao.classtool.datastore.ScheduleTableCollection
import com.juhao.classtool.datastore.scheduleDataStore
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.utils.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupPayload(
    val version: Int = 2,
    val schedule: String = "",
    val tables: String = ""
)

private val backupJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

private val scheduleKey = stringPreferencesKey("schedule")
private val tablesKey = stringPreferencesKey("schedule_tables")

private fun legacyToCollection(scheduleRaw: String): ScheduleTableCollection? =
    runCatching { backupJson.decodeFromString(Schedule.serializer(), scheduleRaw) }
        .getOrNull()
        ?.let { legacy ->
            val t = ScheduleTable(name = "默认日程表", schedule = legacy)
            ScheduleTableCollection(tables = listOf(t), activeTableId = t.id)
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
    var tableCount by remember { mutableIntStateOf(0) }
    var legacyDetected by remember { mutableStateOf(false) }

    fun toast(message: String) {
        RoundToast.show(context, message, RoundToast.LENGTH_SHORT)
    }

    suspend fun readRawPreferences(): Pair<String?, String?> {
        val prefs = context.scheduleDataStore.data.first()
        return prefs[tablesKey] to prefs[scheduleKey]
    }

    suspend fun refreshStatus() {
        val (tablesRaw, scheduleRaw) = readRawPreferences()
        legacyDetected = tablesRaw == null && scheduleRaw != null
        tableCount = when {
            tablesRaw != null -> runCatching {
                backupJson.decodeFromString(ScheduleTableCollection.serializer(), tablesRaw)
            }.getOrNull()?.tables?.size ?: 0
            scheduleRaw != null -> legacyToCollection(scheduleRaw)?.tables?.size ?: 0
            else -> 0
        }
    }

    suspend fun generateBackup() {
        val (tablesRaw, scheduleRaw) = readRawPreferences()
        val tablesJson = tablesRaw
            ?: scheduleRaw?.let {
                legacyToCollection(it)?.let { c ->
                    backupJson.encodeToString(ScheduleTableCollection.serializer(), c)
                }
            }
            ?: ""
        val scheduleJson = scheduleRaw
            ?: backupJson.encodeToString(Schedule.serializer(), scheduleStore.getSchedule())
        backupText = backupJson.encodeToString(
            BackupPayload.serializer(),
            BackupPayload(schedule = scheduleJson, tables = tablesJson)
        )
        refreshStatus()
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

    fun doMigrate() {
        scope.launch {
            val (tablesRaw, scheduleRaw) = readRawPreferences()
            if (scheduleRaw == null) {
                toast("未检测到旧版数据")
                refreshStatus()
                return@launch
            }
            if (tablesRaw != null) {
                toast("当前已是新结构")
                refreshStatus()
                return@launch
            }
            val legacy = legacyToCollection(scheduleRaw)
            if (legacy == null) {
                toast("旧版数据解析失败")
                return@launch
            }
            val result = scheduleStore.restoreTableCollection(legacy)
            if (!result.valid) {
                toast("迁移失败：${result.reason}")
                return@launch
            }
            context.scheduleDataStore.edit { it.remove(scheduleKey) }
            toast("迁移完成")
            generateBackup()
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

        val tablesToRestore = payload.tables.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching {
                backupJson.decodeFromString(ScheduleTableCollection.serializer(), raw)
            }.getOrElse {
                toast("日程表集合解析失败")
                return
            }
        }

        val scheduleToRestore = payload.schedule.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching {
                backupJson.decodeFromString(Schedule.serializer(), raw)
            }.getOrElse {
                toast("日程解析失败")
                return
            }
        }

        val toWrite = when {
            tablesToRestore?.tables?.isNotEmpty() == true -> tablesToRestore
            scheduleToRestore != null -> {
                val t = ScheduleTable(name = "默认日程表", schedule = scheduleToRestore)
                ScheduleTableCollection(tables = listOf(t), activeTableId = t.id)
            }
            else -> {
                toast("没有可还原的内容")
                return
            }
        }

        scope.launch {
            val result = scheduleStore.restoreTableCollection(toWrite)
            if (!result.valid) {
                toast("还原失败：${result.reason}")
                return@launch
            }
            context.scheduleDataStore.edit { it.remove(scheduleKey) }
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

            if (legacyDetected) {
                item {
                    TitleCard(
                        onClick = { },
                        title = { Text("请尽快迁移旧版数据") },
                        subtitle = { Text("未来会移除迁移功能") },
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                    ) {
                        Text("请在下方点击「迁移旧版数据」完成升级")
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
                            text = "当前数据",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "共 $tableCount 个日程表",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (legacyDetected) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "检测到旧版数据，点击下方按钮一键迁移",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = { doMigrate() },
                    label = { Text("迁移旧版数据") },
                    secondaryLabel = { Text("将旧版单日程表搬入新结构") },
                    enabled = legacyDetected,
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Upgrade,
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
                    secondaryLabel = { Text("覆盖当前所有日程表") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
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