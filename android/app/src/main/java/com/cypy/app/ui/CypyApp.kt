package com.cypy.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cypy.app.ui.theme.CypyTheme

@Composable
fun CypyApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    CypyTheme {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate("settings") },
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
