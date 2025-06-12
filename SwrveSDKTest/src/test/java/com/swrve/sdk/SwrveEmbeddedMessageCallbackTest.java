package com.swrve.sdk;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedCampaign;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SwrveEmbeddedMessageCallbackTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        SwrveTestUtils.disableRestClientExecutor(swrveSpy);
        swrveSpy.init(mActivity);
    }

    @Test
    public void testGetEmbeddedMessageFromMessageCenter() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);
        swrveSpy.embeddedListener = (context, message, personalizationProperties, isControl) -> {
            embeddedCallbackBool.set(true);
        };
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded_mc.json");

        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        SwrveEmbeddedCampaign campaign = (SwrveEmbeddedCampaign) campaigns.get(0);
        assertEquals("Kindle", campaign.getName());
        assertNotNull(campaign.getDownloadDate());
        swrveSpy.showMessageCenterCampaign(campaigns.get(0));
        await().untilTrue(embeddedCallbackBool);
    }

    @Test
    public void testGetEmbeddedMessageCenterCampaigns() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded_mc.json");

        List<SwrveEmbeddedMessage> campaigns = SwrveSDK.getEmbeddedMessageCenterCampaigns();
        assertEquals(2, campaigns.size());
        SwrveEmbeddedMessage embeddedMessage133 = campaigns.get(0);
        assertEquals(133, embeddedMessage133.getCampaignId());
        assertEquals("Kindle", embeddedMessage133.getName());
        assertNotNull(embeddedMessage133.getData());
        assertEquals("test string", embeddedMessage133.getData());
        SwrveEmbeddedMessage embeddedMessage134 = campaigns.get(1);
        assertEquals(134, embeddedMessage134.getCampaignId());

        SwrveSDK.removeMessageCenterCampaign(embeddedMessage133.getCampaignId());
        campaigns = SwrveSDK.getEmbeddedMessageCenterCampaigns();
        assertEquals(1, campaigns.size());
        embeddedMessage134 = campaigns.get(0);
        assertEquals(134, embeddedMessage134.getCampaignId());
    }

    @Test
    public void testGetEmbeddedMessageFromMessageCenterWithPersonalization() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);
        swrveSpy.embeddedListener = (context, message, personalizationProperties, isControl) -> {
            String resolvedData = SwrveSDK.getPersonalizedEmbeddedMessageData(message, personalizationProperties);
            embeddedCallbackBool.set((resolvedData != null && resolvedData.equalsIgnoreCase("personalization: WORKING")));
        };

        // Set the personalization provider and the value that is required for the campaign
        final Map<String, String> properties = new HashMap<>();
        properties.put("test_key", "WORKING");
        swrveSpy.personalizationProvider = eventPayload -> properties;
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded_mc_personalization.json");

        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        assertEquals(2, campaigns.size());
        SwrveEmbeddedMessage message = ((SwrveEmbeddedCampaign)campaigns.get(0)).getMessage();
        assertNotNull(message);
        assertEquals("personalization: ${test_key}", message.getData());
        String personalizationData = SwrveSDK.getPersonalizedText(message.getData(), properties);
        assertEquals("personalization: WORKING", personalizationData);
        swrveSpy.showMessageCenterCampaign(campaigns.get(0)); // for embedded message center campaigns, its unlikely you will call "showMessageCenterCampaign"
        await().untilTrue(embeddedCallbackBool);
    }

    @Test
    public void testGetEmbeddedMessageCenterCampaignsWithPersonalization() throws Exception {
        // Set the personalization provider and the value that is required for the campaign
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded_mc_personalization.json");

        List<SwrveEmbeddedMessage> campaigns = SwrveSDK.getEmbeddedMessageCenterCampaigns();
        assertEquals(2, campaigns.size());
        SwrveEmbeddedMessage embeddedMessage = campaigns.get(0);
        assertNotNull(embeddedMessage);
        assertEquals("personalization: ${test_key}", embeddedMessage.getData());
        Map<String, String> properties = new HashMap<>();
        properties.put("test_key", "WORKING");
        String personalizationData = SwrveSDK.getPersonalizedText(embeddedMessage.getData(), properties);
        assertEquals("personalization: WORKING", personalizationData);
        String resolvedData = SwrveSDK.getPersonalizedEmbeddedMessageData(embeddedMessage, properties);
        assertEquals("personalization: WORKING", resolvedData);

        SwrveSDK.removeMessageCenterCampaign(embeddedMessage.getCampaignId());
        campaigns = SwrveSDK.getEmbeddedMessageCenterCampaigns();
        assertEquals(1, campaigns.size());
    }

    @Test
    public void testEmbeddedMessageCallbackFromTrigger() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);
        swrveSpy.eventListener = new SwrveEventListener(swrveSpy, (context, message, personalizationProperties, isControl) -> {
            assertEquals("test string", message.getData());
            embeddedCallbackBool.set(true);
        });
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded.json");

        swrveSpy.event("trigger_embedded");
        await().untilTrue(embeddedCallbackBool);
    }

    @Test
    public void testEmbeddedMessageCallbackFromTriggerWithPayload() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);
        swrveSpy.eventListener = new SwrveEventListener(swrveSpy, (context, message, personalizationProperties, isControl) -> {
            embeddedCallbackBool.set(true);
        });
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded.json");

        HashMap<String, String> testPayload = new HashMap<>();
        testPayload.put("test", "value");
        swrveSpy.event("trigger_embedded", testPayload);
        await().untilTrue(embeddedCallbackBool);
    }

    @Test
    public void testEmbeddedMessageCallbackFromTriggerWithPersonalization() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);
        swrveSpy.eventListener = new SwrveEventListener(swrveSpy, (context, message, personalizationProperties, isControl) -> {
            assertEquals("{\"test\": \"${new_key}\"}", message.getData());
            String resolvedData = SwrveSDK.getPersonalizedText(message.getData(), personalizationProperties);
            assertEquals("{\"test\": \"WORKING\"}", resolvedData);
            embeddedCallbackBool.set((resolvedData != null));
        });

        // Set the personalization provider and the value that is required for the campaign
        swrveSpy.personalizationProvider = eventPayload -> {
            Map<String, String> properties = new HashMap<>();
            properties.put("new_key", "WORKING");
            return properties;
        };
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded.json");

        HashMap<String, String> testPayload = new HashMap<>();
        testPayload.put("test", "value");
        swrveSpy.event("embedded_personalized", testPayload);
        await().untilTrue(embeddedCallbackBool);
    }

    @Test
    public void testEmbeddedMessageImpressionAndEngagementEventCallback() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);
        swrveSpy.eventListener = new SwrveEventListener(swrveSpy, (context, message, personalizationProperties, isControl) -> {
            swrveSpy.embeddedMessageWasShownToUser(message);
            swrveSpy.embeddedMessageButtonWasPressed(message, message.getButtons().get(0));
            embeddedCallbackBool.set(true);
        });
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded.json");

        // Initially count impressions
        SwrveBaseMessage baseMessage = swrveSpy.getBaseMessageForEvent("impression_trigger");
        Assert.assertEquals(0, baseMessage.getCampaign().getImpressions());

        HashMap<String, String> testPayload = new HashMap<>();
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
        Assert.assertEquals(1, baseMessage.getCampaign().getImpressions());
    }
}
