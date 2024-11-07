package com.swrve.sdk.messaging

import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.ISwrveCampaignManager
import com.swrve.sdk.SwrveAssetsQueueItem
import com.swrve.sdk.SwrveBaseTest
import com.swrve.sdk.SwrveCampaignDisplayer
import com.swrve.sdk.SwrveTestUtils
import com.swrve.sdk.config.SwrveConfig
import com.swrve.sdk.config.SwrveConfigBase
import com.swrve.sdk.messaging.SwrveBaseCampaign.SwrveTimezoneType
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.Date

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

        assertEquals(2, campaign.blackoutDates.size.toLong())
        assertEquals("2024-09-11T00:00:00Z", campaign.blackoutDates[0].from)
        assertEquals("2024-09-11T23:59:59Z", campaign.blackoutDates[0].to)
        assertEquals("2024-09-13T00:00:00Z", campaign.blackoutDates[1].from)
        assertEquals("2024-09-13T23:59:59Z", campaign.blackoutDates[1].to)

        assertEquals(2, campaign.intervalTimes.size.toLong())
        assertEquals("09:00:00", campaign.intervalTimes[0].from)
        assertEquals("13:00:00", campaign.intervalTimes[0].to)
        assertEquals("14:00:00", campaign.intervalTimes[1].from)
        assertEquals("17:30:00", campaign.intervalTimes[1].to)
    }
}
