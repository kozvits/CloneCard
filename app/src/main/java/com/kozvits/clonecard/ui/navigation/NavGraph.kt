package com.kozvits.clonecard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kozvits.clonecard.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Главная
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        // Чтение
        composable(Screen.Read.route) {
            ReadScreen(
                onBack = { navController.popBackStack() },
                onCardRead = { _ ->
                    navController.popBackStack()
                }
            )
        }

        // Клонирование
        composable(Screen.Clone.route) {
            CloneScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Сравнение
        composable(Screen.Compare.route) {
            CompareScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Дампы
        composable(Screen.Dumps.route) {
            DumpsScreen(
                onBack = { navController.popBackStack() },
                onDumpClick = { id ->
                    navController.navigate(Screen.DumpDetail.createRoute(id))
                },
                onDumpDelete = { _ -> }
            )
        }

        // Детали дампа
        composable(
            route = Screen.DumpDetail.route,
            arguments = listOf(navArgument("dumpId") { type = NavType.LongType })
        ) { backStackEntry ->
            val dumpId = backStackEntry.arguments?.getLong("dumpId") ?: 0L
            DumpDetailScreen(
                dumpId = dumpId,
                onBack = { navController.popBackStack() },
                onWrite = { id ->
                    navController.navigate(Screen.Write.createRoute(id))
                },
                onCompare = { _ -> },
                onDelete = { _ -> navController.popBackStack() }
            )
        }

        // Запись
        composable(
            route = Screen.Write.route,
            arguments = listOf(navArgument("dumpId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val dumpId = backStackEntry.arguments?.getLong("dumpId") ?: -1L
            CloneScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Настройки
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
