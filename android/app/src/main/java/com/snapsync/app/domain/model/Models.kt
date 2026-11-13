package com.snapsync.app.domain.model

import java.io.File
import java.util.Date

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    LISTENING,
    CONNECTED_RECEIVING,
    ERROR
}

enum class UploadStatus {
    QUEUED,
    UPLOADING,
    UPLOADED,
    READY,
    FAILED,
    PAUSED
}

data class PhotographerUser(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val token: String? = null
)

data class EventItem(
    val id: String,
    val title: String,
    val eventCode: String,
    val eventDate: String,
    val location: String?,
    val status: String,
    val qrCodeUrl: String?,
    val totalPhotosCount: Int = 0,
    val totalStorageBytes: Long = 0L,
    val isPublic: Boolean = true
)

data class CameraDevice(
    val id: String,
    val make: String,
    val model: String,
    val serialNumber: String?,
    val protocol: String = "ftp",
    val defaultFtpPort: Int = 2121,
    val isConnected: Boolean = false
)

data class PhotoRecord(
    val id: String,
    val eventId: String,
    val filename: String,
    val format: String,
    val fileSizeBytes: Long,
    val localFilePath: String,
    val previewFilePath: String? = null,
    val thumbnailFilePath: String? = null,
    val sha256Hash: String,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val iso: Int? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val capturedAt: Date? = null,
    val status: UploadStatus = UploadStatus.QUEUED,
    val uploadProgressPercent: Int = 0,
    val remoteUrl: String? = null
)

data class LiveRelayStats(
    val isFtpRunning: Boolean = false,
    val listeningIp: String = "0.0.0.0",
    val listeningPort: Int = 2121,
    val activeEventId: String? = null,
    val activeEventTitle: String? = null,
    val connectedCameraName: String? = null,
    val totalPhotosReceived: Int = 0,
    val totalPhotosUploaded: Int = 0,
    val pendingUploadsCount: Int = 0,
    val failedUploadsCount: Int = 0,
    val currentUploadSpeedMbps: Double = 0.0,
    val currentlyUploadingFilename: String? = null,
    val currentUploadProgressPercent: Int = 0
)

