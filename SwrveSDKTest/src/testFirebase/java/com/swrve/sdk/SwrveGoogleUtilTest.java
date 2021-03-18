package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.swrve.sdk.SwrveGoogleUtil.CACHE_GOOGLE_ADVERTISING_ID;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(RobolectricTestRunner.class)
public class SwrveGoogleUtilTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = spy(swrveReal);
        SwrveCommon.setSwrveCommon(swrveSpy);
        SwrveBackgroundEventSender backgroundEventSenderMock = mock(SwrveBackgroundEventSender.class);
        doNothing().when(backgroundEventSenderMock).send(anyString(), anyList());
        doReturn(backgroundEventSenderMock).when(swrveSpy).getSwrveBackgroundEventSender(any(Context.class));
    }

    @Test
    public void testObtainRegistrationId() {

        SwrveGoogleUtil googleUtilSpy = spy(new SwrveGoogleUtil(ApplicationProvider.getApplicationContext()));

        SwrveMultiLayerLocalStorage multiLayerLocalStorage = swrveSpy.multiLayerLocalStorage;
        String userId = swrveSpy.getUserId();

        FirebaseMessaging firebaseMessagingMock = mock(FirebaseMessaging.class);
        doReturn(firebaseMessagingMock).when(googleUtilSpy).getFirebaseMessagingInstance();
        Task<String> task = getDummyTask("some_reg_token_id");
        doReturn(task).when(firebaseMessagingMock).getToken();

        googleUtilSpy.obtainRegistrationId(multiLayerLocalStorage, userId);
        assertEquals("some_reg_token_id", googleUtilSpy.registrationId);
    }

    @Test
    public void testObtainGAID() throws Exception {

        SwrveGoogleUtil googleUtilSpy = spy(new SwrveGoogleUtil(ApplicationProvider.getApplicationContext()));

        SwrveMultiLayerLocalStorage multiLayerLocalStorage = swrveSpy.multiLayerLocalStorage;
        String userId = swrveSpy.getUserId();
        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        String uniqueKey = swrveCommon.getUniqueKey(userId);
        multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_GOOGLE_ADVERTISING_ID, "gaid_saved", uniqueKey);

        doReturn("new_gaid").when(googleUtilSpy).getAdvertisingId();

        googleUtilSpy.obtainGAID(multiLayerLocalStorage, userId);

        assertEquals("gaid_saved", googleUtilSpy.advertisingId); // the saved gaid will remain intact until the async code executes with new updated.

        Callable<String> callable = () -> googleUtilSpy.advertisingId;
        await().atMost(1, TimeUnit.SECONDS).until(callable, CoreMatchers.equalTo("new_gaid"));

        assertEquals("new_gaid", googleUtilSpy.advertisingId);
    }

    @Test
    public void testSetRegistrationId() {

        SwrveGoogleUtil googleUtil = new SwrveGoogleUtil(ApplicationProvider.getApplicationContext());

        SwrveMultiLayerLocalStorage multiLayerLocalStorage = swrveSpy.multiLayerLocalStorage;
        String userId = swrveSpy.getUserId();

        googleUtil.saveAndSendRegistrationId(multiLayerLocalStorage, userId, "reg1");
        assertEquals("reg1", googleUtil.getRegistrationId(multiLayerLocalStorage, userId));

        googleUtil.saveAndSendRegistrationId(multiLayerLocalStorage, userId, "reg2");
        assertEquals("reg2", googleUtil.getRegistrationId(multiLayerLocalStorage, userId));
    }

    @Test
    public void testAppendGoogleDeviceUpdate() throws Exception {

        SwrveGoogleUtil googleUtil = new SwrveGoogleUtil(ApplicationProvider.getApplicationContext());
        googleUtil.advertisingId = "test1";
        googleUtil.registrationId = "test2";

        JSONObject deviceUpdate = new JSONObject();
        deviceUpdate.put("existingkey", "existingvalue");

        googleUtil.appendDeviceUpdate(deviceUpdate);
        assertTrue(deviceUpdate.length() == 3);
        assertEquals("existingvalue", deviceUpdate.get("existingkey"));
        assertEquals("test1", deviceUpdate.get("swrve.GAID"));
        assertEquals("test2", deviceUpdate.get("swrve.gcm_token"));
    }

    private Task getDummyTask(String token) {
        return new Task<String>() {
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
            public String getResult() {
                return null;
            }

            @Override
            public <X extends Throwable> String getResult(@NonNull Class<X> aClass) throws X {
                return null;
            }

            @Nullable
            @Override
            public Exception getException() {
                return null;
            }

            @NonNull
            @Override
            public Task<String> addOnSuccessListener(@NonNull OnSuccessListener<? super String> onSuccessListener) {
                onSuccessListener.onSuccess(token);
                return this;
            }

            @NonNull
            @Override
            public Task<String> addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener<? super String> onSuccessListener) {
                return this;
            }

            @NonNull
            @Override
            public Task<String> addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener<? super String> onSuccessListener) {
                return this;
            }

            @NonNull
            @Override
            public Task<String> addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
                return this;
            }

            @NonNull
            @Override
            public Task<String> addOnFailureListener(@NonNull Executor executor, @NonNull OnFailureListener onFailureListener) {
                return this;
            }

            @NonNull
            @Override
            public Task<String> addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener onFailureListener) {
                return this;
            }
        };
    }
}
