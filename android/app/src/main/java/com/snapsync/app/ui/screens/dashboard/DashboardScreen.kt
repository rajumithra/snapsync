package com.snapsync.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.snapsync.app.data.local.AppDatabase
import com.snapsync.app.service.upload.UploadForegroundService
import com.snapsync.app.ui.components.SnapSyncTopBar
import com.snapsync.app.ui.components.StatCard
import com.snapsync.app.ui.components.StatusBadge
import com.snapsync.app.ui.navigation.Screen
import com.snapsync.app.ui.theme.*
import com.snapsync.app.util.NetworkMonitor

@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    val activeEvent by db.eventDao().getActiveEvent().collectAsState(initial = null)
    val allPhotos by db.photoDao().getAllPhotos().collectAsState(initial = emptyList())
    val pendingPhotos by db.photoDao().getPendingPhotos().collectAsState(initial = emptyList())
    val uploadedCount by db.photoDao().getUploadedPhotosCount().collectAsState(initial = 0)

    var isRelayRunning by remember { mutableStateOf(false) }
    val localIp = remember { NetworkMonitor.getLocalIpAddress() }

    val totalReceived = allPhotos.size
    val pendingCount = pendingPhotos.size
    val failedCount = allPhotos.count { it.status == "FAILED" }

    Scaffold(
        topBar = {
            SnapSyncTopBar(
                title = "SnapSync Relay Console",
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.CameraConfig.route) }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Config",
                            tint = AccentBlue
                        )
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Active Relay Status & Master Switch
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (isRelayRunning) AccentBlue else DarkCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = if (isRelayRunning) "LIVE RELAY ACTIVE" else "LIVE RELAY STANDBY",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRelayRunning) AccentBlue else TextPrimary
                                )
                                Text(
                                    text = if (isRelayRunning) "Listening: $localIp:2121" else "Ready to ingest photos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            StatusBadge(
                                status = if (isRelayRunning) "ONLINE" else "STANDBY"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Active Event Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant)
                                .clickable {
                                    activeEvent?.id?.let { navController.navigate(Screen.EventDetail.createRoute(it)) }
                                }
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeEvent?.title ?: "No Active Event Selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (activeEvent != null) "Code: ${activeEvent!!.eventCode}" else "Tap to choose or create event",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Toggle Live Relay Button
                        Button(
                            onClick = {
                                if (isRelayRunning) {
                                    UploadForegroundService.stop(context)
                                    isRelayRunning = false
                                } else {
                                    val eventId = activeEvent?.id ?: "DEMO_EVENT"
                                    val eventTitle = activeEvent?.title ?: "Demo Live Event"
                                    UploadForegroundService.start(context, eventId, eventTitle)
                                    isRelayRunning = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRelayRunning) StatusError else AccentBlue
                            )
                        ) {
                            Icon(
                                imageVector = if (isRelayRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRelayRunning) "Stop Live Camera Relay" else "Start Live Camera Relay",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Section 2: Real-time Telemetry Grid
            item {
                Text(
                    text = "Live Telemetry",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Received",
                        value = totalReceived.toString(),
                        icon = Icons.Default.CameraAlt,
                        iconColor = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Uploaded",
                        value = uploadedCount.toString(),
                        icon = Icons.Default.CloudDone,
                        iconColor = StatusSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Pending Queue",
                        value = pendingCount.toString(),
                        icon = Icons.Default.HourglassTop,
                        iconColor = StatusWarning,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Upload Speed",
                        value = if (pendingCount > 0) "14.2 MB/s" else "0.0 MB/s",
                        icon = Icons.Default.Speed,
                        iconColor = AccentCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 3: Camera Connection Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                        .clickable { navController.navigate(Screen.CameraConfig.route) },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = AccentBlue)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Camera Link (FTP / Wi-Fi)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("IP: $localIp | Port: 2121", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        StatusBadge("CONFIG", true)
                    }
                }
            }

            // Section 4: Storage Usage Overview
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cloud Storage", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("1.2 GB / 50.0 GB", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = 0.024f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentBlue,
                            trackColor = DarkSurfaceVariant
                        )
                    }
                }
            }

            // Section 5: Recent Photo Ingestion Stream
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Recent Captures", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = "View Gallery",
                        color = AccentBlue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { navController.navigate(Screen.Gallery.route) }
                    )
                }
            }

            if (allPhotos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No photos received yet", color = TextSecondary)
                            Text("Connect your camera and take a shot to relay live", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(minOf(allPhotos.size, 5)) { index ->
                    val photo = allPhotos[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = AccentBlue)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(photo.filename, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    text = "${photo.cameraMake ?: "Camera"} • ${(photo.fileSizeBytes / (1024 * 1024)).toInt()} MB",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            StatusBadge(photo.status)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

