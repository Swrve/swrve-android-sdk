package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.OneTimeWorkRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

public class SwrvePushWorkerHelperTest extends SwrveBaseTest {

    @Test
    public void testHandleMapSwrvePush() {
        Map<String, String> data = new HashMap<>();
        data.put(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        data.put("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, data));
        doNothing().when(helperSpy).enqueueWorkRequest(any(Context.class), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertTrue(success);

        assertEquals(2, helperSpy.inputData.size());
        assertEquals("123", helperSpy.inputData.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertEquals("456", helperSpy.inputData.getString("some_key"));
    }

    @Test
    public void testHandleMapSwrveSilentPush() {
        Map<String, String> data = new HashMap<>();
        data.put(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "123");
        data.put("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, data));
        doNothing().when(helperSpy).enqueueWorkRequest(any(Context.class), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertTrue(success);

        assertEquals(2, helperSpy.inputData.size());
        assertEquals("123", helperSpy.inputData.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertEquals("456", helperSpy.inputData.getString("some_key"));
    }

    @Test
    public void testHandleMapNotSwrve() {
        Map<String, String> dataWithNoSwrveKey = new HashMap<>();
        dataWithNoSwrveKey.put("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, dataWithNoSwrveKey));
        doNothing().when(helperSpy).enqueueWorkRequest(any(Context.class), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertFalse(success);
    }

    @Test
    public void testHandleBundleSwrvePush() {
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        bundle.putString("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, bundle));
        doNothing().when(helperSpy).enqueueWorkRequest(any(Context.class), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertTrue(success);

        assertEquals(2, helperSpy.inputData.size());
        assertEquals("123", helperSpy.inputData.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertEquals("456", helperSpy.inputData.getString("some_key"));
    }

    @Test
    public void testHandleBundleSwrveSilentPush() {
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "123");
        bundle.putString("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, bundle));
        doNothing().when(helperSpy).enqueueWorkRequest(any(Context.class), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertTrue(success);

        assertEquals(2, helperSpy.inputData.size());
        assertEquals("123", helperSpy.inputData.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertEquals("456", helperSpy.inputData.getString("some_key"));
    }

    @Test
    public void testHandleBundleNotSwrve() {
        Bundle bundleWithNotSwrveKey = new Bundle();
        bundleWithNotSwrveKey.putString("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, bundleWithNotSwrveKey));
        doNothing().when(helperSpy).enqueueWorkRequest(any(Context.class), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertFalse(success);
    }
}
