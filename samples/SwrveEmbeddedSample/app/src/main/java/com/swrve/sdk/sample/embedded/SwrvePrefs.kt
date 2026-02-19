package com.swrve.sdk.sample.embedded

import android.content.Context
import android.content.SharedPreferences

object SwrvePrefs {
    const val PREFS_NAME = "SwrveSample"
    const val KEY_APP_ID = "SwrveSample_AppId"
    const val KEY_API_KEY = "SwrveSample_ApiKey"
    const val KEY_STACK = "SwrveSample_Stack"
    const val KEY_OFFLINE = "SwrveSample_OfflineMode"

    const val DEFAULT_APP_ID = 123
    const val DEFAULT_API_KEY = "api_key"
    const val DEFAULT_STACK = "us"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isOffline(prefs: SharedPreferences): Boolean =
        if (!prefs.contains(KEY_OFFLINE)) true else prefs.getBoolean(KEY_OFFLINE, false)

    fun loadAppId(prefs: SharedPreferences): Int {
        val s = prefs.getString(KEY_APP_ID, "") ?: ""
        return s.toIntOrNull() ?: DEFAULT_APP_ID
    }

    fun loadApiKey(prefs: SharedPreferences): String =
        prefs.getString(KEY_API_KEY, "")?.takeIf { it.isNotEmpty() } ?: DEFAULT_API_KEY

    fun loadStack(prefs: SharedPreferences): String {
        val s = (prefs.getString(KEY_STACK, DEFAULT_STACK) ?: DEFAULT_STACK).lowercase()
        return if (s in setOf("us", "eu", "fs")) s else DEFAULT_STACK
    }

    fun saveAll(prefs: SharedPreferences, offline: Boolean, appIdText: String, apiKeyText: String, stack: String) {
        prefs.edit()
            .putBoolean(KEY_OFFLINE, offline)
            .putString(KEY_APP_ID, appIdText.trim())
            .putString(KEY_API_KEY, apiKeyText.trim())
            .putString(KEY_STACK, stack.lowercase())
            .apply()
    }
}
