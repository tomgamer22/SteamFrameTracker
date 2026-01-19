package com.Tomgamer.steamframetommorow.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.Tomgamer.steamframetommorow.AlarmActivity
import com.Tomgamer.steamframetommorow.data.ProductStatus

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "steam_frame_availability"
        private const val CHANNEL_NAME = "Steam Frame Availability"
        private const val CHANNEL_DESCRIPTION = "Notifications for Steam Frame availability"
        private const val NOTIFICATION_ID = 1001
        private const val STEAM_STORE_URL = "https://store.steampowered.com/sale/steamframe"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
            enableLights(true)
            setSound(
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI,
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setBypassDnd(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun sendStatusChangeNotification(newStatus: ProductStatus, count: Int = 1, useAlarmMode: Boolean = true) {
        val title = when (newStatus) {
            is ProductStatus.PreorderAvailable -> "Steam Frame Pre-order Available!"
            is ProductStatus.Available -> "Steam Frame Available Now!"
            is ProductStatus.SoldOut -> "Steam Frame Sold Out"
            is ProductStatus.NotAvailable -> "Steam Frame Status Changed"
            is ProductStatus.Unknown -> "Steam Frame Status Update"
        }

        if (useAlarmMode) {
            sendBackupNotifications(newStatus, count)
            triggerSystemTimer(title)
        } else {
            sendBackupNotifications(newStatus, count)
        }
    }

    private fun triggerSystemTimer(message: String) {
        try {
            android.util.Log.d("NotificationHelper", "Launching timer")

            val timerBroadcast = Intent("com.Tomgamer.steamframetommorow.LAUNCH_TIMER").apply {
                setPackage(context.packageName)
                putExtra("extra_message", message)
            }
            context.sendBroadcast(timerBroadcast)
            android.util.Log.d("NotificationHelper", "Timer broadcast sent")

            Thread.sleep(100)
            sendUrgentNotification(message)

        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Timer launch failed", e)
            e.printStackTrace()
            sendUrgentNotification(message)
        }
    }

    private fun sendUrgentNotification(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(STEAM_STORE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create full screen intent for alarm-like behavior
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 URGENT: $message")
            .setContentText("Tap to open Steam Store immediately!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\nTap to visit the Steam Store now!"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            .setFullScreenIntent(fullScreenIntent, true)

        val notification = notificationBuilder.build().apply {
            flags = flags or android.app.Notification.FLAG_INSISTENT
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 999, notification)
            android.util.Log.d("NotificationHelper", "Urgent notification sent")

            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            repeat(2) { index ->
                handler.postDelayed({
                    try {
                        val followUpNotification = notificationBuilder.build().apply {
                            flags = flags or android.app.Notification.FLAG_INSISTENT
                        }
                        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1000 + index, followUpNotification)
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }
                }, (index + 1) * 5000L)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun sendBackupNotifications(newStatus: ProductStatus, count: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val title = when (newStatus) {
            is ProductStatus.PreorderAvailable -> "Steam Frame Pre-order Available!"
            is ProductStatus.Available -> "Steam Frame Available Now!"
            is ProductStatus.SoldOut -> "Steam Frame Sold Out"
            is ProductStatus.NotAvailable -> "Steam Frame Status Changed"
            is ProductStatus.Unknown -> "Steam Frame Status Update"
        }

        val message = when (newStatus) {
            is ProductStatus.PreorderAvailable -> "The Steam Frame is now available for pre-order! Tap to visit the store."
            is ProductStatus.Available -> "The Steam Frame is now available to purchase! Tap to visit the store."
            is ProductStatus.SoldOut -> "The Steam Frame has sold out."
            is ProductStatus.NotAvailable -> "The Steam Frame is currently not available."
            is ProductStatus.Unknown -> "Unable to determine Steam Frame availability."
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(STEAM_STORE_URL))
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        repeat(count) { index ->
            handler.postDelayed({
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText("$message (${index + 1}/$count)")
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setVibrate(longArrayOf(0, 500, 250, 500))
                    .build()

                try {
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + index, notification)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }, index * 800L)
        }
    }

    fun sendTestNotification(count: Int = 1, useAlarmMode: Boolean = false, delaySeconds: Int = 0) {
        if (delaySeconds > 0) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                triggerTestNotification(count, useAlarmMode)
            }, delaySeconds * 1000L)
        } else {
            triggerTestNotification(count, useAlarmMode)
        }
    }

    private fun triggerTestNotification(count: Int, useAlarmMode: Boolean) {
        if (useAlarmMode) {
            sendTestNotifications(count)
            triggerSystemTimer("TEST: Steam Frame Available!")
        } else {
            sendTestNotifications(count)
        }
    }

    private fun sendTestNotifications(count: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        // Send multiple test notifications
        repeat(count) { index ->
            handler.postDelayed({
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("TEST: Steam Frame Available")
                    .setContentText("This is a test notification (${index + 1}/$count).")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                try {
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + index, notification)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }, index * 1000L)
        }
    }
}
