package com.swrve.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class SwrveWithRetryUnitTest extends SwrveBaseTest {

    @Rule
    public RetryRule retryRule = new RetryRule(3); // Retry up to 3 times

    private Swrve swrveSpy;
    private SwrveBackgroundEventSender backgroundEventSenderMock;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();

        backgroundEventSenderMock = mock(SwrveBackgroundEventSender.class);
        doNothing().when(backgroundEventSenderMock).send(anyString(), anyList());
        doReturn(backgroundEventSenderMock).when(swrveSpy).getSwrveBackgroundEventSender(any(Context.class));

        swrveSpy.init(mActivity);
        swrveSpy.activityContext = new WeakReference<>(mActivity);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        SwrveHelper.buildModel = Build.MODEL;
    }

    @Test
    public void testGetJoined() throws Exception {

        assertTrue("Test getting the joined value when sdk has been initialised.", swrveSpy.initialised);
        String joined1 = swrveSpy.getJoined();
        assertTrue("Joined should not be null or empty", SwrveHelper.isNotNullOrEmpty(joined1));
        long joinedLong = new Long(joined1);
        assertTrue("Joined should be greater than zero", joinedLong > 0);

        // recreate the sdk but make sure its not initialised.
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = spy(swrveReal);

        assertFalse("Test getting the joined value when sdk has NOT been initialised.", swrveSpy.initialised);
        String joined2 = swrveSpy.getJoined();
        assertEquals(joined2, joined1);
    }

    @Test
    public void testCheckPermissionChanges() {

        // checkPermissionChanges should be called once upon init in setup
        verify(swrveSpy, times(1)).checkNotificationPermissionChange();
        assertNull(swrveSpy.multiLayerLocalStorage.getCacheEntry("", "permission_notification_current"));

        shadowApplication.grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        swrveSpy.checkNotificationPermissionChange();
        assertEquals("granted", swrveSpy.multiLayerLocalStorage.getCacheEntry("", "permission_current_android.permission.POST_NOTIFICATIONS"));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.permission.android.notification.granted");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, null);

        shadowApplication.denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
        swrveSpy.checkNotificationPermissionChange();
        assertEquals("denied", swrveSpy.multiLayerLocalStorage.getCacheEntry("", "permission_current_android.permission.POST_NOTIFICATIONS"));
        parameters.clear();
        parameters.put("name", "Swrve.permission.android.notification.denied");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, null);
    }

    @Test
    public void testCurrencyGiven() {
        SwrveSDK.currencyGiven("givenCurrency2", 999.56);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("given_currency", "givenCurrency2");
        parameters.put("given_amount", "999.56");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "currency_given", parameters, null);
    }

    @Test
    public void testDeviceUpdate() {
        doReturn(mActivity).when(swrveSpy).getActivityContext();
        JSONObject deviceInfo = swrveSpy.getDeviceInfo();
        SwrveLogger.i("Got deviceInfo:" + deviceInfo);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("attributes", deviceInfo);
        swrveSpy.deviceUpdate(swrveSpy.getUserId(), deviceInfo);
        SwrveTestUtils.assertQueueEvent(swrveSpy, "device_update", parameters, null);
    }
}
