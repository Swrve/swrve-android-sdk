package com.swrve.sdk;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.mockito.Mockito;

public class ActivityLifecycleCallbacksTest extends SwrveBaseTest {

    @Test
    public void testCreateInstance() {
        Application applicationSpy = Mockito.spy(ApplicationProvider.getApplicationContext());
        Swrve swrve = (Swrve) SwrveSDK.createInstance(applicationSpy, 1, "apiKey");
        Mockito.verify(applicationSpy).registerActivityLifecycleCallbacks(swrve);
    }
}
