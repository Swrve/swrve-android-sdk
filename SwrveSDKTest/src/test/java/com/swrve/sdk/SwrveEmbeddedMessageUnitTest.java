package com.swrve.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;


public class SwrveEmbeddedMessageUnitTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        SwrveTestUtils.disableRestClientExecutor(swrveSpy);
        swrveSpy.init(mActivity);
    }

    @Test
    public void testGetEmbeddedMessageForId() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded.json");
        SwrveEmbeddedMessage message = swrveSpy.getEmbeddedMessageForId(20);
        assertNotNull(message);
        assertEquals("test string", message.getData());
        assertEquals(500, message.getPriority());
        assertEquals(500, message.getCampaign().getPriority());
        assertEquals(20, message.getId());
        assertNotNull(message.getButtons());
        assertEquals("Button 1", message.getButtons().get(0));
        assertEquals("Button 2", message.getButtons().get(1));
        assertEquals("Button 3", message.getButtons().get(2));
        assertEquals(SwrveEmbeddedMessage.EMBEDDED_CAMPAIGN_TYPE.OTHER, message.getType());
    }

    @Test
    public void testGetEmbeddedMessageForEvent() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded.json");
        SwrveBaseMessage baseMessage = swrveSpy.getBaseMessageForEvent("trigger_embedded");
        assertNotNull(baseMessage);
        assertTrue(baseMessage instanceof SwrveEmbeddedMessage);
        SwrveEmbeddedMessage message = (SwrveEmbeddedMessage) baseMessage;
        assertEquals("test string", message.getData());
        assertEquals(500, message.getPriority());
        assertEquals(20, message.getId());
        assertNotNull(message.getButtons());
        assertEquals("Button 1", message.getButtons().get(0));
        assertEquals("Button 2", message.getButtons().get(1));
        assertEquals("Button 3", message.getButtons().get(2));
        assertEquals(SwrveEmbeddedMessage.EMBEDDED_CAMPAIGN_TYPE.OTHER, message.getType());
    }

    @Test
    public void testGetEmbeddedMessageForEventWithPayload() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_embedded.json");

        HashMap<String, String> testPayload = new HashMap<String, String>();
        testPayload.put("test", "value");

        SwrveBaseMessage baseMessage = swrveSpy.getBaseMessageForEvent("embedded_payload", testPayload);
        assertNotNull(baseMessage);
        assertTrue(baseMessage instanceof SwrveEmbeddedMessage);
        SwrveEmbeddedMessage message = (SwrveEmbeddedMessage) baseMessage;
        assertEquals("{\"test\": \"json_payload\"}", message.getData());
        assertEquals(600, message.getPriority());
        assertEquals(22, message.getId());
        assertNotNull(message.getButtons());
        assertEquals("Button 1", message.getButtons().get(0));
        assertEquals("Button 2", message.getButtons().get(1));
        assertEquals("Button 3", message.getButtons().get(2));
        assertEquals(SwrveEmbeddedMessage.EMBEDDED_CAMPAIGN_TYPE.JSON, message.getType());
    }
}
