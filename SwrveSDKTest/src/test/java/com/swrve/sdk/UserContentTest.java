package com.swrve.sdk;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

public class UserContentTest extends SwrveBaseTest {

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
                    Assert.assertEquals("apiKey", params.get("api_key"));
                    Assert.assertEquals(swrve.getUserId(), params.get("user"));
                    Assert.assertEquals("1", params.get("embedded_campaign_version"));
                    Assert.assertEquals("8", params.get("version"));
                    Assert.assertEquals("3", params.get("in_app_version"));
                    Assert.assertNotNull(params.get("device_name"));
                    Assert.assertNotNull(params.get("os_version"));
                    Assert.assertNotNull(params.get("app_store"));
                    Assert.assertNotNull(params.get("app_version"));
                    Assert.assertNotNull(params.get("os"));
                    Assert.assertNotNull(params.get("device_type"));
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
