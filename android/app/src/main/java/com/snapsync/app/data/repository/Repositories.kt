package com.snapsync.app.data.repository

import android.content.Context
import com.snapsync.app.data.local.AppDatabase
import com.snapsync.app.data.local.entity.CameraEntity
import com.snapsync.app.data.local.entity.EventEntity
import com.snapsync.app.data.local.entity.PhotoEntity
import com.snapsync.app.data.remote.NetworkClient
import com.snapsync.app.data.remote.dto.CreateEventRequest
import com.snapsync.app.data.remote.dto.RegisterRequest
import com.snapsync.app.domain.model.PhotographerUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthRepository(private val context: Context) {
    private val api = NetworkClient.getApi(context)

    suspend fun login(email: String, pass: String): Result<PhotographerUser> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(email, pass)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                NetworkClient.saveToken(context, data.accessToken)
                Result.success(
                    PhotographerUser(
                        id = data.user.id,
                        email = data.user.email,
                        fullName = data.user.fullName,
                        role = data.user.role,
                        token = data.accessToken
                    )
                )
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, pass: String, name: String, company: String?): Result<PhotographerUser> = withContext(Dispatchers.IO) {
        try {
            val response = api.register(RegisterRequest(email, pass, name, company))
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                NetworkClient.saveToken(context, data.accessToken)
                Result.success(
                    PhotographerUser(
                        id = data.user.id,
                        email = data.user.email,
                        fullName = data.user.fullName,
                        role = data.user.role,
                        token = data.accessToken
                    )
                )
            } else {
                Result.failure(Exception("Registration failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean {
        return NetworkClient.getToken(context) != null
    }

    fun logout() {
        NetworkClient.clearToken(context)
    }
}

class EventRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val eventDao = db.eventDao()
    private val api = NetworkClient.getApi(context)

    fun getAllEvents(): Flow<List<EventEntity>> = eventDao.getAllEvents()
    fun getActiveEvent(): Flow<EventEntity?> = eventDao.getActiveEvent()

    suspend fun refreshEvents(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getEvents()
            if (response.isSuccessful && response.body() != null) {
                val entities = response.body()!!.map {
                    EventEntity(
                        id = it.id,
                        title = it.title,
                        eventCode = it.eventCode,
                        eventDate = it.eventDate,
                        location = it.location,
                        status = it.status,
                        qrCodeUrl = it.qrCodeUrl,
                        totalPhotosCount = it.totalPhotosCount,
                        totalStorageBytes = it.totalStorageBytes,
                        isPublic = it.isPublic
                    )
                }
                eventDao.insertEvents(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch events"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createEvent(title: String, date: String, location: String?, description: String?): Result<EventEntity> = withContext(Dispatchers.IO) {
        try {
            val req = CreateEventRequest(title = title, eventDate = date, location = location, description = description)
            val response = api.createEvent(req)
            if (response.isSuccessful && response.body() != null) {
                val it = response.body()!!
                val entity = EventEntity(
                    id = it.id,
                    title = it.title,
                    eventCode = it.eventCode,
                    eventDate = it.eventDate,
                    location = it.location,
                    status = it.status,
                    qrCodeUrl = it.qrCodeUrl,
                    totalPhotosCount = it.totalPhotosCount,
                    totalStorageBytes = it.totalStorageBytes,
                    isPublic = it.isPublic
                )
                eventDao.insertEvent(entity)
                Result.success(entity)
            } else {
                // Fallback local event creation if offline
                val localCode = "EVT-${UUID.randomUUID().toString().take(6).uppercase()}"
                val fallbackEntity = EventEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    eventCode = localCode,
                    eventDate = date,
                    location = location,
                    status = "active",
                    qrCodeUrl = null
                )
                eventDao.insertEvent(fallbackEntity)
                Result.success(fallbackEntity)
            }
        } catch (e: Exception) {
            // Offline fallback
            val localCode = "EVT-${UUID.randomUUID().toString().take(6).uppercase()}"
            val fallbackEntity = EventEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                eventDate = date,
                location = location,
                eventCode = localCode,
                status = "active",
                qrCodeUrl = null
            )
            eventDao.insertEvent(fallbackEntity)
            Result.success(fallbackEntity)
        }
    }
}

class PhotoRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val photoDao = db.photoDao()

    fun getPhotosForEvent(eventId: String): Flow<List<PhotoEntity>> = photoDao.getPhotosForEvent(eventId)
    fun getAllPhotos(): Flow<List<PhotoEntity>> = photoDao.getAllPhotos()
    fun getPendingPhotos(): Flow<List<PhotoEntity>> = photoDao.getPendingPhotos()
    fun getUploadedCount(): Flow<Int> = photoDao.getUploadedPhotosCount()

    suspend fun deletePhoto(id: String) {
        photoDao.deletePhoto(id)
    }
}

class CameraRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val cameraDao = db.cameraDao()

    fun getAllCameras(): Flow<List<CameraEntity>> = cameraDao.getAllCameras()

    suspend fun saveCamera(camera: CameraEntity) {
        cameraDao.insertCamera(camera)
    }

    suspend fun setCameraConnected(id: String, isConnected: Boolean) {
        cameraDao.setCameraConnected(id, isConnected)
    }
}

