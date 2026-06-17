package com.swrve.sdk

import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.rest.IRESTClient
import com.swrve.sdk.rest.IRESTResponseListener
import org.awaitility.Awaitility
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class UserContentTest : SwrveBaseTest() {
    @Test
    fun testUserContentParamsUponInit() {
        val swrve = SwrveSDK.createInstance(
            ApplicationProvider.getApplicationContext(),
            1,
            "apiKey"
        ) as Swrve
        val callbackCompleted = AtomicBoolean(false)
        swrve.restClient = object : IRESTClient {
            var hasCalledGET: Boolean = false

            override fun get(endpoint: String, callback: IRESTResponseListener) {
                // empty
            }

            override fun get(
                endpoint: String,
                params: Map<String, String>,
                callback: IRESTResponseListener
            ) {
                if (endpoint.contains("user_content")) {
                    hasCalledGET = true
                }

                if (hasCalledGET) {
                    Assert.assertEquals("apiKey", params["api_key"])
                    Assert.assertEquals(swrve.userId, params["user"])
                    Assert.assertEquals("5", params["embedded_campaign_version"])
                    Assert.assertEquals("10", params["version"])
                    Assert.assertEquals("18", params["in_app_version"])
                    Assert.assertEquals("1", params["push_inbox_version"])
                    Assert.assertNotNull(params["device_name"])
                    Assert.assertNotNull(params["os_version"])
                    Assert.assertNotNull(params["app_store"])
                    Assert.assertNotNull(params["app_version"])
                    Assert.assertNotNull(params["os"])
                    Assert.assertNotNull(params["device_type"])
                    callbackCompleted.set(true)
                }
            }

            override fun post(
                endpoint: String,
                encodedBody: String,
                callback: IRESTResponseListener
            ) {
                // empty
            }

            override fun post(
                endpoint: String,
                encodedBody: String,
                callback: IRESTResponseListener,
                contentType: String
            ) {
                // empty
            }
        }

        swrve.init(mActivity)
        Awaitility.await().untilTrue(callbackCompleted)
    }
}
