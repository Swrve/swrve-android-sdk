package com.swrve.sdk.sample.embedded

import androidx.compose.ui.graphics.Color

fun hexToColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        val cleaned = hex.removePrefix("#")
        val parsed = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color((0xFF000000 or parsed).toInt())
            8 -> Color(parsed.toInt())
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
