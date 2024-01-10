package com.swrve.sdk;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.os.Bundle;

import com.huawei.hms.push.RemoteMessage;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;

public class SwrveHuaweiMessageServiceTest extends SwrveBaseTest {

    @Test
    public void testOnMessageReceived() {
        SwrveHmsMessageService service = Robolectric.setupService(SwrveHmsMessageService.class);
        SwrveHmsMessageService serviceSpy = spy(service);
        serviceSpy.onCreate();

        doNothing().when(serviceSpy).handle(any(Bundle.class));

        // Invalid bundle
        RemoteMessage.Builder invalidPushBundle = new RemoteMessage.Builder("dest");
        invalidPushBundle.addData("text", "hello");
        invalidPushBundle.addData("key1", "value1");
        serviceSpy.onMessageReceived(invalidPushBundle.build());
        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(serviceSpy, never()).handle(bundleCaptor.capture());

        // valid bundle but without a sid
        RemoteMessage.Builder validPushBundleWithoutSid = new RemoteMessage.Builder("dest");
        validPushBundleWithoutSid.addData(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        validPushBundleWithoutSid.addData(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        serviceSpy.onMessageReceived(validPushBundleWithoutSid.build());
        bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(serviceSpy, atLeastOnce()).handle(bundleCaptor.capture());
        Bundle actualBundle = bundleCaptor.getAllValues().get(0);
        assertEquals(4, actualBundle.size());
        assertEquals("validBundle", actualBundle.getString("text"));
        assertEquals("1", actualBundle.getString("_p"));

        // valid bundle but with a sid
        RemoteMessage.Builder pushBundleWithSid = new RemoteMessage.Builder("dest");
        pushBundleWithSid.addData(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        pushBundleWithSid.addData(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "2");
        pushBundleWithSid.addData(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, "2");
        pushBundleWithSid.setMessageId("123");
        serviceSpy.onMessageReceived(pushBundleWithSid.build());
        bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(serviceSpy, atLeastOnce()).handle(bundleCaptor.capture());
        assertEquals(2, bundleCaptor.getAllValues().size()); // the size increases to 2
        actualBundle = bundleCaptor.getAllValues().get(1); // second index
        assertEquals(5, actualBundle.size());
        assertEquals("validBundle", actualBundle.getString("text"));
        assertEquals("2", actualBundle.getString("_p"));
        assertEquals("123", actualBundle.getString("provider.message_id"));
        assertEquals("0", actualBundle.getString("provider.sent_time"));
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
