package com.swrve.sdk

import com.swrve.sdk.config.SwrveConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Apis that are callable when the sdk has not been started.
 */
class SwrveSDKNotStartedTest : SwrveBaseTest() {

    @Test
    fun testApisThatDoNotRequireStart() {
        // MANAGED mode with no autostart is the one configuration where an instance exists but the sdk has not started, which is what these apis have to tolerate.
        val config = SwrveConfig()
        config.initMode = SwrveInitMode.MANAGED
        config.isAutoStartLastUser = false
        SwrveTestUtils.createSpyInstance(config)
        assertFalse("Instance should not be started yet", SwrveSDK.isStarted())

        val version = SwrveSDK.getSdkVersion()
        assertTrue("Version should not be blank", version.isNotBlank())
        assertEquals(SwrveBase.getVersion(), version)
    }
}
