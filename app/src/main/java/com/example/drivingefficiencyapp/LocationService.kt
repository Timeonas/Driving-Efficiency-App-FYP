package com.example.drivingefficiencyapp

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.drivingefficiencyapp.ui.StartDriveActivity

class LocationService : Service() {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "driving_tracking_channel"
        private const val NOTIFICATION_ID = 1
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create a pending intent that opens StartDriveActivity when notification is clicked
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, StartDriveActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Build a notification to inform the user that a trip is in progress
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Trip in Progress") //title of the notification
            .setContentText("Recording your trip details") //text content of the notification
            .setSmallIcon(R.drawable.ic_car) //small icon for the notification
            .setContentIntent(pendingIntent) //intent to open StartDriveActivity when the notification is clicked
            .setOngoing(true) //notification ongoing, so it cannot be dismissed by the user
            .setAutoCancel(false) //prevent the notification from being auto-cancelled when clicked
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // behavior for foreground service
            .setSilent(true) //notification silent
            .build() //build the notification

        // Start service in foreground with notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Trip Tracking",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows when a trip is being recorded"
            setShowBadge(false)
            setSound(null, null)
            enableLights(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            importance = NotificationManager.IMPORTANCE_MIN
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowBubbles(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}