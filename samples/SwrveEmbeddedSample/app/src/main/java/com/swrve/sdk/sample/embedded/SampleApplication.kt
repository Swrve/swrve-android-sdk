package com.swrve.sdk.sample.embedded

import android.app.Application
import com.swrve.sdk.SwrveLogger
import timber.log.Timber

class SampleApplication : Application() {

    val fixedBannerFlow = SwrveIntegration.fixedBannerFlow

    override fun onCreate() {
        super.onCreate()

        SwrveLogger.useCustomLogger(true)
        Timber.plant(Timber.DebugTree())

        SwrveIntegration.init(this)
    }
}
