package com.swrve.sdk.firebase;

import android.os.Bundle;

import com.google.firebase.messaging.RemoteMessage;
import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrvePushSDK;
import com.swrve.sdk.SwrveTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class SwrveFirebaseMessagingServiceTest extends SwrveBaseTest {

    @Test
    public void testOnMessageReceived() throws Exception {
        SwrvePushSDK swrvePushSDKSpy = Mockito.mock(SwrvePushSDK.class);
        setSwrvePushSDKInstance(swrvePushSDKSpy);

        SwrveFirebaseMessagingService service = Robolectric.setupService(SwrveFirebaseMessagingService.class);
        service.onCreate();

        RemoteMessage.Builder b = new RemoteMessage.Builder("dest");
        b.addData("text", "hello");
        b.addData("key1", "value1");
        service.onMessageReceived(b.build());

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        ArgumentCaptor<Boolean> booleanCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(swrvePushSDKSpy, Mockito.atLeastOnce()).processRemoteNotification(bundleCaptor.capture(), booleanCaptor.capture());

        Bundle actualBundle = bundleCaptor.getAllValues().get(0);
        assertEquals(2, actualBundle.size());
        assertEquals("hello", actualBundle.getString("text"));
        assertEquals("value1", actualBundle.getString("key1"));
        assertFalse(booleanCaptor.getAllValues().get(0));
    }

    @Test
    public void testOnNewToken() throws Exception {
        Swrve swrveSpy = Mockito.mock(Swrve.class);
        SwrveTestUtils.setSDKInstance(swrveSpy);

        SwrveFirebaseMessagingService service = Robolectric.setupService(SwrveFirebaseMessagingService.class);
        service.onCreate();
        service.onNewToken("new_token");

        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(swrveSpy, Mockito.atLeastOnce()).setRegistrationId(stringCaptor.capture());
        assertEquals("new_token", stringCaptor.getAllValues().get(0));
    }

    private void setSwrvePushSDKInstance(SwrvePushSDK instance) throws Exception {
        Field hack = SwrvePushSDK.class.getDeclaredField("instance");
        hack.setAccessible(true);
        hack.set(null, instance);
    }
}


