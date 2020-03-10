package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static com.swrve.sdk.ISwrveCommon.CACHE_REGISTRATION_ID;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;

@RunWith(RobolectricTestRunner.class)
public class SwrveFirebaseUnitTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        swrveSpy.init(mActivity);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testSetRegistrationId() throws JSONException {
        // Call executors right away to remove threading problems in test
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Runnable runnable = (Runnable) invocation.getArguments()[0];
            runnable.run();
            return null;
        }).when(swrveSpy).storageExecutorExecute(Mockito.any(Runnable.class));

        Mockito.verify(swrveSpy, Mockito.atLeast(1)).queueDeviceUpdateNow(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()); // device info queued once upon init

        swrveSpy.setRegistrationId("reg1");
        assertEquals("reg1", swrveSpy.getRegistrationId());
        Mockito.verify(swrveSpy, Mockito.atLeast(1)).sendDeviceTokenUpdateNow(Mockito.anyString()); // device info queued again so atLeast== 2

        ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
        Mockito.verify(swrveSpy, Mockito.atLeastOnce()).sendEventsInBackground(Mockito.any(), Mockito.any(), events.capture());

        List<ArrayList> capturedProperties = events.getAllValues();
        String jsonString = capturedProperties.get(0).get(0).toString();
        JSONObject jObj = new JSONObject(jsonString);

        assertTrue(jObj.has("time"));
        assertTrue(jObj.has("seqnum"));
        assertEquals("device_update", jObj.get("type"));
        assertEquals("false", jObj.get("user_initiated"));

        // Can also be called from SwrveSDK
        SwrveSDK.setRegistrationId("reg1");
        assertEquals("reg1", swrveSpy.getRegistrationId());
        Mockito.verify(swrveSpy, Mockito.atLeast(1)).sendDeviceTokenUpdateNow(Mockito.anyString()); // device info NOT queued again because the regId has NOT changed so remains atLeast== 2

        swrveSpy.setRegistrationId("reg2");
        assertEquals("reg2", swrveSpy.getRegistrationId());
        Mockito.verify(swrveSpy, Mockito.atLeast(2)).sendDeviceTokenUpdateNow(Mockito.anyString()); // device info queued again because the regId has changed so atLeast== 3
    }

    @Test
    public void testBeforeSendDeviceInfo() throws Exception {
        reset(swrveSpy); // reset so the beforeSendDeviceInfo method is not disabled which is part of setup
        swrveSpy.initialised = false; // reset, due to initialised check added to init

        assertNull(swrveSpy.registrationId);
        Mockito.verify(swrveSpy, never()).queueDeviceUpdateNow(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());

        Task<InstanceIdResult> task = new Task<InstanceIdResult>() {
            @Override
            public boolean isComplete() {
                return false;
            }
            @Override
            public boolean isSuccessful() {
                return false;
            }
            @Override
            public boolean isCanceled() {
                return false;
            }
            @Override
            public InstanceIdResult getResult() {
                return null;
            }
            @Override
            public <X extends Throwable> InstanceIdResult getResult(@NonNull Class<X> aClass) throws X {
                return null;
            }
            @Nullable
            @Override
            public Exception getException() {
                return null;
            }

            @NonNull
            @Override
            public Task<InstanceIdResult> addOnSuccessListener(@NonNull OnSuccessListener<? super InstanceIdResult> onSuccessListener) {
                InstanceIdResult instanceIdResult = mock(InstanceIdResult.class);
                doReturn("some_reg_token_id").when(instanceIdResult).getToken();
                onSuccessListener.onSuccess(instanceIdResult);
                return this;
            }

            @NonNull
            @Override
            public Task<InstanceIdResult> addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener<? super InstanceIdResult> onSuccessListener) {
                return this;
            }
            @NonNull
            @Override
            public Task<InstanceIdResult> addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener<? super InstanceIdResult> onSuccessListener) {
                return this;
            }
            @NonNull
            @Override
            public Task<InstanceIdResult> addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
                return this;
            }
            @NonNull
            @Override
            public Task<InstanceIdResult> addOnFailureListener(@NonNull Executor executor, @NonNull OnFailureListener onFailureListener) {
                return this;
            }
            @NonNull
            @Override
            public Task<InstanceIdResult> addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener onFailureListener) {
                return this;
            }
        };

        FirebaseInstanceId firebaseInstanceIdMock = mock(FirebaseInstanceId.class);
        doReturn(firebaseInstanceIdMock).when(swrveSpy).getFirebaseInstanceId();
        doReturn(task).when(firebaseInstanceIdMock).getInstanceId();

        swrveSpy.init(mActivity);

        assertEquals("some_reg_token_id", swrveSpy.registrationId);

        String savedToken = swrveSpy.multiLayerLocalStorage.getCacheEntry(swrveSpy.getUserId(), CACHE_REGISTRATION_ID);
        assertEquals("some_reg_token_id", savedToken);

        Mockito.verify(swrveSpy, Mockito.atLeast(1)).queueDeviceUpdateNow(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()); // once from init
        Mockito.verify(swrveSpy, Mockito.atLeast(1)).sendDeviceTokenUpdateNow(Mockito.anyString()); // once from from _setRegistrationId
    }

    @Test
    public void testPushListener() throws Exception {
        TestPushListener pushListener = new TestPushListener();

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1234");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);

        SwrveNotificationEngageReceiver pushEngageReceiver = new SwrveNotificationEngageReceiver();
        pushEngageReceiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        assertTrue(pushListener.pushEngaged == false);
        assertTrue(pushListener.receivedPayload == null);

        // Set listener and generate another message
        SwrveSDK.getConfig().setNotificationListener(pushListener);

        pushEngageReceiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        // Expect bundle to have been received
        assertTrue(pushListener.pushEngaged == true);
        assertTrue(pushListener.receivedPayload != null);
    }

    @Test
    public void testPushListenerNestedJson() throws Exception {
        TestPushListener pushListener = new TestPushListener();

        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString(SwrveNotificationConstants.TEXT_KEY, "validBundle");
        extras.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1234");
        extras.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, "{\"key1\":\"value1\",\"key2\":50,\"text\":\"this_should_be_overwritten\"}");
        intent.putExtra(SwrveNotificationConstants.PUSH_BUNDLE, extras);

        SwrveSDK.getConfig().setNotificationListener(pushListener);
        SwrveNotificationEngageReceiver pushEngageReceiver = new SwrveNotificationEngageReceiver();
        pushEngageReceiver.onReceive(RuntimeEnvironment.application.getApplicationContext(), intent);

        // Expect bundle to have been received
        assertTrue(pushListener.pushEngaged == true);
        JSONObject payload = pushListener.receivedPayload;
        assertTrue(payload != null);
        assertEquals("validBundle", payload.getString(SwrveNotificationConstants.TEXT_KEY));
        assertEquals("value1", payload.getString("key1"));
        assertEquals(50, payload.getInt("key2"));
    }

    class TestPushListener implements SwrvePushNotificationListener {
        public boolean pushEngaged = false;
        public JSONObject receivedPayload = null;

        @Override
        public void onPushNotification(JSONObject payload) {
            pushEngaged = true;
            receivedPayload = payload;
        }
    }
}
