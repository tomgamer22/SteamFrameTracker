package com.Tomgamer.steamframetommorow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class TimerLauncherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Steam Frame Alert"

        android.util.Log.d("TimerLauncherReceiver", "Launching timer")

        try {
            val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_LENGTH, 1)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(timerIntent)
            android.util.Log.d("TimerLauncherReceiver", "Timer launched")

        } catch (e: Exception) {
            android.util.Log.e("TimerLauncherReceiver", "Failed to launch timer", e)
            e.printStackTrace()
        }
    }

    companion object {
        const val ACTION_LAUNCH_TIMER = "com.Tomgamer.steamframetommorow.LAUNCH_TIMER"
        const val EXTRA_MESSAGE = "extra_message"
    }
}

