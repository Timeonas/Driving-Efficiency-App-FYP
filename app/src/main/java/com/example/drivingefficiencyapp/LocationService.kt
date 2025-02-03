package com.example.drivingefficiencyapp.location

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.drivingefficiencyapp.R
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

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Tracking Drive")
            .setContentText("Recording your trip details...")
            .setSmallIcon(R.drawable.ic_car)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

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
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Drive Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when a trip is being recorded"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}