package com.swrve.sdk;

import android.app.NotificationManager;

import com.google.gson.JsonSyntaxException;
import com.swrve.sdk.notifications.model.SwrveNotification;
import com.swrve.sdk.notifications.model.SwrveNotificationButton;
import com.swrve.sdk.notifications.model.SwrveNotificationChannel;
import com.swrve.sdk.notifications.model.SwrveNotificationMedia;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SwrveNotificationTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testSwrveNotificationHappyCase() {
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"icon_url\": \"https://icon.png\",\n" +
                " \"version\": 1,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://media.jpg\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://fallback.jpg\",\n" +
                "              \"fallback_sd\": \"https://video.com\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://expanded_icon.png\"}, \n" +
                "\"buttons\": [{\"title\": \"[button text 1]\",\n" +
                "                 \"action_type\": \"open_url\",\n" +
                "                 \"action\": \"https://lovelyURL\"},\n" +
                "                {\"title\": \"[button text 2]\",\n" +
                "                 \"action_type\": \"open_app\"},\n" +
                "                {\"title\": \"[button text 3]\",\n" +
                "                 \"action_type\": \"dismiss\"}]\n" +
                "}\n";

        SwrveNotification payload = SwrveNotification.fromJson(json);
        assertNotNull(payload);
        assertEquals("title", payload.getTitle());
        assertEquals("subtitle", payload.getSubtitle());
        assertEquals("https://icon.png", payload.getIconUrl());

        assertNotNull(payload.getMedia());
        assertEquals("[rich title]", payload.getMedia().getTitle());
        assertEquals("[rich body]", payload.getMedia().getBody());
        assertEquals(SwrveNotificationMedia.MediaType.IMAGE, payload.getMedia().getType());
        assertEquals("https://media.jpg", payload.getMedia().getUrl());
        assertEquals(SwrveNotificationMedia.MediaType.IMAGE, payload.getMedia().getFallbackType());
        assertEquals("https://fallback.jpg", payload.getMedia().getFallbackUrl());
        assertEquals("https://video.com", payload.getMedia().getFallbackSd());

        assertNotNull(payload.getExpanded());
        assertEquals("[expanded title]", payload.getExpanded().getTitle());
        assertEquals("[expanded body]", payload.getExpanded().getBody());
        assertEquals("https://expanded_icon.png", payload.getExpanded().getIconUrl());

        assertNotNull(payload.getButtons());
        assertEquals("[button text 1]", payload.getButtons().get(0).getTitle());
        assertEquals("[button text 2]", payload.getButtons().get(1).getTitle());
        assertEquals("[button text 3]", payload.getButtons().get(2).getTitle());
        assertEquals(SwrveNotificationButton.ActionType.OPEN_URL, payload.getButtons().get(0).getActionType());
        assertEquals(SwrveNotificationButton.ActionType.OPEN_APP, payload.getButtons().get(1).getActionType());
        assertEquals(SwrveNotificationButton.ActionType.DISMISS, payload.getButtons().get(2).getActionType());
        assertEquals("https://lovelyURL", payload.getButtons().get(0).getAction());
    }

    @Test
    public void testSwrveNotificationOnlyExtended() {
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"icon_url\": \"https://icon.png\",\n" +
                " \"version\": 1,\n" +
                " \"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                 \"body\": \"[expanded body]\",\n" +
                "                 \"icon_url\": \"https://expanded_icon.png\"}\n" +
                "}\n";

        SwrveNotification payload = SwrveNotification.fromJson(json);
        assertNotNull(payload);
        assertEquals("title", payload.getTitle());
        assertEquals("subtitle", payload.getSubtitle());
        assertEquals("https://icon.png", payload.getIconUrl());

        assertNull(payload.getMedia());

        assertNotNull(payload.getExpanded());
        assertEquals("[expanded title]", payload.getExpanded().getTitle());
        assertEquals("[expanded body]", payload.getExpanded().getBody());
        assertEquals("https://expanded_icon.png", payload.getExpanded().getIconUrl());

        assertNull(payload.getButtons());
    }

    @Test
    public void testSwrveNotificationMediaOnly() {
        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"icon_url\": \"https://icon.png\",\n" +
                " \"version\": 1,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://media.jpg\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://fallback.jpg\",\n" +
                "              \"fallback_sd\": \"https://video.com\"\n }" +
                "}\n";

        SwrveNotification payload = SwrveNotification.fromJson(json);
        assertNotNull(payload);
        assertEquals("title", payload.getTitle());
        assertEquals("subtitle", payload.getSubtitle());
        assertEquals("https://icon.png", payload.getIconUrl());

        assertNotNull(payload.getMedia());
        assertEquals("[rich title]", payload.getMedia().getTitle());
        assertEquals("[rich body]", payload.getMedia().getBody());
        assertEquals(SwrveNotificationMedia.MediaType.IMAGE, payload.getMedia().getType());
        assertEquals("https://media.jpg", payload.getMedia().getUrl());
        assertEquals(SwrveNotificationMedia.MediaType.IMAGE, payload.getMedia().getFallbackType());
        assertEquals("https://fallback.jpg", payload.getMedia().getFallbackUrl());
        assertEquals("https://video.com", payload.getMedia().getFallbackSd());

        assertNull(payload.getExpanded());
        assertNull(payload.getButtons());
    }

    @Test
    public void testSwrveNotificationNoButtons() {

        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"icon_url\": \"https://icon.png\",\n" +
                " \"version\": 1,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://media.jpg\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://fallback.jpg\",\n" +
                "              \"fallback_sd\": \"https://video.com\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://expanded_icon.png\"} \n" +
                "}\n";
        SwrveNotification payload = SwrveNotification.fromJson(json);
        assertNotNull(payload);
        assertEquals("title", payload.getTitle());
        assertEquals("subtitle", payload.getSubtitle());
        assertEquals("https://icon.png", payload.getIconUrl());

        assertNotNull(payload.getMedia());
        assertEquals("[rich title]", payload.getMedia().getTitle());
        assertEquals("[rich body]", payload.getMedia().getBody());
        assertEquals(SwrveNotificationMedia.MediaType.IMAGE, payload.getMedia().getType());
        assertEquals("https://media.jpg", payload.getMedia().getUrl());
        assertEquals(SwrveNotificationMedia.MediaType.IMAGE, payload.getMedia().getFallbackType());
        assertEquals("https://fallback.jpg", payload.getMedia().getFallbackUrl());
        assertEquals("https://video.com", payload.getMedia().getFallbackSd());

        assertNotNull(payload.getExpanded());
        assertEquals("[expanded title]", payload.getExpanded().getTitle());
        assertEquals("[expanded body]", payload.getExpanded().getBody());
        assertEquals("https://expanded_icon.png", payload.getExpanded().getIconUrl());

        assertNull(payload.getButtons());
    }

    @Test
    public void testSwrveNotificationAdditionalParameters() {

        String json = "{\n" +
                " \"title\": \"title\",\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"ticker\": \"ticker message\",\n" +
                " \"icon_url\": \"https://icon.png\",\n" +
                " \"version\": 1,\n" +
                " \"notification_id\": 222,\n" +
                " \"accent\": \"#00FF0000\",\n" +
                " \"visibility\": \"public\",\n" +
                " \"lock_screen_msg\": \"Lock Screen Message\",\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://media.jpg\"}," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://expanded_icon.png\"}, \n" +
                " \"channel_id\": \"my_notification_channel_id\"\n" +
                "}\n";

        SwrveNotification payload = SwrveNotification.fromJson(json);
        assertNotNull(payload);
        assertEquals(1, payload.getVersion());
        assertEquals("title", payload.getTitle());
        assertEquals("subtitle", payload.getSubtitle());
        assertEquals("ticker message", payload.getTicker());
        assertEquals("https://icon.png", payload.getIconUrl());
        assertEquals(222, payload.getNotificationId());
        assertEquals("#00FF0000", payload.getAccent());
        assertEquals("Lock Screen Message", payload.getLockScreenMsg());
        assertEquals(SwrveNotification.VisibilityType.PUBLIC, payload.getVisibility());

        assertNotNull(payload.getMedia());
        assertEquals("[rich title]", payload.getMedia().getTitle());
        assertEquals("[rich body]", payload.getMedia().getBody());
        assertEquals(SwrveNotificationMedia.MediaType.IMAGE, payload.getMedia().getType());
        assertEquals("https://media.jpg", payload.getMedia().getUrl());

        assertNotNull(payload.getExpanded());
        assertEquals("[expanded title]", payload.getExpanded().getTitle());
        assertEquals("[expanded body]", payload.getExpanded().getBody());
        assertEquals("https://expanded_icon.png", payload.getExpanded().getIconUrl());
        assertEquals("my_notification_channel_id", payload.getChannelId());

        assertNull(payload.getButtons());
    }

    @Test
    public void testSwrveNotificationWrongFormat() throws JsonSyntaxException {
        String json = "{\n" +
                " \"title\": {},\n" +
                " \"subtitle\": \"subtitle\",\n" +
                " \"icon_url\": \"https://icon.png\",\n" +
                " \"version\": 1,\n" +
                " \"media\": { \"title\": \"[rich title]\",\n" +
                "              \"subtitle\": \"[rich subtitle]\",\n" +
                "              \"body\": \"[rich body]\",\n"+
                "              \"type\": \"image\",\n" +
                "              \"url\": \"https://media.jpg\",\n" +
                "              \"fallback_type\": \"image\",\n" +
                "              \"fallback_url\": \"https://fallback.jpg\",\n" +
                "              \"fallback_sd\": \"https://video.com\"\n }," +
                "\"expanded\": { \"title\": \"[expanded title]\",\n" +
                "                \"body\": \"[expanded body]\",\n" +
                "                \"icon_url\": \"https://expanded_icon.png\"} \n" +
                "}\n";
        SwrveNotification payload = SwrveNotification.fromJson(json);
        assertNull(payload);
    }

    @Test
    public void testSwrveNotificationChannel() {
        String[] importanceString = new String[]{"default", "high", "low", "max", "min", "none"};
        int[] importanceInt = new int[]{NotificationManager.IMPORTANCE_DEFAULT, NotificationManager.IMPORTANCE_HIGH, NotificationManager.IMPORTANCE_LOW, NotificationManager.IMPORTANCE_MAX, NotificationManager.IMPORTANCE_MIN, NotificationManager.IMPORTANCE_NONE};

        for (int i = 0; i < importanceString.length; i++) {
            String importanceStringValue = importanceString[i];
            int importanceIntValue = importanceInt[i];

            String json = "{\n" +
                    " \"channel\": { \n" +
                    " \"id\": \"id_value\",\n" +
                    " \"name\": \"name_value\",\n" +
                    " \"importance\": \"" + importanceStringValue + "\"" +
                    "} }\n";

            SwrveNotification payload = SwrveNotification.fromJson(json);
            assertNotNull(payload);
            SwrveNotificationChannel channel = payload.getChannel();
            assertNotNull(channel);

            assertEquals("id_value", channel.getId());
            assertEquals("name_value", channel.getName());
            assertEquals(importanceStringValue, channel.getImportance().toString().toLowerCase());
            assertEquals(importanceIntValue, channel.getAndroidImportance());
        }
    }
}
