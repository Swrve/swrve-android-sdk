package com.swrve.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.localstorage.LocalStorageTestUtils;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.view.SwrveMessageView;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONObject;
import org.junit.Assert;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.swrve.sdk.ISwrveCommon.EVENT_ID_KEY;
import static com.swrve.sdk.ISwrveCommon.EVENT_PAYLOAD_KEY;
import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;
import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CONTEXT_ID_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

public class SwrveTestUtils {

    public static void shutdownAndRemoveSwrveSDKSingletonInstance() throws Exception {
        ISwrveBase swrve = SwrveSDK.getInstance();
        if (swrve != null) {
            swrve.shutdown();
        }
        removeSingleton(SwrveSDKBase.class, "instance");
        LocalStorageTestUtils.closeSQLiteOpenHelperInstance();
    }

    public static void removeSwrveSDKSingletonInstance() throws Exception{
        removeSingleton(SwrveSDKBase.class, "instance");
    }

    public static void removeSingleton(Class clazz, String fieldName) throws Exception {
        removeSingleton(clazz, fieldName, null);
    }

    public static void removeSingleton(Class clazz, String fieldName, Object value) throws Exception {
        Field instance = clazz.getDeclaredField(fieldName);
        instance.setAccessible(true);
        instance.set(null, value);
    }

    public static void setSDKInstance(ISwrveBase instance) throws Exception {
        Field hack = SwrveSDKBase.class.getDeclaredField("instance");
        hack.setAccessible(true);
        hack.set(null, instance);
    }

