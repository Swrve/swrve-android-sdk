package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveEmbeddedMessageConfig;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveBaseMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

public class SwrveEmbeddedMessageCallbackTest extends SwrveBaseTest {

    private Swrve swrveSpy;
    private SwrveConfig config;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        config = new SwrveConfig();
    }

    private void initSDK() throws Exception {
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey", config);
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        swrveSpy.init(mActivity);
    }

    @Test
    public void testGetEmbeddedMessageFromMessageCenter() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);

        SwrveEmbeddedMessageConfig embeddedMessageConfig = new SwrveEmbeddedMessageConfig.Builder().embeddedMessageListener((context, message) -> {
            embeddedCallbackBool.set(true);
        }).build();

        config.setEmbeddedMessageConfig(embeddedMessageConfig);

        initSDK();

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded_mc.json");

        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0));
        await().untilTrue(embeddedCallbackBool);
    }

    @Test
    public void testEmbeddedMessageCallbackFromTrigger() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);

        SwrveEmbeddedMessageConfig embeddedMessageConfig = new SwrveEmbeddedMessageConfig.Builder().embeddedMessageListener((context, message) -> {
            embeddedCallbackBool.set(true);
        }).build();

        config.setEmbeddedMessageConfig(embeddedMessageConfig);

        initSDK();

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded.json");

        swrveSpy.event("trigger_embedded");
        await().untilTrue(embeddedCallbackBool);
    }

    @Test
    public void testEmbeddedMessageCallbackFromTriggerWithPayload() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);
        SwrveEmbeddedMessageConfig embeddedMessageConfig = new SwrveEmbeddedMessageConfig.Builder().embeddedMessageListener((context, message) -> {
            embeddedCallbackBool.set(true);
        }).build();

        config.setEmbeddedMessageConfig(embeddedMessageConfig);
        initSDK();

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded.json");

        HashMap<String, String> testPayload = new HashMap<String, String>();
        testPayload.put("test", "value");

        swrveSpy.event("trigger_embedded", testPayload);
        await().untilTrue(embeddedCallbackBool);
    }

    @Test
    public void testEmbeddedMessageImpressionAndEngagementEventCallback() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);
        SwrveEmbeddedMessageConfig embeddedMessageConfig = new SwrveEmbeddedMessageConfig.Builder().embeddedMessageListener((context, message) -> {
            swrveSpy.embeddedMessageWasShownToUser(message);
            swrveSpy.embeddedMessageButtonWasPressed(message, message.getButtons().get(0));
            embeddedCallbackBool.set(true);
        }).build();

        config.setEmbeddedMessageConfig(embeddedMessageConfig);
        initSDK();

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded.json");

        // Initially count impressions
        SwrveBaseMessage baseMessage = swrveSpy.getBaseMessageForEvent("impression_trigger");
        Assert.assertEquals(0 , baseMessage.getCampaign().getImpressions());

        HashMap<String, String> testPayload = new HashMap<String, String>();
        testPayload.put("test", "value");

        swrveSpy.event("impression_trigger", testPayload);
        await().untilTrue(embeddedCallbackBool);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-23.impression");
        Map<String, Object> payload = new HashMap<>();
        payload.put("embedded", "true");

        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);

        parameters.clear();
        parameters.put("name", "Swrve.Messages.Message-23.click");
        payload.clear();
        payload.put("embedded", "true");
        payload.put("name", "Button 1");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);

        // count impressions again
        baseMessage = swrveSpy.getBaseMessageForEvent("impression_trigger");
        Assert.assertEquals(1 , baseMessage.getCampaign().getImpressions());
    }
}
