package com.swrve.sdk;

import android.content.Intent;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.swrve.sdk.ISwrveCommon.CACHE_NOTIFICATION_CAMPAIGNS_DEBUG;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doNothing;

public class SwrveNotificationToCampaignTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.disableSwrveBackgroundEventSender(swrveSpy);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        doNothing().when(swrveSpy).checkForCampaignAndResourcesUpdates();
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest

        final String userId = swrveSpy.profileManager.getUserId();
        Map<String, String> params = swrveSpy.getContentRequestParams(userId);
        SwrveDeeplinkManager swrveDeeplinkManager = new SwrveDeeplinkManager(params, swrveSpy.getConfig(), swrveSpy.getContext(), swrveSpy.swrveAssetsManager, swrveSpy.restClient);
        SwrveDeeplinkManager swrveDeeplinkManagerSpy = Mockito.spy(swrveDeeplinkManager);
        swrveSpy.swrveDeeplinkManager = swrveDeeplinkManagerSpy;

        IRESTClient restClient = new IRESTClient() {
            @Override
            public void get(String endpoint, IRESTResponseListener callback) {
            }

            @Override
            public void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) throws UnsupportedEncodingException {
                String response = null;
                if (params.containsValue("295412")) {
                    response = SwrveTestUtils.getAssetAsText(mActivity, "ad_journey_campaign_conversation.json");
                }
                if (params.containsValue("295411")) {
                    response = SwrveTestUtils.getAssetAsText(mActivity, "ad_journey_campaign_message.json");
                }
                callback.onResponse(new RESTResponse(200, response, null));
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback) {
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType) {
                String response = null;
                if (endpoint.contains("batch")) {
                    response = "{}";
                }
                callback.onResponse(new RESTResponse(200, response, null));
            }
        };

        swrveSpy.swrveDeeplinkManager.setRestClient(restClient);
        SwrveCommon.setSwrveCommon(swrveSpy);
        swrveSpy.init(mActivity);
    }

    @Test
    public void testEngagingNotificationSetsCampaignId() {
        Intent intent = createPushEngagedIntent();
        assertNotificationSwrveCampaignId(intent, "295411");
    }

    @Test
    public void testEngagingNotificationDoesNotSetsCampaignIdWhenStopped() {
        SwrveSDK.stopTracking();
        Intent intent = createPushEngagedIntent();
        assertNotificationSwrveCampaignId(intent, null); // notificationSwrveCampaignId remains null
    }

    public void assertNotificationSwrveCampaignId(Intent intent, String notificationSwrveCampaignId) {
        assertNull(swrveSpy.notificationSwrveCampaignId);
        SwrveNotificationEngageReceiver notificationEngageReceiver = new SwrveNotificationEngageReceiver();
        notificationEngageReceiver.onReceive(ApplicationProvider.getApplicationContext().getApplicationContext(), intent);
        assertEquals(swrveSpy.notificationSwrveCampaignId, notificationSwrveCampaignId);
    }

    private Intent createPushEngagedIntent() {
        Intent intent = new Intent();

        Bundle swrve = new Bundle();
        swrve.putInt(SwrveNotificationConstants.SWRVE_TRACKING_KEY, 123456);
        swrve.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, "{ \"campaign\": { \"id\": \"295411\" } }");

        Bundle extras = new Bundle();
        extras.putBundle(SwrveNotificationConstants.PUSH_BUNDLE, swrve);
        extras.putInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, 12345);
        intent.putExtras(extras);
        return intent;
    }

    @Test
    public void testClickingButtonNotificationSetsCampaignId() {
        Intent intent = createPushButtonClickedIntent();
        assertNotificationSwrveCampaignId(intent, "295411");
    }

    @Test
    public void testClickingButtonNotificationDoesNotSetsCampaignIdWhenStopped() {
        SwrveSDK.stopTracking();
        Intent intent = createPushButtonClickedIntent();
        assertNotificationSwrveCampaignId(intent, null); // notificationSwrveCampaignId remains null
    }

    private Intent createPushButtonClickedIntent() {
        Intent intent = new Intent();

        Bundle swrve = new Bundle();
        swrve.putInt(SwrveNotificationConstants.SWRVE_TRACKING_KEY, 123456);

        Bundle extras = new Bundle();
        extras.putBundle(SwrveNotificationConstants.PUSH_BUNDLE, swrve);
        extras.putInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, 12345);
        intent.putExtra(SwrveNotificationConstants.CONTEXT_ID_KEY, "2");
        intent.putExtra(SwrveNotificationConstants.BUTTON_TEXT_KEY, "btn3");
        intent.putExtra(SwrveNotificationConstants.CAMPAIGN_TYPE, com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_TYPE_KEY, SwrveNotificationButton.ActionType.OPEN_CAMPAIGN);
        intent.putExtra(SwrveNotificationConstants.PUSH_ACTION_URL_KEY, "295411");
        intent.putExtras(extras);
        return intent;
    }

    @Test
    public void testLoadCampaignFromNotificationResetNull() {
        swrveSpy.setNotificationSwrveCampaignId("295411");
        swrveSpy.loadCampaignFromNotification();

        ArgumentCaptor<Bundle> bundle = ArgumentCaptor.forClass(Bundle.class);
        Mockito.verify(swrveSpy.swrveDeeplinkManager, Mockito.atLeastOnce()).handleDeeplink(bundle.capture());

        // call it again , notificationCampaign id should of been reset to null , so the loadCampaignFromNotification function should do nothing
        swrveSpy.loadCampaignFromNotification();
        Mockito.verify(swrveSpy.swrveDeeplinkManager, Mockito.atMost(1)).handleDeeplink(bundle.capture());
    }

    @Test
    public void testOnResumeLoadCampaign() {
        swrveSpy.setNotificationSwrveCampaignId("295411");

        swrveSpy._onResume(mActivity);

        ArgumentCaptor<Bundle> bundle = ArgumentCaptor.forClass(Bundle.class);
        Mockito.verify(swrveSpy.swrveDeeplinkManager, Mockito.atLeastOnce()).handleDeeplink(bundle.capture());

        ArgumentCaptor<String> campaignIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> actionTypeCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(swrveSpy.swrveDeeplinkManager, Mockito.atLeastOnce()).loadCampaign(campaignIdCaptor.capture(), actionTypeCaptor.capture());

        assertEquals(campaignIdCaptor.getValue(), "295411");
        assertEquals(actionTypeCaptor.getValue(), "notification_to_campaign");
    }

    @Test
    public void testloadCampaignWrittenToDebugCache() throws Exception {
        swrveSpy.swrveDeeplinkManager.loadCampaign("295411", "notification_to_campaign");

        await().until(campaignShown());

        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        final String userId = swrveCommon.getUserId();
        String notificaitonCampaigns = swrveSpy.multiLayerLocalStorage.getCacheEntry(userId, CACHE_NOTIFICATION_CAMPAIGNS_DEBUG);
        JSONObject rootJson = new JSONObject(notificaitonCampaigns);
        JSONObject campaignJson = rootJson.getJSONObject("campaign");
        assertEquals(campaignJson.getInt("id"), 295411);
    }

    private Callable<Boolean> campaignShown() {
        return () -> {
            ShadowActivity shadowMainActivity = Shadows.shadowOf(mActivity);
            Intent nextIntent = shadowMainActivity.peekNextStartedActivity();
            return (nextIntent != null);
        };
    }

    @Test
    public void testPushToInApp() throws Exception {
        SwrveTestUtils.setSDKInstance(null);
        SwrveCommon.setSwrveCommon(null);

        SwrveConfig config = new SwrveConfig();
        Swrve swrveRealTest = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);

        swrveSpy = Mockito.spy(swrveRealTest);
        SwrveTestUtils.disableSwrveBackgroundEventSender(swrveSpy);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveCommon.setSwrveCommon(swrveSpy);

        Intent engagedIntent = createPushEngagedIntent();
        Intent buttonIntent = createPushButtonClickedIntent();

        SwrveNotificationEngageReceiver notificationEngageReceiver = new SwrveNotificationEngageReceiver();
        notificationEngageReceiver.onReceive(ApplicationProvider.getApplicationContext().getApplicationContext(), engagedIntent);
        notificationEngageReceiver.onReceive(ApplicationProvider.getApplicationContext().getApplicationContext(), buttonIntent);

        ArgumentCaptor<String> campaignIdCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(swrveSpy, Mockito.atLeastOnce()).setNotificationSwrveCampaignId(campaignIdCaptor.capture());

        assertEquals(campaignIdCaptor.getValue(), "295411");
    }
}
