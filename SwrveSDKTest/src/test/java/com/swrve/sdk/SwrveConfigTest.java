package com.swrve.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.graphics.Color;
import android.graphics.Typeface;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.config.SwrveStack;
import com.swrve.sdk.messaging.SwrveInAppWindowListener;
import com.swrve.sdk.messaging.SwrveMessageFocusListener;

import org.junit.Test;

import java.net.URL;
import java.util.Locale;

public class SwrveConfigTest extends SwrveBaseTest {

    @Test
    public void testLocaleSetter() {
        SwrveConfig config = new SwrveConfig();
        config.setLanguage(Locale.UK);
        assertEquals("en-GB", config.getLanguage());
    }

    @Test
    public void testDefaultConfig() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.generateUrls(101);
        assertEquals("https://101.api.swrve.com", config.getEventsUrl().toString());
        assertEquals("https://101.content.swrve.com", config.getContentUrl().toString());
    }

    @Test
    public void testEUConfig() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setSelectedStack(SwrveStack.EU);
        config.generateUrls(101);
        assertEquals("https://101.eu-api.swrve.com", config.getEventsUrl().toString());
        assertEquals("https://101.eu-content.swrve.com", config.getContentUrl().toString());
    }

    @Test
    public void testEndpointConfig() throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setEventsUrl(new URL("http://test.event.com"));
        config.setContentUrl(new URL("http://test.content.com"));
        config.generateUrls(101);
        assertEquals("http://test.event.com", config.getEventsUrl().toString());
        assertEquals("http://test.content.com", config.getContentUrl().toString());
    }

    @Test
    public void testUrlDefaults() throws Exception {
        SwrveConfig config;
        config = new SwrveConfig();
        config.generateUrls(101);
        assertEquals("https://101.api.swrve.com", config.getEventsUrl().toString());
        assertEquals("https://101.content.swrve.com", config.getContentUrl().toString());
        assertEquals("https://101.identity.swrve.com", config.getIdentityUrl().toString());
    }

    @Test
    public void testSwrveInAppMessageConfigDefaults() {
        SwrveConfig config = new SwrveConfig();
        SwrveInAppMessageConfig inAppConfig = config.getInAppMessageConfig();

        assertEquals(Color.TRANSPARENT, inAppConfig.getDefaultBackgroundColor());
        assertEquals(Color.argb(100, 0, 0, 0), inAppConfig.getClickColor());
        assertEquals(Color.BLACK, inAppConfig.getPersonalizedTextForegroundColor());
        assertEquals(Color.TRANSPARENT, inAppConfig.getPersonalizedTextBackgroundColor());
        assertNull(inAppConfig.getPersonalizedTextTypeface());
        assertEquals(inAppConfig.getAutoShowMessagesMaxDelay(), 5000);
        assertNull(inAppConfig.getWindowListener());
        assertNull(inAppConfig.getMessageFocusListener());
    }

    @Test
    public void testSwrveInAppMessageConfig() {
        SwrveConfig config = new SwrveConfig();
        SwrveInAppWindowListener inAppWindowListener = window -> { };
        SwrveMessageFocusListener messageFocusListener = (view, gainFocus, direction, previouslyFocusedRect) -> { };
        SwrveInAppMessageConfig.Builder builder = new SwrveInAppMessageConfig.Builder()
                .defaultBackgroundColor(Color.BLACK)
                .clickColor(Color.RED)
                .personalizedTextForegroundColor(Color.YELLOW)
                .personalizedTextBackgroundColor(Color.GREEN)
                .personalizedTextTypeface(Typeface.MONOSPACE)
                .autoShowMessagesMaxDelay(55)
                .windowListener(inAppWindowListener)
                .messageFocusListener(messageFocusListener);

        config.setInAppMessageConfig(builder.build());
        SwrveInAppMessageConfig inAppConfig = config.getInAppMessageConfig();

        assertEquals(Color.BLACK, inAppConfig.getDefaultBackgroundColor());
        assertEquals(Color.RED, inAppConfig.getClickColor());
        assertEquals(Color.YELLOW, inAppConfig.getPersonalizedTextForegroundColor());
        assertEquals(Color.GREEN, inAppConfig.getPersonalizedTextBackgroundColor());
        assertEquals(Typeface.MONOSPACE, inAppConfig.getPersonalizedTextTypeface());
        assertEquals(inAppConfig.getAutoShowMessagesMaxDelay(), 55);
        assertEquals(inAppConfig.getWindowListener(), inAppWindowListener);
        assertEquals(inAppConfig.getMessageFocusListener(), messageFocusListener);
    }
}
