package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.awaitility.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.swrve.sdk.SwrveTrackingState.STARTED;
import static com.swrve.sdk.SwrveTrackingState.STOPPED;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class StopTrackingTest extends SwrveBaseTest {

    private Swrve swrveSpy;
    private SwrveBackgroundEventSender backgroundEventSenderMock;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();

        backgroundEventSenderMock = mock(SwrveBackgroundEventSender.class);
        doNothing().when(backgroundEventSenderMock).send(anyString(), anyList());
        doReturn(backgroundEventSenderMock).when(swrveSpy).getSwrveBackgroundEventSender(any(Context.class));

        swrveSpy.init(mActivity);
    }

    @Test
    public void testStopTracking() {

        assertEquals(STARTED, swrveSpy.profileManager.getTrackingState());
        assertTrue(SwrveSDK.isStarted());
        verify(swrveSpy, times(1)).queueDeviceUpdateNow(anyString(), anyString(), Mockito.anyBoolean()); // device info queued once upon init
        assertTrue(swrveSpy.isSdkReady());

        SwrveSDK.stopTracking();

        assertEquals(STOPPED, swrveSpy.profileManager.getTrackingState());
        assertFalse(SwrveSDK.isStarted());
        verify(swrveSpy, times(2)).queueDeviceUpdateNow(anyString(), anyString(), Mockito.anyBoolean()); // new device info queued after stop
        assertFalse(swrveSpy.isSdkReady());

        verify(swrveSpy, atLeastOnce()).clearAllAuthenticatedNotifications(); // verify clearAllAuthenticatedNotifications is called

        assertNull(swrveSpy.campaignsAndResourcesExecutor);
    }

    @Test
    public void testStopTrackingAndIAM() throws Exception {

        // build IAM and show it
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity.class);
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165);
        SwrveInAppMessageActivity activity = Robolectric.buildActivity(SwrveInAppMessageActivity.class, intent).create().get();

        // verify it is the current activity showing and not finishing
        Activity activityCurrent = swrveSpy.activityContext.get();
        assertEquals(activity, activityCurrent);
        assertFalse(activityCurrent.isFinishing());

        swrveSpy.stopTracking();

        // verify it is finishing after stop
        activityCurrent = swrveSpy.activityContext.get();
        assertTrue(activityCurrent.isFinishing());
    }

    @Test
    public void testStopTrackingAndConversation() throws Exception {

        // build Conversation and show it
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<>());
        assertNotNull(conversation);
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("conversation", conversation);
        ConversationActivity activity = Robolectric.buildActivity(ConversationActivity.class, intent).create().get();

        // verify it is the current activity showing and not finishing
        Activity activityCurrent = swrveSpy.activityContext.get();
        assertEquals(activity, activityCurrent);
        assertFalse(activityCurrent.isFinishing());

        swrveSpy.stopTracking();

        // verify it is finishing after stop
        activityCurrent = swrveSpy.activityContext.get();
        assertTrue(activityCurrent.isFinishing());
    }

    @Test
    public void testStopTrackingAndDeviceUpdate() throws Exception {

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();

        // some setup but crucially set a dummy restclient to intercept device update events via setRestClientToAssertDeviceUpdate method.
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveReal); // wait until swrve instance is fully created before getting a mockito spy.
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        backgroundEventSenderMock = mock(SwrveBackgroundEventSender.class);
        doNothing().when(backgroundEventSenderMock).send(anyString(), anyList());
        doReturn(backgroundEventSenderMock).when(swrveSpy).getSwrveBackgroundEventSender(any(Context.class));

        // test device update sent upon init
        final AtomicBoolean initCallback = new AtomicBoolean(false);
        setRestClientToAssertDeviceUpdate(initCallback, "swrve.tracking_state", "STARTED");
        swrveSpy.init(mActivity);
        await().atMost(Duration.ONE_MINUTE).untilTrue(initCallback); // wait until device info queued and swrve.tracking_state sent

        // test device update after stop called
        final AtomicBoolean stopTrackingCallback = new AtomicBoolean(false);
        setRestClientToAssertDeviceUpdate(stopTrackingCallback, "swrve.tracking_state", "STOPPED");
        SwrveSDK.stopTracking();
        await().atMost(Duration.ONE_MINUTE).untilTrue(stopTrackingCallback); // wait until device info queued and swrve.tracking_state sent
    }

    private void setRestClientToAssertDeviceUpdate(final AtomicBoolean callbackCompleted, String deviceUpdateAttributeName, String deviceUpdateAttributeExpectedValue) {

        // this rest client will swallow rest calls and won't actually make any network calls
        swrveSpy.restClient = new IRESTClient() {

            @Override
            public void get(String endpoint, IRESTResponseListener callback) {
                // empty
            }

            @Override
            public void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) {
                // empty
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback) {

                if (encodedBody.contains("device_update")) {
                    try {
                        JSONObject body = new JSONObject(encodedBody);
                        assertNotNull(body);
                        JSONArray data = body.getJSONArray("data");
                        JSONObject deviceUpdate = null;
                        assertNotNull(data);

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.getJSONObject(i);
                            if (item.getString("type").equals("device_update")) {
                                deviceUpdate = item;
                                break; // only looks at first device_update, there could be multiple.
                            }
                        }
                        assertNotNull(deviceUpdate);

                        JSONObject attributeDevices = deviceUpdate.getJSONObject("attributes");
                        if (attributeDevices.has(deviceUpdateAttributeName)) {
                            String trackingState = attributeDevices.getString(deviceUpdateAttributeName);
                            assertTrue(SwrveHelper.isNotNullOrEmpty(trackingState));
                            assertEquals(deviceUpdateAttributeExpectedValue, attributeDevices.getString(deviceUpdateAttributeName));
                            callbackCompleted.set(true);
                        }
                    } catch (Exception ex) {
                        SwrveLogger.e("Error checking for device_update.", ex);
                    }
                }
                callback.onResponse(new RESTResponse(200, "success", null));
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType) {
                // empty
            }
        };
    }
}
