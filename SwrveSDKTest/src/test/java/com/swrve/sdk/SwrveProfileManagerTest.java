package com.swrve.sdk;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SwrveProfileManagerTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
    }

    @Test
    public void testProfileManager() throws Exception {

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        swrveSpy = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        SwrveTestUtils.setSDKInstance(swrveSpy);

        String userId = swrveSpy.profileManager.getUserId();
        assertTrue("A user should automatically be logged in upon creation of sdk", SwrveHelper.isNotNullOrEmpty(userId));
        String expectedSessionToken = SwrveHelper.generateSessionToken(swrveSpy.apiKey, swrveSpy.appId, userId);
        assertEquals("The sessiontoken is wrong", expectedSessionToken, swrveSpy.profileManager.getSessionToken());
    }
}
