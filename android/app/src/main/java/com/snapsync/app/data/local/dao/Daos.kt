package com.snapsync.app.data.local.dao

import androidx.room.*
import com.snapsync.app.data.local.entity.CameraEntity
import com.snapsync.app.data.local.entity.EventEntity
import com.snapsync.app.data.local.entity.PhotoEntity
import com.snapsync.app.data.local.entity.UploadJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE status = 'active' ORDER BY createdAt DESC LIMIT 1")
    fun getActiveEvent(): Flow<EventEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)
}

@Dao
interface CameraDao {
    @Query("SELECT * FROM cameras ORDER BY isConnected DESC, model ASC")
    fun getAllCameras(): Flow<List<CameraEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(camera: CameraEntity)

    @Update
    suspend fun updateCamera(camera: CameraEntity)

    @Query("UPDATE cameras SET isConnected = :connected WHERE id = :id")
    suspend fun setCameraConnected(id: String, connected: Boolean)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE eventId = :eventId ORDER BY createdAt DESC")
    fun getPhotosForEvent(eventId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY createdAt DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE sha256Hash = :hash LIMIT 1")
    suspend fun getPhotoByHash(hash: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE status IN ('QUEUED', 'UPLOADING') ORDER BY createdAt ASC")
    fun getPendingPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE status IN ('QUEUED', 'UPLOADING') ORDER BY createdAt ASC")
    suspend fun getPendingPhotosList(): List<PhotoEntity>

    @Query("SELECT COUNT(*) FROM photos WHERE eventId = :eventId")
    fun getPhotoCountForEvent(eventId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE status = 'READY'")
    fun getUploadedPhotosCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("UPDATE photos SET status = :status, uploadProgressPercent = :progress WHERE id = :id")
    suspend fun updateUploadProgress(id: String, status: String, progress: Int)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhoto(id: String)
}

@Dao
interface UploadJobDao {
    @Query("SELECT * FROM upload_jobs WHERE status IN ('PENDING', 'IN_PROGRESS') ORDER BY createdAt ASC")
    fun getActiveJobs(): Flow<List<UploadJobEntity>>

    @Query("SELECT * FROM upload_jobs WHERE status = 'FAILED' ORDER BY createdAt DESC")
    fun getFailedJobs(): Flow<List<UploadJobEntity>>

    @Query("SELECT * FROM upload_jobs WHERE photoId = :photoId LIMIT 1")
    suspend fun getJobByPhotoId(photoId: String): UploadJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: UploadJobEntity)

    @Update
    suspend fun updateJob(job: UploadJobEntity)

    @Query("UPDATE upload_jobs SET status = :status, bytesTransferred = :bytes, transferSpeedMbps = :speed WHERE id = :id")
    suspend fun updateJobProgress(id: String, status: String, bytes: Long, speed: Double)

    @Query("DELETE FROM upload_jobs WHERE status = 'COMPLETED'")
    suspend fun clearCompletedJobs()
}

