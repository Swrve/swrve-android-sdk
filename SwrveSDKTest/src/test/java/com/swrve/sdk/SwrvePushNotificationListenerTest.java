package com.swrve.sdk;

import android.content.Intent;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SwrvePushNotificationListenerTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        swrveSpy.init(mActivity);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testPushListener() {
        TestPushListener pushListener = new TestPushListener();

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1234");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);

        SwrveNotificationEngageReceiver pushEngageReceiver = new SwrveNotificationEngageReceiver();
        pushEngageReceiver.onReceive(ApplicationProvider.getApplicationContext(), intent);

        assertTrue(pushListener.pushEngaged == false);
        assertTrue(pushListener.receivedPayload == null);

        // Set listener and generate another message
        SwrveSDK.getConfig().setNotificationListener(pushListener);

        pushEngageReceiver.onReceive(ApplicationProvider.getApplicationContext().getApplicationContext(), intent);

        // Expect bundle to have been received
        assertTrue(pushListener.pushEngaged == true);
        assertTrue(pushListener.receivedPayload != null);
    }

    @Test
    public void testPushListenerNestedJson() throws Exception {
        TestPushListener pushListener = new TestPushListener();

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1234");
        extras.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "{\"key1\":\"value1\",\"key2\":50,\"text\":\"this_should_be_overwritten\"}");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);

        SwrveSDK.getConfig().setNotificationListener(pushListener);
        SwrveNotificationEngageReceiver pushEngageReceiver = new SwrveNotificationEngageReceiver();
        pushEngageReceiver.onReceive(ApplicationProvider.getApplicationContext().getApplicationContext(), intent);

        // Expect bundle to have been received
        assertTrue(pushListener.pushEngaged == true);
        JSONObject payload = pushListener.receivedPayload;
        assertTrue(payload != null);
        assertEquals("validBundle", payload.getString(SwrveNotificationConstants.TEXT_KEY));
        assertEquals("value1", payload.getString("key1"));
        assertEquals(50, payload.getInt("key2"));
    }

    class TestPushListener implements SwrvePushNotificationListener {
        public boolean pushEngaged = false;
        public JSONObject receivedPayload = null;

        @Override
        public void onPushNotification(JSONObject payload) {
            pushEngaged = true;
            receivedPayload = payload;
        }
    }
}
