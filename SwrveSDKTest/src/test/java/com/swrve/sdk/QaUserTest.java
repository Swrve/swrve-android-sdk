package com.swrve.sdk;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.swrve.sdk.ISwrveCommon.CACHE_QA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QaUserTest extends SwrveBaseTest {

    private ISwrveCommon swrveCommonSpy;

    private int appId = 123;
    private String apiKey = "apiKey";
    private String batchUrl = "https://someendpoint.com";
    private String appVersion = "appversion";
    private String deviceId = "4567";

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

        // robolectric does not clean up singletons correctly
        SwrveTestUtils.removeSingleton(QaUser.class, "instance");
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        // robolectric does not clean up singletons correctly
        SwrveTestUtils.removeSingleton(QaUser.class, "instance");
    }

    @Test
    public void testInitAndUpdate() {

        QaUser qaUser = QaUser.getInstance();
        assertFalse(QaUser.isLoggingEnabled());
        assertNotNull(QaUser.restClient);
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
    public void testGeoCampaignTriggered() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn("true").when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).executeRestClient(Mockito.anyString(), Mockito.anyString());
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
        String body = getExpectedBody(qaUser, "geo-sdk","geo-campaign-triggered", logDetails);

        verifyRestRequest(qaUserSpy, body);
    }

    @Test
    public void testGeoCampaignsDownloadedWithNoCampaigns() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn("true").when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).executeRestClient(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(999L).when(qaUserSpy).getTime();

        QaUser.geoCampaignsDownloaded(123, 456, "enter", Collections.<QaGeoCampaignInfo>emptyList());

        String logDetails = "{" +
                "\"geoplace_id\":123," +
                "\"geofence_id\":456," +
                "\"action_type\":\"enter\"," +
                "\"campaigns\":[]" +
                "}";
        String body = getExpectedBody(qaUser, "geo-sdk","geo-campaigns-downloaded", logDetails);

        verifyRestRequest(qaUserSpy, body);
    }

    @Test
    public void testGeoCampaignsDownloadedWithCampaigns() {

        QaUser qaUser = QaUser.getInstance();
        Mockito.doReturn("true").when(swrveCommonSpy).getCachedData(qaUser.userId, CACHE_QA);
        QaUser.update();
        qaUser = QaUser.getInstance();
        assertTrue(QaUser.isLoggingEnabled());

        QaUser qaUserSpy = Mockito.spy(qaUser);
        QaUser.instance = qaUserSpy;

        Mockito.doNothing().when(qaUserSpy).executeRestClient(Mockito.anyString(), Mockito.anyString());
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
        String body = getExpectedBody(qaUser, "geo-sdk","geo-campaigns-downloaded", logDetails);

        verifyRestRequest(qaUserSpy, body);
    }

    private String getExpectedBody(QaUser qaUser, String logSource, String logType, String logDetails) {
        return "{" +
                   "\"user\":\"" + qaUser.userId + "\"," +
                   "\"session_token\":\"" + qaUser.sessionToken + "\"," +
                   "\"version\":\"3\"," +
                   "\"app_version\":\"" + appVersion + "\"," +
                   "\"unique_device_id\":\"" + deviceId + "\"," +
                   "\"data\":[" +
                       "{" +
                           "\"seqnum\":1," +
                           "\"time\":999," +
                           "\"type\":\"qa_log_event\"," +
                           "\"log_source\":\"" + logSource + "\"," +
                           "\"log_type\":\"" + logType + "\"," +
                           "\"log_details\":" + logDetails +
                       "}" +
                   "]" +
               "}";
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
