package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class VibrationRecordingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VibroLog::VibrationRecordingWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Vibration Log"
                acquireWakeLock()
                startForegroundServiceNotification(title)
                startNotificationUpdates()
            }
            ACTION_STOP -> {
                stopRecordingAndService()
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld != true) {
                // Timeout safety of 6 hours
                wakeLock?.acquire(6 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vibration Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live vibration recording progress and metrics"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VibrationRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VibroLog: $title")
            .setContentText(content)
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop & Save", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundServiceNotification(title: String) {
        val notification = buildNotification(title, "Monitoring vibration... Screen lock supported")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startNotificationUpdates() {
        notificationJob?.cancel()
        val manager = VibrationRecorderManager.getInstance(applicationContext)

        notificationJob = serviceScope.launch {
            while (isActive) {
                delay(1200L) // Refresh every 1.2s to prevent notification spam
                val stats = manager.liveStats.value
                val elapsedSec = stats.elapsedMs / 1000
                val minStr = (elapsedSec / 60).toString().padStart(2, '0')
                val secStr = (elapsedSec % 60).toString().padStart(2, '0')

                val content = String.format(
                    Locale.US,
                    "Live: %.2f m/s² | Max: %.2f m/s² | %s:%s (%d pts)",
                    stats.currentIntensity,
                    stats.maxIntensity,
                    minStr,
                    secStr,
                    stats.sampleCount
                )

                val notification = buildNotification("Recording Active", content)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun stopRecordingAndService() {
        val manager = VibrationRecorderManager.getInstance(applicationContext)
        manager.stopRecording(applicationContext)
        releaseWakeLock()
        notificationJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseWakeLock()
        notificationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_TITLE = "extra_title"

        private const val CHANNEL_ID = "vibration_recording_channel"
        private const val NOTIFICATION_ID = 4040
    }
}
