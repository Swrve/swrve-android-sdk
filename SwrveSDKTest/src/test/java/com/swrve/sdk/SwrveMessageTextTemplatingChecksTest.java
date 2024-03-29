package com.swrve.sdk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwrveMessageTextTemplatingChecksTest extends SwrveBaseTest {

    @Test
    public void testChecks() throws Exception {
        SwrveInAppCampaign mockCampaign = Mockito.mock(SwrveInAppCampaign.class);
        String messageJson = SwrveTestUtils.getAssetAsText(mActivity, "personalized_message.json");
        SwrveMessage message = new SwrveMessage(mockCampaign, new JSONObject(messageJson), null);

        assertFalse(SwrveMessageTextTemplatingChecks.checkTextTemplating(message, null));

        // Incremental properties, unless all are in it should return false
        List<String> requiredProps = new ArrayList<>();
        requiredProps.add("test_image_text");
        requiredProps.add("test_button_action");
        requiredProps.add("test_copy_to_clipboard");
        requiredProps.add("test_message_center_subject");


        Map<String, String> properties = new HashMap<>();
        for (String prop : requiredProps) {
            assertFalse(SwrveMessageTextTemplatingChecks.checkTextTemplating(message, properties));
            properties.put(prop, "defined");
        }

        // Last required (no fallback) property set, should return true
        assertTrue(SwrveMessageTextTemplatingChecks.checkTextTemplating(message, properties));
    }

    @Test
    public void testUrlChecks() throws Exception {
        SwrveInAppCampaign mockCampaign = Mockito.mock(SwrveInAppCampaign.class);
        String messageJson = SwrveTestUtils.getAssetAsText(mActivity, "personalized_url_message.json");
        SwrveMessage message = new SwrveMessage(mockCampaign, new JSONObject(messageJson), null);

        assertFalse(SwrveMessageTextTemplatingChecks.checkImageUrlTemplating(message, null));

        List<String> requiredProps = new ArrayList<>();
        requiredProps.add("test_key_one");
        requiredProps.add("test_key_two");

        Map<String, String> properties = new HashMap<>();
        for (String prop : requiredProps) {
            assertFalse(SwrveMessageTextTemplatingChecks.checkImageUrlTemplating(message, properties));
            properties.put(prop, "defined");
        }

        // Last required (no fallback) property set, should return true
        assertTrue(SwrveMessageTextTemplatingChecks.checkTextTemplating(message, properties));

    }
}
