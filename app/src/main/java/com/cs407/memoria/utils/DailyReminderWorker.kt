package com.cs407.memoria.utils


import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Ensure channel exists
        NotificationHelper.createNotificationChannel(applicationContext)

        // Show the notification
        NotificationHelper.showDailyReminderNotification(applicationContext)

        return Result.success()
    }
}