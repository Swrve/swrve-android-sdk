package com.swrve.sdk

import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.SwrveTestUtils.removeSingleton
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn

class QaUserTest : SwrveBaseTest() {

    @JvmField
    @Rule
    var retryRule: RetryRule = RetryRule(3) // Retry up to 3 times

    private var swrveCommonSpy: ISwrveCommon? = null

    private val appId = 123
    private val apiKey = "apiKey"
    private val batchUrl = "https://someendpoint.com"
    private val appVersion = "appversion"
    private val deviceId = "4567"
    private val qaJsonTrue = "{\"reset_device_state\":true,\"logging\":true}"

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val swrveCommonReal = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey") as ISwrveCommon
        swrveCommonSpy = Mockito.spy(swrveCommonReal)
        SwrveCommon.setSwrveCommon(swrveCommonSpy)

        doReturn(appId).`when`(swrveCommonSpy)?.appId
        doReturn(apiKey).`when`(swrveCommonSpy)?.apiKey
        doReturn(batchUrl).`when`(swrveCommonSpy)?.batchURL
        doReturn(appVersion).`when`(swrveCommonSpy)?.appVersion
        doReturn(deviceId).`when`(swrveCommonSpy)?.deviceId

        removeSingleton(QaUser::class.java, "instance") // robolectric does not clean up singletons correctly
    }

    @After
    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        removeSingleton(QaUser::class.java, "instance") // robolectric does not clean up singletons correctly
    }

    @Test
    fun testInitAndUpdate() {
        var qaUser = QaUser.getInstance()
        assertFalse(QaUser.isLoggingEnabled())
        assertFalse(QaUser.isResetDevice())
        assertNotNull(QaUser.restClient)
        assertNull(qaUser.restClientExecutor)

        doReturn(qaJsonTrue).`when`(swrveCommonSpy)?.getCachedData(qaUser.userId, ISwrveCommon.CACHE_QA)
        QaUser.update()
        qaUser = QaUser.getInstance()
        assertTrue(QaUser.isLoggingEnabled())
        assertTrue(QaUser.isResetDevice())

        assertEquals(appId.toLong(), qaUser.appId.toLong())
        assertEquals(apiKey, qaUser.apiKey)
        assertEquals(batchUrl, qaUser.endpoint)
        assertEquals(appVersion, qaUser.appVersion)
        assertEquals(deviceId, qaUser.deviceId)
        assertNotNull(qaUser.restClientExecutor)
    }

    @Test
    fun testInitAndUpdateNonQaUser() {
        var qaUser = QaUser.getInstance()
        assertFalse(QaUser.isLoggingEnabled())
        assertFalse(QaUser.isResetDevice())
        assertNotNull(QaUser.restClient)
        assertNull(qaUser.restClientExecutor)

        doReturn("{\"reset_device_state\":false,\"logging\":false}").`when`(swrveCommonSpy)?.getCachedData(qaUser.userId, ISwrveCommon.CACHE_QA)
        QaUser.update()
        qaUser = QaUser.getInstance()
        assertFalse(QaUser.isLoggingEnabled())
        assertFalse(QaUser.isResetDevice())

        assertEquals(0, qaUser.appId.toLong())
        assertNull(apiKey, qaUser.apiKey)
        assertNull(batchUrl, qaUser.endpoint)
        assertNull(appVersion, qaUser.appVersion)
        assertNull(deviceId, qaUser.deviceId)
        assertNull(qaUser.restClientExecutor)
    }

    @Test
    fun testCampaignsDownloaded() {
        var qaUser = QaUser.getInstance()
        doReturn(qaJsonTrue).`when`(swrveCommonSpy)?.getCachedData(qaUser.userId, ISwrveCommon.CACHE_QA)
        QaUser.update()
        qaUser = QaUser.getInstance()
        assertTrue(QaUser.isLoggingEnabled())

        val qaUserSpy = Mockito.spy(qaUser)
        QaUser.instance = qaUserSpy

        doNothing().`when`(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong())
        doReturn(999L).`when`(qaUserSpy).time

        val campaignInfoList: MutableList<QaCampaignInfo> = ArrayList()
        campaignInfoList.add(QaCampaignInfo(1, 11, QaCampaignInfo.CAMPAIGN_TYPE.IAM, false, ""))

        QaUser.campaignsDownloaded(campaignInfoList)

        // @formatter:off
        val logDetails =
            "{" +
                    "\"campaigns\":[" +
                        "{" +
                            "\"id\":1," +
                            "\"variant_id\":11," +
                            "\"type\":\"iam\"" +
                        "}" +
                    "]" +
            "}"
        // @formatter:on
        val event = getExpectedEvent("sdk", "campaigns-downloaded", logDetails)
        verifyEventQueued(qaUserSpy, event)
    }

    @Test
    fun testCampaignsAppRuleTriggered() {
        var qaUser = QaUser.getInstance()
        doReturn(qaJsonTrue).`when`(swrveCommonSpy)?.getCachedData(qaUser.userId, ISwrveCommon.CACHE_QA)
        QaUser.update()
        qaUser = QaUser.getInstance()
        assertTrue(QaUser.isLoggingEnabled())

        val qaUserSpy = Mockito.spy(qaUser)
        QaUser.instance = qaUserSpy

        doNothing().`when`(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong())
        doReturn(999L).`when`(qaUserSpy).time

        val campaignInfoList: MutableList<QaCampaignInfo> = ArrayList()
        campaignInfoList.add(QaCampaignInfo(1, 11, QaCampaignInfo.CAMPAIGN_TYPE.IAM, false, ""))

        val payload: MutableMap<String, String> = HashMap()
        payload["k1"] = "v1"
        payload["k2"] = "v2"
        QaUser.campaignsAppRuleTriggered("myevent", payload, "Too soon")

        // @formatter:off
        val logDetails =
            "{" +
                    "\"event_name\":\"myevent\"," +
                    "\"event_payload\":{" +
                        "\"k1\":\"v1\"," +
                        "\"k2\":\"v2\"" +
                    "}," +
                    "\"displayed\":false," +
                    "\"reason\":\"Too soon\"," +
                    "\"campaigns\":[]" +
            "}"
        // @formatter:on
        val event = getExpectedEvent("sdk", "campaign-triggered", logDetails)
        verifyEventQueued(qaUserSpy, event)
    }

    @Test
    fun testCampaignTriggeredIam() {
        var qaUser = QaUser.getInstance()
        doReturn(qaJsonTrue).`when`(swrveCommonSpy)?.getCachedData(qaUser.userId, ISwrveCommon.CACHE_QA)
        QaUser.update()
        qaUser = QaUser.getInstance()
        assertTrue(QaUser.isLoggingEnabled())

        val qaUserSpy = Mockito.spy(qaUser)
        QaUser.instance = qaUserSpy

        doNothing().`when`(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong())
        doReturn(999L).`when`(qaUserSpy).time

        val qaCampaignInfoMap: MutableMap<Int, QaCampaignInfo> = HashMap()
        qaCampaignInfoMap[1] = QaCampaignInfo(1, 11, QaCampaignInfo.CAMPAIGN_TYPE.IAM, false, "")
        qaCampaignInfoMap[2] = QaCampaignInfo(2, 22, QaCampaignInfo.CAMPAIGN_TYPE.IAM, false, "")

        val payload: MutableMap<String, String> = HashMap()
        payload["k1"] = "v1"
        payload["k2"] = "v2"
        QaUser.campaignTriggeredMessage("myevent", payload, false, qaCampaignInfoMap)

        // @formatter:off
        val logDetails =
            "{" +
                    "\"event_name\":\"myevent\"," +
                    "\"event_payload\":{" +
                        "\"k1\":\"v1\"," +
                        "\"k2\":\"v2\"" +
                    "}," +
                    "\"displayed\":false," +
                    "\"reason\":\"The loaded campaigns returned no message\"," +
                    "\"campaigns\":[" +
                        "{" +
                            "\"id\":1," +
                            "\"variant_id\":11," +
                            "\"type\":\"iam\"," +
                            "\"displayed\":false," +
                            "\"reason\":\"\"" +
                        "}," +
                        "{" +
                            "\"id\":2," +
                            "\"variant_id\":22," +
                            "\"type\":\"iam\"," +
                            "\"displayed\":false," +
                            "\"reason\":\"\"" +
                        "}" +
                    "]" +
            "}"
        // @formatter:on
        val event = getExpectedEvent("sdk", "campaign-triggered", logDetails)
        verifyEventQueued(qaUserSpy, event)
    }

    @Test
    fun testCampaignButtonClicked() {
        var qaUser = QaUser.getInstance()
        doReturn(qaJsonTrue).`when`(swrveCommonSpy)?.getCachedData(qaUser.userId, ISwrveCommon.CACHE_QA)
        QaUser.update()
        qaUser = QaUser.getInstance()
        assertTrue(QaUser.isLoggingEnabled())

        val qaUserSpy = Mockito.spy(qaUser)
        QaUser.instance = qaUserSpy

        doNothing().`when`(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong())
        doReturn(999L).`when`(qaUserSpy).time

        QaUser.campaignButtonClicked(1, 2, "mybutton", "deeplink", "some_url")

        // @formatter:off
        val logDetails =
            "{" +
                    "\"campaign_id\":1," +
                    "\"variant_id\":2," +
                    "\"button_name\":\"mybutton\"," +
                    "\"action_type\":\"deeplink\"," +
                    "\"action_value\":\"some_url\"" +
            "}"
        // @formatter:on
        val event = getExpectedEvent("sdk", "campaign-button-clicked", logDetails)
        verifyEventQueued(qaUserSpy, event)
    }

    @Test
    fun testAssetFailedToDownload() {
        var qaUser = QaUser.getInstance()
        doReturn(qaJsonTrue).`when`(swrveCommonSpy)?.getCachedData(qaUser.userId, ISwrveCommon.CACHE_QA)
        QaUser.update()
        qaUser = QaUser.getInstance()
        assertTrue(QaUser.isLoggingEnabled())

        val qaUserSpy = Mockito.spy(qaUser)
        QaUser.instance = qaUserSpy

        doNothing().`when`(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong())
        doReturn(999L).`when`(qaUserSpy).time

        QaUser.assetFailedToDownload("aaaabbbbccccdddd", "httsdaohasdsa.co", "malformed url")

        // @formatter:off
        val logDetails =
            "{" +
                    "\"asset_name\":\"aaaabbbbccccdddd\"," +
                    "\"image_url\":\"httsdaohasdsa.co\"," +
                    "\"reason\":\"malformed url\"" +
            "}"
        // @formatter:on
        val event = getExpectedEvent("sdk", "asset-failed-to-download", logDetails)
        verifyEventQueued(qaUserSpy, event)
    }

    @Test
    fun testAssetFailedToDisplay() {
        var qaUser = QaUser.getInstance()
        doReturn(qaJsonTrue).`when`(swrveCommonSpy)?.getCachedData(qaUser.userId, ISwrveCommon.CACHE_QA)
        QaUser.update()
        qaUser = QaUser.getInstance()
        assertTrue(QaUser.isLoggingEnabled())

        val qaUserSpy = Mockito.spy(qaUser)
        QaUser.instance = qaUserSpy

        doNothing().`when`(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong())
        doReturn(999L).`when`(qaUserSpy).time

        QaUser.assetFailedToDisplay(1, 2, "test_asset_name", "\${url}", "resolved.url", true, "Asset not in Cache")

        // @formatter:off
        val logDetails =
            "{" +
                    "\"campaign_id\":1," +
                    "\"variant_id\":2," +
                    "\"unresolved_url\":\"\${url}\"," +
                    "\"has_fallback\":true," +
                    "\"reason\":\"Asset not in Cache\"," +
                    "\"image_url\":\"resolved.url\"," +
                    "\"asset_name\":\"test_asset_name\"" +
            "}"
        // @formatter:on
        val event = getExpectedEvent("sdk", "asset-failed-to-display", logDetails)
        verifyEventQueued(qaUserSpy, event)
    }

    @Test
    fun testEmbeddedPersonalizationFailed() {
        var qaUser = QaUser.getInstance()
        doReturn(qaJsonTrue).`when`(swrveCommonSpy)?.getCachedData(qaUser.userId, ISwrveCommon.CACHE_QA)
        QaUser.update()
        qaUser = QaUser.getInstance()
        assertTrue(QaUser.isLoggingEnabled())

        val qaUserSpy = Mockito.spy(qaUser)
        QaUser.instance = qaUserSpy

        doNothing().`when`(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong())
        doReturn(999L).`when`(qaUserSpy).time

        QaUser.embeddedPersonalizationFailed(1, 2, "\${user.broken}", "Failed to resolve personalization")

        // @formatter:off
        val logDetails =
            "{" +
                    "\"campaign_id\":1," +
                    "\"variant_id\":2," +
                    "\"unresolved_data\":\"\${user.broken}\"," +
                    "\"reason\":\"Failed to resolve personalization\"" +
            "}"
        // @formatter:on
        val event = getExpectedEvent("sdk", "embedded-personalization-failed", logDetails)
        verifyEventQueued(qaUserSpy, event)
    }

    @Test
    fun testWrappedEvent() {
        var qaUser = QaUser.getInstance()
        doReturn(qaJsonTrue).`when`(swrveCommonSpy)?.getCachedData(qaUser.userId, ISwrveCommon.CACHE_QA)
        QaUser.update()
        qaUser = QaUser.getInstance()
        assertTrue(QaUser.isLoggingEnabled())

        val qaUserSpy = Mockito.spy(qaUser)
        QaUser.instance = qaUserSpy

        doNothing().`when`(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong())
        doReturn(999L).`when`(qaUserSpy).time

        // @formatter:off
        val event1 =
            "{" +
                    "\"type\":\"session_start\"," +
                    "\"time\":\"123\"," +
                    "\"seqnum\":\"1\"" +
            "}"
        val event2 =
            "{" +
                    "\"type\":\"device_update\"," +
                    "\"time\":\"124\"," +
                    "\"seqnum\":\"2\"," +
                    "\"attributes\":" +
                    "{" +
                        "\"swrve.device_name\":\"Google Android\"," +
                        "\"swrve.os_version\":9" +
                    "}" +
            "}"
        // @formatter:on
        val events: MutableList<String> = ArrayList()
        events.add(event1)
        events.add(event2)

        QaUser.wrappedEvents(events)

        // @formatter:off
        val expectedEvent1 =
            "{" +
                    "\"time\":999," +
                    "\"type\":\"qa_log_event\"," +
                    "\"log_source\":\"sdk\"," +
                    "\"log_type\":\"event\"," +
                    "\"log_details\":" +
                    "{" +
                        "\"type\":\"session_start\"," +
                        "\"seqnum\":1," +
                        "\"client_time\":123," +
                        "\"payload\":\"{}\"," +
                        "\"parameters\":{}" +
                    "}" +
            "}"
        val expectedEvent2 =
            "{" +
                    "\"time\":999," +
                    "\"type\":\"qa_log_event\"," +
                    "\"log_source\":\"sdk\"," +
                    "\"log_type\":\"event\"," +
                    "\"log_details\":" +
                    "{" +
                        "\"type\":\"device_update\"," +
                        "\"seqnum\":2," +
                        "\"client_time\":124," +
                        "\"payload\":\"{}\"," +
                        "\"parameters\":" +
                        "{" +
                            "\"attributes\":" +
                            "{" +
                                "\"swrve.device_name\":\"Google Android\"," +
                                "\"swrve.os_version\":9" +
                            "}" +
                        "}" +
                    "}" +
            "}"

        // @formatter:on
        assertEquals(2, qaUserSpy.qaLogQueue.size.toLong())
        assertEquals(expectedEvent1, qaUserSpy.qaLogQueue[0])
        assertEquals(expectedEvent2, qaUserSpy.qaLogQueue[1])
    }

    // @formatter:off
    private fun getExpectedEvent(logSource:String, logType:String, logDetails:String): String {
        return "{" +
                "\"time\":999," +
                "\"type\":\"qa_log_event\"," +
                "\"log_source\":\"" + logSource + "\"," +
                "\"log_type\":\"" + logType + "\"," +
                "\"log_details\":" + logDetails +
            "}"
    }

    // @formatter:on
    private fun verifyEventQueued(qaUserSpy: QaUser, event: String) {
        assertEquals(1, qaUserSpy.qaLogQueue.size.toLong())
        assertEquals(event, qaUserSpy.qaLogQueue[0])
    }
}
