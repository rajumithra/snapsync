package com.snapsync.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.snapsync.app.data.local.dao.CameraDao
import com.snapsync.app.data.local.dao.EventDao
import com.snapsync.app.data.local.dao.PhotoDao
import com.snapsync.app.data.local.dao.UploadJobDao
import com.snapsync.app.data.local.entity.CameraEntity
import com.snapsync.app.data.local.entity.EventEntity
import com.snapsync.app.data.local.entity.PhotoEntity
import com.snapsync.app.data.local.entity.UploadJobEntity

@Database(
    entities = [
        EventEntity::class,
        CameraEntity::class,
        PhotoEntity::class,
        UploadJobEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun cameraDao(): CameraDao
    abstract fun photoDao(): PhotoDao
    abstract fun uploadJobDao(): UploadJobDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "snapsync.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

