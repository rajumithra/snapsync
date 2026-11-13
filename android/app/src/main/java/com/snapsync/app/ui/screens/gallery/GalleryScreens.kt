package com.snapsync.app.ui.screens.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.snapsync.app.data.local.AppDatabase
import com.snapsync.app.data.local.entity.PhotoEntity
import com.snapsync.app.ui.components.SnapSyncTopBar
import com.snapsync.app.ui.components.StatusBadge
import com.snapsync.app.ui.navigation.Screen
import com.snapsync.app.ui.theme.*
import java.io.File

@Composable
fun GalleryScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val photos by db.photoDao().getAllPhotos().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            SnapSyncTopBar(
                title = "Live Photo Stream (${photos.size})"
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No photos captured yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("When camera shoots, images stream here live", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(photos) { photo ->
                    PhotoGridThumbnail(photo = photo) {
                        navController.navigate(Screen.PhotoViewer.createRoute(photo.id))
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGridThumbnail(photo: PhotoEntity, onClick: () -> Unit) {
    val imagePath = photo.thumbnailFilePath ?: photo.previewFilePath ?: photo.localFilePath

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(imagePath))
                .crossfade(true)
                .build(),
            contentDescription = photo.filename,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Status badge overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = if (photo.status == "READY") Icons.Default.CloudDone else Icons.Default.CloudUpload,
                contentDescription = null,
                tint = if (photo.status == "READY") StatusSuccess else StatusWarning,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PhotoViewerScreen(photoId: String, navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    var photo by remember { mutableStateOf<PhotoEntity?>(null) }

    LaunchedEffect(photoId) {
        photo = db.photoDao().getPhotoById(photoId)
    }

    Scaffold(
        topBar = {
            SnapSyncTopBar(
                title = photo?.filename ?: "Photo Preview",
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (photo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            val imagePath = photo!!.previewFilePath ?: photo!!.localFilePath

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // High-res preview image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(imagePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = photo!!.filename,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // EXIF Metadata Sheet
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = photo!!.filename,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            StatusBadge(photo!!.status)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Camera: ${photo!!.cameraMake ?: "Unknown"} ${photo!!.cameraModel ?: ""}".trim(), color = TextSecondary, fontSize = 12.sp)
                            Text("Format: ${photo!!.format} (${(photo!!.fileSizeBytes / (1024 * 1024)).toInt()} MB)", color = TextSecondary, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ISO: ${photo!!.iso ?: "Auto"}", color = TextMuted, fontSize = 12.sp)
                            Text("Shutter: ${photo!!.shutterSpeed ?: "--"}", color = TextMuted, fontSize = 12.sp)
                            Text("Aperture: ${photo!!.aperture ?: "--"}", color = TextMuted, fontSize = 12.sp)
                            Text("Focal: ${photo!!.focalLength ?: "--"}", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