    public static String getAssetAsText(Context context, String assetName) {
        InputStream in = null;
        String result = null;
        try {
            URL resource = context.getClassLoader().getResource(assetName);
            Assert.assertNotNull(resource);
            in = resource.openStream();
            Assert.assertNotNull(in);
            java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
            result = s.hasNext() ? s.next() : "";
            Assert.assertFalse(result.length() == 0);
        } catch (IOException ex) {
            SwrveLogger.e("Error getting asset as text:%s", ex, assetName);
            fail("Error getting asset as text:" + assetName + " ex:" + ex.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * Loads the campaigns from json file into swrve sdk
     * @param swrve sdk
     * @param campaignFileName the cfile name in assets folder containing the campaign json
     * @param assets an array of downloaded assets so campaign is eligible (font or image)
     * @throws Exception If there's an error
     */
    public static void loadCampaignsFromFile(Context context, Swrve swrve, String campaignFileName, String... assets) throws Exception {
        String json = SwrveTestUtils.getAssetAsText(context, campaignFileName);
        JSONObject jsonObject = new JSONObject(json);
        swrve.loadCampaignsFromJSON(swrve.getUserId(), jsonObject, swrve.campaignsState, false);
        if (assets.length > 0) {
            Set<String> assetsOnDisk = new HashSet<>();
            for(String asset : assets) {
                assetsOnDisk.add(asset);
                SwrveTestUtils.writeFileToCache(ApplicationProvider.getApplicationContext().getCacheDir(), asset);
            }
            ((SwrveAssetsManagerImp)swrve.swrveAssetsManager).assetsOnDisk = assetsOnDisk;
        }
    }

    public static String takeScreenshot(SwrveMessageView view) {
        view.layout(0, 0, view.getFormat().getSize().x, view.getFormat().getSize().y);
        return takeScreenshot((View) view);
    }

    public static String takeScreenshot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        view.draw(canvas);

        // Convert to base64 bitmap
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos); //bm is the bitmap object
        bitmap.recycle();
        byte[] b = baos.toByteArray();

        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    public static ISwrveCampaignManager getTestSwrveCampaignManager() {
        return new ISwrveCampaignManager() {
            @Override
            public Date getNow() {
                return new Date();
            }

            @Override
            public Date getInitialisedTime() {
                return new Date();
            }

            @Override
            public File getCacheDir() {
                return new File("");
            }

            @Override
            public Set<String> getAssetsOnDisk() {
                Set<String> set = new HashSet<>();
                set.add("asset1");
                return set;
            }

            @Override
            public SwrveConfigBase getConfig() {
                return new SwrveConfig();
            }

            @Override
            public String getAppStoreURLForApp(int appId) {
                return "";
            }

            @Override
            public void buttonWasPressedByUser(SwrveButton button) {
                // empty
            }

            @Override
            public void messageWasShownToUser(SwrveMessageFormat messageFormat) {
                // empty
            }
        };
    }

    public static void writeFileToCache(File cache, String filename) {
        File file = new File(cache, filename);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file, false);
            fileWriter.write("empty");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null) fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void disableAssetsManager(Swrve swrve) {
        SwrveAssetsManager swrveAssetsManagerSpy = Mockito.spy(swrve.swrveAssetsManager);
        Mockito.doNothing().when(swrveAssetsManagerSpy).downloadAssets(Mockito.anySet(), Mockito.any(SwrveAssetsCompleteCallback.class));
        swrve.swrveAssetsManager = swrveAssetsManagerSpy;
    }

    public static void disableBeforeSendDeviceInfo(Swrve swrveReal, Swrve swrveSpy) {
        Application application = ApplicationProvider.getApplicationContext();
        application.unregisterActivityLifecycleCallbacks(swrveReal);
        swrveSpy.registerActivityLifecycleCallbacks();
        Mockito.doNothing().when(swrveSpy).beforeSendDeviceInfo(any(Context.class));
    }

    public static void disableRestClientExecutor(Swrve swrveSpy) {
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
    }

    /*
     * Using Mockito to verify events are sent to the queueEvent method.
     * Call Mockito.reset(swrveSpy) before doing the test but can't be guaranteed that this is the only event
     * captured to the queueEvent method, so a search is done.
     */
    public static void assertQueueEvent(Swrve swrveSpy, String expectedEventType, Map<String, Object> expectedParameters, Map<String, Object> expectedPayload) {
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> parametersCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Boolean> triggerEventListenerCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(swrveSpy, Mockito.atLeastOnce()).queueEvent(userIdCaptor.capture(), eventTypeCaptor.capture(), parametersCaptor.capture(), payloadCaptor.capture(), triggerEventListenerCaptor.capture());

        List<String> userIds = userIdCaptor.getAllValues();
        List<String> capturedEventTypes = eventTypeCaptor.getAllValues();
        List<Map> capturedParameters = parametersCaptor.getAllValues();
        List<Map> capturedPayload = payloadCaptor.getAllValues();

        // assert userId
        assertTrue("Asserting userId: " + swrveSpy.getUserId() + " in userIds:" + userIds, userIds.contains(swrveSpy.getUserId()));

        // assert event type
        assertTrue("Asserting eventType: " + expectedEventType + " in capturedEventTypes:" + capturedEventTypes, capturedEventTypes.contains(expectedEventType));

        // find the indices at which they were found so rest of parameters and payloads can be eliminated.
        List<Integer> matchedIndices = new ArrayList<>();
        for (int i = 0; i < capturedEventTypes.size(); i++) {
            String capturedEventType = capturedEventTypes.get(i);
            if (capturedEventType.equals(expectedEventType)) {
                matchedIndices.add(i);
            }
        }

        // assert parameters
        if (expectedParameters != null && expectedParameters.size() > 0) {
            boolean hasMatches = filterMatchesFromListMap(matchedIndices, expectedParameters, capturedParameters);
            assertTrue("Asserting expectedParameters:" + expectedParameters + " in:" + capturedParameters, hasMatches);
        }

        // assert payload
        if (expectedPayload != null && expectedPayload.size() > 0) {
            boolean hasMatches = filterMatchesFromListMap(matchedIndices, expectedPayload, capturedPayload);
            assertTrue("Asserting expectedPayload:" + expectedPayload + " in:" + capturedPayload, hasMatches);
        }

        if (matchedIndices.size() == 0) {
            fail("Event not queued. eventType:" + expectedEventType + "\nparameters:" + expectedParameters + "\npayload:" + expectedPayload);
        }
    }

    private static boolean filterMatchesFromListMap(List<Integer> matchedIndices, Map<String, Object> expected, List<Map> actual) {
        List<Integer> indicesToRemove = new ArrayList<>();
        for (Integer index : matchedIndices) { // only iterate through the known matched indices
            Map capturedParameterMap = actual.get(index);
            for (int i = 0; i < actual.size(); i++) {
                boolean matchesAll = true;
                for (Map.Entry<String, Object> entry : expected.entrySet()) { // iterate through expected results
                    String expectedKey = entry.getKey();
                    Object expectedValue = entry.getValue();
                    if (!capturedParameterMap.containsKey(expectedKey)) {
                        matchesAll = false;
                        break;
                    } else if (!capturedParameterMap.get(expectedKey).toString().equals(expectedValue.toString())) {
                        matchesAll = false;
                        break;
                    }
                }
                if (!matchesAll) {
                    indicesToRemove.add(index);
                    break;
                }
            }
        }
        matchedIndices.removeAll(indicesToRemove);
        return matchedIndices.size() > 0;
    }

    public static IRESTClient createFakeRestClient(final int responseCode) {
        return new IRESTClient() {
            @Override
            public void get(String endpoint, IRESTResponseListener callback) {
                // unused
            }
            @Override
            public void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) {
                // unused
            }
            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback) {
                RESTResponse response = new RESTResponse(responseCode, String.valueOf(responseCode), createFakeResponseHeaders(responseCode));
                callback.onResponse(response);
            }
            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType) {
                RESTResponse response = new RESTResponse(responseCode, String.valueOf(responseCode), createFakeResponseHeaders(responseCode));
                callback.onResponse(response);
            }
        };
    }

    private static Map<String, List<String>> createFakeResponseHeaders(final int responseCode) {
        Map<String, List<String>> map = new HashMap<>();
        addHeadersToMap(map, null, "HTTP/1.1 " + responseCode);
        addHeadersToMap(map, "Connection", "Close");
        addHeadersToMap(map, "Content-Type", "text/plain");
        addHeadersToMap(map, "X-Android-Received-Millis", "1565687743788");
        addHeadersToMap(map, "X-Android-Response-Source", "NETWORK");
        addHeadersToMap(map, "X-Android-Selected-Protocol", "http/1.1");
        addHeadersToMap(map, "X-Android-Sent-Millis", "1565687743648");
        return map;
    }

    private static void addHeadersToMap(Map<String, List<String>> map, String key, String value) {
        List<String> headers = new ArrayList<>();
        headers.add(value);
        map.put(key, headers);
    }

    public static Date parseDate(String date){
        Date parsed = new Date();
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            parsed = simpleDateFormat.parse(date);
        } catch (Exception ex) {
            SwrveLogger.e("Error parseDate:" + date, ex);
        }
        return parsed;
    }

    public static void setRestClientWithGetResponse(Swrve swrve, final String response) {
        swrve.restClient = new IRESTClient() {
            @Override
            public void get(String endpoint, IRESTResponseListener callback) {
                callback.onResponse(new RESTResponse(200, response, null));
            }

            @Override
            public void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) throws UnsupportedEncodingException {
                callback.onResponse(new RESTResponse(200, response, null));
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback) {
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType) {
            }
        };
    }

    public static void onCreate(ISwrve swrve, Activity activity) {
        ((Swrve)swrve).onCreate(activity);
    }

    public static void assertGenericEvent(String eventJson, String expectedContextId, String expectedCampaignType, String expectedActionType, Map<String, String> expectedPayload) {
        Gson gson = new Gson(); // eg: {"type":"generic_campaign_event","time":1499179867473,"seqnum":1,"actionType":"button_click","campaignType":"push","contextId":"0","id":"4567","payload":{"buttonText":"btn1"}}
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> event = gson.fromJson(eventJson, type);
        assertTrue(event.containsKey("type"));
        assertEquals(EVENT_TYPE_GENERIC_CAMPAIGN, event.get(EVENT_TYPE_KEY));
        assertTrue(event.containsKey(EVENT_ID_KEY));
        if (SwrveHelper.isNotNullOrEmpty(expectedContextId)) {
            assertEquals(expectedContextId, event.get(GENERIC_EVENT_CONTEXT_ID_KEY));
        }
        assertEquals(expectedCampaignType, event.get(GENERIC_EVENT_CAMPAIGN_TYPE_KEY));
        assertEquals(expectedActionType, event.get(GENERIC_EVENT_ACTION_TYPE_KEY));
        assertTrue(event.containsKey("time"));
        assertTrue(event.containsKey("seqnum"));

        if (expectedPayload != null && expectedPayload.size() > 0) {
            assertTrue(event.containsKey(EVENT_PAYLOAD_KEY));
            Map<String, String> actualPayload = (Map) event.get(EVENT_PAYLOAD_KEY);
            assertEquals(expectedPayload, actualPayload);
        }
    }

    public static Swrve createSpyInstance() throws Exception {
        return createSpyInstance(new SwrveConfig());
    }

    public static Swrve createSpyInstance(SwrveConfig config) throws Exception {
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);
        Swrve swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        Mockito.doNothing().when(swrveSpy).checkForCampaignAndResourcesUpdates();
        return swrveSpy;
    }

    public static void runSingleThreaded(Swrve swrveSpy) {
        Answer<Boolean> answer = invocation -> {
            Runnable runnable = (Runnable) invocation.getArguments()[0];
            runnable.run();
            return true;
        };
        // execute the rest and storage calls on same threads
        doAnswer(answer).when(swrveSpy).restClientExecutorExecute(any(Runnable.class));
        doAnswer(answer).when(swrveSpy).storageExecutorExecute(any(Runnable.class));
    }
}
