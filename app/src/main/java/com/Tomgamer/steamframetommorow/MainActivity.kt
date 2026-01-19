package com.Tomgamer.steamframetommorow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.*
import com.Tomgamer.steamframetommorow.data.PreferencesRepository
import com.Tomgamer.steamframetommorow.data.ProductStatus
import com.Tomgamer.steamframetommorow.data.SteamApi
import com.Tomgamer.steamframetommorow.notifications.NotificationHelper
import com.Tomgamer.steamframetommorow.ui.theme.SteamFrameTommorowTheme
import com.Tomgamer.steamframetommorow.utils.AlarmScheduler
import com.Tomgamer.steamframetommorow.worker.SteamCheckWorker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var prefsRepository: PreferencesRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var alarmScheduler: AlarmScheduler

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Default test notification
            notificationHelper.sendTestNotification(1, false, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsRepository = PreferencesRepository(this)
        notificationHelper = NotificationHelper(this)
        alarmScheduler = AlarmScheduler(this)

        enableEdgeToEdge()
        setContent {
            SteamFrameTommorowTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        prefsRepository = prefsRepository,
                        onCheckNow = { checkNow() },
                        onToggleMonitoring = { enabled, interval -> toggleMonitoring(enabled, interval) },
                        onRequestPermission = { count, useAlarm -> requestNotificationPermission(count, useAlarm) }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission(count: Int = 1, useAlarmMode: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    notificationHelper.sendTestNotification(count, useAlarmMode, 10)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            notificationHelper.sendTestNotification(count, useAlarmMode, 10)
        }
    }

    private fun checkNow() {
        val workRequest = OneTimeWorkRequestBuilder<SteamCheckWorker>()
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun toggleMonitoring(enabled: Boolean, intervalMinutes: Long) {
        if (enabled) {
            alarmScheduler.scheduleAlarm(intervalMinutes)
        } else {
            alarmScheduler.cancelAlarm()
        }
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    prefsRepository: PreferencesRepository,
    onCheckNow: () -> Unit,
    onToggleMonitoring: (Boolean, Long) -> Unit,
    onRequestPermission: (Int, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val lastStatus by prefsRepository.lastStatus.collectAsState(initial = ProductStatus.Unknown)
    val lastCheckTime by prefsRepository.lastCheckTime.collectAsState(initial = 0L)
    val monitoringEnabled by prefsRepository.monitoringEnabled.collectAsState(initial = false)
    val notificationCount by prefsRepository.notificationCount.collectAsState(initial = 10)
    val checkInterval by prefsRepository.checkIntervalMinutes.collectAsState(initial = 15L)
    val testModeEnabled by prefsRepository.testModeEnabled.collectAsState(initial = false)
    val useAlarmMode by prefsRepository.useAlarmMode.collectAsState(initial = true)

    var notificationCountInput by remember { mutableStateOf(notificationCount.toString()) }
    var checkIntervalInput by remember { mutableStateOf(checkInterval.toString()) }
    var isChecking by remember { mutableStateOf(false) }
    var currentStatus by remember { mutableStateOf<ProductStatus>(ProductStatus.Unknown) }
    var lastCheckResult by remember { mutableStateOf("") }

    LaunchedEffect(lastStatus) {
        currentStatus = lastStatus
    }

    LaunchedEffect(notificationCount) {
        notificationCountInput = notificationCount.toString()
    }

    LaunchedEffect(checkInterval) {
        checkIntervalInput = checkInterval.toString()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Steam Frame Tommorow",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Hopium",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Made by tomgamer_",
            style = MaterialTheme.typography.bodySmall
            //style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center)

        )

        Spacer(modifier = Modifier.height(8.dp))

        // Current Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (currentStatus) {
                    is ProductStatus.Available, is ProductStatus.PreorderAvailable ->
                        MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Current Status",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentStatus.toDisplayString(),
                    style = MaterialTheme.typography.headlineSmall
                )
                if (lastCheckTime > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last checked: ${formatTime(lastCheckTime)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (lastCheckResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastCheckResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        HorizontalDivider()

        // Monitoring Toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Monitoring",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = monitoringEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                prefsRepository.setMonitoringEnabled(enabled)
                                val interval = checkIntervalInput.toLongOrNull() ?: 1L // Default to 1 minute
                                onToggleMonitoring(enabled, interval)
                            }
                        }
                    )
                }
                if (monitoringEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "checks every $checkIntervalInput minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Alarm Mode Toggle
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use Alarm Mode",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (useAlarmMode) "Spam notifications + Alarm" else "Simple notifications only",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useAlarmMode,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                prefsRepository.setUseAlarmMode(enabled)
                            }
                        }
                    )
                }
                if (useAlarmMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Check Interval Setting
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Check Interval (minutes)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = checkIntervalInput,
                    onValueChange = { newValue ->
                        checkIntervalInput = newValue
                        val interval = newValue.toLongOrNull()
                        if (interval != null && interval > 0) {
                            scope.launch {
                                prefsRepository.setCheckInterval(interval)
                                if (monitoringEnabled) {
                                    onToggleMonitoring(true, interval)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text("1") }
                )
                Spacer(modifier = Modifier.height(4.dp))

                val intervalValue = checkIntervalInput.toLongOrNull() ?: 1L
                when {
                    intervalValue <= 1 -> {
                        Text(
                            text = "Continuous background service + 1-minute checks (High battery usage!)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        Text(
                            text = "Standard ${intervalValue}-minute intervals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Notification Count Setting
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Number of Notifications",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notificationCountInput,
                    onValueChange = { newValue ->
                        notificationCountInput = newValue
                        val count = newValue.toIntOrNull()
                        if (count != null && count > 0) {
                            scope.launch {
                                prefsRepository.setNotificationCount(count)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text("10") }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "How many notifications to send when Steam Frame becomes available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // Action Buttons
        Button(
            onClick = {
                isChecking = true
                lastCheckResult = "Checking API..."
                scope.launch {
                    val newStatus = SteamApi.checkSteamFrameAvailability(testModeEnabled)
                    currentStatus = newStatus
                    prefsRepository.saveLastStatus(newStatus)
                    prefsRepository.saveLastCheckTime(System.currentTimeMillis())
                    lastCheckResult = "API returned: ${newStatus.toDisplayString()}"
                    isChecking = false
                }
                onCheckNow()
            },
            enabled = !isChecking,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isChecking) "Checking..." else "Check Now (Manual Test)")
        }

        OutlinedButton(
            onClick = {
                val count = notificationCountInput.toIntOrNull() ?: 1
                onRequestPermission(count, useAlarmMode)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Notification (10s Delay)")
        }

        Spacer(modifier = Modifier.height(8.dp))

    }
}

private fun formatTime(timeMillis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}
