package com.snapsync.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("company_name") val companyName: String? = null,
    val role: String = "photographer"
)

data class UserDto(
    val id: String,
    val email: String,
    @SerializedName("full_name") val fullName: String,
    val role: String
)

data class AuthResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: UserDto
)

data class EventDto(
    val id: String,
    val title: String,
    @SerializedName("event_code") val eventCode: String,
    @SerializedName("event_date") val eventDate: String,
    val location: String?,
    val description: String?,
    val status: String,
    @SerializedName("qr_code_url") val qrCodeUrl: String?,
    @SerializedName("total_photos_count") val totalPhotosCount: Int,
    @SerializedName("total_storage_bytes") val totalStorageBytes: Long,
    @SerializedName("is_public") val isPublic: Boolean
)

data class CreateEventRequest(
    val title: String,
    @SerializedName("event_date") val eventDate: String,
    val location: String? = null,
    val description: String? = null,
    @SerializedName("is_public") val isPublic: Boolean = true,
    @SerializedName("allow_face_search") val allowFaceSearch: Boolean = true
)

data class PhotoUploadUrlRequestDto(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("file_format") val fileFormat: String,
    @SerializedName("file_size_bytes") val fileSizeBytes: Long,
    @SerializedName("sha256_hash") val sha256Hash: String,
    @SerializedName("camera_make") val cameraMake: String? = null,
    @SerializedName("camera_model") val cameraModel: String? = null,
    val iso: Int? = null,
    @SerializedName("shutter_speed") val shutterSpeed: String? = null,
    val aperture: String? = null,
    @SerializedName("focal_length") val focalLength: String? = null,
    @SerializedName("lens_model") val lensModel: String? = null
)

data class PhotoUploadUrlResponseDto(
    @SerializedName("photo_id") val photoId: String,
    @SerializedName("upload_url_original") val uploadUrlOriginal: String,
    @SerializedName("upload_url_preview") val uploadUrlPreview: String?,
    @SerializedName("upload_url_thumbnail") val uploadUrlThumbnail: String?,
    @SerializedName("storage_provider") val storageProvider: String,
    @SerializedName("expires_in_seconds") val expiresInSeconds: Int
)

data class ConfirmUploadRequestDto(
    @SerializedName("photo_id") val photoId: String,
    @SerializedName("bytes_uploaded") val bytesUploaded: Long,
    val status: String = "ready"
)

data class PhotoResponseDto(
    val id: String,
    @SerializedName("event_id") val eventId: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("file_format") val fileFormat: String,
    @SerializedName("file_size_bytes") val fileSizeBytes: Long,
    @SerializedName("public_url_original") val publicUrlOriginal: String?,
    @SerializedName("public_url_preview") val publicUrlPreview: String?,
    @SerializedName("public_url_thumbnail") val publicUrlThumbnail: String?,
    val status: String
)

data class DashboardStatsDto(
    @SerializedName("connection_status") val connectionStatus: String,
    @SerializedName("active_event_id") val activeEventId: String?,
    @SerializedName("active_event_title") val activeEventTitle: String?,
    @SerializedName("connected_cameras_count") val connectedCamerasCount: Int,
    @SerializedName("total_photos_received") val totalPhotosReceived: Int,
    @SerializedName("total_photos_uploaded") val totalPhotosUploaded: Int,
    @SerializedName("pending_uploads") val pendingUploads: Int,
    @SerializedName("failed_uploads") val failedUploads: Int,
    @SerializedName("upload_speed_mbps") val uploadSpeedMbps: Double,
    @SerializedName("storage_used_bytes") val storageUsedBytes: Long,
    @SerializedName("storage_limit_bytes") val storageLimitBytes: Long
)

