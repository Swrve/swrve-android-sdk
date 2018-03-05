package com.swrve.sdk;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

public class SwrveSwrveABTestDetailsTest extends SwrveBaseTest {
    private Swrve swrveSpy;
    private SwrveResourceManager resourceManager;

    private class TestSwrveResourceManager extends SwrveResourceManager {
        public CountDownLatch calledLatch;

        public TestSwrveResourceManager() {
            super();
            calledLatch = new CountDownLatch(1);
        }

        @Override
        public void setABTestDetailsFromJSON(JSONObject abTestInfoCollectionJson) {
            super.setABTestDetailsFromJSON(abTestInfoCollectionJson);
            calledLatch.countDown();
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        resourceManager = new TestSwrveResourceManager();
        swrveSpy.resourceManager = resourceManager;
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test(timeout = 5000)
    public void testAbTestDetails() throws Exception {
        String campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity, "ab_test_information.json");
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);

        swrveSpy.init(mActivity);
        Mockito.reset(swrveSpy);

        ((TestSwrveResourceManager)swrveSpy.getResourceManager()).calledLatch.await();

        List<SwrveABTestDetails> abTestDetails = swrveSpy.getResourceManager().getABTestDetails();
        assertEquals(2, abTestDetails.size());

        SwrveABTestDetails details1 = abTestDetails.get(0);
        assertEquals("12", details1.getId());
        assertEquals("AB test Name 1", details1.getName());
        assertEquals(1, details1.getCaseIndex());

        SwrveABTestDetails details2 = abTestDetails.get(1);
        assertEquals("13", details2.getId());
        assertEquals("AB test Name 2", details2.getName());
        assertEquals(4, details2.getCaseIndex());
    }
}