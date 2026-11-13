package com.snapsync.app.ui.screens.events

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.snapsync.app.data.local.AppDatabase
import com.snapsync.app.data.local.entity.EventEntity
import com.snapsync.app.data.repository.EventRepository
import com.snapsync.app.ui.components.SnapSyncTopBar
import com.snapsync.app.ui.components.StatusBadge
import com.snapsync.app.ui.navigation.Screen
import com.snapsync.app.ui.theme.*
import com.snapsync.app.util.QrCodeGenerator
import kotlinx.coroutines.launch

@Composable
fun EventListScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { EventRepository(context) }
    val events by repository.getAllEvents().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repository.refreshEvents()
    }

    Scaffold(
        topBar = {
            SnapSyncTopBar(
                title = "Event Management",
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.CreateEvent.route) }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Event", tint = AccentBlue)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateEvent.route) },
                containerColor = AccentBlue,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No events created yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Create an event to start live photo transmission", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate(Screen.CreateEvent.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Create First Event")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events) { event ->
                    EventCard(event = event) {
                        navController.navigate(Screen.EventDetail.createRoute(event.id))
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(event: EventEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                StatusBadge(event.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Code: ${event.eventCode}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${event.eventDate} • ${event.location ?: "Venue"}",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = "${event.totalPhotosCount} photos",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CreateEventScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { EventRepository(context) }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("2026-11-20") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var allowFaceSearch by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SnapSyncTopBar(
                title = "Create New Event",
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Event Name / Title *") },
                placeholder = { Text("e.g. Vance & Sterling Royal Wedding") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = eventDate,
                onValueChange = { eventDate = it },
                label = { Text("Event Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location / Venue") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description & Notes") },
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Enable AI Face Matching for Guests", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Guests scan event QR and match selfies", color = TextMuted, fontSize = 12.sp)
                }
                Switch(checked = allowFaceSearch, onCheckedChange = { allowFaceSearch = it })
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    isLoading = true
                    scope.launch {
                        repository.createEvent(title.trim(), eventDate.trim(), location.trim(), description.trim())
                        isLoading = false
                        navController.popBackStack()
                    }
                },
                enabled = title.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Create Event & Generate QR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EventDetailScreen(eventId: String, navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    var event by remember { mutableStateOf<EventEntity?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(eventId) {
        val ev = db.eventDao().getEventById(eventId)
        event = ev
        if (ev != null) {
            val shareUrl = "http://localhost:3000/gallery/${ev.eventCode}"
            qrBitmap = QrCodeGenerator.generateQrBitmap(shareUrl, 512)
        }
    }

    Scaffold(
        topBar = {
            SnapSyncTopBar(
                title = event?.title ?: "Event Details",
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (event == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Event Header
                Text(event!!.title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text("Code: ${event!!.eventCode} • ${event!!.eventDate}", color = AccentCyan, fontSize = 14.sp)

                // QR Code Card for Guest Sharing & Face Matching
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Guest Live Gallery & Face Match QR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Guests scan to view live photos & match their faces",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBackground)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "Event QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "http://localhost:3000/gallery/${event!!.eventCode}",
                            color = AccentBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

