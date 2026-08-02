package com.cypy.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cypy.app.ui.theme.ColorSaver
import com.cypy.app.ui.theme.CypyTheme
import com.cypy.app.ui.theme.DefaultThemeColor

private enum class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    TRANSLATE("translate", "Translate", Icons.Filled.Translate, Icons.Outlined.Translate),
    HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
fun CypyApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    val initialDarkTheme = isSystemInDarkTheme()
    var darkTheme by rememberSaveable { mutableStateOf(initialDarkTheme) }
    var pureBlack by rememberSaveable { mutableStateOf(false) }
    var themeColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(DefaultThemeColor) }

    CypyTheme(
        darkTheme = darkTheme,
        pureBlack = pureBlack,
        themeColor = themeColor,
    ) {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            bottomBar = {
                NavigationBar {
                    BottomTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = BottomTab.TRANSLATE.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(BottomTab.TRANSLATE.route) {
                    MainScreen(viewModel = viewModel)
                }
                composable(BottomTab.HISTORY.route) {
                    HistoryScreen(viewModel = viewModel)
                }
                composable(BottomTab.SETTINGS.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        darkTheme = darkTheme,
                        onDarkThemeChange = { darkTheme = it },
                        pureBlack = pureBlack,
                        onPureBlackChange = { pureBlack = it },
                        themeColor = themeColor,
                        onThemeColorChange = { themeColor = it },
                    )
                }
            }
        }
    }
}
