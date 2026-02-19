package com.swrve.sdk.sample.embedded

import android.content.Context
import com.swrve.sdk.ISwrveCampaignManager
import com.swrve.sdk.SwrveCampaignDisplayer
import com.swrve.sdk.config.SwrveConfig
import com.swrve.sdk.config.SwrveConfigBase
import com.swrve.sdk.messaging.SwrveEmbeddedCampaign
import com.swrve.sdk.messaging.SwrveEmbeddedMessage
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.Date

class DemoCampaignsManager {

    fun getDemoCampaign(context: Context, trigger: String): SwrveEmbeddedMessage? {
        val embeddedMessages = getDemoCampaigns(context)
        for (embedded in embeddedMessages) {
            if (embedded.campaign.triggers.any { it.eventName?.contains(trigger, ignoreCase = true) == true }) {
                return embedded
            }
        }
        return null
    }

    fun getDemoCampaigns(context: Context): List<SwrveEmbeddedMessage> {
        return try {
            val jsonStr = context.assets.open("demo_campaigns.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val campaigns = root.optJSONArray("campaigns") ?: JSONArray()
            val entries = mutableListOf<SwrveEmbeddedMessage>()
            val manager = DummyCampaignManager(context)
            val displayer = SwrveCampaignDisplayer()
            for (i in 0 until campaigns.length()) {
                val campaignJson = campaigns.getJSONObject(i)
                val campaign = SwrveEmbeddedCampaign(manager, displayer, campaignJson)
                val messageData = campaignJson.optJSONObject("embedded_message") ?: continue
                val embeddedMessage = SwrveEmbeddedMessage(campaign, messageData)
                entries.add(embeddedMessage)
            }
            entries
        } catch (e: Exception) {
            Timber.tag("EmbeddedSample").e(e, "Error loading demo campaigns")
            emptyList()
        }
    }

    private class DummyCampaignManager(private val context: Context) : ISwrveCampaignManager {
        private val config: SwrveConfig = SwrveConfig()

        override fun getNow(): Date = Date()

        override fun getInitialisedTime(): Date = Date()

        override fun getCacheDir(): File = context.cacheDir

        override fun getAssetsOnDisk(): Set<String> = emptySet()

        override fun getConfig(): SwrveConfigBase = config
    }
}
