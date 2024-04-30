package com.disc.hdfccard

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.START_STICKY
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.content.ContextCompat.getSystemService

class MyForegroundService : Service() {

    private val notificationChannelId = "foreground_service_channel"
    private val notificationId = 1

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(notificationId, createNotification())
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Channel for foreground service notifications"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("HDFC CARD")
            .setContentText("Thank you for using our app...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon drawable
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't intend to bind to this service from other components
    }
}
