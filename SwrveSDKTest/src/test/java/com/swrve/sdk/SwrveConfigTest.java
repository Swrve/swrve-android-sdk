package com.swrve.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveEmbeddedMessageConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.config.SwrveStack;
import com.swrve.sdk.messaging.SwrveClipboardButtonListener;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveDismissButtonListener;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedMessageListener;
import com.swrve.sdk.messaging.SwrveInAppWindowListener;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SwrveConfigTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

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
        assertEquals(Color.argb(100, 0, 190, 152), inAppConfig.getFocusColor());
        assertEquals(Color.BLACK, inAppConfig.getPersonalisedTextForegroundColor());
        assertEquals(Color.TRANSPARENT, inAppConfig.getPersonalisedTextBackgroundColor());
        assertNull(inAppConfig.getPersonalisedTextTypeface());
        assertTrue(inAppConfig.isHideToolbar());
        assertEquals(inAppConfig.getAutoShowMessagesMaxDelay(), 5000);
        assertNull(inAppConfig.getWindowListener());
    }

    @Test
    public void testSwrveInAppMessageConfig() {
        SwrveConfig config = new SwrveConfig();
        SwrveInAppWindowListener inAppWindowListener = window -> { };
        SwrveInAppMessageConfig.Builder builder = new SwrveInAppMessageConfig.Builder()
                .defaultBackgroundColor(Color.BLACK)
                .clickColor(Color.RED)
                .focusColor(Color.BLUE)
                .personalisedTextForegroundColor(Color.YELLOW)
                .personalisedTextBackgroundColor(Color.GREEN)
                .personalisedTextTypeface(Typeface.MONOSPACE)
                .hideToolbar(false)
                .autoShowMessagesMaxDelay(55)
                .windowListener(inAppWindowListener);

        config.setInAppMessageConfig(builder.build());
        SwrveInAppMessageConfig inAppConfig = config.getInAppMessageConfig();

        assertEquals(Color.BLACK, inAppConfig.getDefaultBackgroundColor());
        assertEquals(Color.RED, inAppConfig.getClickColor());
        assertEquals(Color.BLUE, inAppConfig.getFocusColor());
        assertEquals(Color.YELLOW, inAppConfig.getPersonalisedTextForegroundColor());
        assertEquals(Color.GREEN, inAppConfig.getPersonalisedTextBackgroundColor());
        assertEquals(Typeface.MONOSPACE, inAppConfig.getPersonalisedTextTypeface());
        assertFalse("hideToolbar should be set to 'false'" ,inAppConfig.isHideToolbar());
        assertEquals(inAppConfig.getAutoShowMessagesMaxDelay(), 55);
        assertEquals(inAppConfig.getWindowListener(), inAppWindowListener);
    }

    @Test
    public void testButtonSetters() {
        CustomButtonListener customListener = new CustomButtonListener();
        InstallButtonListener installListener = new InstallButtonListener();
        DismissButtonListener dismissListener = new DismissButtonListener();
        ClipboardButtonListener clipboardListener = new ClipboardButtonListener();

        SwrveConfig config = new SwrveConfig();
        SwrveInAppMessageConfig.Builder builder = new SwrveInAppMessageConfig.Builder()
                .customButtonListener(customListener)
                .installButtonListener(installListener)
                .dismissButtonListener(dismissListener)
                .clipboardButtonListener(clipboardListener);

        config.setInAppMessageConfig(builder.build());
        SwrveInAppMessageConfig inAppConfig = config.getInAppMessageConfig();
        assertEquals(customListener, inAppConfig.getCustomButtonListener());
        assertEquals(installListener, inAppConfig.getInstallButtonListener());
        assertEquals(dismissListener, inAppConfig.getDismissButtonListener());
        assertEquals(clipboardListener, inAppConfig.getClipboardButtonListener());

    }

    @Test
    public void testSwrveEmbeddedMessageConfig() {
        EmbedddedButtonListener embedddedButtonListener = new EmbedddedButtonListener();
        SwrveConfig config = new SwrveConfig();
        SwrveEmbeddedMessageConfig.Builder builder = new SwrveEmbeddedMessageConfig.Builder()
                .embeddedMessageListener(embedddedButtonListener);

        config.setEmbeddedMessageConfig(builder.build());
        SwrveEmbeddedMessageConfig embeddedMessageConfig = config.getEmbeddedMessageConfig();
        assertEquals(embedddedButtonListener, embeddedMessageConfig.getEmbeddedMessageListener());
    }

    private class CustomButtonListener implements SwrveCustomButtonListener {
        @Override
        public void onAction(String customAction) {
        }
    }

    private class InstallButtonListener implements SwrveInstallButtonListener {
        @Override
        public boolean onAction(String appStoreLink) {
            return false;
        }
    }

    private class DismissButtonListener implements SwrveDismissButtonListener {
        @Override
        public void onAction(String campaignSubject, String buttonName) {
        }
    }

    private class ClipboardButtonListener implements SwrveClipboardButtonListener {

        @Override
        public void onAction(String clipboardContents) {
        }
    }

    private class EmbedddedButtonListener implements SwrveEmbeddedMessageListener {

        @Override
        public void onMessage(Context context, SwrveEmbeddedMessage message) {
        }
    }
}
