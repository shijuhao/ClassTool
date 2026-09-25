package com.juhao.classtool.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.juhao.classtool.ui.about.AboutScreen
import com.juhao.classtool.ui.countdown.*
import com.juhao.classtool.ui.game.*
import com.juhao.classtool.ui.game.gamemenu.GameMenu
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.ui.settings.*
import com.juhao.classtool.ui.todo.*
import com.juhao.classtool.ui.tool.*
import com.juhao.classtool.ui.tool.toolmenu.ToolMenu

fun EntryProviderScope<NavKey>.scheduleEntries() {
    entry<EditScheduleNavScreen> {
        EditScheduleScreen()
    }
    entry<CourseScheduleNavScreen> {
        CourseScheduleScreen()
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
    onNavigate: (AppKey) -> Unit,
) {
    entry<SettingsNavScreen> {
        SettingsScreen(
            onNavigateToBackup = { onNavigate(BackupRestoreNavScreen) }
        )
    }
    entry<BackupRestoreNavScreen> {
        BackupRestoreScreen()
    }
    entry<AboutNavScreen> {
        AboutScreen()
    }
}