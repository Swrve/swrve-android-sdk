package com.swrve.sdk;

import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.model.PayloadButton;
import com.swrve.sdk.model.PushPayload;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.manifest.BroadcastReceiverData;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwrvePushEngageReceiverTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
        SwrvePushSDK.createInstance(mActivity);
        // Do not init the sdk, as SwrvePushEngageReceiver can/will be executed cold
    }

    @After
    public void tearDown() throws Exception {
        SwrveTestUtils.removeSingleton(SwrvePushSDK.class, "instance");
    }

    @Test
    public void testReceiverInManifest() throws Exception {
        List<BroadcastReceiverData> receiverDataList = shadowApplication.getAppManifest().getBroadcastReceivers();
        boolean inManifest = false;
        for (BroadcastReceiverData receiverData : receiverDataList) {
            if (receiverData.getClassName().equals("com.swrve.sdk.SwrvePushEngageReceiver")) {
                inManifest = true;
                break;
            }
        }
        assertTrue(inManifest);
    }

    @Test
    public void testReceiverOpenActivity() throws Exception {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("text", "validBundle");
        extras.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "1234");
        intent.putExtra(SwrvePushConstants.PUSH_BUNDLE, extras);

        SwrvePushEngageReceiver pushEngageReceiver = new SwrvePushEngageReceiver();
        pushEngageReceiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        Intent nextIntent = shadowApplication.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals("com.swrve.sdk.test.MainActivity", nextIntent.getComponent().getClassName());

        List<Intent> broadcastIntents = mShadowActivity.getBroadcastIntents();
        assertNotNull(broadcastIntents);
        assertEquals(2, broadcastIntents.size());
        assertEquals("com.swrve.sdk.SwrveEngageEventSender", broadcastIntents.get(0).getComponent().getShortClassName());
        assertEquals("android.intent.action.CLOSE_SYSTEM_DIALOGS", broadcastIntents.get(1).getAction());
        String pushId = (String) broadcastIntents.get(0).getExtras().get(SwrvePushConstants.SWRVE_TRACKING_KEY);
        assertNotNull(pushId);
        assertEquals("1234", pushId);
    }

    @Test
    public void testReceiverOpenDeeplink() throws Exception {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("customdata", "customdata_value");
        extras.putString(SwrvePushConstants.DEEPLINK_KEY, "swrve://deeplink/campaigns");
        extras.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrvePushConstants.PUSH_BUNDLE, extras);

        SwrvePushEngageReceiver pushEngageReceiver = new SwrvePushEngageReceiver();
        pushEngageReceiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();

        Intent nextStartedActivity = mShadowActivity.getNextStartedActivity();
        assertNotNull(nextStartedActivity);
        assertEquals("swrve://deeplink/campaigns", nextStartedActivity.getData().toString());
        assertTrue(nextStartedActivity.hasExtra("customdata"));
        assertEquals("customdata_value", nextStartedActivity.getStringExtra("customdata"));
    }

    @Test
    public void testReceiverPressedUrlAction() throws Exception {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrvePushConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrvePushConstants.PUSH_NOTIFICATION_ID, 1);
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_TYPE_KEY, PayloadButton.ActionType.OPEN_URL);
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_KEY, "1");
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_URL_KEY, "swrve://deeplink/campaigns");

        SwrvePushEngageReceiver pushEngageReceiver = new SwrvePushEngageReceiver();
        pushEngageReceiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();

        Intent nextStartedActivity = mShadowActivity.getNextStartedActivity();
        assertNotNull(nextStartedActivity);
        assertEquals("swrve://deeplink/campaigns", nextStartedActivity.getData().toString());
    }

    @Test
    public void testReceiverPressedActionClosedNotification() throws Exception {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrvePushConstants.SWRVE_TRACKING_KEY, "4567");
        intent.putExtra(SwrvePushConstants.PUSH_BUNDLE, extras);
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_TYPE_KEY, PayloadButton.ActionType.OPEN_URL);
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_KEY, "1");
        intent.putExtra(SwrvePushConstants.PUSH_ACTION_URL_KEY, "swrve://deeplink/campaigns");
        intent.putExtra(SwrvePushConstants.PUSH_NOTIFICATION_ID, 1);

        SwrvePushEngageReceiver pushEngageReceiver = new SwrvePushEngageReceiver();
        SwrvePushEngageReceiver receiverSpy = Mockito.spy(pushEngageReceiver);
        Mockito.doNothing().when(receiverSpy).closeNotification(1); // assets are manually mocked
        receiverSpy.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);
        Mockito.verify(receiverSpy).closeNotification(1);

        Robolectric.flushBackgroundThreadScheduler();
        Robolectric.flushForegroundThreadScheduler();
    }
}
