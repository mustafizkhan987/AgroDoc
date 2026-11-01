package com.agrodoc.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agrodoc.app.R

private data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.HOME, R.string.nav_home, Icons.Default.Home),
    BottomNavItem(NavRoutes.CROPS, R.string.nav_crops, Icons.Default.Eco),
    BottomNavItem(NavRoutes.LIBRARY, R.string.nav_library, Icons.Default.MenuBook),
    BottomNavItem(NavRoutes.ASSISTANT, R.string.nav_assistant, Icons.Default.QuestionAnswer),
    BottomNavItem(NavRoutes.HISTORY, R.string.nav_history, Icons.Default.History),
    BottomNavItem(NavRoutes.SETTINGS, R.string.nav_settings, Icons.Default.Settings),
)

/** Routes where the bottom navigation bar should be hidden. */
private val routesWithoutBottomBar = setOf(
    NavRoutes.LANGUAGE,
    NavRoutes.CAMERA,
    NavRoutes.RESULT,
    "${NavRoutes.RESULT}?scanId={scanId}",
)

/**
 * Top-level scaffold hosting the 5-tab bottom navigation bar and the nav graph.
 */
@Composable
fun MainScaffold(startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = currentRoute !in routesWithoutBottomBar &&
        currentRoute?.startsWith(NavRoutes.RESULT) != true &&
        currentRoute?.startsWith("crop_detail") != true &&
        currentRoute?.startsWith("disease_detail") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AgroDocNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
