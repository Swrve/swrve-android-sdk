package com.swrve.sdk;

import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveOrientation;

import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

public class SwrveMessagePriorityTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        SwrveTestUtils.disableRestClientExecutor(swrveSpy);
        swrveSpy.init(mActivity);
    }

    @Test
    public void testPriorityWithAvailableMessage() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_priority.json", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");
        SwrveBaseMessage baseMessage = swrveSpy.getBaseMessageForEvent("priority_trigger");
        assertNotNull(baseMessage);
        assertTrue("baseMessage should be instanceof SwrveMessage", baseMessage instanceof SwrveMessage);
        assertEquals(165, baseMessage.getId());
    }

    @Test
    public void testPriorityMessageAssetNotAvailable() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_priority.json"); // missing asset 97c5df26c8e8fcff8dbda7e662d4272a6a94af7e
        SwrveBaseMessage baseMessage = swrveSpy.getBaseMessageForEvent("priority_trigger");
        assertNotNull(baseMessage);
        assertTrue("baseMessage should be instanceof SwrveEmbeddedMessage", baseMessage instanceof SwrveEmbeddedMessage);
        assertEquals(20, baseMessage.getId());
    }

    @Test
    public void testPriorityMessageDifferentOrientation() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_priority.json"); // missing asset 97c5df26c8e8fcff8dbda7e662d4272a6a94af7e
        HashMap<String, String> emptyPayload = new HashMap<String, String>();
        SwrveBaseMessage baseMessage = swrveSpy.getBaseMessageForEvent("priority_trigger", emptyPayload, SwrveOrientation.Portrait);
        assertNotNull(baseMessage);
        assertTrue("baseMessage should be instanceof SwrveEmbeddedMessage", baseMessage instanceof SwrveEmbeddedMessage);
        assertEquals(20, baseMessage.getId());
    }

    @Test
    public void testPriorityMessagePayload() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_priority.json", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");
        HashMap<String, String> testPayload = new HashMap<String, String>();
        testPayload.put("test", "value");
        SwrveBaseMessage baseMessage = swrveSpy.getBaseMessageForEvent("priority_payload", testPayload);
        assertNotNull(baseMessage);
        assertTrue("baseMessage should be instanceof SwrveMessage", baseMessage instanceof SwrveMessage);
        assertEquals(166, baseMessage.getId());
    }
}
