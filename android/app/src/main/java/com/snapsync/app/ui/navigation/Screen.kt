package com.snapsync.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")

    // Bottom Navigation Screens
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Events : Screen("events", "Events", Icons.Filled.Event, Icons.Outlined.Event)
    object Transfers : Screen("transfers", "Transfers", Icons.Filled.SyncAlt, Icons.Outlined.SyncAlt)
    object Gallery : Screen("gallery", "Gallery", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)

    // Sub-screens
    object CameraConfig : Screen("camera_config", "Camera Connection")
    object CreateEvent : Screen("create_event", "Create Event")
    object EventDetail : Screen("event_detail/{eventId}", "Event Details") {
        fun createRoute(eventId: String) = "event_detail/$eventId"
    }
    object PhotoViewer : Screen("photo_viewer/{photoId}", "Photo Preview") {
        fun createRoute(photoId: String) = "photo_viewer/$photoId"
    }

    companion object {
        val bottomNavItems = listOf(Dashboard, Events, Transfers, Gallery, Settings)
    }
}

