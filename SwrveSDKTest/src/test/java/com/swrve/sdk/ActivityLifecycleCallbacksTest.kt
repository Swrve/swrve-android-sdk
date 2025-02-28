package com.swrve.sdk

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.mockito.Mockito

class ActivityLifecycleCallbacksTest : SwrveBaseTest() {
    @Test
    fun testCreateInstance() {
        val applicationSpy = Mockito.spy(ApplicationProvider.getApplicationContext<Application>())
        val swrve = SwrveSDK.createInstance(applicationSpy, 1, "apiKey") as Swrve
        Mockito.verify(applicationSpy).registerActivityLifecycleCallbacks(swrve)
    }
}