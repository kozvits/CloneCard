package com.kozvits.clonecard.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val shortDesc: String
) {
    data object Home : Screen(
        route = "home",
        title = "CloneCard",
        icon = Icons.Filled.Home,
        shortDesc = "Главная"
    )

    data object Read : Screen(
        route = "read",
        title = "Чтение карты",
        icon = Icons.Filled.Nfc,
        shortDesc = "Считать карту"
    )

    data object Write : Screen(
        route = "write/{dumpId}",
        title = "Запись карты",
        icon = Icons.Filled.Edit,
        shortDesc = "Записать дамп"
    ) {
        fun createRoute(dumpId: Long = -1L) = "write/$dumpId"
    }

    data object Clone : Screen(
        route = "clone",
        title = "Клонирование",
        icon = Icons.Filled.ContentCopy,
        shortDesc = "Клонировать карту"
    )

    data object Compare : Screen(
        route = "compare",
        title = "Сравнение",
        icon = Icons.Filled.CompareArrows,
        shortDesc = "Сравнить карты"
    )

    data object Dumps : Screen(
        route = "dumps",
        title = "Сохранённые дампы",
        icon = Icons.Filled.Folder,
        shortDesc = "Управление дампами"
    )

    data object DumpDetail : Screen(
        route = "dump/{dumpId}",
        title = "Просмотр дампа",
        icon = Icons.Filled.Description,
        shortDesc = "Детали дампа"
    ) {
        fun createRoute(dumpId: Long) = "dump/$dumpId"
    }

    data object Settings : Screen(
        route = "settings",
        title = "Настройки",
        icon = Icons.Filled.Settings,
        shortDesc = "Настройки"
    )

    companion object {
        val mainMenuItems = listOf(Home, Read, Clone, Compare, Dumps)
        val bottomNavItems = listOf(Home, Read, Dumps)
    }
}
