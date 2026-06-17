package com.swrve.sdk.messaging

import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.ISwrveCampaignManager
import com.swrve.sdk.SwrveAssetsQueueItem
import com.swrve.sdk.SwrveBaseTest
import com.swrve.sdk.SwrveCampaignDisplayer
import com.swrve.sdk.SwrveFreemarkerEvaluator
import com.swrve.sdk.SwrveHelper
import com.swrve.sdk.SwrveTestUtils
import com.swrve.sdk.config.SwrveConfig
import com.swrve.sdk.config.SwrveConfigBase
import com.swrve.sdk.messaging.SwrveBaseCampaign.SwrveTimezoneType
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.Date
import java.util.TimeZone

class SwrveBaseCampaignUnitTest : SwrveBaseTest() {
    private var dummyICampaignManager: ISwrveCampaignManager? = null
    private var nowDate: Date? = null

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        nowDate = Date()
        dummyICampaignManager = object : ISwrveCampaignManager {
            override fun getNow(): Date {
                return nowDate as Date
            }

            override fun getInitialisedTime(): Date {
                return nowDate as Date
            }

            override fun getCacheDir(): File {
                return File("cache")
            }

            override fun getAssetsOnDisk(): Set<String> {
                return HashSet()
            }

            override fun getConfig(): SwrveConfigBase {
                return SwrveConfig()
            }
        }
    }

    @Test
    @Throws(JSONException::class)
    fun testFreemarkerEnabledParsedFromJSON() {
        val campaignData = JSONObject().apply {
            put("id", 1)
            put("freemarker_enabled", true)
        }
        val campaign = SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), campaignData, HashSet(), null)
        assertTrue(campaign.isFreemarkerEnabled)
    }

    @Test
    @Throws(JSONException::class)
    fun testFreemarkerEnabledDefaultsFalse() {
        val campaignData = JSONObject().apply {
            put("id", 1)
        }
        val campaign = SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), campaignData, HashSet(), null)
        assertFalse(campaign.isFreemarkerEnabled)
    }

    @Test
    @Throws(JSONException::class)
    fun testUseLocalTimezoneMapping() {
        // useLocalTimezone() is the single source of truth that drives FreeMarker date built-ins
        // to evaluate in device-local time (LOCAL) vs UTC (GLOBAL) — mirroring start/end date parsing.
        val global = SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), JSONObject().apply { put("id", 1); put("timezone_type", "global") }, HashSet(), null)
        assertFalse(global.useLocalTimezone())

        val local = SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), JSONObject().apply { put("id", 1); put("timezone_type", "local") }, HashSet(), null)
        assertTrue(local.useLocalTimezone())

        // Missing timezone_type (legacy campaigns) defaults to UTC.
        val missing = SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), JSONObject().apply { put("id", 1) }, HashSet(), null)
        assertFalse(missing.useLocalTimezone())
    }

    @Test
    @Throws(JSONException::class)
    fun testBlackoutsAndIntervalsFromJSON() {
        val json = SwrveTestUtils.getAssetAsText(ApplicationProvider.getApplicationContext(), "campaign_blackouts_and_intervals.json")
        val campaigns = JSONObject(json)
        val campaignData = campaigns.getJSONArray("campaigns").getJSONObject(0)
        val assetsQueue: Set<SwrveAssetsQueueItem> = HashSet()
        val campaign = SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), campaignData, assetsQueue, null)
        assertNotNull(campaign)

        assertEquals(Date.from(Instant.parse("2024-09-10T00:00:00Z")), campaign.startDate)
        assertEquals(Date.from(Instant.parse("2050-09-10T00:00:00Z")), campaign.endDate)

        assertEquals(SwrveTimezoneType.GLOBAL, campaign.getTimezoneType())

        assertEquals(2, campaign.blackoutDates.orEmpty().size.toLong())
        val blackoutDates = campaign.blackoutDates.orEmpty()
        if (blackoutDates.isNotEmpty()) {
            assertEquals("2024-09-11T00:00:00Z", blackoutDates[0].from)
            assertEquals("2024-09-11T23:59:59Z", blackoutDates[0].to)
            assertEquals("2024-09-13T00:00:00Z", blackoutDates[1].from)
            assertEquals("2024-09-13T23:59:59Z", blackoutDates[1].to)
        } else {
            fail("blackoutDates list is empty")
        }

        assertEquals(2, campaign.intervalTimes.orEmpty().size.toLong())
        val intervalTimes = campaign.intervalTimes.orEmpty()
        if (intervalTimes.isNotEmpty()) {
            assertEquals(2, intervalTimes.size.toLong())
            assertEquals("09:00:00", intervalTimes[0].from)
            assertEquals("13:00:00", intervalTimes[0].to)
            assertEquals("14:00:00", intervalTimes[1].from)
            assertEquals("17:30:00", intervalTimes[1].to)
        } else {
            fail("intervalTimes list is empty")
        }
    }

    // Minimal campaign JSON with a dynamic_image_url containing a FreeMarker conditional.
    private fun freemarkerDynamicUrlCampaignJson(freemarkerEnabled: Boolean) = """
        {
          "id": 999,
          "freemarker_enabled": $freemarkerEnabled,
          "message": {
            "template": {
              "template_id": "1",
              "formats": [{
                "orientation": "landscape",
                "size": {"w": {"type":"number","value":320}, "h": {"type":"number","value":240}},
                "images": [{
                  "name": "bg",
                  "dynamic_image_url": "https://cdn.example.com/<#if points?number gt 1000>gold<#else>standard</#if>.png",
                  "x": {"type":"number","value":0},
                  "y": {"type":"number","value":0},
                  "w": {"type":"number","value":900},
                  "h": {"type":"number","value":600}
                }],
                "buttons": [],
                "name": "Test",
                "language": "en-US"
              }]
            },
            "id": 1,
            "name": "Test",
            "rules": {}
          }
        }
    """.trimIndent()

    @Test
    fun testQueueAssetsResolvesFreemarkerDynamicImageUrl() {
        val properties = mapOf("points" to "1500")
        val assetsQueue = HashSet<SwrveAssetsQueueItem>()
        SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), JSONObject(freemarkerDynamicUrlCampaignJson(true)), assetsQueue, properties)

        val resolvedUrl = "https://cdn.example.com/gold.png"
        val expectedSha1 = SwrveHelper.sha1(resolvedUrl.toByteArray())
        assertTrue("Resolved FreeMarker URL should be queued", assetsQueue.any { it.name == expectedSha1 })
    }

    @Test
    fun testQueueAssetsDoesNotEvaluateFreemarkerTagsWhenDisabled() {
        val properties = mapOf("points" to "1500")
        val assetsQueue = HashSet<SwrveAssetsQueueItem>()
        // freemarkerEnabled=false: the legacy engine finds no ${...} tokens so returns the raw URL unchanged.
        // The raw (unevaluated) URL is queued; the FreeMarker-resolved URL is not.
        SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), JSONObject(freemarkerDynamicUrlCampaignJson(false)), assetsQueue, properties)

        val rawUrl = "https://cdn.example.com/<#if points?number gt 1000>gold<#else>standard</#if>.png"
        val rawSha1 = SwrveHelper.sha1(rawUrl.toByteArray())
        assertTrue("Raw unevaluated URL should be queued in legacy mode", assetsQueue.any { it.name == rawSha1 })

        val goldSha1 = SwrveHelper.sha1("https://cdn.example.com/gold.png".toByteArray())
        assertFalse("FreeMarker expression should not be evaluated in legacy mode", assetsQueue.any { it.name == goldSha1 })
    }

    // Minimal campaign JSON whose dynamic_image_url branches on a calendar-day (?date) comparison,
    // with a configurable timezone_type. Used to prove timezone_type is wired end-to-end from the
    // campaign through SwrveTextTemplating into the FreeMarker evaluator.
    private fun freemarkerDateUrlCampaignJson(timezoneType: String) = """
        {
          "id": 999,
          "freemarker_enabled": true,
          "timezone_type": "$timezoneType",
          "message": {
            "template": {
              "template_id": "1",
              "formats": [{
                "orientation": "landscape",
                "size": {"w": {"type":"number","value":320}, "h": {"type":"number","value":240}},
                "images": [{
                  "name": "bg",
                  "dynamic_image_url": "https://cdn.example.com/<#if Recipient.expiry?date gt .now?date>future<#else>past</#if>.png",
                  "x": {"type":"number","value":0},
                  "y": {"type":"number","value":0},
                  "w": {"type":"number","value":900},
                  "h": {"type":"number","value":600}
                }],
                "buttons": [],
                "name": "Test",
                "language": "en-US"
              }]
            },
            "id": 1,
            "name": "Test",
            "rules": {}
          }
        }
    """.trimIndent()

    // End-to-end: the campaign's timezone_type must reach the FreeMarker evaluator so ?date
    // comparisons resolve in the correct timezone. .now is 23:30 UTC on Apr 16 — still Apr 16 in
    // UTC (GLOBAL) but already Apr 17 in UTC+14 (LOCAL/device). Against an expiry of Apr 17,
    // `expiry?date gt .now?date` is true under GLOBAL (17 > 16 → "future") and false under
    // LOCAL (17 > 17 → "past"), so the resolved+queued dynamic URL differs by timezone_type.
    @Test
    fun testDynamicImageUrlDateConditionUsesCampaignTimezone() {
        val savedNowProvider = SwrveFreemarkerEvaluator.nowProvider
        val savedTimeZone = TimeZone.getDefault()
        try {
            SwrveFreemarkerEvaluator.nowProvider = { Date.from(Instant.parse("2026-04-16T23:30:00Z")) }
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+14:00")) // fixed +14h offset — no tz-database dependency, no DST
            val properties = mapOf("Recipient.expiry" to "2026-04-17")

            val globalQueue = HashSet<SwrveAssetsQueueItem>()
            SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), JSONObject(freemarkerDateUrlCampaignJson("global")), globalQueue, properties)
            val futureSha1 = SwrveHelper.sha1("https://cdn.example.com/future.png".toByteArray())
            assertTrue("GLOBAL campaign should evaluate ?date in UTC and queue the 'future' URL", globalQueue.any { it.name == futureSha1 })

            val localQueue = HashSet<SwrveAssetsQueueItem>()
            SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), JSONObject(freemarkerDateUrlCampaignJson("local")), localQueue, properties)
            val pastSha1 = SwrveHelper.sha1("https://cdn.example.com/past.png".toByteArray())
            assertTrue("LOCAL campaign should evaluate ?date in device timezone and queue the 'past' URL", localQueue.any { it.name == pastSha1 })
        } finally {
            SwrveFreemarkerEvaluator.nowProvider = savedNowProvider
            TimeZone.setDefault(savedTimeZone)
        }
    }

    @Test
    fun testAreAssetsReadyWithFreemarkerDynamicImageUrl() {
        val properties = mapOf("points" to "1500")
        val campaign = SwrveInAppCampaign(dummyICampaignManager, SwrveCampaignDisplayer(), JSONObject(freemarkerDynamicUrlCampaignJson(true)), HashSet(), properties)
        val message = campaign.getMessage()

        val resolvedUrl = "https://cdn.example.com/gold.png"
        val resolvedSha1 = SwrveHelper.sha1(resolvedUrl.toByteArray())

        assertTrue("Message should be ready when resolved asset SHA1 is on disk", message.areAssetsReady(setOf(resolvedSha1), properties))
        assertFalse("Message should not be ready when resolved asset is absent", message.areAssetsReady(emptySet(), properties))
    }
}
