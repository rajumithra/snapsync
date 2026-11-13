package com.snapsync.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val eventCode: String,
    val eventDate: String,
    val location: String?,
    val status: String,
    val qrCodeUrl: String?,
    val totalPhotosCount: Int = 0,
    val totalStorageBytes: Long = 0L,
    val isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey val id: String,
    val make: String,
    val model: String,
    val serialNumber: String?,
    val protocol: String = "ftp",
    val defaultFtpPort: Int = 2121,
    val isConnected: Boolean = false,
    val lastConnectedAt: Long? = null
)

@Entity(
    tableName = "photos",
    indices = [
        Index(value = ["eventId"]),
        Index(value = ["status"]),
        Index(value = ["sha256Hash"])
    ]
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val filename: String,
    val format: String,
    val fileSizeBytes: Long,
    val localFilePath: String,
    val previewFilePath: String?,
    val thumbnailFilePath: String?,
    val sha256Hash: String,
    val cameraMake: String?,
    val cameraModel: String?,
    val iso: Int?,
    val shutterSpeed: String?,
    val aperture: String?,
    val focalLength: String?,
    val capturedAt: Long?,
    val status: String, // QUEUED, UPLOADING, UPLOADED, READY, FAILED
    val uploadProgressPercent: Int = 0,
    val remoteUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "upload_jobs",
    indices = [Index(value = ["photoId"], unique = true), Index(value = ["status"])]
)
data class UploadJobEntity(
    @PrimaryKey val id: String,
    val photoId: String,
    val eventId: String,
    val attemptCount: Int = 0,
    val maxRetries: Int = 5,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long,
    val transferSpeedMbps: Double = 0.0,
    val status: String, // PENDING, IN_PROGRESS, COMPLETED, FAILED, PAUSED
    val errorMessage: String? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

