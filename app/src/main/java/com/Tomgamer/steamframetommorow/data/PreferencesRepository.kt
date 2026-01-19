package com.Tomgamer.steamframetommorow.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "steam_frame_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        private val LAST_STATUS_KEY = stringPreferencesKey("last_status")
        private val LAST_CHECK_TIME_KEY = longPreferencesKey("last_check_time")
        private val MONITORING_ENABLED_KEY = stringPreferencesKey("monitoring_enabled")
        private val CHECK_FREQUENCY_KEY = longPreferencesKey("check_frequency_minutes")
        private val NOTIFICATION_COUNT_KEY = intPreferencesKey("notification_count")
        private val CHECK_INTERVAL_MINUTES_KEY = longPreferencesKey("check_interval_minutes")
        private val TEST_MODE_KEY = stringPreferencesKey("test_mode_enabled")
        private val USE_ALARM_MODE_KEY = stringPreferencesKey("use_alarm_mode")
    }

    val lastStatus: Flow<ProductStatus> = context.dataStore.data.map { prefs ->
        val statusString = prefs[LAST_STATUS_KEY] ?: "UNKNOWN"
        ProductStatus.fromStorageString(statusString)
    }

    val lastCheckTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_CHECK_TIME_KEY] ?: 0L
    }

    val monitoringEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[MONITORING_ENABLED_KEY] != "false"
    }

    val checkFrequencyMinutes: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[CHECK_FREQUENCY_KEY] ?: 30L
    }

    val notificationCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATION_COUNT_KEY] ?: 10
    }

    val checkIntervalMinutes: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[CHECK_INTERVAL_MINUTES_KEY] ?: 1L // Default to 1 minute for aggressive monitoring
    }

    val testModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[TEST_MODE_KEY] == "true"
    }

    val useAlarmMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[USE_ALARM_MODE_KEY] != "false" // Default to true (alarm mode)
    }

    suspend fun saveLastStatus(status: ProductStatus) {
        context.dataStore.edit { prefs ->
            prefs[LAST_STATUS_KEY] = status.toStorageString()
        }
    }

    suspend fun saveLastCheckTime(timeMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_CHECK_TIME_KEY] = timeMillis
        }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[MONITORING_ENABLED_KEY] = enabled.toString()
        }
    }

    suspend fun setCheckFrequency(minutes: Long) {
        context.dataStore.edit { prefs ->
            prefs[CHECK_FREQUENCY_KEY] = minutes
        }
    }

    suspend fun setNotificationCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATION_COUNT_KEY] = count
        }
    }

    suspend fun setCheckInterval(minutes: Long) {
        context.dataStore.edit { prefs ->
            prefs[CHECK_INTERVAL_MINUTES_KEY] = minutes
        }
    }

    suspend fun setTestModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[TEST_MODE_KEY] = enabled.toString()
        }
    }

    suspend fun setUseAlarmMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_ALARM_MODE_KEY] = enabled.toString()
        }
    }
}
