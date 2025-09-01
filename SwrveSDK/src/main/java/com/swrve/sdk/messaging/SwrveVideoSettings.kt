package com.swrve.sdk.messaging

import org.json.JSONObject

data class SwrveVideoSettings (
    val autoPlay: Boolean,
    val loop: Boolean,
    val fillScreen: Boolean,
    val showControls: Boolean
) {
    constructor(json: JSONObject) : this(
        autoPlay = json.optBoolean("auto_play", true),
        loop = json.optBoolean("loop", true),
        fillScreen = json.optBoolean("fill_screen", false),
        showControls = json.optBoolean("show_controls", true)
    )
}