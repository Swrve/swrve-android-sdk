package com.swrve.sdk;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

public class SwrveHuaweiDeviceInfoTest extends SwrveBaseTest {

    @Test
    public void testDeviceUpdateUponInit() {

        Swrve swrve = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        final AtomicBoolean callbackCompleted = new AtomicBoolean(false);
        swrve.restClient = new IRESTClient() {

            @Override
            public void get(String endpoint, IRESTResponseListener callback) {
                // empty
            }

            @Override
            public void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) {
                // empty
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback) {

                if (encodedBody.contains("device_update")) {
                    try {
                        JSONObject body = new JSONObject(encodedBody);
                        Assert.assertNotNull(body);
                        JSONArray data = body.getJSONArray("data");
                        JSONObject deviceUpdate = null;
                        Assert.assertNotNull(data);

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.getJSONObject(i);
                            if (item.getString("type").equals("device_update")) {
                                deviceUpdate = item;
                            }
                        }

                        Assert.assertNotNull(deviceUpdate);
                        JSONObject attributeDevices = deviceUpdate.getJSONObject("attributes");
                        assertEquals("huawei", attributeDevices.getString("swrve.sdk_flavour"));
                        assertEquals("huawei", attributeDevices.getString("swrve.app_store"));
                        assertEquals("huawei-android", attributeDevices.getString("swrve.os"));

                        callbackCompleted.set(true);
                    } catch (Exception ex) {
                        SwrveLogger.e("Error:", ex);
                    }
                }
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType) {
                // empty
            }
        };

        swrve.init(mActivity);
        await().untilTrue(callbackCompleted);
    }

    @Test
    public void testUserContentParamsUponInit() {

        Swrve swrve = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        final AtomicBoolean callbackCompleted = new AtomicBoolean(false);
        swrve.restClient = new IRESTClient() {

            @Override
            public void get(String endpoint, IRESTResponseListener callback) {
                // empty
            }

            @Override
            public void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) {

                if (endpoint.contains("user_content")) {
                    assertEquals("huawei-android", params.get("os"));
                    assertEquals("mobile", params.get("device_type"));
                    callbackCompleted.set(true);
                }
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback) {
                // empty
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType) {
                // empty
            }
        };

        swrve.init(mActivity);
        await().untilTrue(callbackCompleted);
    }
}
