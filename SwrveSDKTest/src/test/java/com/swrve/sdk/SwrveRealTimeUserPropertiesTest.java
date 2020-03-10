package com.swrve.sdk;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.Map;

public class SwrveRealTimeUserPropertiesTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.disableAssetsManager(swrveSpy);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test(timeout = 5000)
    public void testRealTimeUserProperties() throws Exception {
        String campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity, "real_time_user_properties_test.json");
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);

        swrveSpy.init(mActivity);
        Mockito.reset(swrveSpy);

        swrveSpy.getRealTimeUserProperties(new SwrveRealTimeUserPropertiesListener() {
            @Override
            public void onRealTimeUserPropertiesSuccess(Map<String, String> properties, String propertiesAsJSON) {
                Assert.assertTrue("property key should be present" , properties.containsKey("test_id"));
                Assert.assertEquals("{\"test_id\": \"test_value\"}", propertiesAsJSON);
                Assert.assertEquals("test_value", properties.get("test_id"));
            }

            @Override
            public void onRealTimeUserPropertiesError(Exception exception) {

            }
        });
    }
}
