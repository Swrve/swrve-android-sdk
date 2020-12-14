package com.swrve.sdk;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwrveBindableTest extends SwrveBaseTest {

    private Swrve swrve;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrve = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrve.init(mActivity);
    }

    @Test
    public void testRotation() {
        // This would be called onCreate
        swrve.onCreate(mActivity);
        swrve.onResume(mActivity);

        // Simulate activity flow when rotating
        swrve.onPause();
        swrve.onDestroy(mActivity);
        // onCreate and onResume would be called again
        swrve.onCreate(mActivity);
        swrve.onResume(mActivity);

        // SDK shouldn't destroy itself when rotating
        assertFalse(swrve.destroyed);

        // Fake activity going away
        swrve.destroyed = true;
        swrve.onPause();
        swrve.onDestroy(mActivity);

        // SDK should be destroyed as last activity is going away
        assertTrue(swrve.destroyed);
    }

    @Test
    public void testReinitialiseInstance() {
        // Fake shutdown
        swrve.onCreate(mActivity);
        swrve.shutdown();

        // Check that the SDK can be initialized again
        swrve.onCreate(mActivity);
        assertFalse(swrve.destroyed);
    }
}
