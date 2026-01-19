package com.Tomgamer.steamframetommorow.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.Tomgamer.steamframetommorow.receiver.AlarmReceiver
import com.Tomgamer.steamframetommorow.service.SteamMonitorService

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(intervalMinutes: Long) {
        // If user wants very frequent checks (1-5 minutes), use foreground service
        if (intervalMinutes <= 5) {
            scheduleAggressiveMonitoring(intervalMinutes)
        } else {
            scheduleNormalAlarms(intervalMinutes)
        }
    }

    private fun scheduleAggressiveMonitoring(intervalMinutes: Long) {
        // Start foreground service for continuous monitoring
        SteamMonitorService.start(context)

        // ALSO schedule backup alarms every minute as redundancy
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = intervalMinutes * 60 * 1000

        // Cancel any existing alarms first
        alarmManager.cancel(pendingIntent)

        // Schedule exact repeating alarms every minute
        if (alarmManager.canScheduleExactAlarms()) {
            // For 1-minute intervals, schedule multiple overlapping alarms to ensure reliability
            for (i in 0 until 3) {
                val staggeredIntent = Intent(context, AlarmReceiver::class.java)
                val staggeredPendingIntent = PendingIntent.getBroadcast(
                    context,
                    i, // Different request code for each alarm
                    staggeredIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + intervalMillis + (i * 20000), // Stagger by 20 seconds
                    staggeredPendingIntent
                )
            }
        } else {
            // Fallback to inexact alarm
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, // Minimum Android allows
                pendingIntent
            )
        }
    }

    private fun scheduleNormalAlarms(intervalMinutes: Long) {
        // Stop foreground service if it's running
        SteamMonitorService.stop(context)

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMillis = intervalMinutes * 60 * 1000

        // Cancel any existing alarms first
        cancelAlarm()

        // Schedule repeating alarm
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                intervalMillis,
                pendingIntent
            )
        } else {
            // Fallback to inexact alarm
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                intervalMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm() {
        // Stop foreground service
        SteamMonitorService.stop(context)

        // Cancel all alarm variations
        for (i in 0 until 3) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

