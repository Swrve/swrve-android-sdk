package com.swrve.sdk;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageListener;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.test.MainActivity;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.swrve.sdk.ISwrveCommon.SDK_PREFS_NAME;
import static junit.framework.TestCase.assertTrue;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SwrveInitModeTest extends SwrveBaseTest {

    private Swrve swrveReal;
    private Swrve swrveSpy;

    private SwrveNotificationConfig notificationConfig = new SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, com.swrve.sdk.test.R.drawable.ic_launcher, null)
            .activityClass(MainActivity.class)
            .build();

    private void createSwrveSpy(SwrveConfig config) throws Exception {
        swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);
        swrveSpy = Mockito.spy(swrveReal);
        doNothing().when(swrveSpy).beforeSendDeviceInfo(any(Context.class));
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveCommon.setSwrveCommon(swrveSpy);
        doReturn(true).when(swrveSpy).restClientExecutorExecute(any(Runnable.class)); // disable rest
        doNothing().when(swrveSpy).sendEventsInBackground(any(Context.class), anyString(), any(ArrayList.class));
        SwrveTestUtils.disableSwrveBackgroundEventSender(swrveSpy);
    }

    @Test
    public void testAutoInitMode() throws Exception {
        SwrveConfig config = new SwrveConfig();
        assertEquals(config.getInitMode(), SwrveInitMode.AUTO);

        createSwrveSpy(config);

        swrveSpy.onCreate(mActivity);
        assertTrue(swrveReal.isStarted());
        assertTrue(isLifecycleRegistered(swrveReal));
    }

    @Test
    public void testManagedInitMode() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.MANAGED);
        config.setAutoStartLastUser(false);

        createSwrveSpy(config);

        swrveSpy.onActivityCreated(mActivity, null);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy); // required so previous line call to onActivityCreated fully executes.
        assertFalse(swrveSpy.isStarted());
        assertFalse(isLifecycleRegistered(swrveSpy));

        swrveSpy.start(mActivity);
        assertTrue(swrveSpy.isStarted());
        assertTrue(isLifecycleRegistered(swrveReal));
    }

    private boolean isLifecycleRegistered(Swrve swrveSpy) throws Exception {
        // Check if the app lifecycle has been registered
        Method method = Application.class.getDeclaredMethod("collectActivityLifecycleCallbacks");
        method.setAccessible(true);
        Object[] lifecycleCallbacks = (Object[]) method.invoke(mActivity.getApplication());
        boolean foundCallback = false;
        if (lifecycleCallbacks != null) {
            for (int i = 0; i < lifecycleCallbacks.length && !foundCallback; i++) {
                foundCallback = (lifecycleCallbacks[i] == swrveSpy);
            }
        }
        return foundCallback;
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testManagedInitModeIdentityThrowsException() throws Exception {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Cannot call Identify when running on SwrveInitMode.MANAGED mode");

        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.MANAGED);

        Swrve swrve = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);
        swrve.identify("exernal", new SwrveIdentityResponse() {
            @Override
            public void onSuccess(String status, String swrveId) {
            }

            @Override
            public void onError(int responseCode, String errorMessage) {
            }
        });
    }

    @Test
    public void testAutoInitModeCannotCallStartWithUserId() {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Cannot call start method when running on SwrveInitMode.AUTO mode");

        Swrve swrve = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrve.start(mActivity, "another_user");
    }

    private void mockPreviousUserInStorage(String userId) {
        // Set previous user id saved to disk
        SharedPreferences settings = mActivity.getSharedPreferences(SDK_PREFS_NAME, 0);
        settings.edit().putString("userId", userId).commit();
    }

    @Test
    public void testMethodsDoNothingManagedMode() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.MANAGED);
        createSwrveSpy(config);
        assertMethodsDoNothing();
    }

    @Test
    public void testMethodsDoNothingAutoMode() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.AUTO);
        createSwrveSpy(config);
        assertMethodsDoNothing();
    }

    private void assertMethodsDoNothing() throws Exception {
        HashMap testMap = new HashMap();
        testMap.put("key", "value");

        // Cannot call these methods when it is not inited
        swrveSpy.sessionStart();
        swrveSpy.sessionEnd();
        swrveSpy.event("test");
        swrveSpy.event("test", testMap);
        swrveSpy.purchase("item_purchase", "€", 99, 5);
        swrveSpy.currencyGiven("gold", 20);
        swrveSpy.userUpdate(new HashMap<>());
        swrveSpy.userUpdate("Property", new Date());
        SwrveIAPRewards rewards = new SwrveIAPRewards();
        swrveSpy.iap(2, "com.swrve.product2", 1.99, "EUR");
        swrveSpy.iap(2, "com.swrve.product2", 1.99, "EUR", rewards);
        assertNoEventsWereQueued();

        SwrveResourceManager rm1 = swrveSpy.getResourceManager();
        SwrveResourceManager rm2 = swrveSpy.getResourceManager();
        assertNotNull(rm1);
        assertNotNull(rm2);
        assertNotEquals(rm1, rm2);

        final boolean[] userResourcesSuccessCalled = {false};
        swrveSpy.getUserResources(new SwrveUserResourcesListener() {
            @Override
            public void onUserResourcesSuccess(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
                userResourcesSuccessCalled[0] = true;
            }

            @Override
            public void onUserResourcesError(Exception exception) {
            }
        });
        assertFalse(userResourcesSuccessCalled[0]);

        final boolean[] userResourcesDiffSuccessCalled = {false};
        swrveSpy.getUserResourcesDiff(new SwrveUserResourcesDiffListener() {
            @Override
            public void onUserResourcesDiffSuccess(Map<String, Map<String, String>> oldResourcesValues, Map<String, Map<String, String>> newResourcesValues, String resourcesAsJSON) {
                userResourcesDiffSuccessCalled[0] = true;
            }

            @Override
            public void onUserResourcesDiffError(Exception exception) {

            }
        });
        assertFalse(userResourcesDiffSuccessCalled[0]);

        swrveSpy.sendQueuedEvents();
        verify(swrveSpy, Mockito.atMost(0)).restClientExecutorExecute(Mockito.any(Runnable.class));

        swrveSpy.flushToDisk();
        verify(swrveSpy, Mockito.atMost(0)).storageExecutorExecute(Mockito.any(Runnable.class));

        assertEquals(null, swrveSpy.getJoined());
        assertEquals(0, swrveSpy.getDeviceInfo().length());

        swrveSpy.refreshCampaignsAndResources();
        verify(swrveSpy, Mockito.atMost(0)).restClientExecutorExecute(Mockito.any(Runnable.class));

        swrveSpy.buttonWasPressedByUser(null);
        assertNoEventsWereQueued();

        swrveSpy.messageWasShownToUser(null);
        assertNoEventsWereQueued();

        assertNull(swrveSpy.getAppStoreURLForApp(1));

        Date d1 = swrveSpy.getInitialisedTime();
        Date d2 = swrveSpy.getInitialisedTime();
        assertNotNull(d1);
        assertNotNull(d2);
        assertNotSame(d1, d2);

        assertEquals(0, swrveSpy.getMessageCenterCampaigns().size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape).size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns((Map<String, String>) null).size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape, null).size());

        final boolean[] messageListenerCalled = {false};
        swrveSpy.setMessageListener(new SwrveMessageListener() {
            @Override
            public void onMessage(SwrveMessage message) {
                onMessage(message, null);
            }

            @Override
            public void onMessage(SwrveMessage message, Map<String, String> properties) {
                messageListenerCalled[0] = true;
            }
        });
        String text = SwrveTestUtils.getAssetAsText(mActivity, "campaign_trigger_condition.json");
        assertNotNull(text);
        JSONObject jsonObject = new JSONObject(text);
        SwrveInAppCampaign campaign = new SwrveInAppCampaign(SwrveTestUtils.getTestSwrveCampaignManager(), new SwrveCampaignDisplayer(), jsonObject, new HashSet<>(), null);
        swrveSpy.showMessageCenterCampaign(campaign);
        assertFalse(messageListenerCalled[0]);

        swrveSpy.removeMessageCenterCampaign(campaign);
        swrveSpy.markMessageCenterCampaignAsSeen(campaign);
        assertEquals(campaign.getStatus(), SwrveCampaignState.Status.Unseen);

        Bundle bundle = new Bundle();
        swrveSpy.handleDeferredDeeplink(bundle);
        swrveSpy.handleDeeplink(bundle);
        assertNull(swrveSpy.swrveDeeplinkManager);

        swrveSpy.setLanguage(Locale.CANADA);
        assertEquals("en-CA", swrveSpy.getLanguage());

        assertEquals("apiKey", swrveSpy.getApiKey());

        assertNotNull(swrveSpy.getUserId());

        swrveSpy.queueConversationEvent("name", "payload", "page", 0, testMap);
        assertNoEventsWereQueued();

        assertNull(swrveSpy.getExternalUserId());

        swrveSpy.setCustomPayloadForConversationInput(testMap);
        assertNull(SwrveConversationEventHelper.getCustomPayload());
    }

    private void assertProcessedEngagedIntent() throws PendingIntent.CanceledException {
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        bundle.putString(SwrveNotificationConstants.SWRVE_INFLUENCED_WINDOW_MINS_KEY, "720");
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "body");
        bundle.putString("customData", "some custom values");
        bundle.putString("sound", "default");
        int firstTimestamp = (int) (new Date().getTime() % Integer.MAX_VALUE);
        bundle.putString(SwrveNotificationConstants.TIMESTAMP_KEY, Integer.toString(firstTimestamp));

        SwrveNotificationBuilder builderSpy = spy(new SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig));
        SwrveNotificationTestUtils.displayNotification(mActivity, builderSpy, bundle);

        Notification notification = SwrveNotificationTestUtils.assertNotification("body", "content://settings/system/notification_sound", bundle);
        SwrveNotificationTestUtils.assertNumberOfNotifications(1);

        notification.contentIntent.send();

        // Launch SwrveNotificationEngageReceiver (imitate single engagement with notification)
        List<Intent> broadcastIntents = mShadowActivity.getBroadcastIntents();
        assertEquals(1, broadcastIntents.size());
        Intent engageEventIntent = broadcastIntents.get(0);
        SwrveNotificationEngageReceiver engageReceiver = new SwrveNotificationEngageReceiver();
        // Clear pending intents
        mShadowActivity.getBroadcastIntents().clear();
        engageReceiver.onReceive(mActivity, engageEventIntent);

        List<Intent> broadcastIntentsAfterOnReceive = mShadowActivity.getBroadcastIntents();
        assertEquals(1, broadcastIntentsAfterOnReceive.size());
        assertEquals("android.intent.action.CLOSE_SYSTEM_DIALOGS", broadcastIntentsAfterOnReceive.get(0).getAction());

        // Should send an engagement event
        String expectedEvent = "{" +
                "\"type\":\"event\"," +
                "\"time\":987654321," +
                "\"seqnum\":1," +
                "\"name\":\"Swrve.Messages.Push-1.engaged\"" +
                "}";
        ArrayList<String> expectedEventArrayList = new ArrayList<>();
        expectedEventArrayList.add(expectedEvent);
        verify(swrveSpy, Mockito.atLeastOnce()).sendEventsInBackground(mActivity, SwrveSDK.getUserId(), expectedEventArrayList);
    }

    @Test
    public void testEngagedNoUserEver() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.MANAGED);

        createSwrveSpy(config);

        assertProcessedEngagedIntent();
    }

    @Test
    public void testEngagedNotStarted() throws Exception {
        mockPreviousUserInStorage("previous_user_id");

        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.MANAGED);
        config.setAutoStartLastUser(false);

        createSwrveSpy(config);

        assertProcessedEngagedIntent();
    }

    private void assertNoEventsWereQueued() {
        verify(swrveSpy, Mockito.atMost(0)).queueEvent(anyString(), Mockito.any(Map.class), Mockito.any(Map.class));
        verify(swrveSpy, Mockito.atMost(0)).queueEvent(anyString(), anyString(), Mockito.any(Map.class), Mockito.any(Map.class), anyBoolean());
    }

    private String getStoredUserId() {
        // Should not be stored
        SharedPreferences settings = mActivity.getSharedPreferences(SDK_PREFS_NAME, 0);
        return settings.getString("userId", null);
    }

    @Test
    public void testAutoStartOnManagedMode() throws Exception {
        mockPreviousUserInStorage("previous_user_id");

        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.MANAGED);
        assertTrue(config.isAutoStartLastUser());

        createSwrveSpy(config);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveReal);

        swrveSpy.onCreate(mActivity);
        assertTrue(swrveReal.isStarted());
        assertTrue(isLifecycleRegistered(swrveReal));
        assertEquals("previous_user_id", swrveSpy.getUserId());
        assertEquals("previous_user_id", getStoredUserId());

        SwrveTestUtils.flushLifecycleExecutorQueue(swrveReal);
        assertTrue(swrveReal.isStarted());
    }

    @Test
    public void testAutoStartOnAutoMode() throws Exception {
        mockPreviousUserInStorage("previous_user_id");

        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.AUTO);
        assertTrue(config.isAutoStartLastUser());

        createSwrveSpy(config);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveReal);

        swrveSpy.onCreate(mActivity);
        assertTrue(swrveReal.isStarted());
        assertTrue(isLifecycleRegistered(swrveReal));
        assertEquals("previous_user_id", swrveSpy.getUserId());
        assertEquals("previous_user_id", getStoredUserId());

        SwrveTestUtils.flushLifecycleExecutorQueue(swrveReal);
        assertTrue(swrveReal.isStarted());
    }

    @Test
    public void testAutoStartOnNoPreviousUserManagedMode() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.MANAGED);
        assertTrue(config.isAutoStartLastUser());

        createSwrveSpy(config);

        swrveSpy.onActivityCreated(mActivity, null);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy); // required so previous line call to onActivityCreated fully executes.
        assertFalse(swrveSpy.isStarted());
        assertTrue(isLifecycleRegistered(swrveReal)); // lifecycle is always registered as of SDK 8.0

        // Should not be stored
        assertNull(getStoredUserId());

        swrveReal.onActivityCreated(mActivity, null); // fake activity lifecycle called and verify lifecycle gets unregistered
        assertTrue(isLifecycleRegistered(swrveReal)); // lifecycle is always registered as of SDK 8.0
        assertFalse(swrveReal.isStarted());

        SwrveTestUtils.flushLifecycleExecutorQueue(swrveReal);
        assertFalse(swrveReal.isStarted());
    }

    @Test
    public void testAutoStartOnNoPreviousUserAutoMode() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.AUTO);
        assertTrue(config.isAutoStartLastUser());

        createSwrveSpy(config);

        swrveSpy.onActivityCreated(mActivity, null);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveReal); // required so previous line call to onActivityCreated fully executes.
        assertTrue(swrveReal.isStarted());
        assertTrue(isLifecycleRegistered(swrveReal)); // lifecycle is always registered as of SDK 8.0
        // userid should be stored
        assertNotNull(getStoredUserId());
    }

    @Test
    public void testAutoStartOffManagedMode() throws Exception {
        mockPreviousUserInStorage("previous_user_id");

        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.MANAGED);
        config.setAutoStartLastUser(false);

        createSwrveSpy(config);

        swrveSpy.onActivityCreated(mActivity, null);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy); // required so previous line call to onActivityCreated fully executes.
        assertFalse(swrveSpy.isStarted());
        assertTrue(isLifecycleRegistered(swrveReal)); // lifecycle is always registered as of SDK 8.0
        assertEquals("previous_user_id", swrveSpy.getUserId());
        assertEquals("previous_user_id", getStoredUserId());

        SwrveTestUtils.flushLifecycleExecutorQueue(swrveReal);
        assertFalse(swrveReal.isStarted());

        SwrveSDK.start(mActivity);
        assertTrue(swrveSpy.isStarted());

        // Create another instance with same config and verify it is not started.
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        createSwrveSpy(config);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy);
        assertFalse(swrveSpy.isStarted());
    }

    @Test
    public void testAutoStartOffAutoMode() throws Exception {
        mockPreviousUserInStorage("previous_user_id");

        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.AUTO);
        config.setAutoStartLastUser(false);

        createSwrveSpy(config);

        swrveSpy.onActivityCreated(mActivity, null);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy); // required so previous line call to onActivityCreated fully executes.
        assertFalse(swrveSpy.isStarted());
        assertTrue(isLifecycleRegistered(swrveReal)); // lifecycle is always registered as of SDK 8.0
        assertEquals("previous_user_id", swrveSpy.getUserId());
        assertEquals("previous_user_id", getStoredUserId());

        SwrveTestUtils.flushLifecycleExecutorQueue(swrveReal);
        assertFalse(swrveReal.isStarted());

        // start the sdk
        swrveSpy.start(mActivity);
        assertTrue(swrveSpy.isStarted());

        // Create another instance with same config and verify it is not started.
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        createSwrveSpy(config);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy);
        assertFalse(swrveSpy.isStarted());
    }

    @Test
    public void testAutoStartOffAutoMode_startViaIdentify() throws Exception {
        mockPreviousUserInStorage("previous_user_id");

        SwrveConfig config = new SwrveConfig();
        config.setInitMode(SwrveInitMode.AUTO);
        config.setAutoStartLastUser(false);

        createSwrveSpy(config);

        swrveSpy.onActivityCreated(mActivity, null);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy); // required so previous line call to onActivityCreated fully executes.
        assertFalse(swrveSpy.isStarted());
        assertTrue(isLifecycleRegistered(swrveReal)); // lifecycle is always registered as of SDK 8.0
        assertEquals("previous_user_id", swrveSpy.getUserId());
        assertEquals("previous_user_id", getStoredUserId());

        SwrveTestUtils.flushLifecycleExecutorQueue(swrveReal);
        assertFalse(swrveReal.isStarted());

        // mock the profileManager identify so it does returns successfully
        SwrveProfileManager profileManagerSpy = Mockito.spy(swrveSpy.profileManager);
        swrveSpy.profileManager = profileManagerSpy;
        doAnswer((Answer<Void>) invocation -> {
            SwrveIdentityResponse callback = (SwrveIdentityResponse) invocation.getArguments()[3];
            callback.onSuccess("", "previous_user_id");

            return null;
        }).when(swrveSpy.profileManager).identify(anyString(), anyString(), anyString(), any(SwrveIdentityResponse.class));

        // start the sdk by calling identify method
        final AtomicBoolean identityCallback = new AtomicBoolean(false);
        swrveSpy.identify("externalUserId", new SwrveIdentityResponse() {
            @Override
            public void onSuccess(String status, String swrveId) {
                identityCallback.set(true);
            }
            @Override
            public void onError(int responseCode, String errorMessage) {
                identityCallback.set(true);
            }
        });
        await().untilTrue(identityCallback);

        assertTrue(swrveSpy.isStarted());

        // Create another instance with same config and verify it is not started.
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        createSwrveSpy(config);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy);
        assertFalse(swrveSpy.isStarted());
    }
}
