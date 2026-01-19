package com.Tomgamer.steamframetommorow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.Tomgamer.steamframetommorow.worker.SteamCheckWorker

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Trigger a work request to check Steam API
        val workRequest = OneTimeWorkRequestBuilder<SteamCheckWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

