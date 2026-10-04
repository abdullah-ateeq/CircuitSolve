package com.example.circuitsolve.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.circuitsolve.data.AppSettings
import com.example.circuitsolve.ui.about.AboutScreen
import com.example.circuitsolve.ui.calculator.CalculatorScreen
import com.example.circuitsolve.ui.components.CircuitIcons
import com.example.circuitsolve.ui.history.HistoryScreen
import com.example.circuitsolve.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

object Routes {
    const val CALCULATOR = "calculator"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination(Routes.CALCULATOR, "Calculator", CircuitIcons.Resistor),
    Destination(Routes.HISTORY, "History", CircuitIcons.History),
    Destination(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
    Destination(Routes.ABOUT, "About", Icons.Outlined.Info),
)

@Composable
fun CircuitSolveApp(viewModel: CircuitSolveViewModel, settings: AppSettings) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val history by viewModel.history.collectAsStateWithLifecycle()

    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomBar(navController) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CALCULATOR,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Routes.CALCULATOR) {
                CalculatorScreen(viewModel = viewModel, settings = settings, onMessage = showMessage)
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    history = history,
                    decimals = settings.decimalPlaces,
                    onOpen = { record ->
                        viewModel.restore(record)
                        navController.navigateToTab(Routes.CALCULATOR)
                    },
                    onDelete = { viewModel.deleteHistory(it.id) },
                    onClearAll = viewModel::clearHistory,
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    onUpdate = viewModel::updateSettings,
                    onResetSettings = {
                        viewModel.resetSettings()
                        showMessage("Settings reset")
                    },
                    onClearHistory = {
                        viewModel.clearHistory()
                        showMessage("History cleared")
                    },
                )
            }
            composable(Routes.ABOUT) { AboutScreen() }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { navController.navigateToTab(destination.route) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
