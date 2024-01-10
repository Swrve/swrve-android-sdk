package com.swrve.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SwrvePushWorkerHelperTest extends SwrveBaseTest {

    @Test
    public void testHandleMapSwrvePush() {
        Map<String, String> mapData = new HashMap<>();
        mapData.put(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        mapData.put("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, mapData));
        doNothing().when(helperSpy).enqueueUniqueWorkRequest(any(Context.class), anyString(), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertTrue(success);

        Data data = helperSpy.resolveData();
        assertEquals(2, data.size());
        assertEquals("123", data.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertEquals("456", data.getString("some_key"));
    }

    @Test
    public void testHandleDuplicateSwrvePush() {
        Map<String, String> mapData = new HashMap<>();
        mapData.put(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        mapData.put(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, "456");
        mapData.put("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, mapData));
        doNothing().when(helperSpy).enqueueUniqueWorkRequest(any(Context.class), anyString(), any(OneTimeWorkRequest.class));

        assertTrue(helperSpy.handle());
        assertFalse(helperSpy.handle());
    }

    @Test
    public void testHandleMapSwrveSilentPush() {
        Map<String, String> mapData = new HashMap<>();
        mapData.put(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "123");
        mapData.put("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, mapData));
        doNothing().when(helperSpy).enqueueUniqueWorkRequest(any(Context.class), anyString(), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertTrue(success);

        Data data = helperSpy.resolveData();
        assertEquals(2, data.size());
        assertEquals("123", data.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertEquals("456", data.getString("some_key"));
    }

    @Test
    public void testHandleMapNotSwrve() {
        Map<String, String> dataWithNoSwrveKey = new HashMap<>();
        dataWithNoSwrveKey.put("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, dataWithNoSwrveKey));
        doNothing().when(helperSpy).enqueueUniqueWorkRequest(any(Context.class), anyString(), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertFalse(success);
    }

    @Test
    public void testHandleBundleSwrvePush() {
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123");
        bundle.putString("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, bundle));
        doNothing().when(helperSpy).enqueueUniqueWorkRequest(any(Context.class), anyString(), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertTrue(success);

        Data data = helperSpy.resolveData();
        assertEquals(2, data.size());
        assertEquals("123", data.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY));
        assertEquals("456", data.getString("some_key"));
    }

    @Test
    public void testHandleBundleSwrveSilentPush() {
        Bundle bundle = new Bundle();
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "123");
        bundle.putString("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, bundle));
        doNothing().when(helperSpy).enqueueUniqueWorkRequest(any(Context.class), anyString(), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertTrue(success);

        Data data = helperSpy.resolveData();
        assertEquals(2, data.size());
        assertEquals("123", data.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY));
        assertEquals("456", data.getString("some_key"));
    }

    @Test
    public void testHandleBundleNotSwrve() {
        Bundle bundleWithNotSwrveKey = new Bundle();
        bundleWithNotSwrveKey.putString("some_key", "456");
        SwrvePushWorkerHelper helperSpy = spy(new SwrvePushWorkerHelper(ApplicationProvider.getApplicationContext(), SwrvePushManagerWorker.class, bundleWithNotSwrveKey));
        doNothing().when(helperSpy).enqueueUniqueWorkRequest(any(Context.class), anyString(), any(OneTimeWorkRequest.class));
        boolean success = helperSpy.handle();
        assertFalse(success);
    }
}
