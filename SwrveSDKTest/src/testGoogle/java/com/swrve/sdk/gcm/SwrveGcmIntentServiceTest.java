package com.swrve.sdk.gcm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrvePushSDK;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class SwrveGcmIntentServiceTest extends SwrveBaseTest {

    private SwrveGcmIntentService service;
    private TestableSwrvePushSDK swrvePushSDK;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SwrvePushSDK.createInstance(RuntimeEnvironment.application);
        swrvePushSDK = new TestableSwrvePushSDK(RuntimeEnvironment.application);
        setSwrvePushSDKInstance(swrvePushSDK);
        service = new SwrveGcmIntentService();
        service.onCreate();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testOnMessageReceived() throws Exception {
        //Check no payload scenario
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString("text", "");
        intent.putExtras(bundle);
        service.onMessageReceived("", bundle);
        assertEquals(1, swrvePushSDK.processRemoteNotificationExecuted);
    }

    class TestableSwrvePushSDK extends SwrvePushSDK {
        int processRemoteNotificationExecuted = 0;

        public TestableSwrvePushSDK(Context context) {
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


