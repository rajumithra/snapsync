package com.snapsync.app.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.snapsync.app.data.remote.NetworkClient
import com.snapsync.app.data.repository.AuthRepository
import com.snapsync.app.ui.components.SnapSyncTopBar
import com.snapsync.app.ui.navigation.Screen
import com.snapsync.app.ui.theme.*

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val authRepo = remember { AuthRepository(context) }

    var serverUrl by remember { mutableStateOf(NetworkClient.defaultBaseUrl) }
    var autoUpload by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(false) }
    var preserveRaw by remember { mutableStateOf(true) }
    var ftpPort by remember { mutableStateOf("2121") }

    Scaffold(
        topBar = {
            SnapSyncTopBar(title = "App Settings")
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
            // Section 1: Backend & Cloud Target
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cloud Backend Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = {
                                serverUrl = it
                                NetworkClient.setServerUrl(context, it)
                            },
                            label = { Text("FastAPI Server Base URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Section 2: Upload Transmission Rules
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Transmission Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        SettingToggleRow("Auto Cloud Upload", "Upload new photos immediately upon camera ingestion", autoUpload) {
                            autoUpload = it
                        }
                        Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 8.dp))

                        SettingToggleRow("Wi-Fi Only Mode", "Pause uploads when connected via mobile cellular data", wifiOnly) {
                            wifiOnly = it
                        }
                        Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 8.dp))

                        SettingToggleRow("Preserve RAW Files", "Keep original sensor binaries alongside JPEGs", preserveRaw) {
                            preserveRaw = it
                        }
                    }
                }
            }

            // Section 3: Embedded FTP Configuration
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Camera FTP Server Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = ftpPort,
                            onValueChange = { ftpPort = it },
                            label = { Text("FTP Listening Port (Default 2121)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Section 4: Account & Logout
            item {
                Button(
                    onClick = {
                        authRepo.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = StatusError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of Studio Account", color = StatusError, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(desc, color = TextMuted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

