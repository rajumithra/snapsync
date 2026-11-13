package com.snapsync.app.service.upload

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.snapsync.app.MainActivity
import com.snapsync.app.R
import com.snapsync.app.service.ftp.EmbeddedFtpServer
import com.snapsync.app.service.ftp.FtpEventListener
import com.snapsync.app.util.NetworkMonitor
import kotlinx.coroutines.*
import java.io.File

class UploadForegroundService : Service(), FtpEventListener {

    private val CHANNEL_ID = "snapsync_live_relay"
    private val NOTIFICATION_ID = 1001

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var ftpServer: EmbeddedFtpServer? = null
    private var uploadEngine: UploadEngine? = null

    private var activeEventId: String = "DEFAULT"
    private var activeEventTitle: String = "Live Event"

    companion object {
        const val ACTION_START = "ACTION_START_RELAY"
        const val ACTION_STOP = "ACTION_STOP_RELAY"
        const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"
        const val EXTRA_EVENT_TITLE = "EXTRA_EVENT_TITLE"

        fun start(context: Context, eventId: String, eventTitle: String) {
            val intent = Intent(context, UploadForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_EVENT_TITLE, eventTitle)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, UploadForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        uploadEngine = UploadEngine(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                activeEventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: "DEFAULT"
                activeEventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Live Event"

                startForeground(NOTIFICATION_ID, buildNotification("Initializing Live Relay..."))
                startRelayEngine()
            }
            ACTION_STOP -> {
                stopRelayEngine()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startRelayEngine() {
        val storageDir = File(getExternalFilesDir(null), "photos/$activeEventId")
        storageDir.mkdirs()

        val localIp = NetworkMonitor.getLocalIpAddress()
        val port = 2121

        ftpServer = EmbeddedFtpServer(
            port = port,
            storageDir = storageDir,
            username = "snapsync",
            password = "snapsync",
            localIp = localIp
        )
        ftpServer?.setListener(this)
        ftpServer?.start()

        uploadEngine?.start()

        // Monitor upload progress and update sticky notification
        serviceScope.launch {
            uploadEngine?.activePhotoName?.collect { filename ->
                if (filename != null) {
                    val speed = uploadEngine?.currentSpeedMbps?.value ?: 0.0
                    val text = "Uploading $filename (${"%.1f".format(speed)} Mbps)"
                    updateNotification(text)
                } else {
                    updateNotification("FTP Ready at $localIp:$port • Waiting for shots...")
                }
            }
        }
    }

    private fun stopRelayEngine() {
        ftpServer?.stop()
        uploadEngine?.stop()
        serviceScope.cancel()
    }

    // FtpEventListener callbacks
    override fun onServerStarted(ip: String, port: Int) {
        updateNotification("FTP Listening on $ip:$port • Connect camera to shoot")
    }

    override fun onServerStopped() {
        updateNotification("Relay server stopped")
    }

    override fun onClientConnected(clientIp: String) {
        updateNotification("Camera connected ($clientIp) • Ready")
    }

    override fun onClientDisconnected(clientIp: String) {
        val ip = NetworkMonitor.getLocalIpAddress()
        updateNotification("FTP Listening on $ip:2121 • Waiting for shots...")
    }

    override fun onTransferStarted(filename: String, clientIp: String) {
        updateNotification("Receiving $filename from camera...")
    }

    override fun onTransferCompleted(file: File, filename: String, clientIp: String, durationMs: Long) {
        serviceScope.launch(Dispatchers.IO) {
            uploadEngine?.ingestReceivedFile(file, activeEventId)
        }
    }

    override fun onTransferFailed(filename: String, reason: String) {
        updateNotification("Transfer error for $filename: $reason")
    }

    override fun onServerError(error: String) {
        updateNotification("FTP Error: $error")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SnapSync Live Relay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live camera ingestion and cloud upload progress."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SnapSync • $activeEventTitle")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopRelayEngine()
    }
}

