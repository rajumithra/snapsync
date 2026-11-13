package com.snapsync.app

import android.app.Application
import android.util.Log
import com.snapsync.app.data.local.AppDatabase

class SnapSyncApp : Application() {
    val TAG = "SnapSyncApp"

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing SnapSync Live Photo Relay Engine...")

        // Warm up local database
        AppDatabase.getInstance(this)
    }
}

