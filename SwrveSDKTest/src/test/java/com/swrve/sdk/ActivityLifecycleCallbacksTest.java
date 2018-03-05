package com.swrve.sdk;

import android.annotation.TargetApi;
import android.app.Application;
import android.os.Build;

import org.junit.Test;
import org.mockito.Mockito;

import static org.robolectric.RuntimeEnvironment.application;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ActivityLifecycleCallbacksTest extends SwrveBaseTest {

    @Test
    public void testCreateInstance() throws Exception {
        Application applicationSpy = Mockito.spy(application);
        Swrve swrve = (Swrve) SwrveSDK.createInstance(applicationSpy, 1, "apiKey");
        Mockito.verify(applicationSpy).registerActivityLifecycleCallbacks(swrve);
    }

}
