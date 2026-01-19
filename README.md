# Steam Frame Monitor

An Android app that monitors the Steam Frame availability on the Steam Store and sends notifications when it becomes available for purchase or pre-order.

## Features

- **Real-time Monitoring**: Checks Steam Store API for Steam Frame availability
- **Smart Notifications**: Get notified only when status changes to Available or Pre-order Available
- **Alarm Mode**: Launch a 1-second timer in Clock app with urgent notifications
- **Customizable Alerts**: Configure notification count (1-10) and check intervals
- **Test Mode**: Test your notification setup without waiting for real status changes
- **Background Monitoring**: Uses WorkManager for reliable background checks
- **Boot Persistence**: Automatically restarts monitoring after device reboot

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection
- Notification permissions (Android 13+)

## Permissions

- **INTERNET**: Check Steam Store API
- **POST_NOTIFICATIONS**: Send availability notifications
- **SCHEDULE_EXACT_ALARM**: Schedule precise monitoring intervals
- **RECEIVE_BOOT_COMPLETED**: Restart monitoring after reboot
- **WAKE_LOCK**: Keep device awake during checks
- **FOREGROUND_SERVICE**: Run continuous monitoring service
- **SET_ALARM**: Launch Clock app timer in alarm mode

## Usage

1. Launch the app
2. Grant required permissions
3. Enable monitoring with the toggle switch
4. Configure your preferences:
   - Notification count
   - Alarm mode (timer alerts)
   - Test mode
5. The app will monitor in the background and notify you when Steam Frame becomes available

## Configuration

### Alarm Mode
When enabled, triggers:
- 1-second timer in default Clock app
- Multiple urgent notifications with vibration
- Full-screen alerts (Android 14+)

### Test Mode
Forces status changes to test your notification setup without waiting for actual availability changes.

## Architecture

- **Kotlin** + **Jetpack Compose** for modern Android UI
- **WorkManager** for reliable background work
- **Retrofit** + **Moshi** for Steam API integration
- **DataStore** for preferences storage
- **Coroutines** for async operations

## Building

```bash
./gradlew assembleDebug
```

## License

This project is for personal use only.

