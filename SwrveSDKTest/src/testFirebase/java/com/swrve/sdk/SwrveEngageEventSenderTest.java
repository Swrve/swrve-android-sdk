package com.swrve.sdk;

import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.config.SwrveConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.manifest.BroadcastReceiverData;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwrveEngageEventSenderTest extends SwrveBaseTest {

    private Swrve swrveSpy;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
        SwrveConfig config = new SwrveConfig();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveCommon.setSwrveCommon(swrveSpy);

        Mockito.doNothing().when(swrveSpy).downloadAssets(Mockito.anySet()); // assets are manually mocked
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest

        // do not init the sdk, as SwrvePushEngageReceiver can/will be executed cold
    }

    @After
    public void tearDown() throws Exception {
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }
    
    @Test
    public void testReceiverInManifest() throws Exception {
        List<BroadcastReceiverData> receiverDataList = shadowApplication.getAppManifest().getBroadcastReceivers();
        boolean inManifest = false;
        for (BroadcastReceiverData receiverData : receiverDataList) {
            if (receiverData.getClassName().equals("com.swrve.sdk.SwrveEngageEventSender")) {
                inManifest = true;
                break;
            }
        }
        assertTrue(inManifest);
    }

    @Test
    public void testEventQueued() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(SwrvePushConstants.SWRVE_TRACKING_KEY, "4567");

        SwrveEngageEventSender engageEventSender = new SwrveEngageEventSender();
        engageEventSender.onReceive(mActivity, intent);

        List<String> events = assertEventCount(1);
        assertEngagedEvent(events.get(0), "Swrve.Messages.Push-4567.engaged");
    }

    private List<String> assertEventCount(int count) {
        List<String> events = null;
        List<Intent> broadcastIntents = mShadowActivity.getBroadcastIntents();
        if (count == 0) {
            assertEquals(0, broadcastIntents.size());
        } else {
            assertEquals(1, broadcastIntents.size());
            assertEquals("com.swrve.sdk.SwrveWakefulReceiver", broadcastIntents.get(0).getComponent().getShortClassName());
            events = (List) broadcastIntents.get(0).getExtras().get(SwrveBackgroundEventSender.EXTRA_EVENTS);
            assertNotNull(events);
            assertEquals(count, events.size());
        }
        return events;
    }

    private void assertEngagedEvent(String eventJson, String eventName) {
        Gson gson = new Gson(); // eg: {"type":"event","time":1466519995192,"name":"Swrve.Messages.Push-1.engaged"}
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> event = gson.fromJson(eventJson, type);
        assertEquals(4, event.size());
        assertTrue(event.containsKey("type"));
        assertEquals("event", event.get("type"));
        assertTrue(event.containsKey("name"));
        assertEquals(eventName, event.get("name"));
        assertTrue(event.containsKey("time"));
        assertTrue(event.containsKey("seqnum"));
    }

}
