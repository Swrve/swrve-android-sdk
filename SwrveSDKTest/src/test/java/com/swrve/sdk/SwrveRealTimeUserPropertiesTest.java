package com.swrve.sdk;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SwrveRealTimeUserPropertiesTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.disableAssetsManager(swrveSpy);
    }

    @Test
    public void testRealTimeUserProperties() {
        String campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity, "real_time_user_properties_test.json");
        SwrveTestUtils.runSingleThreaded(swrveSpy); // need to run it single threaded because setting the value is a multi-threaded procedure
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);

        swrveSpy.init(mActivity);

        assertNotNull(swrveSpy.realTimeUserProperties);
        assertTrue("property key should be present", swrveSpy.realTimeUserProperties.containsKey("test_id"));
        assertEquals("test_value", swrveSpy.realTimeUserProperties.get("test_id"));
    }

    @Test
    public void testRealTimeUserPropertiesGetterFromCache() {
        String campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity, "real_time_user_properties_test.json");
        SwrveTestUtils.runSingleThreaded(swrveSpy); // need to run it single threaded because setting the value is a multi-threaded procedure
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);

        swrveSpy.init(mActivity);

        final AtomicBoolean callbackBool = new AtomicBoolean(false);
        swrveSpy.getRealTimeUserProperties(new SwrveRealTimeUserPropertiesListener() {
            @Override
            public void onRealTimeUserPropertiesSuccess(Map<String, String> properties, String propertiesAsJSON) {
                assertTrue("property key should be present", properties.containsKey("test_id"));
                assertEquals("test_value", properties.get("test_id"));
                callbackBool.set(true);
            }

            @Override
            public void onRealTimeUserPropertiesError(Exception exception) {
                SwrveLogger.e("getRealTimeUserProperties failed:", exception);
                Assert.fail("onRealTimeUserPropertiesError error");
                callbackBool.set(true);
            }
        });

        await().untilTrue(callbackBool);
    }
}
