package com.snapsync.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.snapsync.app.ui.components.SnapSyncBottomBar
import com.snapsync.app.ui.screens.auth.LoginScreen
import com.snapsync.app.ui.screens.auth.RegisterScreen
import com.snapsync.app.ui.screens.camera.CameraConnectionScreen
import com.snapsync.app.ui.screens.dashboard.DashboardScreen
import com.snapsync.app.ui.screens.events.CreateEventScreen
import com.snapsync.app.ui.screens.events.EventDetailScreen
import com.snapsync.app.ui.screens.events.EventListScreen
import com.snapsync.app.ui.screens.gallery.GalleryScreen
import com.snapsync.app.ui.screens.gallery.PhotoViewerScreen
import com.snapsync.app.ui.screens.settings.SettingsScreen
import com.snapsync.app.ui.screens.transfers.TransfersScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SnapSyncBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth
            composable(Screen.Login.route) { LoginScreen(navController) }
            composable(Screen.Register.route) { RegisterScreen(navController) }

            // Core Bottom Navigation Screens
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Events.route) { EventListScreen(navController) }
            composable(Screen.Transfers.route) { TransfersScreen(navController) }
            composable(Screen.Gallery.route) { GalleryScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }

            // Feature Sub-Screens
            composable(Screen.CameraConfig.route) { CameraConnectionScreen(navController) }
            composable(Screen.CreateEvent.route) { CreateEventScreen(navController) }

            composable(
                route = Screen.EventDetail.route,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                EventDetailScreen(eventId = eventId, navController = navController)
            }

            composable(
                route = Screen.PhotoViewer.route,
                arguments = listOf(navArgument("photoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val photoId = backStackEntry.arguments?.getString("photoId") ?: ""
                PhotoViewerScreen(photoId = photoId, navController = navController)
            }
        }
    }
}

