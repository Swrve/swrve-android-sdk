package com.swrve.sdk.sample.pushnotification

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.swrve.sdk.SwrveNotificationConfig
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.config.SwrveConfig

/**
 * Initialises the Swrve SDK. This is the whole integration — the SDK must be created once, here in Application.onCreate, before anything else touches it.
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = SwrveConfig()
        // config.selectedStack = SwrveStack.EU // To use the EU stack instead, uncomment this line

        // Notifications are posted to a channel you own.
        val channel = createDefaultNotificationChannel()

        val notificationConfig = SwrveNotificationConfig.Builder(
            R.drawable.mg_notification_icon, // small icon, shown in the status bar
            channel
        )
            .activityClass(MainActivity::class.java) // opened when the notification is tapped
            .largeIconDrawableId(R.drawable.mg_notification_icon)
            // Keep in step with the accent colour in Theme.kt, so notifications look like the app.
            .accentColorHex("#007AFF")
            // Events listed here trigger the OS notification permission prompt on Android 13+. Send one when your UI has explained why you need it.
            .pushNotificationPermissionEvents(listOf(EVENT_NOTIFICATION_PERMISSION_REQUEST))
            .build()
        config.notificationConfig = notificationConfig

        SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config)
    }

    /**
     * Notification channels only exist from Android 8 (API 26). Below that there is nothing to create — return null and the SDK posts notifications without a channel.
     */
    private fun createDefaultNotificationChannel(): NotificationChannel? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Swrve default channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        return channel
    }

    companion object {
        // Replace with your Swrve app ID and API key, from Swrve dashboard > Settings > Integration settings.
        private const val YOUR_APP_ID = 0
        private const val YOUR_API_KEY = "YOUR_API_KEY"

        private const val NOTIFICATION_CHANNEL_ID = "123"

        const val EVENT_NOTIFICATION_PERMISSION_REQUEST = "notification_permission_request"
    }
}
