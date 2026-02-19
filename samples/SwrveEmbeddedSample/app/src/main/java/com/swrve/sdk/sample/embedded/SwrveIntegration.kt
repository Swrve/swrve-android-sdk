package com.swrve.sdk.sample.embedded

import android.app.Application
import android.content.Context
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.config.SwrveConfig
import com.swrve.sdk.config.SwrveEmbeddedMessageConfig
import com.swrve.sdk.config.SwrveStack
import com.swrve.sdk.messaging.SwrveEmbeddedListener
import com.swrve.sdk.messaging.SwrveEmbeddedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL

object SwrveIntegration {

    val fixedBannerFlow = MutableSharedFlow<Map<String, Any?>>(replay = 0, extraBufferCapacity = 1)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private const val EMBEDDED_SCHEMA_VERSION = 1

    fun init(application: Application) {

        val embeddedListener = SwrveEmbeddedListener { _: Context?, embeddedMessage: SwrveEmbeddedMessage, personalizationProperties: MutableMap<String, String>, isControl: Boolean ->
            if (isControl) {
                SwrveSDK.embeddedControlMessageImpressionEvent(embeddedMessage)
                return@SwrveEmbeddedListener
            }

            val isValidBanner = try {
                val obj = JSONObject(embeddedMessage.data ?: "{}")
                val version = obj.optInt("version", 1)
                if (version > EMBEDDED_SCHEMA_VERSION) {
                    return@SwrveEmbeddedListener
                }
                obj.optString("type").equals("banner", ignoreCase = true)
            } catch (_: Exception) {
                false
            }
            if (isValidBanner) {
                val data = SwrveSDK.getPersonalizedEmbeddedMessageData(embeddedMessage, personalizationProperties)
                appScope.launch {
                    fixedBannerFlow.emit(
                        mapOf(
                            "message" to embeddedMessage, "data" to data
                        )
                    )
                }
            }
        }

        val embeddedMessageConfig = SwrveEmbeddedMessageConfig.Builder().embeddedListener(embeddedListener).build()
        val config = SwrveConfig()
        config.embeddedMessageConfig = embeddedMessageConfig

        val prefs = SwrvePrefs.prefs(application)
        val offlineMode = SwrvePrefs.isOffline(prefs)
        val stack = SwrvePrefs.loadStack(prefs)
        when (stack) {
            "eu" -> config.selectedStack = SwrveStack.EU
            "us" -> config.selectedStack = SwrveStack.US
            else -> {
                config.setContentUrl(URL("https://us-fs26-content.swrve.com"))
                config.setEventsUrl(URL("https://us-fs26-api.swrve.com"))
                config.setIdentityUrl(URL("https://us-fs26-identity.swrve.com"))
            }
        }

        val appId = if (offlineMode) SwrvePrefs.DEFAULT_APP_ID else SwrvePrefs.loadAppId(prefs)
        val apiKey = if (offlineMode) SwrvePrefs.DEFAULT_API_KEY else SwrvePrefs.loadApiKey(prefs)

        SwrveSDK.createInstance(application, appId, apiKey, config)
    }
}
