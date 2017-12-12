package com.swrve.sdk;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;

import static com.swrve.sdk.ISwrveCommon.CACHE_LOCATION_CAMPAIGNS;
import static com.swrve.sdk.ISwrveCommon.CACHE_QA;
import static com.swrve.sdk.QaUser.RATE_LIMIT_MAX_ALLOWED_REQUESTS;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QaUserTest extends SwrveBaseTest {

    private ISwrveCommon swrveCommonSpy;

    private int appId = 123;
    private String apiKey = "apiKey";
    private String batchUrl = "https://someendpoint.com";
    private String appVersion = "appversion";
    private short deviceId = 4567;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        ISwrveCommon swrveCommonReal = (ISwrveCommon) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveCommonSpy = Mockito.spy(swrveCommonReal);
        SwrveCommon.setSwrveCommon(swrveCommonSpy);

        Mockito.doReturn(appId).when(swrveCommonSpy).getAppId();
        Mockito.doReturn(apiKey).when(swrveCommonSpy).getApiKey();
        Mockito.doReturn(batchUrl).when(swrveCommonSpy).getBatchURL();
        Mockito.doReturn(appVersion).when(swrveCommonSpy).getAppVersion();
        Mockito.doReturn(deviceId).when(swrveCommonSpy).getDeviceId();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        // robolectric does not clean up singletons correctly
        SwrveTestUtils.removeSingleton(QaUser.class, "instance");
        SwrveTestUtils.removeSingleton(QaUser.class, "campaignTriggeredLogTimeQueue", new ArrayBlockingQueue(RATE_LIMIT_MAX_ALLOWED_REQUESTS));
        SwrveTestUtils.removeSingleton(QaUser.class, "rateLimitCooloffUntilTime", 0);
    }

    @Test
    public void testInitAndUpdate() throws Exception {

        QaUser qaUser = QaUser.getInstance();
        assertFalse(QaUser.isLoggingEnabled());
        assertNotNull(qaUser.restClient);
        assertNull(qaUser.restClientExecutor);

        Mockito.doReturn("true").when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        assertEquals(appId, qaUser.appId);
        assertEquals(apiKey, qaUser.apiKey);
        assertEquals(batchUrl, qaUser.endpoint);
        assertEquals(appVersion, qaUser.appVersion);
        assertEquals(deviceId, qaUser.deviceId);
        assertNotNull(qaUser.restClientExecutor);
    }

    @Test
    public void testLocationCampaignEngaged() throws Exception {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn("true").when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        qaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).executeRestClient(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(999l).when(qaUserSpy).getTime();

        QaUser.locationCampaignEngaged("plotId", 12, 122, "{}");

        String logDetails =
                "{" +
                    "\"plot_campaign_id\":\"plotId\"," +
                    "\"campaign_id\":12," +
                    "\"variant_id\":122," +
                    "\"variant_payload\":{}" +
                "}";
        String body = getExpectedBody(qaUser, "location-campaign-engaged", logDetails);

        verifyRestRequest(qaUserSpy, body);
    }

    @Test
    public void testLocationCampaignsDownloadedWithNoCampaigns() throws Exception {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn("true").when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        qaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).executeRestClient(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(999l).when(qaUserSpy).getTime();

        QaUser.locationCampaignsDownloaded();

        String logDetails = "{\"campaigns\":[]}";
        String body = getExpectedBody(qaUser, "location-campaigns-downloaded", logDetails);

        verifyRestRequest(qaUserSpy, body);
    }

    @Test
    public void testLocationCampaignsDownloadedWithCampaigns() throws Exception {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn("true").when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        qaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).executeRestClient(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(999l).when(qaUserSpy).getTime();
        String locationCampaigns = "" +
                "    {\n" +
                "        \"campaigns\": {\n" +
                "            \"391\": {\n" +
                "                \"version\": 1,\n" +
                "                \"message\": {\n" +
                "                    \"id\": 381,\n" +
                "                    \"body\": \"\",\n" +
                "                    \"payload\": \"{}\"\n" +
                "                }\n" +
                "            },\n" +
                "            \"392\": {\n" +
                "                \"version\": 1,\n" +
                "                \"message\": {\n" +
                "                    \"id\": 382,\n" +
                "                    \"body\": \"\",\n" +
                "                    \"payload\": \"{}\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }";
        Mockito.doReturn(locationCampaigns).when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_LOCATION_CAMPAIGNS);

        QaUser.locationCampaignsDownloaded();

        String logDetails =
                "{\"campaigns\":[" +
                    "{" +
                        "\"id\":\"391\"," +
                        "\"variant_id\":381" +
                    "},{" +
                        "\"id\":\"392\"," +
                        "\"variant_id\":382" +
                    "}" +
                "]}";
        String body = getExpectedBody(qaUser, "location-campaigns-downloaded", logDetails);

        verifyRestRequest(qaUserSpy, body);
    }

    @Test
    public void testLocationCampaignTriggered() throws Exception {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn("true").when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        qaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).executeRestClient(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(999l).when(qaUserSpy).getTime();

        Collection<QaLocationCampaignTriggered> locationCampignTriggeredList = new ArrayList<>();
        locationCampignTriggeredList.add(new QaLocationCampaignTriggered(1, 2, "3", true, "reason1"));
        locationCampignTriggeredList.add(new QaLocationCampaignTriggered(4, 5, "6", false, "reason2"));
        QaUser.locationCampaignTriggered(locationCampignTriggeredList);

        String logDetails =
                "{\"campaigns\":[" +
                        "{" +
                            "\"id\":1," +
                            "\"variant_id\":2," +
                            "\"plot_id\":\"3\"," +
                            "\"displayed\":true," +
                            "\"reason\":\"reason1\"" +
                        "},{" +
                            "\"id\":4," +
                            "\"variant_id\":5," +
                            "\"plot_id\":\"6\"," +
                            "\"displayed\":false," +
                            "\"reason\":\"reason2\"" +
                        "}" +
                    "]}";
        String body = getExpectedBody(qaUser, "location-campaign-triggered", logDetails);

        verifyRestRequest(qaUserSpy, body);
    }

    @Test
    public void testRateLimiting() throws Exception {

        QaUser qaUser = QaUser.getInstance();
        long cooloffTime = qaUser.RATE_LIMIT_COOLOFF_TIME_MILLS;

        // 3 requests and hit the limit
        assertFalse("Should be no rate limiting", qaUser.isRateLimited(1000));
        assertFalse("Should be no rate limiting", qaUser.isRateLimited(2000));
        assertFalse("Should be no rate limiting", qaUser.isRateLimited(3000));
        assertTrue("rate limit exceeded, so should be starting cooloff", qaUser.isRateLimited(4000));

        // bunch of requests during the cooloff period
        assertTrue("should be in a cooloff", qaUser.isRateLimited(3995 + cooloffTime));
        assertTrue("should be in a cooloff", qaUser.isRateLimited(3996 + cooloffTime));
        assertTrue("should be in a cooloff", qaUser.isRateLimited(3997 + cooloffTime));
        assertTrue("should be in a cooloff", qaUser.isRateLimited(3998 + cooloffTime));
        assertTrue("should be in a cooloff", qaUser.isRateLimited(3999 + cooloffTime));

        // cooloff period finishes and 3 more requests and hit the limit
        assertFalse("cooloff finished, should be no rate limiting", qaUser.isRateLimited(5000 + cooloffTime));
        assertFalse("cooloff finished, should be no rate limiting", qaUser.isRateLimited(6000 + cooloffTime));
        assertFalse("cooloff finished, should be no rate limiting", qaUser.isRateLimited(7000 + cooloffTime));
        assertTrue("rate limit exceeded, so should be starting cooloff", qaUser.isRateLimited(8000 + cooloffTime));

        // bunch of requests during the cooloff period
        assertTrue("should be in a cooloff", qaUser.isRateLimited(9000 + cooloffTime));
        assertTrue("should be in a cooloff", qaUser.isRateLimited(10000 + cooloffTime));
        assertTrue("should be in a cooloff", qaUser.isRateLimited(20000 + cooloffTime));
        assertTrue("should be in a cooloff", qaUser.isRateLimited(30000 + cooloffTime));

        // cooloff period finishes and 3 more requests and hit the limit
        assertTrue("should be in a cooloff", qaUser.isRateLimited(7000 + cooloffTime + cooloffTime));
        assertFalse("cooloff finished, should be no rate limiting", qaUser.isRateLimited(8000 + cooloffTime + cooloffTime));
        assertFalse("cooloff finished, should be no rate limiting", qaUser.isRateLimited(9000 + cooloffTime + cooloffTime));
        assertFalse("cooloff finished, should be no rate limiting", qaUser.isRateLimited(10000 + cooloffTime + cooloffTime));
        assertTrue("rate limit exceeded, so should be starting cooloff", qaUser.isRateLimited(11000 + cooloffTime + cooloffTime));
    }

    private String getExpectedBody(QaUser qaUser, String logType, String logDetails) {
        String body =
                "{" +
                    "\"user\":\"" + qaUser.userId + "\"," +
                    "\"session_token\":\"" + qaUser.sessionToken + "\"," +
                    "\"version\":\"2\"," +
                    "\"app_version\":\"" + appVersion + "\"," +
                    "\"device_id\":" + deviceId + "," +
                    "\"data\":[" +
                        "{" +
                            "\"seqnum\":1," +
                            "\"time\":999," +
                            "\"type\":\"qa_log_event\"," +
                            "\"log_source\":\"location-sdk\"," +
                            "\"log_type\":\"" + logType + "\"," +
                            "\"log_details\":" + logDetails +
                        "}" +
                    "]" +
                "}";
        return body;
    }

    private void verifyRestRequest(QaUser qaUserSpy, String body) {
        ArgumentCaptor<String> endPointCaptor= ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(qaUserSpy, Mockito.atLeastOnce()).executeRestClient(endPointCaptor.capture(), bodyCaptor.capture());
        assertEquals(1, endPointCaptor.getAllValues().size());
        assertEquals(batchUrl, endPointCaptor.getAllValues().get(0) );
        assertEquals(1, bodyCaptor.getAllValues().size());
        assertEquals(body, bodyCaptor.getAllValues().get(0));
    }
}
