package com.juhao.classtool.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import com.juhao.classtool.ui.about.AboutScreen
import com.juhao.classtool.ui.countdown.*
import com.juhao.classtool.ui.game.*
import com.juhao.classtool.ui.game.gamemenu.GameMenu
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.ui.settings.*
import com.juhao.classtool.ui.todo.*
import com.juhao.classtool.ui.tool.*
import com.juhao.classtool.ui.tool.toolmenu.ToolMenu

fun EntryProviderScope<NavKey>.scheduleEntries(
    onBack: () -> Unit,
    onNavigate: (AppKey) -> Unit,
    activitySelection: MutableState<PendingActivitySelection?>,
    courseSelection: MutableState<PendingCourseSelection?>
) {
    entry<TimeTableNavScreen> {
        TimeTableScreen(
            onNavigate = onNavigate
        )
    }

    entry<AddScheduleEventNavScreen> { key ->
        ScheduleEventEditScreen(
            draft = key.draft,
            onBack = onBack,
            onNavigate = onNavigate,
            activitySelection = activitySelection
        )
    }

    entry<EditScheduleEventNavScreen> { key ->
        ScheduleEventEditScreen(
            draft = key.draft,
            onBack = onBack,
            onNavigate = onNavigate,
            activitySelection = activitySelection
        )
    }

    entry<CourseTableNavScreen> {
        CourseTableScreen(
            onNavigate = onNavigate
        )
    }

    entry<CourseEditNavScreen> { key ->
        CourseEditScreen(
            draft = key.draft,
            onBack = onBack,
            onNavigate = onNavigate,
            courseSelection = courseSelection
        )
    }

    entry<ActivityPresetPickerNavScreen> { key ->
        ActivityPresetPickerScreen(
            draft = key.draft,
            onBack = onBack,
            onSelect = { name, color ->
                activitySelection.value = PendingActivitySelection(key.draft.copy(type = ScheduleEventType.ACTIVITY, name = name, color = color))
                onBack()
            },
            onCustom = { name, color ->
                onNavigate(
                    CustomPresetNavScreen(
                        target = PresetTarget.Activity(key.draft),
                        initialName = name,
                        initialColor = if (name.isBlank()) null else color ?: key.draft.color
                    )
                )
            }
        )
    }

    entry<CoursePresetPickerNavScreen> { key ->
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        CoursePresetPickerScreen(
            draft = key.draft,
            onBack = onBack,
            onSelect = { name, color ->
                if (key.directSave) {
                    scope.launch {
                        val store = ScheduleDataStore(context)
                        if (name.isBlank()) {
                            store.clearCourseForWeekday(key.draft.eventId, key.draft.weekday)
                        } else {
                            store.setCourseForWeekday(
                                key.draft.eventId,
                                key.draft.weekday,
                                name,
                                color
                            )
                        }
                        onBack()
                    }
                } else {
                    courseSelection.value = PendingCourseSelection(
                        key.draft.copy(
                            name = name.takeIf { it.isNotBlank() },
                            color = color
                        )
                    )
                    onBack()
                }
            },
            onCustom = { name, color ->
                onNavigate(
                    CustomPresetNavScreen(
                        target = PresetTarget.Course(key.draft),
                        initialName = name,
                        initialColor = if (name.isBlank()) null else color ?: key.draft.color,
                        directSave = key.directSave
                    )
                )
            }
        )
    }

    entry<CustomPresetNavScreen> { key ->
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        CustomPresetScreen(
            target = key.target,
            initialName = key.initialName,
            initialColor = key.initialColor,
            onBack = onBack,
            onConfirm = { name, color ->
                when (val target = key.target) {
                    is PresetTarget.Activity -> {
                        activitySelection.value = PendingActivitySelection(
                            target.draft.copy(
                                type = ScheduleEventType.ACTIVITY,
                                name = name,
                                color = color
                            )
                        )
                        onBack()
                        onBack()
                    }

                    is PresetTarget.Course -> {
                        if (key.directSave) {
                            scope.launch {
                                ScheduleDataStore(context).setCourseForWeekday(
                                    target.draft.eventId,
                                    target.draft.weekday,
                                    name,
                                    color
                                )
                                onBack()
                                onBack()
                            }
                        } else {
                            courseSelection.value = PendingCourseSelection(
                                target.draft.copy(
                                    name = name,
                                    color = color
                                )
                            )
                            onBack()
                            onBack()
                        }
                    }
                }
            }
        )
    }

    entry<AdjustmentNavScreen> {
        AdjustmentScreen()
    }
}

