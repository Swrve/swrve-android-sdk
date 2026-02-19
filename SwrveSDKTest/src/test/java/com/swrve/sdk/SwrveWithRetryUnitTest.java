package com.swrve.sdk;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_SENT_TIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.WorkManagerTestInitHelper;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
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

    @Test
    public void testSendDeviceUpdateWithUpdatedNotificationPermission() {
        doReturn(mActivity).when(swrveSpy).getActivityContext();

        // send device update with denied notification permission
        JSONObject deviceInfo = swrveSpy.getDeviceInfo();
        assertEquals("denied", deviceInfo.optString("swrve.permission.android.notification"));
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("attributes", deviceInfo);
        SwrveSDK.sendDeviceUpdate();
        SwrveTestUtils.assertQueueEvent(swrveSpy, "device_update", parameters, null);

        // send device update with granted notification permission
        shadowApplication.grantPermissions(new String[]{POST_NOTIFICATIONS}); // grant the POST_NOTIFICATIONS permission
        deviceInfo = swrveSpy.getDeviceInfo();
        assertEquals("granted", deviceInfo.optString("swrve.permission.android.notification"));
        parameters = new HashMap<>();
        parameters.put("attributes", deviceInfo);
        SwrveSDK.sendDeviceUpdate();
        SwrveTestUtils.assertQueueEvent(swrveSpy, "device_update", parameters, null);
    }

    @Test
    public void testIsSwrvePush() {
        // Empty map -> false
        Map<String, String> data = new HashMap<>();
        assertFalse(SwrveSDK.isSwrvePush(data));

        // Irrelevant key -> false
        data.put("foo", "bar");
        assertFalse(SwrveSDK.isSwrvePush(data));

        // Normal tracking key present -> true
        data.clear();
        data.put(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        assertTrue(SwrveSDK.isSwrvePush(data));

        // Normal tracking key with empty value -> false
        data.clear();
        data.put(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "");
        assertFalse(SwrveSDK.isSwrvePush(data));

        // Silent tracking key present -> true
        data.clear();
        data.put(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "456");
        assertTrue(SwrveSDK.isSwrvePush(data));
    }

    @Test
    public void testHandleSwrvePush() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx);

        Map<String, String> data = new HashMap<>();
        data.put(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");

        String messageId = "456";
        boolean handled = SwrveSDK.handleSwrvePush(ctx, data, messageId, 2222L);
        assertTrue(handled);

        assertEquals("456", data.get(GENERIC_EVENT_PAYLOAD_MSG_ID));
        assertEquals("2222", data.get(GENERIC_EVENT_PAYLOAD_SENT_TIME));

        String uniqueWorkName = "SwrvePushWorkerHelper_" + messageId;
        List<WorkInfo> infos = WorkManager.getInstance(ctx).getWorkInfosForUniqueWork(uniqueWorkName).get();
        assertNotNull(infos);
        assertFalse(infos.isEmpty());
        assertEquals(WorkInfo.State.SUCCEEDED, infos.getFirst().getState());
    }

}
