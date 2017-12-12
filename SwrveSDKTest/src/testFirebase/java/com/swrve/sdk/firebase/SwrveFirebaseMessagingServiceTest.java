package com.swrve.sdk.firebase;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.RemoteMessage;
import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrvePushSDK;
import com.swrve.sdk.SwrveTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class SwrveFirebaseMessagingServiceTest extends SwrveBaseTest {

    private SwrveFirebaseMessagingService service;
    private TestableSwrvePushSDK swrvePushSDK;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SwrvePushSDK.createInstance(RuntimeEnvironment.application);
        swrvePushSDK = new TestableSwrvePushSDK(RuntimeEnvironment.application);
        setSwrvePushSDKInstance(swrvePushSDK);
        service = new SwrveFirebaseMessagingService();
        service.onCreate();

        FirebaseApp.initializeApp(mActivity);
    }

    @After
    public void tearDown() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testOnMessageReceived() throws Exception {
        RemoteMessage.Builder b = new RemoteMessage.Builder("dest");
        b.addData("text", "");

        service.onMessageReceived(b.build());
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


