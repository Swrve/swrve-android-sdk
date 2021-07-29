package com.swrve.sdk;

import android.content.Context;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageListener;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.swrve.sdk.SwrveTrackingState.STARTED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StopTrackingAndPublicApisTest extends SwrveBaseTest {

    private Swrve swrveSpy;
    private SwrveBackgroundEventSender backgroundEventSenderMock;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();

        backgroundEventSenderMock = mock(SwrveBackgroundEventSender.class);
        doNothing().when(backgroundEventSenderMock).send(anyString(), anyList());
        doReturn(backgroundEventSenderMock).when(swrveSpy).getSwrveBackgroundEventSender(any(Context.class));

        // fake init the sdk.
        SwrveProfileManager mockProfileManager = mock(SwrveProfileManager.class);
        when(mockProfileManager.getTrackingState()).thenReturn(STARTED);
        when(mockProfileManager.getUserId()).thenReturn("userid");
        swrveSpy.profileManager = mockProfileManager;
        swrveSpy.started = true;
        SwrveAssetsManager mockAssetsManager = mock(SwrveAssetsManager.class);
        when(mockAssetsManager.getStorageDir()).thenReturn(new File("test"));
        swrveSpy.swrveAssetsManager = mockAssetsManager;
    }

    @Test
    public void testSDKStoppedAndPublicApis() throws Exception {

        assertTrue(swrveSpy.isSdkReady());

        // Stop the sdk and add all the external API calls and objects that a customer can invoke
        SwrveSDK.stopTracking();
        assertFalse(swrveSpy.isSdkReady());

        Map<String, String> payload = new HashMap<>();
        payload.put("key", "value");
        SwrveIAPRewards rewards = new SwrveIAPRewards("USD", 99);
        rewards.addCurrency("USD", 100);
        rewards.addItem("stars", 1);
        JSONObject rewardsJson = rewards.getRewardsJSON();
        assertNotNull(rewardsJson);

        SwrveSDK.sessionStart();
        verify(swrveSpy, never())._sessionStart();

        SwrveSDK.sessionEnd();
        verify(swrveSpy, never())._sessionEnd();

        SwrveSDK.event("new_event");
        verify(swrveSpy, never())._event("new_event");

        SwrveSDK.event("event", payload);
        verify(swrveSpy, never())._event("new_event", payload);

        SwrveSDK.purchase("item", "USB", 10, 1);
        verify(swrveSpy, never())._purchase("item", "USB", 10, 1);

        SwrveSDK.currencyGiven("diamonds", 99);
        verify(swrveSpy, never())._currencyGiven("diamonds", 99);

        SwrveSDK.userUpdate(payload);
        verify(swrveSpy, never())._userUpdate(payload);

        SwrveSDK.iap(1, "productId", 0.99, "USB");
        verify(swrveSpy, never())._iap(1, "productId", 0.99, "USB");

        SwrveSDK.iap(1, "productId", 0.99, "USB", rewards);
        verify(swrveSpy, never())._iap(1, "productId", 0.99, "USB", rewards);

        SwrveSDK.getResourceManager();
        verify(swrveSpy, never())._getResourceManager();

        SwrveSDK.setResourcesListener(() -> {
        });
        SwrveSDK.getUserResources(new SwrveUserResourcesListener() {
            @Override
            public void onUserResourcesSuccess(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
            }

            @Override
            public void onUserResourcesError(Exception exception) {
            }
        });
        verify(swrveSpy, never())._getUserResources(any(SwrveUserResourcesListener.class));
        SwrveSDK.getUserResourcesDiff(new SwrveUserResourcesDiffListener() {
            @Override
            public void onUserResourcesDiffSuccess(Map<String, Map<String, String>> oldResourcesValues, Map<String, Map<String, String>> newResourcesValues, String resourcesAsJSON) {
            }

            @Override
            public void onUserResourcesDiffError(Exception exception) {
            }
        });
        verify(swrveSpy, never())._getUserResourcesDiff(any(SwrveUserResourcesDiffListener.class));

        // _sendQueuedEvents gets delayed called from checkForCampaignAndResourcesUpdates so one execution might escape through.
        // So for purpose of this test we check atMost 1.
        verify(swrveSpy, atMost(1))._sendQueuedEvents(anyString(), anyString(), anyBoolean());
        SwrveSDK.sendQueuedEvents();
        SwrveSDK.sendQueuedEvents();
        verify(swrveSpy, atMost(1))._sendQueuedEvents(anyString(), anyString(), anyBoolean());

        SwrveSDK.flushToDisk();
        verify(swrveSpy, never())._flushToDisk();

        SwrveSDK.setLanguage(Locale.JAPAN);
        SwrveSDK.getLanguage();

        String apiKey = SwrveSDK.getApiKey();
        assertNotNull(apiKey);

        String userId = SwrveSDK.getUserId();
        assertNotNull(userId);

        JSONObject deviceInfo = SwrveSDK.getDeviceInfo();
        assertNotNull(deviceInfo);

        SwrveSDK.refreshCampaignsAndResources();
        verify(swrveSpy, never())._refreshCampaignsAndResources();

        String appStoreUrl = SwrveSDK.getAppStoreURLForApp(572);
        assertNull(appStoreUrl);

        File cacheDir = SwrveSDK.getCacheDir();
        assertNotNull(cacheDir);

        SwrveSDK.setMessageListener(new SwrveMessageListener() {
            @Override
            public void onMessage(SwrveMessage message) {
            }

            @Override
            public void onMessage(SwrveMessage message, Map<String, String> properties) {
            }
        });

        Date initialisedTime = SwrveSDK.getInitialisedTime();
        assertNotNull(initialisedTime);

        SwrveConfigBase config = SwrveSDK.getConfig();
        assertNotNull(config);

        doNothing().when(swrveSpy)._identify(anyString(), any(SwrveIdentityResponse.class));
        SwrveSDK.identify("", new SwrveIdentityResponse() {
            @Override
            public void onSuccess(String status, String swrveId) {
            }
            @Override
            public void onError(int responseCode, String errorMessage) {
            }
        });
        verify(swrveSpy, times(1))._identify(anyString(), any(SwrveIdentityResponse.class)); // note this api is allowed.

        // change initmode to MANAGED and stop it again
        Mockito.reset(swrveSpy);
        doNothing().when(swrveSpy).queueDeviceUpdateNow(anyString(), anyString(), anyBoolean());
        SwrveSDK.stopTracking();
        SwrveSDK.getConfig().setInitMode(SwrveInitMode.MANAGED);

        doNothing().when(swrveSpy).start(mActivity);
        SwrveSDK.start(mActivity);
        verify(swrveSpy, times(1)).start(mActivity); // note this api is allowed.

        doNothing().when(swrveSpy).start(mActivity, "custom_user_id");
        SwrveSDK.start(mActivity, "custom_user_id");
        verify(swrveSpy, times(1)).start(mActivity, "custom_user_id"); // note this api is allowed.

        SwrveSDK.shutdown();
        verify(swrveSpy, times(1))._shutdown(); // note this api is allowed.


        Mockito.reset(swrveSpy);
    }
}
