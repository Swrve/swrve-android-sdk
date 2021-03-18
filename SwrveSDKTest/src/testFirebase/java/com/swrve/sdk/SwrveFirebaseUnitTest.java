package com.swrve.sdk;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class SwrveFirebaseUnitTest extends SwrveBaseTest {

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

    @Test
    public void testFlavour() {
        assertEquals("firebase", swrveSpy.FLAVOUR.toString());
    }

    @Test
    public void testSetRegistrationId() throws JSONException {
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

        SwrveGoogleUtil googleUtilSpy = spy(swrveSpy.googleUtil);
        swrveSpy.googleUtil = googleUtilSpy;
        googleUtilSpy.advertisingId = "testadvertisingId";

        SwrveSDK.setRegistrationId("reg2");
        verify(googleUtilSpy).saveAndSendRegistrationId(multiLayerLocalStorage, userId, "reg2");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
        Mockito.verify(swrveSpy, atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), events.capture());

        List<ArrayList> capturedProperties = events.getAllValues();
        String jsonString = capturedProperties.get(0).get(0).toString();
        JSONObject jsonObject = new JSONObject(jsonString);

        assertTrue(jsonObject.has("time"));
        assertTrue(jsonObject.has("seqnum"));
        assertEquals("device_update", jsonObject.get("type"));
        assertEquals("false", jsonObject.get("user_initiated"));
        assertTrue(jsonObject.has("attributes"));
        JSONObject attributes = (JSONObject) jsonObject.get("attributes");
        assertTrue(attributes.length() == 2);
        assertEquals("reg2", attributes.get("swrve.gcm_token"));
        assertEquals("testadvertisingId", attributes.get("swrve.GAID"));
    }
}
