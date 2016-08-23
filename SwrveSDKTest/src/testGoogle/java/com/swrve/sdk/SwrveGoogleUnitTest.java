package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SwrveGoogleUnitTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testManualRegistrationId() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setSenderId("12345");
        config.setPushRegistrationAutomatic(false);
        Swrve swrve = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        swrve.onCreate(mActivity);
        swrve.setRegistrationId("manual_reg_id");
        JSONObject deviceValuesToSend = swrve.getDeviceInfo();
        assertEquals("manual_reg_id", deviceValuesToSend.get("swrve.gcm_token"));
        swrve.onDestroy(mActivity);
    }
}
