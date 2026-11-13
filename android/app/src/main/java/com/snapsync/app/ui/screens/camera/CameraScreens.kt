package com.snapsync.app.ui.screens.camera

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.snapsync.app.ui.components.SnapSyncTopBar
import com.snapsync.app.ui.components.StatusBadge
import com.snapsync.app.ui.theme.*
import com.snapsync.app.util.NetworkMonitor

@Composable
fun CameraConnectionScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val localIp = remember { NetworkMonitor.getLocalIpAddress() }

    var selectedTab by remember { mutableStateOf(0) } // 0 = FTP Config, 1 = Sony Guide, 2 = Canon Guide, 3 = Nikon / USB

    Scaffold(
        topBar = {
            SnapSyncTopBar(
                title = "Camera Connectivity",
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
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
            // Live FTP Receiver Credentials Box
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, AccentBlue, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WifiTethering, contentDescription = null, tint = AccentBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("FTP Server Credentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            StatusBadge("READY")
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        FtpParamRow("FTP Host / IP", localIp) { clipboardManager.setText(AnnotatedString(localIp)) }
                        FtpParamRow("FTP Port", "2121") { clipboardManager.setText(AnnotatedString("2121")) }
                        FtpParamRow("Username", "snapsync") { clipboardManager.setText(AnnotatedString("snapsync")) }
                        FtpParamRow("Password", "snapsync") { clipboardManager.setText(AnnotatedString("snapsync")) }
                        FtpParamRow("Passive (PASV) Mode", "Enabled / ON") {}
                        FtpParamRow("Target Directory", "/") {}
                    }
                }
            }

            // Tabs for Guides
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = AccentBlue
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Sony Alpha", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Canon EOS", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Nikon & USB", fontSize = 12.sp) }
                    )
                }
            }

            // Guide Contents
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        when (selectedTab) {
                            0 -> {
                                Text("How Live Relay Works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("1. Turn on Android Hotspot or connect both Camera and Phone to the same Wi-Fi router.", color = TextSecondary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("2. Configure FTP transfer inside camera network settings using the IP and Port above.", color = TextSecondary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("3. Turn on 'Auto Transfer During Shooting' on your camera.", color = TextSecondary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("4. Click the shutter: photos land on your phone within 1 second and upload directly to cloud storage!", color = TextSecondary, fontSize = 13.sp)
                            }
                            1 -> {
                                Text("Sony Alpha Setup (A7 IV, A7R V, A1, A9)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Menu -> Network -> Transfer/Remote -> FTP Transfer Func.", color = TextSecondary, fontSize = 13.sp)
                                Text("• Set FTP Function = ON", color = TextSecondary, fontSize = 13.sp)
                                Text("• Server Setting 1 -> Host = $localIp, Port = 2121", color = TextSecondary, fontSize = 13.sp)
                                Text("• User = snapsync, Password = snapsync, PASV = ON", color = TextSecondary, fontSize = 13.sp)
                                Text("• Auto FTP Transfer = ON (Target: JPEG Only or RAW+JPEG)", color = TextSecondary, fontSize = 13.sp)
                            }
                            2 -> {
                                Text("Canon EOS Setup (R5, R6 II, R3)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Menu -> Network -> FTP Transfer -> Create with Wizard", color = TextSecondary, fontSize = 13.sp)
                                Text("• Connect to Android Wi-Fi / Hotspot", color = TextSecondary, fontSize = 13.sp)
                                Text("• Server address = $localIp, Port = 2121, Passive = Enable", color = TextSecondary, fontSize = 13.sp)
                                Text("• Target folder = /, Login = snapsync / snapsync", color = TextSecondary, fontSize = 13.sp)
                                Text("• Auto transfer after shooting = Enable", color = TextSecondary, fontSize = 13.sp)
                            }
                            3 -> {
                                Text("Nikon Z Series & USB Tethering", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• Nikon: Network Menu -> Connect to FTP Server -> Create Profile ($localIp:2121). Set Auto Send = ON.", color = TextSecondary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("• USB Tethering: Connect USB-C cable between camera and Android phone. Set camera USB mode to 'PC Remote' / 'MTP'. Android will automatically prompt for PTP permission.", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FtpParamRow(label: String, value: String, onCopy: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            if (onCopy != null) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = AccentBlue, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

