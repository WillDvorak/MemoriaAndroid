package com.cs407.memoria

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cs407.memoria.utils.PermissionHelper
import com.cs407.memoria.utils.DailyReminderScheduler
import com.cs407.memoria.utils.NotificationHelper


class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Once permission is granted, schedule the daily reminder
                DailyReminderScheduler.scheduleDailyReminder(this)
            } else {
                // User denied
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure the channel exists
        NotificationHelper.createNotificationChannel(this)

        // Ask for notification permission (and schedule daily worker)
        ensureNotificationPermissionAndSchedule()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Use NavPages which handles all navigation and screens
                    NavPages()
                }
            }
        }
    }

    private fun ensureNotificationPermissionAndSchedule() {
        if (PermissionHelper.hasNotificationPermission(this)) {
            // Already have permission → schedule (will be unique)
            DailyReminderScheduler.scheduleDailyReminder(this)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Older Android: permission is effectively granted
            DailyReminderScheduler.scheduleDailyReminder(this)
        }
    }
}