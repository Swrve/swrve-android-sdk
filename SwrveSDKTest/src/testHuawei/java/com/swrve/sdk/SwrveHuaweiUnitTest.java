package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.CACHE_DEVICE_PROP_KEY;
import static com.swrve.sdk.SwrveHuaweiUtil.HMS_CACHE_REGISTRATION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.shadows.gms.Shadows.shadowOf;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.huawei.hms.aaid.HmsInstanceId;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.robolectric.shadows.gms.common.ShadowGoogleApiAvailability;

import java.util.ArrayList;
import java.util.List;

public class SwrveHuaweiUnitTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = spy(swrveReal);
        SwrveSDKBase.instance = swrveSpy;
        SwrveCommon.setSwrveCommon(swrveSpy);
        SwrveBackgroundEventSender backgroundEventSenderMock = mock(SwrveBackgroundEventSender.class);
        doNothing().when(backgroundEventSenderMock).send(anyString(), anyList());
        doReturn(backgroundEventSenderMock).when(swrveSpy).getSwrveBackgroundEventSender(any(Context.class));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testFlavour() {
        assertEquals("huawei", swrveSpy.FLAVOUR.toString());
    }

    @Test
    public void testPlatformUtil() throws Exception {

        // default for tests will be that google is not available therefore the platformUtil should be SwrveHuaweiUtil
        assertNotNull(swrveSpy.platformUtil);
        assertTrue(swrveSpy.platformUtil instanceof SwrveHuaweiUtil);

        // Mock GoogleApiAvailability to return google IS available and recreate swrve instance
        final ShadowGoogleApiAvailability shadowGoogleApiAvailability = shadowOf(GoogleApiAvailability.getInstance());
        final int expectedCode = ConnectionResult.SUCCESS;
        shadowGoogleApiAvailability.setIsGooglePlayServicesAvailable(expectedCode);

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        swrveSpy = SwrveTestUtils.createSpyInstance();

        assertNotNull(swrveSpy.platformUtil);
        assertTrue(swrveSpy.platformUtil instanceof SwrveGoogleUtil);
    }

    @Test
    public void testSetRegistrationIdHuawei() throws JSONException {

        // Call executors right away to remove threading problems in test
        Mockito.doAnswer((Answer<Void>) invocation -> {
            if (invocation.getArguments().length > 0) {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
            }
            return null;
        }).when(swrveSpy).storageExecutorExecute(any(Runnable.class));

        SwrveMultiLayerLocalStorage multiLayerLocalStorage = swrveSpy.multiLayerLocalStorage;
        String userId = swrveSpy.getUserId();
        SwrveHuaweiUtil huaweiUtilSpy = spy(new SwrveHuaweiUtil(ApplicationProvider.getApplicationContext(), SwrveTrackingState.UNKNOWN));
        swrveSpy.platformUtil = huaweiUtilSpy;

        SwrveSDK.setRegistrationId("reg2");
        verify(huaweiUtilSpy).saveAndSendRegistrationId(multiLayerLocalStorage, userId, "reg2");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), events.capture());

        List<ArrayList> capturedProperties = events.getAllValues();
        String jsonString = capturedProperties.get(0).get(0).toString();
        JSONObject jsonObject = new JSONObject(jsonString);

        assertTrue(jsonObject.has("time"));
        assertTrue(jsonObject.has("seqnum"));
        assertEquals("device_update", jsonObject.get("type"));
        assertEquals("false", jsonObject.get("user_initiated"));
        assertTrue(jsonObject.has("attributes"));
        JSONObject attributes = (JSONObject) jsonObject.get("attributes");
        assertTrue(attributes.length() == 1);
        assertEquals("reg2", attributes.get("swrve.hms_token"));
    }

    @Test
    public void testSetRegistrationIdFirebase() throws JSONException {
        // Call executors right away to remove threading problems in test
        Mockito.doAnswer((Answer<Void>) invocation -> {
            if (invocation.getArguments().length > 0) {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
            }
            return null;
        }).when(swrveSpy).storageExecutorExecute(any(Runnable.class));

        SwrveMultiLayerLocalStorage multiLayerLocalStorage = swrveSpy.multiLayerLocalStorage;
        String userId = swrveSpy.getUserId();
        SwrveGoogleUtil googleUtilSpy = spy(new SwrveGoogleUtil(ApplicationProvider.getApplicationContext(), SwrveTrackingState.UNKNOWN));
        swrveSpy.platformUtil = googleUtilSpy;
        googleUtilSpy.advertisingId = "testadvertisingId";

        SwrveSDK.setRegistrationId("reg2");
        verify(googleUtilSpy).saveAndSendRegistrationId(multiLayerLocalStorage, userId, "reg2");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), events.capture());

        List<ArrayList> capturedProperties = events.getAllValues();
        String jsonString = capturedProperties.get(0).get(0).toString();
        JSONObject jsonObject = new JSONObject(jsonString);

        assertTrue(jsonObject.has("time"));
        assertTrue(jsonObject.has("seqnum"));
        assertEquals("device_update", jsonObject.get("type"));
        assertEquals("false", jsonObject.get("user_initiated"));
        assertTrue(jsonObject.has("attributes"));
        JSONObject attributes = (JSONObject) jsonObject.get("attributes");
        assertTrue(attributes.length() == 3);
        assertEquals("reg2", attributes.get("swrve.gcm_token"));
        assertEquals("testadvertisingId", attributes.get("swrve.GAID"));
        assertEquals(1, attributes.get("swrve.play_services_available"));
    }

    @Test
    public void testBeforeSendDeviceInfoHuawei() {

        // Call executors right away to remove threading problems in test
        Mockito.doAnswer((Answer<Void>) invocation -> {
            if (invocation.getArguments().length > 0) {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
            }
            return null;
        }).when(swrveSpy).storageExecutorExecute(any(Runnable.class));

        SwrveHuaweiUtil huaweiUtilSpy = spy(new SwrveHuaweiUtil(ApplicationProvider.getApplicationContext(), SwrveTrackingState.UNKNOWN));
        swrveSpy.platformUtil = huaweiUtilSpy;

        swrveSpy.beforeSendDeviceInfo(mActivity);
        assertTrue(SwrveHelper.isNullOrEmpty(huaweiUtilSpy.registrationId));

        swrveSpy.setRegistrationId("some_reg_token_id");
        assertEquals("some_reg_token_id", huaweiUtilSpy.registrationId);

        String savedToken = swrveSpy.multiLayerLocalStorage.getCacheEntry(CACHE_DEVICE_PROP_KEY, HMS_CACHE_REGISTRATION_ID); // blank userId used for saving regId.
        assertEquals("some_reg_token_id", savedToken);
    }

    @Test
    public void testBeforeSendDeviceInfoFirebase() {

        // Call executors right away to remove threading problems in test
        Mockito.doAnswer((Answer<Void>) invocation -> {
            if (invocation.getArguments().length > 0) {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
            }
            return null;
        }).when(swrveSpy).storageExecutorExecute(any(Runnable.class));

        SwrveGoogleUtil googleUtilSpy = spy(new SwrveGoogleUtil(ApplicationProvider.getApplicationContext(), SwrveTrackingState.UNKNOWN));
        doNothing().when(googleUtilSpy).init(any(SwrveMultiLayerLocalStorage.class), anyString(), anyBoolean(), anyBoolean());
        swrveSpy.platformUtil = googleUtilSpy;

        swrveSpy.beforeSendDeviceInfo(mActivity);
        verify(googleUtilSpy, atLeastOnce()).init(any(SwrveMultiLayerLocalStorage.class), anyString(), anyBoolean(), anyBoolean());
        // No need to test further as there are other tests in Firbase flavour that cover this.
    }

    @Test
    public void testExtraDeviceInfoHuawei() throws Exception {

        SwrveHuaweiUtil huaweiUtilSpy = spy(new SwrveHuaweiUtil(ApplicationProvider.getApplicationContext(), SwrveTrackingState.UNKNOWN));
        swrveSpy.platformUtil = huaweiUtilSpy;

        JSONObject deviceUpdate = new JSONObject();
        deviceUpdate.put("existingkey", "existingvalue");

        huaweiUtilSpy.registrationId = "testreg";

        swrveSpy.extraDeviceInfo(deviceUpdate);
        assertTrue(deviceUpdate.length() == 2);
        assertEquals("existingvalue", deviceUpdate.get("existingkey"));
        assertEquals("testreg", deviceUpdate.get("swrve.hms_token"));
    }

    @Test
    public void testExtraDeviceInfoFirebase() throws Exception {

        SwrveGoogleUtil googleUtilSpy = spy(new SwrveGoogleUtil(ApplicationProvider.getApplicationContext(), SwrveTrackingState.UNKNOWN));
        swrveSpy.platformUtil = googleUtilSpy;

        JSONObject deviceUpdate = new JSONObject();
        deviceUpdate.put("existingkey", "existingvalue");

        googleUtilSpy.registrationId = "testreg";

        swrveSpy.extraDeviceInfo(deviceUpdate);
        assertTrue(deviceUpdate.length() == 3);
        assertEquals("existingvalue", deviceUpdate.get("existingkey"));
        assertEquals("testreg", deviceUpdate.get("swrve.gcm_token"));
        assertEquals(1, deviceUpdate.get("swrve.play_services_available"));
    }

    @Test
    public void testRegistrationId() {

        // Call executors right away to remove threading problems in test
        Mockito.doAnswer((Answer<Void>) invocation -> {
            if (invocation.getArguments().length > 0) {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
            }
            return null;
        }).when(swrveSpy).storageExecutorExecute(any(Runnable.class));

        SwrveHuaweiUtil huaweiUtilSpy = spy(new SwrveHuaweiUtil(ApplicationProvider.getApplicationContext(), SwrveTrackingState.UNKNOWN));
        swrveSpy.platformUtil = huaweiUtilSpy;

        swrveSpy.setRegistrationId("some_reg_token_id");
        String savedToken1 = huaweiUtilSpy.getRegistrationId(swrveSpy.multiLayerLocalStorage);
        assertEquals("some_reg_token_id", savedToken1);
    }

    @Test
    public void testInitAppId() {
        // Call executors right away to remove threading problems in test
        Mockito.doAnswer((Answer<Void>) invocation -> {
            if (invocation.getArguments().length > 0) {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
            }
            return null;
        }).when(swrveSpy).storageExecutorExecute(any(Runnable.class));

        SwrveHuaweiUtil huaweiUtilSpy = spy(new SwrveHuaweiUtil(ApplicationProvider.getApplicationContext(), SwrveTrackingState.UNKNOWN));
        swrveSpy.platformUtil = huaweiUtilSpy;
        doReturn(null).when(huaweiUtilSpy).getHMSInstance();
        doReturn("SomeRegId").when(huaweiUtilSpy).getRegistrationId(swrveSpy.multiLayerLocalStorage);
        doReturn("SomeUserId").when(swrveSpy).getUserId();

        Mockito.doAnswer((Answer<Void>) invocation -> {
            if (invocation.getArguments().length > 0) {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
            }
            return null;
        }).when(huaweiUtilSpy).executeSafeRunnable(any(Runnable.class));

        // null
        doReturn(null).when(huaweiUtilSpy).getAppId();
        swrveSpy.beforeSendDeviceInfo(mActivity);

        // empty
        doReturn("").when(huaweiUtilSpy).getAppId();
        swrveSpy.beforeSendDeviceInfo(mActivity);

        // valid id
        doReturn("SomeAppId").when(huaweiUtilSpy).getAppId();
        swrveSpy.beforeSendDeviceInfo(mActivity);

        verify(huaweiUtilSpy, times(1)).registerForTokenInBackground(eq(swrveSpy.multiLayerLocalStorage), eq("SomeUserId"), eq("SomeAppId"), eq("SomeRegId"));
    }

    @Test
    public void testRegisterInBackground() {
        SwrveHuaweiUtil huaweiUtilSpy = spy(new SwrveHuaweiUtil(ApplicationProvider.getApplicationContext(), SwrveTrackingState.UNKNOWN));
        swrveSpy.platformUtil = huaweiUtilSpy;
        doReturn(mock(HmsInstanceId.class)).when(huaweiUtilSpy).getHMSInstance();

        // null
        doReturn(null).when(huaweiUtilSpy).getToken(any(HmsInstanceId.class), anyString());
        huaweiUtilSpy.registerForTokenInBackground(swrveSpy.multiLayerLocalStorage, "SomeUserId", "SomeAppId", "SomeRegId");

        // empty
        doReturn("").when(huaweiUtilSpy).getToken(any(HmsInstanceId.class), anyString());
        huaweiUtilSpy.registerForTokenInBackground(swrveSpy.multiLayerLocalStorage, "SomeUserId", "SomeAppId", "SomeRegId");

        // same ids
        doReturn("SomeRegId").when(huaweiUtilSpy).getToken(any(HmsInstanceId.class), anyString());
        huaweiUtilSpy.registerForTokenInBackground(swrveSpy.multiLayerLocalStorage, "SomeUserId", "SomeAppId", "SomeRegId");

        //different ids, should call saveAndSendRegistrationId
        doReturn("SomeNewRegId").when(huaweiUtilSpy).getToken(any(HmsInstanceId.class), anyString());
        huaweiUtilSpy.registerForTokenInBackground(swrveSpy.multiLayerLocalStorage, "SomeUserId", "SomeAppId", "SomeRegId");

        verify(huaweiUtilSpy, times(1)).saveAndSendRegistrationId(eq(swrveSpy.multiLayerLocalStorage), eq("SomeUserId"), eq("SomeNewRegId"));
    }
}
