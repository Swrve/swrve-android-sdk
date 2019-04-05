package com.swrve.sdk;

import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

public class EventsTest extends SwrveBaseTest {

    @Test
    public void testEventsUponInit()  {

        // Use a custom restclient to consume the rest calls executed. There are 3 events expected to be sent
        // upon init of the sdk. Its not guaranteed that these 3 events will be sent in one batch, hence the
        // test below will wait until these 3 events are sent to the rest client. It will timeout if they do
        // not get sent.

        Swrve swrve = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        final AtomicBoolean restCallback = new AtomicBoolean(false);
        swrve.restClient = new IRESTClient() {

            boolean hasSessionStartSent = false;
            boolean hasFirstSessionSent = false;
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
                if(encodedBody.contains("session_start")){
                    hasSessionStartSent = true;
                }
                if(encodedBody.contains("device_update")){
                    hasDeviceUpdateSent = true;
                }
                if(encodedBody.contains("Swrve.first_session")){
                    hasFirstSessionSent = true;
                }

                if(hasSessionStartSent && hasDeviceUpdateSent && hasFirstSessionSent) {
                    restCallback.set(true);
                }
            }

            @Override
            public void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType) {
                // empty
            }
        };

        swrve.init(mActivity);
        await().untilTrue(restCallback);
    }
}
