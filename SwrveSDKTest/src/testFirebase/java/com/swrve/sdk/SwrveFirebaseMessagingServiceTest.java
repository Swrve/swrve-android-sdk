package com.swrve.sdk;

import android.os.Bundle;

import com.google.firebase.messaging.RemoteMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;

@RunWith(RobolectricTestRunner.class)
public class SwrveFirebaseMessagingServiceTest extends SwrveBaseTest {

    @Test
    public void testOnMessageReceived() {
        SwrveFirebaseMessagingService service = Robolectric.setupService(SwrveFirebaseMessagingService.class);
        SwrveFirebaseMessagingService serviceSpy = Mockito.spy(service);
        serviceSpy.onCreate();

        SwrvePushManagerImp mockSwrvePushManagerImp = Mockito.mock(SwrvePushManagerImp.class);
        Mockito.doNothing().when(mockSwrvePushManagerImp).processMessage(Mockito.any(Bundle.class));
        Mockito.doReturn(mockSwrvePushManagerImp).when(serviceSpy).getSwrvePushManager();

        RemoteMessage.Builder invalidPushBundle = new RemoteMessage.Builder("dest");
        invalidPushBundle.addData("text", "hello");
        invalidPushBundle.addData("key1", "value1");
        serviceSpy.onMessageReceived(invalidPushBundle.build());

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        Mockito.verify(mockSwrvePushManagerImp, never()).processMessage(bundleCaptor.capture());

        RemoteMessage.Builder validPushBundle = new RemoteMessage.Builder("dest");
        validPushBundle.addData(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        validPushBundle.addData(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        serviceSpy.onMessageReceived(validPushBundle.build());

        bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        Mockito.verify(mockSwrvePushManagerImp, Mockito.atLeastOnce()).processMessage(bundleCaptor.capture());

        Bundle actualBundle = bundleCaptor.getAllValues().get(0);
        assertEquals(2, actualBundle.size());
        assertEquals("validBundle", actualBundle.getString("text"));
        assertEquals("1", actualBundle.getString("_p"));
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
}
