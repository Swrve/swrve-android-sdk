package com.swrve.sdk

import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.rest.IRESTClient
import com.swrve.sdk.rest.IRESTResponseListener
import org.awaitility.Awaitility.await
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class EventsTest : SwrveBaseTest() {
    @Test
    fun testEventsUponInit() {
        // Use a custom restclient to consume the rest calls executed. There are 3 events expected to be sent
        // upon init of the sdk. Its not guaranteed that these 3 events will be sent in one batch, hence the
        // test below will wait until these 3 events are sent to the rest client. It will timeout if they do
        // not get sent.

        val swrve = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey") as Swrve
        val restCallback = AtomicBoolean(false)
        swrve.restClient = object : IRESTClient {
            var hasSessionStartSent: Boolean = false
            var hasFirstSessionSent: Boolean = false
            var hasDeviceUpdateSent: Boolean = false

            override fun get(endpoint: String, callback: IRESTResponseListener) {
                // empty
            }

            override fun get(endpoint: String, params: Map<String, String>, callback: IRESTResponseListener) {
                // empty
            }

            override fun post(endpoint: String, encodedBody: String, callback: IRESTResponseListener) {
                if (encodedBody.contains("session_start")) {
                    hasSessionStartSent = true
                }
                if (encodedBody.contains("device_update")) {
                    hasDeviceUpdateSent = true
                }
                if (encodedBody.contains("Swrve.first_session")) {
                    hasFirstSessionSent = true
                }

                if (hasSessionStartSent && hasDeviceUpdateSent && hasFirstSessionSent) {
                    restCallback.set(true)
                }
            }

            override fun post(endpoint: String, encodedBody: String, callback: IRESTResponseListener, contentType: String) {
                // empty
            }
        }

        swrve.init(mActivity)
        await().untilTrue(restCallback)
    }
}
