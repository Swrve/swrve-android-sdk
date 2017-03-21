package com.swrve.sdk.adm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrvePushSDK;
import com.swrve.sdk.SwrveTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.IntentServiceController;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class SwrveAdmIntentServiceTest extends SwrveBaseTest {

    private SwrveAdmIntentService swrveAdmService;
    private TestableSwrvePushSDK swrvePushSDK;

    @Before
    public void setUp() throws Exception {
        SwrvePushSDK.createInstance(RuntimeEnvironment.application);
        swrvePushSDK = new TestableSwrvePushSDK(RuntimeEnvironment.application, true);
        setSwrvePushSDKInstance(swrvePushSDK);
        ShadowLog.stream = System.out;
        IntentServiceController<SwrveAdmIntentService> serviceController = IntentServiceController.of(Robolectric.getShadowsAdapter(), new SwrveAdmIntentService(), null);
        serviceController.create();
        swrveAdmService = serviceController.get();
    }

    @After
    public void tearDown() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testOnMessage() throws Exception {
        //Check null scenario
        swrveAdmService.onMessage(null);
        assertEquals(0, swrvePushSDK.processRemoteNotificationExecuted);

        //Check no payload scenario
        Intent intent = new Intent();
        Bundle missingTrackingKey = new Bundle();
        missingTrackingKey.putString("text", "");
        intent.putExtras(missingTrackingKey);
        swrveAdmService.onMessage(intent);
        assertEquals(1, swrvePushSDK.processRemoteNotificationExecuted);
    }

    @Test
    public void testRegistrationError() throws Exception {
        swrveAdmService.onRegistrationError("Some error message, should not crash.");
    }

    @Test
    public void testUnregistered() throws Exception {
        swrveAdmService.onUnregistered("Some text relating to being unregistered. Should not crash.");
    }

    @Test
    public void testRegistered() throws Exception {
        swrveAdmService.onRegistered("MyFakeADMToken");
    }

    class TestableSwrvePushSDK extends SwrvePushSDK {
        int processRemoteNotificationExecuted = 0;

        public TestableSwrvePushSDK(Context context, boolean isPushEnabled) {
            super(context);
        }

        @Override
        public void processRemoteNotification(Bundle msg, boolean checkDupes) {
            processRemoteNotificationExecuted = 1;
        }
    }

    public void setSwrvePushSDKInstance(SwrvePushSDK instance) throws Exception {
        Field hack = SwrvePushSDK.class.getDeclaredField("instance");
        hack.setAccessible(true);
        hack.set(null, instance);
    }
}
