package com.swrve.sdk;

import android.os.Bundle;

import com.huawei.hms.push.RemoteMessage;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SwrveHuaweiMessageServiceTest extends SwrveBaseTest {

    @Test
    public void testOnMessageReceived() {
        SwrveHmsMessageService service = Robolectric.setupService(SwrveHmsMessageService.class);
        SwrveHmsMessageService serviceSpy = spy(service);
        serviceSpy.onCreate();

        doNothing().when(serviceSpy).handle(any(Bundle.class));

        RemoteMessage.Builder invalidPushBundle = new RemoteMessage.Builder("dest");
        invalidPushBundle.addData("text", "hello");
        invalidPushBundle.addData("key1", "value1");
        serviceSpy.onMessageReceived(invalidPushBundle.build());

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(serviceSpy, never()).handle(bundleCaptor.capture());

        RemoteMessage.Builder validPushBundle = new RemoteMessage.Builder("dest");
        validPushBundle.addData(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        validPushBundle.addData(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        serviceSpy.onMessageReceived(validPushBundle.build());

        bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(serviceSpy, atLeastOnce()).handle(bundleCaptor.capture());

        Bundle actualBundle = bundleCaptor.getAllValues().get(0);
        assertEquals(2, actualBundle.size());
        assertEquals("validBundle", actualBundle.getString("text"));
        assertEquals("1", actualBundle.getString("_p"));
    }

    @Test
    public void testOnNewToken() throws Exception {
        Swrve swrveSpy = mock(Swrve.class);
        SwrveTestUtils.setSDKInstance(swrveSpy);

        SwrveHmsMessageService service = Robolectric.setupService(SwrveHmsMessageService.class);
        service.onCreate();
        service.onNewToken("new_token");

        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(swrveSpy, atLeastOnce()).setRegistrationId(stringCaptor.capture());
        assertEquals("new_token", stringCaptor.getAllValues().get(0));
    }
}
