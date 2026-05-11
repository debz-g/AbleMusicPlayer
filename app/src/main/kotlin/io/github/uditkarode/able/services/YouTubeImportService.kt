package io.github.uditkarode.able.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import io.github.uditkarode.able.R
import io.github.uditkarode.able.utils.YouTubeMusicImport
import kotlin.concurrent.thread

class YouTubeImportService : Service() {
    companion object {
        private const val NOTIF_ID = 4
        private const val CHANNEL_ID = "AbleYouTubeImport"
    }

    private lateinit var builder: Notification.Builder
    private lateinit var notificationManager: NotificationManager
    @Volatile private var importThreadFinished = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        startForeground(NOTIF_ID, builder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val playlistUrl = intent?.getStringExtra("playlistUrl") ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        // Prevent concurrent imports
        if (YouTubeMusicImport.isImporting) {
            stopSelf()
            return START_NOT_STICKY
        }

        MusicService.registeredClients.forEach { it.spotifyImportChange(true) }

        thread {
            val success = YouTubeMusicImport.importPlaylist(playlistUrl, builder, this)
            importThreadFinished = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(if (success) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
            }
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        YouTubeMusicImport.cancelImport()
        if (!importThreadFinished) {
            notificationManager.cancel(NOTIF_ID)
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "YouTube Music Import",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }

        builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        builder.apply {
            setContentTitle(getString(R.string.init_import))
            setContentText(getString(R.string.pl_wait))
            setSubText("YouTube Music ${getString(R.string.imp)}")
            setSmallIcon(R.drawable.ic_download_icon)
            setOngoing(true)
            setProgress(100, 0, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
        }
    }
}
