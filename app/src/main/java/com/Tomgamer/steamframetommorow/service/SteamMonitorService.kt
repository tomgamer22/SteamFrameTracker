package com.Tomgamer.steamframetommorow.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.Tomgamer.steamframetommorow.MainActivity
import com.Tomgamer.steamframetommorow.utils.NetworkUtils
import com.Tomgamer.steamframetommorow.worker.SteamCheckWorker
import kotlinx.coroutines.*

class SteamMonitorService : Service() {

    companion object {
        private const val TAG = "SteamMonitorService"
        private const val SERVICE_ID = 2001
        private const val CHANNEL_ID = "steam_monitor_service"
        private const val CHANNEL_NAME = "Steam Frame Monitor"

        fun start(context: Context) {
            val intent = Intent(context, SteamMonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, SteamMonitorService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var serviceScope: CoroutineScope
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(SERVICE_ID, createNotification())
        startMonitoring()

        // Return START_STICKY so the service is restarted if killed by system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }

    private fun startMonitoring() {
        stopMonitoring() // Cancel any existing job

        monitorJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Check internet connection
                    val hasInternet = NetworkUtils.isInternetAvailable(this@SteamMonitorService)
                    val connectionType = NetworkUtils.getConnectionType(this@SteamMonitorService)

                    if (hasInternet) {
                        Log.d(TAG, "Internet available ($connectionType) - scheduling check")

                        // Schedule a Steam check using WorkManager
                        val workRequest = OneTimeWorkRequestBuilder<SteamCheckWorker>()
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build()
                            )
                            .build()

                        WorkManager.getInstance(this@SteamMonitorService)
                            .enqueue(workRequest)

                        // Update notification with last check time
                        updateNotification(connectionType, true)
                    } else {
                        Log.w(TAG, "No internet connection - waiting for network")
                        // Update notification to show waiting for network
                        updateNotification(connectionType, false)
                    }

                    // Wait 1 minute before next check
                    delay(60_000) // 1 minute = 60,000 milliseconds

                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitoring loop", e)
                    // If something fails, wait and try again
                    delay(30_000) // Wait 30 seconds on error
                }
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW // Low importance = less intrusive
        ).apply {
            description = "Background Steam Frame monitoring"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Steam Frame Monitor Running")
        .setContentText("Checking Steam every minute for availability")
        .setSmallIcon(android.R.drawable.ic_menu_search)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true) // Cannot be dismissed
        .setContentIntent(createContentPendingIntent())
        .addAction(createStopAction())
        .build()

    private fun updateNotification(connectionType: String = "Unknown", hasInternet: Boolean = true) {
        val statusText = if (hasInternet) {
            "Last check: ${getCurrentTime()} via $connectionType • Next in 1 min"
        } else {
            " Waiting for internet connection ($connectionType)"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Steam Frame Monitor Running")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(createContentPendingIntent())
            .addAction(createStopAction())
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SERVICE_ID, notification)
    }

    private fun createContentPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(this, SteamMonitorService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_pause,
            "Stop",
            stopPendingIntent
        ).build()
    }

    private fun getCurrentTime(): String {
        val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return formatter.format(java.util.Date())
    }
}
