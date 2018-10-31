package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveStack;

import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

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
    public void testAutoShowMessagesMaxDelay() {
        SwrveConfig config = new SwrveConfig();
        assertEquals(config.getAutoShowMessagesMaxDelay(), 5000);
    }
}
