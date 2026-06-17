package com.swrve.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;
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

    private static final String VALID_VISIBLE_IF_MESSAGE_JSON =
            "{\"id\":300,\"name\":\"t\",\"template\":{\"formats\":[{\"name\":\"t\",\"orientation\":\"landscape\"," +
            "\"size\":{\"w\":{\"type\":\"number\",\"value\":320},\"h\":{\"type\":\"number\",\"value\":240}}," +
            "\"images\":[]," +
            "\"buttons\":[{\"name\":\"b\",\"visible_if\":\"show_button?boolean\"," +
            "\"x\":{\"type\":\"number\",\"value\":0},\"y\":{\"type\":\"number\",\"value\":0}," +
            "\"w\":{\"type\":\"number\",\"value\":100},\"h\":{\"type\":\"number\",\"value\":50}," +
            "\"image_up\":{\"type\":\"asset\",\"value\":\"1\"}," +
            "\"action\":{\"type\":\"text\",\"value\":\"\"},\"type\":{\"type\":\"text\",\"value\":\"DISMISS\"}}]}]}}";

    // Image text is a FreeMarker conditional that renders empty when the key is absent — should NOT suppress.
    private static final String FREEMARKER_CONDITIONAL_TEXT_JSON =
            "{\"id\":300,\"name\":\"t\",\"template\":{\"formats\":[{\"name\":\"t\",\"orientation\":\"landscape\"," +
            "\"size\":{\"w\":{\"type\":\"number\",\"value\":320},\"h\":{\"type\":\"number\",\"value\":240}}," +
            "\"images\":[{\"name\":\"img\",\"text\":{\"type\":\"text\",\"value\":\"<#if missing??>Sale!</#if>\"}," +
            "\"x\":{\"type\":\"number\",\"value\":0},\"y\":{\"type\":\"number\",\"value\":0}," +
            "\"w\":{\"type\":\"number\",\"value\":100},\"h\":{\"type\":\"number\",\"value\":50}}]," +
            "\"buttons\":[]}]}}";

    private static final String MALFORMED_VISIBLE_IF_MESSAGE_JSON =
            "{\"id\":300,\"name\":\"t\",\"template\":{\"formats\":[{\"name\":\"t\",\"orientation\":\"landscape\"," +
            "\"size\":{\"w\":{\"type\":\"number\",\"value\":320},\"h\":{\"type\":\"number\",\"value\":240}}," +
            "\"images\":[]," +
            "\"buttons\":[{\"name\":\"b\",\"visible_if\":\"[[[ INVALID FREEMARKER\"," +
            "\"x\":{\"type\":\"number\",\"value\":0},\"y\":{\"type\":\"number\",\"value\":0}," +
            "\"w\":{\"type\":\"number\",\"value\":100},\"h\":{\"type\":\"number\",\"value\":50}," +
            "\"image_up\":{\"type\":\"asset\",\"value\":\"1\"}," +
            "\"action\":{\"type\":\"text\",\"value\":\"\"},\"type\":{\"type\":\"text\",\"value\":\"DISMISS\"}}]}]}}";

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
    public void testFreemarkerEmptyRenderDoesNotSuppressCampaign() throws Exception {
        SwrveInAppCampaign mockCampaign = Mockito.mock(SwrveInAppCampaign.class);
        Mockito.when(mockCampaign.isFreemarkerEnabled()).thenReturn(true);
        SwrveMessage message = new SwrveMessage(mockCampaign, new JSONObject(FREEMARKER_CONDITIONAL_TEXT_JSON), null);
        // Key absent → conditional renders empty — FreeMarker campaigns must not suppress on empty output.
        assertTrue(SwrveMessageTextTemplatingChecks.checkTextTemplating(message, new HashMap<>()));
    }

    @Test
    public void testVisibleIfValidExpressionPassesCheck() throws Exception {
        SwrveInAppCampaign mockCampaign = Mockito.mock(SwrveInAppCampaign.class);
        SwrveMessage message = new SwrveMessage(mockCampaign, new JSONObject(VALID_VISIBLE_IF_MESSAGE_JSON), null);
        Map<String, String> props = new HashMap<>();
        props.put("show_button", "true");
        assertTrue(SwrveMessageTextTemplatingChecks.checkTextTemplating(message, props));
    }

    @Test
    public void testVisibleIfMalformedExpressionFailsCheck() throws Exception {
        SwrveInAppCampaign mockCampaign = Mockito.mock(SwrveInAppCampaign.class);
        SwrveMessage message = new SwrveMessage(mockCampaign, new JSONObject(MALFORMED_VISIBLE_IF_MESSAGE_JSON), null);
        assertFalse(SwrveMessageTextTemplatingChecks.checkTextTemplating(message, new HashMap<>()));
    }

    @Test
    public void testVisibleIfMalformedExpressionThrowsCorrectReason() throws Exception {
        SwrveInAppCampaign mockCampaign = Mockito.mock(SwrveInAppCampaign.class);
        SwrveMessage message = new SwrveMessage(mockCampaign, new JSONObject(MALFORMED_VISIBLE_IF_MESSAGE_JSON), null);
        SwrveSDKTextTemplatingException ex = assertThrows(SwrveSDKTextTemplatingException.class,
                () -> SwrveMessageTextTemplatingChecks.checkTextTemplatingOrThrow(message, new HashMap<>()));
        assertEquals("visible_if condition could not be evaluated", ex.getMessage());
    }
}
