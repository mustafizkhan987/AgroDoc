package com.agrodoc.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.agrodoc.app.ui.screens.about.AboutScreen
import com.agrodoc.app.ui.screens.assistant.AssistantScreen
import com.agrodoc.app.ui.screens.camera.CameraScreen
import com.agrodoc.app.ui.screens.crops.CropDetailScreen
import com.agrodoc.app.ui.screens.crops.CropsScreen
import com.agrodoc.app.ui.screens.fertilizer.FertilizerScreen
import com.agrodoc.app.ui.screens.history.HistoryScreen
import com.agrodoc.app.ui.screens.home.HomeScreen
import com.agrodoc.app.ui.screens.library.DiseaseDetailScreen
import com.agrodoc.app.ui.screens.library.LibraryScreen
import com.agrodoc.app.ui.screens.onboarding.LanguageScreen
import com.agrodoc.app.ui.screens.result.ResultScreen
import com.agrodoc.app.ui.screens.settings.SettingsScreen

/**
 * Root navigation graph.
 * [startDestination] is supplied by [MainActivity] based on whether the user
 * has already chosen a language (read from DataStore on the splash).
 */
@Composable
fun AgroDocNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(NavRoutes.LANGUAGE) {
            LanguageScreen(
                onLanguageChosen = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LANGUAGE) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                onScanClick = { navController.navigate(NavRoutes.CAMERA) },
                onNavigateToCrops = { navController.navigate(NavRoutes.CROPS) },
                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                onHistoryItemClick = { scanId ->
                    navController.navigate("${NavRoutes.RESULT}?scanId=$scanId")
                },
            )
        }

        composable(NavRoutes.CROPS) {
            CropsScreen(onCropClick = { navController.navigate(NavRoutes.cropDetail(it)) })
        }
        composable(NavRoutes.CROP_DETAIL) { entry ->
            val cropId = entry.arguments?.getString("cropId").orEmpty()
            CropDetailScreen(cropId, onDiseaseClick = { navController.navigate(NavRoutes.diseaseDetail(it)) }, onFertilizerClick = { navController.navigate(NavRoutes.fertilizer(it)) })
        }

        composable(NavRoutes.CAMERA) {
            CameraScreen(
                onResultReady = { navController.navigate(NavRoutes.RESULT) },
                onBack = { navController.popBackStack() },
            )
        }

        // Result screen can be reached from the camera flow (no scanId) or from
        // history (with a scanId to reload from Room).
        composable(
            route = "${NavRoutes.RESULT}?scanId={scanId}",
            arguments = listOf(
                navArgument("scanId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            ResultScreen(
                onRetake = { navController.navigate(NavRoutes.CAMERA) },
                onBack = { navController.popBackStack() },
                scanId = it.arguments?.getString("scanId")?.toLongOrNull(),
            )
        }

        composable(NavRoutes.HISTORY) {
            HistoryScreen(
                onItemClick = { scanId ->
                    navController.navigate("${NavRoutes.RESULT}?scanId=$scanId")
                },
            )
        }

        composable(NavRoutes.LIBRARY) { LibraryScreen(onDiseaseClick = { navController.navigate(NavRoutes.diseaseDetail(it)) }) }
        composable(NavRoutes.DISEASE_DETAIL) { entry -> DiseaseDetailScreen(entry.arguments?.getString("diseaseId").orEmpty(), onBack = { navController.popBackStack() }) }
        composable(NavRoutes.ASSISTANT) { AssistantScreen() }
        composable("${NavRoutes.FERTILIZER}?cropId={cropId}", arguments = listOf(navArgument("cropId") { type = NavType.StringType; nullable = true; defaultValue = null })) { entry ->
            FertilizerScreen(initialCropId = entry.arguments?.getString("cropId"))
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onAboutClick = { navController.navigate(NavRoutes.ABOUT) }
            )
        }

        composable(NavRoutes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
