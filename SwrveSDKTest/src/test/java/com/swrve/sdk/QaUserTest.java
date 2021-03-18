package com.swrve.sdk;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.swrve.sdk.ISwrveCommon.CACHE_QA;
import static com.swrve.sdk.QaCampaignInfo.CAMPAIGN_TYPE.CONVERSATION;
import static com.swrve.sdk.QaCampaignInfo.CAMPAIGN_TYPE.IAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QaUserTest extends SwrveBaseTest {

    private ISwrveCommon swrveCommonSpy;

    private int appId = 123;
    private String apiKey = "apiKey";
    private String batchUrl = "https://someendpoint.com";
    private String appVersion = "appversion";
    private String deviceId = "4567";
    private String qaJsonTrue = "{\"reset_device_state\":true,\"logging\":true}";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ISwrveCommon swrveCommonReal = (ISwrveCommon) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveCommonSpy = Mockito.spy(swrveCommonReal);
        SwrveCommon.setSwrveCommon(swrveCommonSpy);

        Mockito.doReturn(appId).when(swrveCommonSpy).getAppId();
        Mockito.doReturn(apiKey).when(swrveCommonSpy).getApiKey();
        Mockito.doReturn(batchUrl).when(swrveCommonSpy).getBatchURL();
        Mockito.doReturn(appVersion).when(swrveCommonSpy).getAppVersion();
        Mockito.doReturn(deviceId).when(swrveCommonSpy).getDeviceId();

        SwrveTestUtils.removeSingleton(QaUser.class, "instance"); // robolectric does not clean up singletons correctly
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        SwrveTestUtils.removeSingleton(QaUser.class, "instance"); // robolectric does not clean up singletons correctly
    }

    @Test
    public void testInitAndUpdate() {

        QaUser qaUser = QaUser.getInstance();
        assertFalse(QaUser.isLoggingEnabled());
        assertFalse(QaUser.isResetDevice());
        assertNotNull(QaUser.restClient);
        assertNull(qaUser.restClientExecutor);

        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());
        assertTrue(QaUser.isResetDevice());

        assertEquals(appId, qaUser.appId);
        assertEquals(apiKey, qaUser.apiKey);
        assertEquals(batchUrl, qaUser.endpoint);
        assertEquals(appVersion, qaUser.appVersion);
        assertEquals(deviceId, qaUser.deviceId);
        assertNotNull(qaUser.restClientExecutor);
    }

    @Test
    public void testInitAndUpdateNonQaUser() {

        QaUser qaUser = QaUser.getInstance();
        assertFalse(QaUser.isLoggingEnabled());
        assertFalse(QaUser.isResetDevice());
        assertNotNull(QaUser.restClient);
        assertNull(qaUser.restClientExecutor);

        Mockito.doReturn("{\"reset_device_state\":false,\"logging\":false}").when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertFalse(QaUser.isLoggingEnabled());
        assertFalse(QaUser.isResetDevice());

        assertEquals(0, qaUser.appId);
        assertNull(apiKey, qaUser.apiKey);
        assertNull(batchUrl, qaUser.endpoint);
        assertNull(appVersion, qaUser.appVersion);
        assertNull(deviceId, qaUser.deviceId);
        assertNull(qaUser.restClientExecutor);
    }

    @Test
    public void testGeoCampaignTriggered() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        Collection<QaGeoCampaignInfo> geoCampignInfoList = new ArrayList<>();
        geoCampignInfoList.add(new QaGeoCampaignInfo(1, true, "reason1"));
        geoCampignInfoList.add(new QaGeoCampaignInfo(4, false, "reason2"));
        QaUser.geoCampaignTriggered(123, 456, "enter", geoCampignInfoList);

        String logDetails =
                "{" +
                        "\"geoplace_id\":123," +
                        "\"geofence_id\":456," +
                        "\"action_type\":\"enter\"," +
                        "\"campaigns\":[" +
                        "{" +
                        "\"variant_id\":1," +
                        "\"displayed\":true," +
                        "\"reason\":\"reason1\"" +
                        "},{" +
                        "\"variant_id\":4," +
                        "\"displayed\":false," +
                        "\"reason\":\"reason2\"" +
                        "}" +
                        "]}";
        String event = getExpectedEvent("geo-sdk", "geo-campaign-triggered", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testGeoCampaignsDownloadedWithNoCampaigns() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        QaUser.geoCampaignsDownloaded(123, 456, "enter", Collections.<QaGeoCampaignInfo>emptyList());

        String logDetails = "{" +
                "\"geoplace_id\":123," +
                "\"geofence_id\":456," +
                "\"action_type\":\"enter\"," +
                "\"campaigns\":[]" +
                "}";
        String event = getExpectedEvent("geo-sdk", "geo-campaigns-downloaded", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testGeoCampaignsDownloadedWithCampaigns() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        List<QaGeoCampaignInfo> geoCampaignsDownloaded = new ArrayList();
        geoCampaignsDownloaded.add(new QaGeoCampaignInfo(1, false, ""));
        geoCampaignsDownloaded.add(new QaGeoCampaignInfo(2, false, ""));

        QaUser.geoCampaignsDownloaded(123, 456, "enter", geoCampaignsDownloaded);

        String logDetails =
                "{" +
                        "\"geoplace_id\":123," +
                        "\"geofence_id\":456," +
                        "\"action_type\":\"enter\"," +
                        "\"campaigns\":[" +
                        "{\"variant_id\":1}," +
                        "{\"variant_id\":2}" +
                        "]" +
                        "}";
        String event = getExpectedEvent("geo-sdk", "geo-campaigns-downloaded", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testCampaignsDownloaded() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        List<QaCampaignInfo> campaignInfoList = new ArrayList<>();
        campaignInfoList.add(new QaCampaignInfo(1, 11, IAM, false, ""));
        campaignInfoList.add(new QaCampaignInfo(2, 22, CONVERSATION, false, ""));

        QaUser.campaignsDownloaded(campaignInfoList);

        // @formatter:off
        String logDetails =
                "{" +
                        "\"campaigns\":[" +
                            "{" +
                                "\"id\":1," +
                                "\"variant_id\":11," +
                                "\"type\":\"iam\"" +
                            "}," +
                            "{" +
                                "\"id\":2," +
                                "\"variant_id\":22," +
                                "\"type\":\"conversation\"" +
                            "}" +
                        "]" +
                 "}";
        // @formatter:on
        String event = getExpectedEvent("sdk", "campaigns-downloaded", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testCampaignsAppRuleTriggered() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        List<QaCampaignInfo> campaignInfoList = new ArrayList<>();
        campaignInfoList.add(new QaCampaignInfo(1, 11, IAM, false, ""));
        campaignInfoList.add(new QaCampaignInfo(2, 22, CONVERSATION, false, ""));

        Map<String, String> payload = new HashMap<>();
        payload.put("k1", "v1");
        payload.put("k2", "v2");
        QaUser.campaignsAppRuleTriggered("myevent", payload, "Too soon");

        // @formatter:off
        String logDetails =
                "{" +
                        "\"event_name\":\"myevent\"," +
                        "\"event_payload\":{" +
                                "\"k1\":\"v1\"," +
                                "\"k2\":\"v2\"" +
                            "}," +
                        "\"displayed\":false," +
                        "\"reason\":\"Too soon\"," +
                        "\"campaigns\":[]" +
                 "}";
        // @formatter:on
        String event = getExpectedEvent("sdk", "campaign-triggered", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testCampaignTriggeredConversation() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        Map<Integer, QaCampaignInfo> qaCampaignInfoMap = new HashMap<>();
        qaCampaignInfoMap.put(1, new QaCampaignInfo(1, 11, CONVERSATION, false, ""));
        qaCampaignInfoMap.put(2, new QaCampaignInfo(2, 22, CONVERSATION, false, ""));

        Map<String, String> payload = new HashMap<>();
        payload.put("k1", "v1");
        payload.put("k2", "v2");
        QaUser.campaignTriggeredConversation("myevent", payload, false, qaCampaignInfoMap);

        // @formatter:off
        String logDetails =
                "{" +
                        "\"event_name\":\"myevent\"," +
                        "\"event_payload\":{" +
                                "\"k1\":\"v1\"," +
                                "\"k2\":\"v2\"" +
                            "}," +
                        "\"displayed\":false," +
                        "\"reason\":\"The loaded campaigns returned no conversation\"," +
                        "\"campaigns\":[" +
                            "{" +
                                "\"id\":1," +
                                "\"variant_id\":11," +
                                "\"type\":\"conversation\"," +
                                "\"displayed\":false," +
                                "\"reason\":\"\"" +
                            "}," +
                            "{" +
                                "\"id\":2," +
                                "\"variant_id\":22," +
                                "\"type\":\"conversation\"," +
                                "\"displayed\":false," +
                                "\"reason\":\"\"" +
                            "}" +
                        "]" +
                 "}";
        // @formatter:on
        String event = getExpectedEvent("sdk", "campaign-triggered", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testCampaignTriggeredConversationDisplayed() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        Map<Integer, QaCampaignInfo> qaCampaignInfoMap = new HashMap<>();
        qaCampaignInfoMap.put(1, new QaCampaignInfo(1, 11, CONVERSATION, false, ""));
        qaCampaignInfoMap.put(2, new QaCampaignInfo(2, 22, CONVERSATION, true, ""));

        Map<String, String> payload = new HashMap<>();
        payload.put("k1", "v1");
        payload.put("k2", "v2");
        QaUser.campaignTriggeredConversation("myevent", payload, true, qaCampaignInfoMap);

        // @formatter:off
        String logDetails =
                "{" +
                        "\"event_name\":\"myevent\"," +
                        "\"event_payload\":{" +
                                "\"k1\":\"v1\"," +
                                "\"k2\":\"v2\"" +
                            "}," +
                        "\"displayed\":true," +
                        "\"reason\":\"\"," +
                        "\"campaigns\":[" +
                            "{" +
                                "\"id\":1," +
                                "\"variant_id\":11," +
                                "\"type\":\"conversation\"," +
                                "\"displayed\":false," +
                                "\"reason\":\"\"" +
                            "}," +
                            "{" +
                                "\"id\":2," +
                                "\"variant_id\":22," +
                                "\"type\":\"conversation\"," +
                                "\"displayed\":true," +
                                "\"reason\":\"\"" +
                            "}" +
                        "]" +
                 "}";
        // @formatter:on
        String event = getExpectedEvent("sdk", "campaign-triggered", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testCampaignTriggeredIam() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        Map<Integer, QaCampaignInfo> qaCampaignInfoMap = new HashMap<>();
        qaCampaignInfoMap.put(1, new QaCampaignInfo(1, 11, IAM, false, ""));
        qaCampaignInfoMap.put(2, new QaCampaignInfo(2, 22, IAM, false, ""));

        Map<String, String> payload = new HashMap<>();
        payload.put("k1", "v1");
        payload.put("k2", "v2");
        QaUser.campaignTriggeredMessage("myevent", payload, false, qaCampaignInfoMap);

        // @formatter:off
        String logDetails =
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
                "}";
        // @formatter:on
        String event = getExpectedEvent("sdk", "campaign-triggered", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testCampaignTriggeredIamNoDisplay() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        Map<Integer, QaCampaignInfo> qaCampaignInfoMap = new HashMap<>();
        qaCampaignInfoMap.put(1, new QaCampaignInfo(1, 11, IAM, false, ""));
        qaCampaignInfoMap.put(2, new QaCampaignInfo(2, 22, IAM, false, ""));

        Map<String, String> payload = new HashMap<>();
        payload.put("k1", "v1");
        payload.put("k2", "v2");
        QaUser.campaignTriggeredMessageNoDisplay("myevent", payload);

        // @formatter:off
        String logDetails =
                "{" +
                    "\"event_name\":\"myevent\"," +
                    "\"event_payload\":{" +
                        "\"k1\":\"v1\"," +
                        "\"k2\":\"v2\"" +
                    "}," +
                    "\"displayed\":false," +
                    "\"reason\":\"No In App Message triggered because Conversation displayed\"," +
                    "\"campaigns\":[]" +
                "}";
        // @formatter:on
        String event = getExpectedEvent("sdk", "campaign-triggered", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testCampaignTriggeredIamDisplayed() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        Map<Integer, QaCampaignInfo> qaCampaignInfoMap = new HashMap<>();
        qaCampaignInfoMap.put(1, new QaCampaignInfo(1, 11, IAM, false, ""));
        qaCampaignInfoMap.put(2, new QaCampaignInfo(2, 22, IAM, true, ""));

        Map<String, String> payload = new HashMap<>();
        payload.put("k1", "v1");
        payload.put("k2", "v2");
        QaUser.campaignTriggeredConversation("myevent", payload, true, qaCampaignInfoMap);

        // @formatter:off
        String logDetails =
                "{" +
                        "\"event_name\":\"myevent\"," +
                        "\"event_payload\":{" +
                                "\"k1\":\"v1\"," +
                                "\"k2\":\"v2\"" +
                            "}," +
                        "\"displayed\":true," +
                        "\"reason\":\"\"," +
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
                                "\"displayed\":true," +
                                "\"reason\":\"\"" +
                            "}" +
                        "]" +
                 "}";
        // @formatter:on
        String event = getExpectedEvent("sdk", "campaign-triggered", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testCampaignButtonClicked() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        QaUser.campaignButtonClicked(1, 2, "mybutton", "deeplink", "some_url");

        // @formatter:off
        String logDetails =
                "{" +
                    "\"campaign_id\":1," +
                    "\"variant_id\":2," +
                    "\"button_name\":\"mybutton\"," +
                    "\"action_type\":\"deeplink\"," +
                    "\"action_value\":\"some_url\"" +
                 "}";
        // @formatter:on
        String event = getExpectedEvent("sdk", "campaign-button-clicked", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testAssetFailedToDownload() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        QaUser.assetFailedToDownload("aaaabbbbccccdddd", "httsdaohasdsa.co", "malformed url");

        // @formatter:off
        String logDetails =
                "{" +
                        "\"asset_name\":\"aaaabbbbccccdddd\"," +
                        "\"image_url\":\"httsdaohasdsa.co\"," +
                        "\"reason\":\"malformed url\"" +
                "}";
        // @formatter:on
        String event = getExpectedEvent("sdk", "asset-failed-to-download", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testAssetFailedToDisplay() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        QaUser.assetFailedToDisplay(1, 2, "test_asset_name", "${url}", "resolved.url", true, "Asset not in Cache");

        // @formatter:off
        String logDetails =
                "{" +
                        "\"campaign_id\":1," +
                        "\"variant_id\":2," +
                        "\"unresolved_url\":\"${url}\"," +
                        "\"has_fallback\":true," +
                        "\"reason\":\"Asset not in Cache\"," +
                        "\"image_url\":\"resolved.url\"," +
                        "\"asset_name\":\"test_asset_name\"" +
                "}";
        // @formatter:on
        String event = getExpectedEvent("sdk", "asset-failed-to-display", logDetails);
        verifyEventQueued(qaUserSpy, event);
    }

    @Test
    public void testWrappedEvent() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn(qaJsonTrue).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).scheduleRepeatingQueueFlush(Mockito.anyLong());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        // @formatter:off
        String event1 =
                "{" +
                        "\"type\":\"session_start\"," +
                        "\"time\":\"123\"," +
                        "\"seqnum\":\"1\"" +
                 "}";
        String event2 =
                "{" +
                        "\"type\":\"device_update\"," +
                        "\"time\":\"124\"," +
                        "\"seqnum\":\"2\"," +
                        "\"attributes\":" +
                            "{" +
                                "\"swrve.device_name\":\"Google Android\"," +
                                "\"swrve.os_version\":9" +
                            "}" +
                 "}";
        // @formatter:on
        List<String> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);

        QaUser.wrappedEvents(events);

        // @formatter:off
        String expectedEvent1 =
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
                 "}";
        String expectedEvent2 =
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
                "}";
        // @formatter:on

        assertEquals(2, qaUserSpy.qaLogQueue.size());
        assertEquals(expectedEvent1, qaUserSpy.qaLogQueue.get(0));
        assertEquals(expectedEvent2, qaUserSpy.qaLogQueue.get(1));
    }

    // @formatter:off
    private String getExpectedEvent(String logSource, String logType, String logDetails) {
        return "{" +
                    "\"time\":999," +
                    "\"type\":\"qa_log_event\"," +
                    "\"log_source\":\"" + logSource + "\"," +
                    "\"log_type\":\"" + logType + "\"," +
                    "\"log_details\":" + logDetails +
                "}";
    }
    // @formatter:on

    private void verifyEventQueued(QaUser qaUserSpy, String event) {
        assertEquals(1, qaUserSpy.qaLogQueue.size());
        assertEquals(event, qaUserSpy.qaLogQueue.get(0));
    }
}
