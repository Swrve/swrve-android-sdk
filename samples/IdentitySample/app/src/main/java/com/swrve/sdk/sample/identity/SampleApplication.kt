package com.swrve.sdk.sample.identity

import android.app.Application
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.config.SwrveConfig

/**
 * The one line that shapes this whole sample integration is [SwrveConfig.setAutoStartLastUser].
 */
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = SwrveConfig()
        // config.selectedStack = SwrveStack.EU // To use the EU stack instead, uncomment this line

        // autoStartLastUser==false: nothing is tracked until start() or identify() is called, so this app sends nothing at all until the user signs in. See MainActivity for how the UI waits.
        // autoStartLastUser==true: The default is true, the SDK tracks anonymously from launch. What happens to that activity on identify() depends on the user — if the ID is new to Swrve the anonymous
        // user becomes the identified one and its activity carries over; if Swrve already knows the ID, the SDK switches to that existing user and the anonymous activity stays behind.
        // Flip this line to try it; the login flow is unchanged either way.
        config.setAutoStartLastUser(false)

        SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config)
    }

    companion object {
        // Replace with your Swrve app ID and API key, from Swrve dashboard > Settings > Integration settings.
        private const val YOUR_APP_ID = 0
        private const val YOUR_API_KEY = "YOUR_API_KEY"

        // Sent either side of identify() so the change in user attribution is visible in Swrve.
        const val EVENT_BEFORE_IDENTIFY = "sample.before_identify"
        const val EVENT_AFTER_IDENTIFY = "sample.after_identify"
    }
}
