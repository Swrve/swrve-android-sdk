package com.swrve.sdk.sample.pushinbox

import android.app.Application
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.config.SwrveConfig

/**
 * The inbox needs no push setup — creating the SDK is the whole integration.
 *
 * That is worth understanding rather than working around: inbox messages arrive in the same content response as campaigns, not in the notification.
 * So the list populates whether or not this app can receive push, which is why there is no FCM configuration, no permission prompt and no service here.
 * See PushNotificationSample for the push side.
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = SwrveConfig()
        // config.selectedStack = SwrveStack.EU // To use the EU stack instead, uncomment this line

        SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config)

        // Fires when the inbox is first loaded and whenever it changes, including when a content refresh brings new messages in while the app is open.
        // Registered here rather than in a screen, because the inbox is app state.
        SwrveSDK.setPushInboxUpdateListener(InboxStore)
    }

    companion object {
        // Replace with your Swrve app ID and API key, from Swrve dashboard > Settings > Integration settings.
        private const val YOUR_APP_ID = 0
        private const val YOUR_API_KEY = "YOUR_API_KEY"
    }
}
