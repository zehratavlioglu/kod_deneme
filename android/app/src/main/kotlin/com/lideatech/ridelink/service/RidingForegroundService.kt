package com.lideatech.ridelink.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lideatech.ridelink.MainActivity
import com.lideatech.ridelink.core.RideLinkRuntime

class RidingForegroundService : Service() {
    private val runtime by lazy { RideLinkRuntime.initialize(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopRidingMode()
            ACTION_START -> {
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= 29) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                val debugPhoneAudio = intent.getBooleanExtra(EXTRA_DEBUG_PHONE_AUDIO, false)
                runCatching { runtime.start(debugPhoneAudio) }
                    .onFailure {
                        runtime.stop()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runtime.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopRidingMode() {
        runtime.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, RidingForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("RideLink sürüş modu")
            .setContentText("Yakındaki sürücüler ve mikrofon oturumu etkin")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, "Durdur", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RideLink sürüş modu",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Aktif Nearby bağlantısı ve mikrofon kullanımını gösterir"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.lideatech.ridelink.action.START"
        const val ACTION_STOP = "com.lideatech.ridelink.action.STOP"
        const val EXTRA_DEBUG_PHONE_AUDIO = "debug_phone_audio"
        private const val CHANNEL_ID = "riding_session"
        private const val NOTIFICATION_ID = 4101
    }
}
