package com.snapsync.app.ui.screens.transfers

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.snapsync.app.data.local.entity.PhotoEntity
import com.snapsync.app.ui.components.SnapSyncTopBar
import com.snapsync.app.ui.components.StatusBadge
import com.snapsync.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TransfersScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    val pendingPhotos by db.photoDao().getPendingPhotos().collectAsState(initial = emptyList())
    val allPhotos by db.photoDao().getAllPhotos().collectAsState(initial = emptyList())

    val completedPhotos = allPhotos.filter { it.status == "READY" }
    val failedPhotos = allPhotos.filter { it.status == "FAILED" }

    var isPaused by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SnapSyncTopBar(
                title = "Upload Telemetry & Queue",
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            // Clear completed
                        }
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Completed", tint = TextMuted)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Active Transfer Progress Header
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, if (pendingPhotos.isNotEmpty()) AccentBlue else DarkCardBorder, RoundedCornerShape(16.dp)),
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
                                    text = if (pendingPhotos.isNotEmpty()) "Transferring to Cloud" else "Upload Queue Idle",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (pendingPhotos.isNotEmpty()) "${pendingPhotos.size} photos queued" else "All photos synced to cloud",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            StatusBadge(if (pendingPhotos.isNotEmpty()) "UPLOADING" else "IDLE")
                        }

                        if (pendingPhotos.isNotEmpty()) {
                            val active = pendingPhotos.first()
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = active.filename,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = (active.uploadProgressPercent / 100f).coerceIn(0.05f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = AccentBlue,
                                trackColor = DarkSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "${active.uploadProgressPercent}% completed", color = TextMuted, fontSize = 11.sp)
                                Text(text = "16.8 MB/s", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Controls: Pause / Resume / Retry
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { isPaused = !isPaused },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = if (isPaused) "Resume" else "Pause", fontSize = 12.sp)
                            }

                            if (failedPhotos.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            failedPhotos.forEach { photo ->
                                                db.photoDao().updateUploadProgress(photo.id, "QUEUED", 0)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusWarning.copy(alpha = 0.2f))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Failed", color = StatusWarning, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Queue Items List
            item {
                Text(
                    text = "Transfer Queue (${pendingPhotos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            if (pendingPhotos.isEmpty()) {
                item {
                    Text(
                        text = "No pending uploads in queue.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                items(pendingPhotos) { photo ->
                    TransferItemRow(photo = photo)
                }
            }

            // Section 3: Completed Transmissions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Completed (${completedPhotos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }

            items(completedPhotos.take(10)) { photo ->
                TransferItemRow(photo = photo)
            }
        }
    }
}

@Composable
fun TransferItemRow(photo: PhotoEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (photo.status) {
                    "READY" -> Icons.Default.CheckCircle
                    "UPLOADING" -> Icons.Default.CloudUpload
                    "FAILED" -> Icons.Default.Error
                    else -> Icons.Default.Schedule
                },
                contentDescription = null,
                tint = when (photo.status) {
                    "READY" -> StatusSuccess
                    "UPLOADING" -> AccentBlue
                    "FAILED" -> StatusError
                    else -> StatusWarning
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = photo.filename, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    text = "${(photo.fileSizeBytes / (1024 * 1024)).toInt()} MB • ${photo.format}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            StatusBadge(photo.status)
        }
    }
}

