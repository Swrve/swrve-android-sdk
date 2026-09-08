package com.swrve.sdk.sample.messagecenter

import android.app.Application
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.config.SwrveConfig

/**
 * Message Center needs no push setup — creating the SDK is the whole integration. Campaigns marked
 * as Message Center in the dashboard are then available through the APIs used in [MainActivity].
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = SwrveConfig()
        // config.selectedStack = SwrveStack.EU // To use the EU stack instead, uncomment this line

        SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config)
    }

    companion object {
        // Replace with your Swrve app ID and API key, from Swrve dashboard > Settings > Integration settings.
        private const val YOUR_APP_ID = 0
        private const val YOUR_API_KEY = "YOUR_API_KEY"
    }
}
