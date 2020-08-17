package com.swrve.sdk;

import com.swrve.sdk.messaging.SwrveMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SwrveQaUserTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
    }

    @Test
    public void testQAResetDevice() {

        // execute the rest and storage calls on same threads
        SwrveTestUtils.runSingleThreaded(swrveSpy);

        // QaUser is not enabled
        String campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity, "campaign_qa_non_qa.json");
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.init(mActivity);
        assertFalse(QaUser.isLoggingEnabled());
        assertFalse(QaUser.isResetDevice());

        // First impression
        SwrveMessage message = swrveSpy.getMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
        swrveSpy.messageWasShownToUser(message.getFormats().get(0));

        // Impression rule still applies
        message = swrveSpy.getMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // Becomes a QA user
        campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity, "campaign_qa_reset.json");
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.refreshCampaignsAndResources();
        assertTrue(QaUser.isLoggingEnabled());
        assertTrue(QaUser.isResetDevice());

        // First impression
        message = swrveSpy.getMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
        swrveSpy.messageWasShownToUser(message.getFormats().get(0));

        // Impression rule still applies
        message = swrveSpy.getMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // Reload campaigns
        swrveSpy.refreshCampaignsAndResources();

        // Impression rule still applies
        message = swrveSpy.getMessageForEvent("Swrve.currency_given");
        assertNull(message);
    }

    @Test
    public void testQAUserThenNormalUser() {

        // execute the rest and storage calls on same threads
        SwrveTestUtils.runSingleThreaded(swrveSpy);

        // There should be a QA user
        String campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity, "campaign_qa_reset.json");
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.init(mActivity);
        assertTrue(QaUser.isLoggingEnabled());
        assertTrue(QaUser.isResetDevice());

        // No qa user on subsequent response
        campaignsResponseJson = SwrveTestUtils.getAssetAsText(mActivity, "campaign_qa_non_qa.json");
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson);
        swrveSpy.refreshCampaignsAndResources();
        assertFalse(QaUser.isLoggingEnabled());
        assertFalse(QaUser.isResetDevice());
    }
}
