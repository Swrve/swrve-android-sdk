package com.swrve.sdk;

import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.adm.SwrveAdmIntentService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.IntentServiceController;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class SwrveAdmIntentServiceTest extends SwrveBaseTest {

    private SwrveAdmIntentService service;
    private SwrvePushSDK swrvePushSDKSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        SwrvePushSDK swrvePushSDK = new SwrvePushSDK(RuntimeEnvironment.application);
        swrvePushSDKSpy = spy(swrvePushSDK);
        SwrvePushSDK.instance = swrvePushSDKSpy;
        IntentServiceController<SwrveAdmIntentService> serviceController = IntentServiceController.of(Robolectric.getShadowsAdapter(), new SwrveAdmIntentService(), null);
        serviceController.create();
        service = serviceController.get();
    }

    @Test
    public void testOnMessage() {
        // Check null scenario
        service.onMessage(null);
        verify(swrvePushSDKSpy, never()).processNotification(any(Bundle.class));

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString("customData", "some custom values");
        extras.putString("sound", "default");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1");
        extras.putString(SwrveNotificationConstants.TIMESTAMP_KEY, "1234");
        intent.putExtras(extras);

        service.onMessage(intent);

        ArgumentCaptor<Bundle> extrasCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(swrvePushSDKSpy, times(1)).processNotification(extrasCaptor.capture());

        assertEquals("validBundle", extrasCaptor.getValue().getString(SwrveNotificationConstants.TEXT_KEY));
        assertEquals("some custom values", extrasCaptor.getValue().getString("customData"));
        assertEquals("default", extrasCaptor.getValue().getString("sound"));
        assertEquals("1", extrasCaptor.getValue().getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertEquals("1234", extrasCaptor.getValue().getString(SwrveNotificationConstants.TIMESTAMP_KEY));
    }
}
