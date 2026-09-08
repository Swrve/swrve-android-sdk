package com.swrve.sdk.sample.multiplepushproviders

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.swrve.sdk.SwrveNotificationConfig
import com.swrve.sdk.SwrvePushNotificationListener
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.config.SwrveConfig

/**
 * Standard Swrve initialisation — identical to PushNotificationSample. What makes this sample different is [MyFirebaseMessagingService],
 * declared in the manifest, which takes ownership of incoming FCM messages and relays Swrve's to the SDK.
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = SwrveConfig()
        // config.selectedStack = SwrveStack.EU // To use the EU stack instead, uncomment this line

        // Notifications are posted to a channel you own.
        val channel = createDefaultNotificationChannel()

        config.notificationConfig = SwrveNotificationConfig.Builder(
            R.drawable.mg_notification_icon, // small icon, shown in the status bar
            channel
        )
            .activityClass(MainActivity::class.java) // opened when the notification is tapped
            .largeIconDrawableId(R.drawable.mg_notification_icon)
            // Keep in step with the accent colour in Theme.kt, so notifications look like the app.
            .accentColorHex("#007AFF")
            .pushNotificationPermissionEvents(listOf(EVENT_NOTIFICATION_PERMISSION_REQUEST)) // Needed so the sample can obtain notification permission on Android 13+ — without it no push arrives and the relay cannot be observed.
            .build()

        // Called when the user taps a Swrve notification, with its full payload — useful for reading custom key/value pairs set on the campaign.
        config.notificationListener = SwrvePushNotificationListener { payload ->
            if (payload.has(CUSTOM_PAYLOAD_KEY)) {
                Log.d(LOG_TAG, "Received push payload ${payload.optString(CUSTOM_PAYLOAD_KEY)}")
            }
        }

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

        // Not private — MyFirebaseMessagingService reuses this channel for non-Swrve pushes.
        const val NOTIFICATION_CHANNEL_ID = "123"
        private const val CUSTOM_PAYLOAD_KEY = "custom_key"
        private const val LOG_TAG = "SwrveSample"

        const val EVENT_NOTIFICATION_PERMISSION_REQUEST = "notification_permission_request"
    }
}
