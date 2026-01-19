package com.Tomgamer.steamframetommorow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.Tomgamer.steamframetommorow.data.PreferencesRepository
import com.Tomgamer.steamframetommorow.service.SteamMonitorService
import com.Tomgamer.steamframetommorow.utils.AlarmScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Use a coroutine to check preferences and restart monitoring
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefsRepository = PreferencesRepository(context)
                    val monitoringEnabled = prefsRepository.monitoringEnabled.first()
                    val checkInterval = prefsRepository.checkIntervalMinutes.first()

                    if (monitoringEnabled) {
                        // Restart monitoring after boot
                        val alarmScheduler = AlarmScheduler(context)
                        alarmScheduler.scheduleAlarm(checkInterval)

                        // If using aggressive monitoring (1-5 minutes), restart the foreground service
                        if (checkInterval <= 5) {
                            SteamMonitorService.start(context)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
