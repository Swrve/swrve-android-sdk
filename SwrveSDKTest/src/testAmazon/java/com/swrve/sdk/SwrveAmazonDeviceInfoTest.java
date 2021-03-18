package com.swrve.sdk;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

public class SwrveAmazonDeviceInfoTest extends SwrveBaseTest {

    @Test
    public void testDeviceUpdateUponInit() {

        // Following the same pattern as EventsTest by creating a customRestClient to verify what is
        // sent with an init. Only this test specifically looks at the device update properties.

        Swrve swrve = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        final AtomicBoolean callbackCompleted = new AtomicBoolean(false);
        swrve.restClient = new IRESTClient() {

            boolean hasDeviceUpdateSent = false;

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
                    hasDeviceUpdateSent = true;
                }

                if (hasDeviceUpdateSent) {
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
                        Assert.assertEquals("amazon", attributeDevices.getString("swrve.sdk_flavour"));
                        Assert.assertEquals("amazon", attributeDevices.getString("swrve.app_store"));
                        Assert.assertEquals("amazon-android", attributeDevices.getString("swrve.os"));

                    } catch (JSONException err) {
                        callbackCompleted.set(false);
                    }

                    callbackCompleted.set(true);
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

            boolean hasCalledGET = false;

            @Override
            public void get(String endpoint, IRESTResponseListener callback) {
                // empty
            }

            @Override
            public void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) {

                if (endpoint.contains("user_content")) {
                    hasCalledGET = true;
                }

                if (hasCalledGET) {
                    Assert.assertEquals("amazon-android", params.get("os"));
                    Assert.assertEquals("mobile", params.get("device_type"));
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
