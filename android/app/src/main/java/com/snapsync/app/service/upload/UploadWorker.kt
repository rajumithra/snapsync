package com.snapsync.app.service.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.snapsync.app.data.local.AppDatabase

class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uploadEngine = UploadEngine(applicationContext)
        uploadEngine.start()
        // Run until queue is empty or timeout
        kotlinx.coroutines.delay(5000)
        uploadEngine.stop()
        return Result.success()
    }
}

