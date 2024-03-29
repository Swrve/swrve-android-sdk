package com.swrve.sdk;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class SwrveCampaignAndResourcesTest extends SwrveBaseTest {

    private Swrve swrveSpy;
    private int campaignAndResourcesUpdatesCounter = 0;
    private boolean refreshCampaignsAndResourcesCalled = false;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        SwrveTestUtils.disableRestClientExecutor(swrveSpy);
    }

    @Before
    public void tearDown() throws Exception {
        super.tearDown();
        // change default back -1 because one of the tests changes it. Important this is done to avoid affecting other tests because it is a static variable
        SwrveImp.DEFAULT_IDENTIFY_REFRESH_PERIOD = -1;
    }

    @Test
    public void testStartCampaignsAndResourcesTimer() {
        // Mock calls to checkForCampaignAndResourcesUpdates to do nothing and count them
        mockAndCountCallsToCheckForCampaignAndResourcesUpdates();

        assertNull(swrveSpy.campaignsAndResourcesExecutor);

        swrveSpy.init(mActivity);

        assertNotNull(swrveSpy.campaignsAndResourcesExecutor);

        ArgumentCaptor<Boolean> sessionStartCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(swrveSpy, atLeastOnce()).startCampaignsAndResourcesTimer(sessionStartCaptor.capture());
        assertEquals(true, sessionStartCaptor.getValue());
        Mockito.verify(swrveSpy, atLeastOnce()).refreshCampaignsAndResources();

        assertEquals(true, swrveSpy.eventsWereSent);
    }

    @Test
    public void testStartCampaignsAndResourcesTimerFromIAP() {
        // Mock calls to checkForCampaignAndResourcesUpdates to do nothing and count them
        mockAndCountCallsToCheckForCampaignAndResourcesUpdates();
        swrveSpy.campaignsAndResourcesFlushFrequency = 10000;

        assertNull(swrveSpy.campaignsAndResourcesExecutor);

        swrveSpy.init(mActivity);

        swrveSpy._iap(1, "productId", 2, "currency",
                new SwrveIAPRewards(), "receipt", "receiptSignature", "paymentProvider");


        assertNotNull(swrveSpy.campaignsAndResourcesExecutor);

        ArgumentCaptor<Boolean> sessionStartCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(swrveSpy, atLeastOnce()).startCampaignsAndResourcesTimer(sessionStartCaptor.capture());
        assertEquals(false, sessionStartCaptor.getValue());
        Mockito.verify(swrveSpy, atMost(1)).refreshCampaignsAndResources(); // campaigns don't need to be refreshed

        assertEquals(true, swrveSpy.eventsWereSent);
    }

    @Test
    public void testCheckForCampaignAndResourcesUpdatesTimer() {
        // This test is to simply verify the repeating timer kicks off every 500ms

        // Set up that that the checkForCampaignAndResourcesUpdates is called every 500ms.
        SharedPreferences settings = mActivity.getSharedPreferences("swrve_prefs", 0);
        settings.edit().putInt("swrve_cr_flush_frequency", 500).apply();

        // Mock calls to checkForCampaignAndResourcesUpdates to do nothing and count them
        mockAndCountCallsToCheckForCampaignAndResourcesUpdates();

        swrveSpy.init(mActivity);

        Callable<Integer> callable = () -> campaignAndResourcesUpdatesCounter;
        // await max of 7 seconds and verify checkForCampaignAndResourcesUpdates method is called at least an arbitrary 10 times.
        await("The checkForCampaignAndResourcesUpdates method timer should be called repeatedly every 500ms")
                .atMost(7000, TimeUnit.MILLISECONDS).until(callable, CoreMatchers.equalTo(10));
    }

    @Test
    public void testCheckForCampaignAndResourcesUpdates() throws Exception {

        final String userId = swrveSpy.profileManager.getUserId();
        final String sessionToken = swrveSpy.profileManager.getSessionToken();

        swrveSpy.campaignsAndResourcesFlushRefreshDelay = 500;
        swrveSpy.initialisedTime = new Date();

        // Mock calls to refreshCampaignsAndResources to do nothing and count them
        mockAndCheckCallsToRefreshCampaignsAndResources();

        // -verify no refresh
        swrveSpy.eventsWereSent = false;
        swrveSpy.checkForCampaignAndResourcesUpdates();
        Callable<Boolean> callable = () -> refreshCampaignsAndResourcesCalled;
        // await max of 2 seconds and verify refreshCampaignsAndResources IS NOT called.
        await().atMost(2000, TimeUnit.MILLISECONDS).until(callable, CoreMatchers.equalTo(false));
        verify(swrveSpy, never())._sendQueuedEvents(userId, sessionToken, false);
        verify(swrveSpy, never())._sendQueuedEvents(userId, sessionToken, true);

        // -verify refresh happens when eventsWereSent==true
        swrveSpy.eventsWereSent = true;
        swrveSpy.checkForCampaignAndResourcesUpdates();
        callable = () -> refreshCampaignsAndResourcesCalled;
        // await max of 2 seconds and verify refreshCampaignsAndResources IS called.
        await().atMost(2000, TimeUnit.MILLISECONDS).until(callable, CoreMatchers.equalTo(true));
        assertEquals("eventsWereSent should be reset to false if refreshing ", false, swrveSpy.eventsWereSent);
        verify(swrveSpy, never())._sendQueuedEvents(userId, sessionToken, false);

        // -verify no refresh happens
        refreshCampaignsAndResourcesCalled = false; // reset to false
        swrveSpy.eventsWereSent = false;
        swrveSpy.checkForCampaignAndResourcesUpdates();
        callable = () -> refreshCampaignsAndResourcesCalled;
        // await max of 2 seconds and verify refreshCampaignsAndResources IS NOT called.
        await().atMost(2000, TimeUnit.MILLISECONDS).until(callable, CoreMatchers.equalTo(false));
        verify(swrveSpy, never())._sendQueuedEvents(userId, sessionToken, false);
        verify(swrveSpy, never())._sendQueuedEvents(userId, sessionToken, true);

        // -verify refresh happens and events are sent when eventsWereSent==true and an event is queued
        swrveSpy.eventsWereSent = true;
        swrveSpy.multiLayerLocalStorage.addEvent(swrveSpy.getUserId(), "eventString");
        swrveSpy.checkForCampaignAndResourcesUpdates();
        callable = () -> refreshCampaignsAndResourcesCalled;
        // await max of 2 seconds and verify refreshCampaignsAndResources IS called.
        await().atMost(2000, TimeUnit.MILLISECONDS).until(callable, CoreMatchers.equalTo(true));
        assertEquals("eventsWereSent should be reset to false if refreshing ", false, swrveSpy.eventsWereSent);
        verify(swrveSpy, times(1))._sendQueuedEvents(userId, sessionToken, false);

        // -verify refresh happens when events in the queue
        refreshCampaignsAndResourcesCalled = false; // reset to false
        swrveSpy.eventsWereSent = false;
        swrveSpy.multiLayerLocalStorage.addEvent(swrveSpy.getUserId(), "eventString");
        swrveSpy.checkForCampaignAndResourcesUpdates();
        callable = () -> refreshCampaignsAndResourcesCalled;
        // await max of 2 seconds and verify refreshCampaignsAndResources IS called.
        await().atMost(2000, TimeUnit.MILLISECONDS).until(callable, CoreMatchers.equalTo(true));
        assertEquals("eventsWereSent should be reset to false if refreshing ", false, swrveSpy.eventsWereSent);
        verify(swrveSpy, times(2))._sendQueuedEvents(userId, sessionToken, false);
    }

    @Test
    public void testInvokeListenerOnceUserResources() {
        // execute the rest and storage calls on same threads
        SwrveTestUtils.runSingleThreaded(swrveSpy);
        String campaignsResponseJson  = "{\"user_resources\": [{\"test_id\": \"test_value\"}]}";

        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.campaignsAndResourcesInitialized = true;
        swrveSpy.init(mActivity);

        verify(swrveSpy, times(1)).invokeResourceListener();
    }

    @Test
    public void testInvokeListenerOnceRTUP() {
        // execute the rest and storage calls on same threads
        SwrveTestUtils.runSingleThreaded(swrveSpy);
        String campaignsResponseJson  = "{\"real_time_user_properties\": {\"test_id\": \"test_value\"}}";

        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.campaignsAndResourcesInitialized = false;
        swrveSpy.init(mActivity);

        verify(swrveSpy, times(1)).invokeResourceListener();
    }

    @Test
    public void testInvokeListenerOnceForBoth() {
        // execute the rest and storage calls on same threads
        SwrveTestUtils.runSingleThreaded(swrveSpy);
        String campaignsResponseJson  = SwrveTestUtils.getAssetAsText(mActivity, "rtup_resources.json");
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.init(mActivity);

        verify(swrveSpy, times(1)).invokeResourceListener();
    }

    @Test
    public void testInvokeListenerOnceFirstTime() {
        // execute the rest and storage calls on same threads
        SwrveTestUtils.runSingleThreaded(swrveSpy);
        String campaignsResponseJson = "{}";
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.campaignsAndResourcesInitialized = false;
        swrveSpy.init(mActivity);

        verify(swrveSpy, times(1)).invokeResourceListener();
    }

    @Test
    public void testInvokeListenerNotCalled() {
        // execute the rest and storage calls on same threads
        SwrveTestUtils.runSingleThreaded(swrveSpy);
        String campaignsResponseJson = "{}";
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.campaignsAndResourcesInitialized = true;
        swrveSpy.init(mActivity);

        verify(swrveSpy, times(0)).invokeResourceListener();
    }

    @Test
    public void testSameIdentifyRefreshPeriod() {
        SwrveTestUtils.runSingleThreaded(swrveSpy);
        SwrveImp.DEFAULT_IDENTIFY_REFRESH_PERIOD = 21; // change default to 21
        assertEquals(21, swrveSpy.getIdentifyPeriod());
        String campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity, "campaign_with_identify_refresh_period.json");
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.init(mActivity);
        assertEquals(21, swrveSpy.getIdentifyPeriod());
        verify(swrveSpy, times(1)).reIdentifyUser(); // reIdentifyUser should be called once
    }

    @Test
    public void testNewIdentifyRefreshPeriodTriggersReIdentifyUser() {
        SwrveTestUtils.runSingleThreaded(swrveSpy);
        assertEquals(-1, swrveSpy.getIdentifyPeriod());
        String campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity, "campaign_with_identify_refresh_period.json");
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.init(mActivity);
        assertEquals(21, swrveSpy.getIdentifyPeriod()); // verify new value is saved to storage
        verify(swrveSpy, times(2)).reIdentifyUser(); // reIdentifyUser should be called twice
    }

    // Private helper methods

    private void mockAndCountCallsToCheckForCampaignAndResourcesUpdates() {
        doAnswer((Answer<Void>) invocation -> {
            SwrveLogger.d("checkForCampaignAndResourcesUpdates method is mocked to do nothing. Called counter:%s", campaignAndResourcesUpdatesCounter);
            campaignAndResourcesUpdatesCounter++;
            return null;
        }).when(swrveSpy).checkForCampaignAndResourcesUpdates();
    }

    private void mockAndCheckCallsToRefreshCampaignsAndResources() {
        doAnswer((Answer<Void>) invocation -> {
            SwrveLogger.d("refreshCampaignsAndResources method is mocked to do nothing.");
            refreshCampaignsAndResourcesCalled = true;
            return null;
        }).when(swrveSpy).refreshCampaignsAndResources();
    }
}
