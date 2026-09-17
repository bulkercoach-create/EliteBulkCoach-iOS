package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import java.util.concurrent.TimeUnit

class InactivityWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Disabled per user request: No automated 3-day inactivity notifications
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "inactivity_channel"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "inactivity_check_work"

        fun scheduleInactivityCheck(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            } catch (_: Exception) {}
        }

        fun showInactivityNotification(context: Context) {
            // Disabled per user request
        }
    }
}
