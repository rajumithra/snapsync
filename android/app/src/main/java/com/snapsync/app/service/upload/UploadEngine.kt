package com.snapsync.app.service.upload

import android.content.Context
import android.util.Log
import com.snapsync.app.data.local.AppDatabase
import com.snapsync.app.data.local.entity.PhotoEntity
import com.snapsync.app.data.local.entity.UploadJobEntity
import com.snapsync.app.data.remote.NetworkClient
import com.snapsync.app.data.remote.dto.ConfirmUploadRequestDto
import com.snapsync.app.data.remote.dto.PhotoUploadUrlRequestDto
import com.snapsync.app.util.ExifExtractor
import com.snapsync.app.util.ImageProcessor
import com.snapsync.app.util.NetworkMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class UploadEngine(private val context: Context) {
    private val TAG = "UploadEngine"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val db = AppDatabase.getInstance(context)
    private val photoDao = db.photoDao()
    private val uploadJobDao = db.uploadJobDao()

    private val isRunning = AtomicBoolean(false)
    private var isPaused = false

    // Progress StateFlow for UI binding
    private val _currentSpeedMbps = MutableStateFlow(0.0)
    val currentSpeedMbps = _currentSpeedMbps.asStateFlow()

    private val _activePhotoName = MutableStateFlow<String?>(null)
    val activePhotoName = _activePhotoName.asStateFlow()

    private val _activePhotoProgress = MutableStateFlow(0)
    val activePhotoProgress = _activePhotoProgress.asStateFlow()

    fun start() {
        if (isRunning.getAndSet(true)) return
        isPaused = false
        Log.i(TAG, "UploadEngine started.")

        scope.launch {
            while (isRunning.get()) {
                if (!isPaused) {
                    processNextInQueue()
                }
                delay(1500)
            }
        }
    }

    fun pause() {
        isPaused = true
        Log.i(TAG, "UploadEngine paused.")
    }

    fun resume() {
        isPaused = false
        Log.i(TAG, "UploadEngine resumed.")
    }

    fun stop() {
        isRunning.set(false)
        scope.cancel()
        Log.i(TAG, "UploadEngine stopped.")
    }

    /**
     * Ingests a raw or JPEG file received from camera FTP/USB
     */
    suspend fun ingestReceivedFile(file: File, eventId: String) = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "Cannot ingest non-existent or empty file: ${file.name}")
            return@withContext null
        }

        val hash = ImageProcessor.calculateSha256(file)

        // Duplicate check
        val existing = photoDao.getPhotoByHash(hash)
        if (existing != null) {
            Log.w(TAG, "Duplicate photo detected (${file.name}), skipping ingestion.")
            return@withContext existing
        }

        val photoId = UUID.randomUUID().toString()
        val ext = file.extension.uppercase()

        // Extract EXIF tags
        val exif = ExifExtractor.extract(file)

        // Generate preview & thumbnail
        val previewDir = File(context.getExternalFilesDir(null), "previews")
        val thumbDir = File(context.getExternalFilesDir(null), "thumbs")
        previewDir.mkdirs()
        thumbDir.mkdirs()

        val previewFile = File(previewDir, "$photoId.webp")
        val thumbFile = File(thumbDir, "$photoId.webp")

        ImageProcessor.generateThumbnail(file, thumbFile)
        ImageProcessor.generatePreview(file, previewFile)

        val photoEntity = PhotoEntity(
            id = photoId,
            eventId = eventId,
            filename = file.name,
            format = ext,
            fileSizeBytes = file.length(),
            localFilePath = file.absolutePath,
            previewFilePath = if (previewFile.exists()) previewFile.absolutePath else null,
            thumbnailFilePath = if (thumbFile.exists()) thumbFile.absolutePath else null,
            sha256Hash = hash,
            cameraMake = exif.make,
            cameraModel = exif.model,
            iso = exif.iso,
            shutterSpeed = exif.shutterSpeed,
            aperture = exif.aperture,
            focalLength = exif.focalLength,
            capturedAt = exif.dateTaken?.time ?: file.lastModified(),
            status = "QUEUED",
            uploadProgressPercent = 0
        )
        photoDao.insertPhoto(photoEntity)

        val jobEntity = UploadJobEntity(
            id = UUID.randomUUID().toString(),
            photoId = photoId,
            eventId = eventId,
            totalBytes = file.length(),
            status = "PENDING"
        )
        uploadJobDao.insertJob(jobEntity)

        Log.i(TAG, "Ingested photo successfully: ${file.name} (ID: $photoId)")
        return@withContext photoEntity
    }

    private suspend fun processNextInQueue() {
        val pendingPhotos = photoDao.getPendingPhotosList()
        if (pendingPhotos.isEmpty()) {
            _activePhotoName.value = null
            _activePhotoProgress.value = 0
            _currentSpeedMbps.value = 0.0
            return
        }

        val photo = pendingPhotos.first()
        val file = File(photo.localFilePath)
        if (!file.exists()) {
            photoDao.updateUploadProgress(photo.id, "FAILED", 0)
            return
        }

        _activePhotoName.value = photo.filename
        _activePhotoProgress.value = 0
        photoDao.updateUploadProgress(photo.id, "UPLOADING", 0)

        val api = NetworkClient.getApi(context)

        try {
            // Step 1: Request upload URL from backend
            val urlReq = PhotoUploadUrlRequestDto(
                eventId = photo.eventId,
                originalFilename = photo.filename,
                fileFormat = photo.format,
                fileSizeBytes = photo.fileSizeBytes,
                sha256Hash = photo.sha256Hash,
                cameraMake = photo.cameraMake,
                cameraModel = photo.cameraModel,
                iso = photo.iso,
                shutterSpeed = photo.shutterSpeed,
                aperture = photo.aperture,
                focalLength = photo.focalLength
            )

            val urlRes = api.requestUploadUrl(urlReq)
            if (!urlRes.isSuccessful || urlRes.body() == null) {
                Log.e(TAG, "Failed to get upload URL: ${urlRes.errorBody()?.string()}")
                photoDao.updateUploadProgress(photo.id, "FAILED", 0)
                return
            }

            val uploadInfo = urlRes.body()!!
            val startTime = System.currentTimeMillis()

            // Step 2: Upload file (either to S3 presigned URL or direct upload fallback)
            if (uploadInfo.storageProvider == "s3") {
                uploadToS3(uploadInfo.uploadUrlOriginal, file, photo.id)
            } else {
                uploadDirect(uploadInfo.photoId, file, photo.id)
            }

            val durationSeconds = maxOf(0.1, (System.currentTimeMillis() - startTime) / 1000.0)
            val speedMbps = ((file.length() * 8.0) / (durationSeconds * 1024 * 1024))
            _currentSpeedMbps.value = speedMbps

            // Step 3: Confirm upload with backend
            val confirmRes = api.confirmUpload(
                ConfirmUploadRequestDto(
                    photoId = uploadInfo.photoId,
                    bytesUploaded = file.length(),
                    status = "ready"
                )
            )

            if (confirmRes.isSuccessful) {
                photoDao.updateUploadProgress(photo.id, "READY", 100)
                Log.i(TAG, "Photo ${photo.filename} successfully uploaded and confirmed! Speed: ${"%.2f".format(speedMbps)} Mbps")
            } else {
                photoDao.updateUploadProgress(photo.id, "FAILED", 0)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading photo ${photo.filename}: ${e.message}")
            photoDao.updateUploadProgress(photo.id, "FAILED", 0)
        }
    }

    private suspend fun uploadToS3(s3Url: String, file: File, photoId: String) = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().build()
        val totalBytes = file.length()
        var bytesUploaded = 0L

        val mediaType = "application/octet-stream".toMediaTypeOrNull()

        val requestBody = object : RequestBody() {
            override fun contentType() = mediaType
            override fun contentLength() = totalBytes

            override fun writeTo(sink: okio.BufferedSink) {
                val buffer = ByteArray(16384)
                FileInputStream(file).use { fis ->
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                        bytesUploaded += read
                        val progress = ((bytesUploaded.toDouble() / totalBytes) * 100).toInt()
                        _activePhotoProgress.value = progress
                        scope.launch {
                            photoDao.updateUploadProgress(photoId, "UPLOADING", progress)
                        }
                    }
                }
            }
        }

        val request = Request.Builder()
            .url(s3Url)
            .put(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("S3 upload failed with HTTP code ${response.code}")
        }
    }

    private suspend fun uploadDirect(serverPhotoId: String, file: File, photoId: String) = withContext(Dispatchers.IO) {
        val api = NetworkClient.getApi(context)
        val fileReqBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file)
        val bodyPart = MultipartBody.Part.createFormData("file", file.name, fileReqBody)

        val res = api.directUpload(serverPhotoId, "original", bodyPart)
        if (!res.isSuccessful) {
            throw Exception("Direct upload failed: ${res.errorBody()?.string()}")
        }
        _activePhotoProgress.value = 100
    }
}

