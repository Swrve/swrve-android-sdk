package com.swrve.sdk;

import android.content.Intent;
import android.os.Bundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.never;

@RunWith(RobolectricTestRunner.class)
public class SwrveAdmIntentServiceTest extends SwrveBaseTest {

    @Test
    public void testOnMessage() {
        SwrveAdmIntentService service = Robolectric.setupService(SwrveAdmIntentService.class);
        SwrveAdmIntentService serviceSpy = Mockito.spy(service);
        serviceSpy.onCreate();

        SwrvePushServiceManager mockSwrvePushServiceManager = Mockito.mock(SwrvePushServiceManager.class);
        Mockito.doNothing().when(mockSwrvePushServiceManager).processMessage(Mockito.any(Bundle.class));
        Mockito.doReturn(mockSwrvePushServiceManager).when(serviceSpy).getSwrvePushServiceManager();

        // Check null scenario
        serviceSpy.onMessage(null);

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        Mockito.verify(mockSwrvePushServiceManager, never()).processMessage(bundleCaptor.capture());

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString("customData", "some custom values");
        extras.putString("sound", "default");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        extras.putString(SwrveNotificationConstants.TIMESTAMP_KEY, "1234");
        intent.putExtras(extras);

        serviceSpy.onMessage(intent);

        ArgumentCaptor<Bundle> extrasCaptor = ArgumentCaptor.forClass(Bundle.class);
        Mockito.verify(mockSwrvePushServiceManager, Mockito.atLeastOnce()).processMessage(extrasCaptor.capture());

        assertEquals("validBundle", extrasCaptor.getValue().getString(SwrveNotificationConstants.TEXT_KEY));
        assertEquals("some custom values", extrasCaptor.getValue().getString("customData"));
        assertEquals("default", extrasCaptor.getValue().getString("sound"));
        assertEquals("1", extrasCaptor.getValue().getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertEquals("1234", extrasCaptor.getValue().getString(SwrveNotificationConstants.TIMESTAMP_KEY));
    }
}
