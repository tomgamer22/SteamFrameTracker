package com.Tomgamer.steamframetommorow.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.Tomgamer.steamframetommorow.data.PreferencesRepository
import com.Tomgamer.steamframetommorow.data.ProductStatus
import com.Tomgamer.steamframetommorow.data.SteamApi
import com.Tomgamer.steamframetommorow.notifications.NotificationHelper
import kotlinx.coroutines.flow.first

class SteamCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val prefsRepository = PreferencesRepository(context)
    private val notificationHelper = NotificationHelper(context)

    override suspend fun doWork(): Result {
        return try {
            val monitoringEnabled = prefsRepository.monitoringEnabled.first()
            if (!monitoringEnabled) {
                return Result.success()
            }

            val testMode = prefsRepository.testModeEnabled.first()
            val currentStatus = SteamApi.checkSteamFrameAvailability(testMode)
            val lastStatus = prefsRepository.lastStatus.first()

            prefsRepository.saveLastCheckTime(System.currentTimeMillis())

            if (hasSignificantChange(lastStatus, currentStatus)) {
                val notificationCount = prefsRepository.notificationCount.first()
                val useAlarmMode = prefsRepository.useAlarmMode.first()

                notificationHelper.sendStatusChangeNotification(currentStatus, notificationCount, useAlarmMode)
                prefsRepository.saveLastStatus(currentStatus)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun hasSignificantChange(oldStatus: ProductStatus, newStatus: ProductStatus): Boolean {
        return when {
            oldStatus !is ProductStatus.PreorderAvailable && newStatus is ProductStatus.PreorderAvailable -> true
            oldStatus !is ProductStatus.Available && newStatus is ProductStatus.Available -> true
            else -> false
        }
    }
}