fun EntryProviderScope<NavKey>.countdownEntries(
    onBack: () -> Unit,
    onNavigate: (AppKey) -> Unit,
) {
    entry<CountdownNavScreen> {
        CountdownScreen(
            onNavigate = { key -> onNavigate(key as AppKey) }
        )
    }
    entry<AddCountdownNavScreen> {
        CountdownEditScreen(
            dayId = null,
            onBack = onBack
        )
    }
    entry<EditCountdownNavScreen> { key ->
        CountdownEditScreen(
            dayId = key.id,
            onBack = onBack
        )
    }
    entry<CountdownDetailNavScreen> { key ->
        CountdownDetailScreen(
            dayId = key.id,
            onEdit = { onNavigate(EditCountdownNavScreen(key.id)) },
            onBack = onBack
        )
    }
}

fun EntryProviderScope<NavKey>.todoEntries(
    onBack: () -> Unit,
    onNavigate: (AppKey) -> Unit,
) {
    entry<TodoNavScreen> {
        TodoScreen(
            onNavigate = { key -> onNavigate(key as AppKey) }
        )
    }
    entry<AddTodoNavScreen> {
        TodoEditScreen(
            itemId = null,
            onBack = onBack
        )
    }
    entry<EditTodoNavScreen> { key ->
        TodoEditScreen(
            itemId = key.id,
            onBack = onBack
        )
    }
    entry<TodoDetailNavScreen> { key ->
        TodoDetailScreen(
            itemId = key.id,
            onEdit = { onNavigate(EditTodoNavScreen(key.id)) },
            onBack = onBack
        )
    }
}

fun EntryProviderScope<NavKey>.toolEntries(
    onNavigate: (AppKey) -> Unit,
) {
    entry<ToolMenuNavScreen> {
        ToolMenu(onChangePage = { onNavigate(it) })
    }
    entry<TimerNavScreen> {
        TimerScreen()
    }
    entry<ToolCountdownNavScreen> {
        ToolCountdownScreen()
    }
    entry<ClockNavScreen> {
        ClockScreen()
    }
}

fun EntryProviderScope<NavKey>.gameEntries(
    onNavigate: (AppKey) -> Unit,
) {
    entry<GameMenuNavScreen> {
        GameMenu(onChangePage = { onNavigate(it) })
    }
    entry<ReactionNavScreen> {
        ReactionScreen()
    }
    entry<CoinNavScreen> {
        CoinScreen()
    }
    entry<DiceNavScreen> {
        DiceScreen()
    }
}

fun EntryProviderScope<NavKey>.settingsEntries(
    onBack: () -> Unit,
    onNavigate: (AppKey) -> Unit
) {
    entry<SettingsNavScreen> {
        SettingsScreen(onChangePage = { onNavigate(it) })
    }
    entry<ThemeNavScreen> {
        ThemeScreen(onNavigate = { onNavigate(it) })
    }
    entry<CustomThemeNavScreen> {
        CustomThemeScreen(
            onBack = onBack
        )
    }
    entry<DeveloperNavScreen> {
        DeveloperScreen()
    }
    entry<BackupRestoreNavScreen> {
        BackupRestoreScreen()
    }
    entry<ScheduleTableNavScreen> {
        ScheduleTableScreen()
    }
    entry<AboutNavScreen> {
        AboutScreen()
    }
}