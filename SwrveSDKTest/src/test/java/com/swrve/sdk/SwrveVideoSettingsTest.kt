package com.swrve.sdk

import com.swrve.sdk.messaging.SwrveVideoSettings
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SwrveVideoSettingsTest : SwrveBaseTest() {

    @Test
    fun testInitializerWithJSON() {
        val json = JSONObject()
            .put("auto_play", false)
            .put("loop", true)
            .put("fill_screen", true)
            .put("show_controls", false)

        val settings = SwrveVideoSettings(json)

        assertFalse("autoPlay should be false from JSON", settings.autoPlay)
        assertTrue("loop should be true from JSON", settings.loop)
        assertTrue("fillScreen should be true from JSON", settings.fillScreen)
        assertFalse("showControls should be false from JSON", settings.showControls)
    }

    @Test
    fun testInitializerWithPartialJSON() {
        val json = JSONObject()
            .put("auto_play", false)

        val settings = SwrveVideoSettings(json)

        assertFalse("autoPlay should be false from JSON", settings.autoPlay)
        assertTrue("loop should remain the default value of true", settings.loop)
        assertFalse("fillScreen should remain the default value of false", settings.fillScreen)
        assertTrue("showControls should remain the default value of true", settings.showControls)
    }
}
