package com.kozvits.clonecard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kozvits.clonecard.MainActivity
import com.kozvits.clonecard.data.repository.DumpRepository
import com.kozvits.clonecard.ui.screens.*
import kotlinx.coroutines.CoroutineScope

@Composable
fun NavGraph(
    navController: NavHostController,
    repository: DumpRepository,
    activity: MainActivity,
    nfcState: MutableState<MainActivity.NfcStatus>,
    pendingAction: MutableState<MainActivity.PendingNfcAction?>,
    scope: CoroutineScope
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                nfcState = nfcState,
                activity = activity,
                pendingAction = pendingAction,
                scope = scope
            )
        }

        composable(Screen.Read.route) {
            ReadScreen(
                onBack = { navController.popBackStack() },
                repository = repository,
                activity = activity,
                nfcState = nfcState,
                pendingAction = pendingAction,
                scope = scope
            )
        }

        composable(Screen.Clone.route) {
            CloneScreen(
                onBack = { navController.popBackStack() },
                repository = repository,
                activity = activity,
                nfcState = nfcState,
                pendingAction = pendingAction,
                scope = scope
            )
        }

        composable(Screen.Compare.route) {
            CompareScreen(
                onBack = { navController.popBackStack() },
                repository = repository,
                activity = activity,
                nfcState = nfcState,
                pendingAction = pendingAction,
                scope = scope
            )
        }

        composable(Screen.Dumps.route) {
            DumpsScreen(
                onBack = { navController.popBackStack() },
                onDumpClick = { id ->
                    navController.navigate(Screen.DumpDetail.createRoute(id))
                },
                repository = repository,
                scope = scope
            )
        }

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
                onDelete = { id ->
                    scope.run { kotlinx.coroutines.GlobalScope.launch { repository.deleteDumpById(id) } }
                    navController.popBackStack()
                },
                repository = repository,
                scope = scope,
                activity = activity,
                pendingAction = pendingAction,
                nfcState = nfcState
            )
        }

        composable(
            route = Screen.Write.route,
            arguments = listOf(navArgument("dumpId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val dumpId = backStackEntry.arguments?.getLong("dumpId") ?: -1L
            CloneScreen(
                onBack = { navController.popBackStack() },
                repository = repository,
                activity = activity,
                nfcState = nfcState,
                pendingAction = pendingAction,
                scope = scope,
                preSelectedDumpId = dumpId
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                repository = repository,
                scope = scope,
                activity = activity,
                pendingAction = pendingAction,
                nfcState = nfcState
            )
        }
    }
}
