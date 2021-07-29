package com.swrve.sdk;

import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.ibm.icu.impl.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwrveTextTemplatingTest extends SwrveBaseTest {

    @Test
    public void testTemplating() throws Exception {

        Map<String, String> properties = new HashMap<>();
        properties.put("campaignId", "1");
        properties.put("item.label", "some_label");
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        String text = "Welcome to ${item.label}. And another ${key1}/${key2}";
        String templated = SwrveTextTemplating.apply(text, properties);
        assertEquals("Welcome to some_label. And another value1/value2", templated);

        try {
            text = "THIS SHOULD throw SwrveSDKTextTemplatingException: Welcome to ${item.label}. And another ${key3}/${key4}"; // these values aren't in the customFields
            templated = SwrveTextTemplating.apply(text, properties);
            assertEquals("", templated);
            fail("This line should not be reached. An exception should have been thrown.");
        } catch (SwrveSDKTextTemplatingException e) {
            // empty
        } catch (Exception e) {
            fail("This line should not be reached. An exception should have been thrown." + e.getMessage());
        }

        text = "http://someurl.com/${item.label}/key1=${key1}&blah=${key2}&key1=${key1}";
        templated = SwrveTextTemplating.apply(text, properties);
        assertEquals("http://someurl.com/some_label/key1=value1&blah=value2&key1=value1", templated);

        try { // test with null property
            properties = new HashMap<>();
            properties.put("campaignId", "1");
            properties.put("itemlabel", "somelabel");
            properties.put("customFields", null);
            text = "THIS SHOULD throw SwrveSDKTextTemplatingException: And another ${missing}"; // these values aren't in the customFields
            templated = SwrveTextTemplating.apply(text, properties);
            assertEquals("", templated);
            fail("This line should not be reached. An exception should have been thrown.");
        } catch (SwrveSDKTextTemplatingException e) {
            // empty
        } catch (Exception e) {
            fail("This line should not be reached. An exception should have been thrown." + e.getMessage());
        }
    }

    @Test
    public void testTemplatingWithFallback() throws Exception {

        Map<String, String> properties = new HashMap<>();
        properties.put("campaignId", "1");
        properties.put("item.label", "some_label");
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        String text = "Welcome to ${item.label}. And another ${key1}/${key2} ${item.label|fallback=\"fallback property\"}";
        String templated = SwrveTextTemplating.apply(text, properties);
        assertEquals("Welcome to some_label. And another value1/value2 some_label", templated);

        properties = new HashMap<>();
        properties.put("campaignId", "1");
        properties.put("item.label", "");
        properties.put("key1", "value1");
        properties.put("key2", "value2");
        text = "Welcome to ${item.label|fallback=\"hello\"}. And another ${key1}/${key2}/${key3|fallback=\"ola\"} ${item.label|fallback=\"bye\"}";
        templated = SwrveTextTemplating.apply(text, properties);
        assertEquals("Welcome to hello. And another value1/value2/ola bye", templated);

        properties = new HashMap<>();
        properties.put("campaignId", "1");
        properties.put("item.label", "");
        text = "http://www.deeplink.com/param1=${param1|fallback=\"1\"}&param2=${param2|fallback=\"2\"}";
        templated = SwrveTextTemplating.apply(text, properties);
        assertEquals("http://www.deeplink.com/param1=1&param2=2", templated);
    }

    @Test
    public void testTemplatingOnJSON() throws Exception {

        Map<String, String> properties = new HashMap<>();
        properties.put("campaignId", "1");
        properties.put("item.label", "swrve");
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        String text = "{\"${item.label}\": \"${key1}/${key2}\", \"keys\": \"${key1}/${key2}\"}";
        String templated = SwrveTextTemplating.applytoJSON(text, properties);
        assertEquals("{\"swrve\": \"value1/value2\", \"keys\": \"value1/value2\"}", templated);

        try {
            text = "{\"${item.label}\": \"${key1}/${key2}\", \"keys\": \"${key3}/${key4}\"}"; // these values aren't in the customFields
            templated = SwrveTextTemplating.applytoJSON(text, properties);
            assertEquals("", templated);
            fail("This line should not be reached. An exception should have been thrown.");
        } catch (SwrveSDKTextTemplatingException e) {
            // empty
        } catch (Exception e) {
            fail("This line should not be reached. An exception should have been thrown." + e.getMessage());
        }
    }

    @Test
    public void testTemplatingOnJSONWithFallback() throws Exception {

        Map<String, String> properties = new HashMap<>();
        String text = "{\"key\":\"${user.firstname|fallback=\\\"working\\\"}\"}";
        String templated = SwrveTextTemplating.applytoJSON(text, properties);
        assertEquals("{\"key\":\"working\"}", templated);

        properties = new HashMap<>();
        text = "{\"${user.firstname|fallback=\\\"key\\\"}\":\"${user.firstname|fallback=\\\"working\\\"}\"}";
        templated = SwrveTextTemplating.applytoJSON(text, properties);
        assertEquals("{\"key\":\"working\"}", templated);
    }

    @Test
    public void testTemplatingHasPatternMatch() {
        String hasPattern = "Welcome to ${item.label}. And another ${key1}/${key2}";
        assertTrue("text should be recognised to have pattern in it", SwrveTextTemplating.hasPatternMatch(hasPattern));

        String hasNotPattern = "Welcome to $$$$$$$$P{";
        assertFalse("text should not have a pattern found in it", SwrveTextTemplating.hasPatternMatch(hasNotPattern));

        String plainText = "plain ol text";
        assertFalse("plain text is still falase", SwrveTextTemplating.hasPatternMatch(plainText));
    }

    @Test
    public void testFallbackWorksWithoutProperties() throws SwrveSDKTextTemplatingException {
        String text = "Welcome to ${item.label|fallback=\"fallback property\"}";

        String templated = SwrveTextTemplating.apply(text, new HashMap<>());
        assertEquals("Welcome to fallback property", templated);

        templated = SwrveTextTemplating.apply(text, null);
        assertEquals("Welcome to fallback property", templated);
    }
}
