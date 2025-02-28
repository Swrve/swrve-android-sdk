package com.swrve.sdk

import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test


class SwrveTextTemplatingTest : SwrveBaseTest() {

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @After
    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
    }

    @Test
    @Throws(Exception::class)
    fun testTemplating() {
        var properties: MutableMap<String?, String?> = HashMap()
        properties["campaignId"] = "1"
        properties["item.label"] = "some_label"
        properties["key1"] = "value1"
        properties["key2"] = "value2"

        var text = "Welcome to \${item.label}. And another \${key1}/\${key2}"
        var templated = SwrveTextTemplating.apply(text, properties)
        assertEquals("Welcome to some_label. And another value1/value2", templated)

        try {
            text =
                "THIS SHOULD throw SwrveSDKTextTemplatingException: Welcome to \${item.label}. And another \${key3}/\${key4}"
            templated = SwrveTextTemplating.apply(text, properties)
            assertEquals("", templated)
            fail("This line should not be reached. An exception should have been thrown.")
        } catch (e: SwrveSDKTextTemplatingException) {
            // expected exception
        } catch (e: Exception) {
            fail("This line should not be reached. An exception should have been thrown. " + e.message)
        }

        text = "http://someurl.com/\${item.label}/key1=\${key1}&blah=\${key2}&key1=\${key1}"
        templated = SwrveTextTemplating.apply(text, properties)
        assertEquals("http://someurl.com/some_label/key1=value1&blah=value2&key1=value1", templated)

        // Test with null property
        properties = HashMap()
        properties["campaignId"] = "1"
        properties["itemlabel"] = "somelabel"
        properties["customFields"] = null

        try {
            text = "THIS SHOULD throw SwrveSDKTextTemplatingException: And another \${missing}"
            templated = SwrveTextTemplating.apply(text, properties)
            assertEquals("", templated)
            fail("This line should not be reached. An exception should have been thrown.")
        } catch (e: SwrveSDKTextTemplatingException) {
            // expected exception
        } catch (e: Exception) {
            fail("This line should not be reached. An exception should have been thrown. " + e.message)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testTemplatingWithFallback() {
        var properties: MutableMap<String?, String?> = HashMap()
        properties["campaignId"] = "1"
        properties["item.label"] = "some_label"
        properties["key1"] = "value1"
        properties["key2"] = "value2"

        var text =
            "Welcome to \${item.label}. And another \${key1}/\${key2} \${item.label|fallback=\"fallback property\"}"
        var templated = SwrveTextTemplating.apply(text, properties)
        assertEquals("Welcome to some_label. And another value1/value2 some_label", templated)

        properties = HashMap()
        properties["campaignId"] = "1"
        properties["item.label"] = ""
        properties["key1"] = "value1"
        properties["key2"] = "value2"

        text =
            "Welcome to \${item.label|fallback=\"hello\"}. And another \${key1}/\${key2}/\${key3|fallback=\"ola\"} \${item.label|fallback=\"bye\"}"
        templated = SwrveTextTemplating.apply(text, properties)
        assertEquals("Welcome to hello. And another value1/value2/ola bye", templated)

        properties = HashMap()
        properties["campaignId"] = "1"
        properties["item.label"] = ""

        text =
            "http://www.deeplink.com/param1=\${param1|fallback=\"1\"}&param2=\${param2|fallback=\"2\"}"
        templated = SwrveTextTemplating.apply(text, properties)
        assertEquals("http://www.deeplink.com/param1=1&param2=2", templated)
    }

    @Test
    @Throws(Exception::class)
    fun testTemplatingOnJSON() {
        val properties: MutableMap<String, String> = HashMap()
        properties["campaignId"] = "1"
        properties["item.label"] = "swrve"
        properties["key1"] = "value1"
        properties["key2"] = "value2"

        var text = "{\"\${item.label}\": \"\${key1}/\${key2}\", \"keys\": \"\${key1}/\${key2}\"}"
        var templated = SwrveTextTemplating.applytoJSON(text, properties)
        assertEquals("{\"swrve\": \"value1/value2\", \"keys\": \"value1/value2\"}", templated)

        try {
            text = "{\"\${item.label}\": \"\${key1}/\${key2}\", \"keys\": \"\${key3}/\${key4}\"}"
            templated = SwrveTextTemplating.applytoJSON(text, properties)
            assertEquals("", templated)
            fail("This line should not be reached. An exception should have been thrown.")
        } catch (e: SwrveSDKTextTemplatingException) {
            // expected exception
        } catch (e: Exception) {
            fail("This line should not be reached. An exception should have been thrown. " + e.message)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testTemplatingOnJSONWithFallback() {
        var properties: Map<String?, String?> = HashMap()
        var text = "{\"key\":\"\${user.firstname|fallback=\\\"working\\\"}\"}"
        var templated = SwrveTextTemplating.applytoJSON(text, properties)
        assertEquals("{\"key\":\"working\"}", templated)

        properties = HashMap()
        text =
            "{\"\${user.firstname|fallback=\\\"key\\\"}\":\"\${user.firstname|fallback=\\\"working\\\"}\"}"
        templated = SwrveTextTemplating.applytoJSON(text, properties)
        assertEquals("{\"key\":\"working\"}", templated)
    }

    @Test
    fun testTemplatingHasPatternMatch() {
        val hasPattern = "Welcome to \${item.label}. And another \${key1}/\${key2}"
        assertTrue(
            "text should be recognised to have pattern in it",
            SwrveTextTemplating.hasPatternMatch(hasPattern)
        )

        val hasNotPattern = "Welcome to $$$$$$$\$P{"
        assertFalse(
            "text should not have a pattern found in it",
            SwrveTextTemplating.hasPatternMatch(hasNotPattern)
        )

        val plainText = "plain ol text"
        assertFalse("plain text is still false", SwrveTextTemplating.hasPatternMatch(plainText))
    }

    @Test
    @Throws(SwrveSDKTextTemplatingException::class)
    fun testFallbackWorksWithoutProperties() {
        val text = "Welcome to \${item.label|fallback=\"fallback property\"}"

        var templated = SwrveTextTemplating.apply(text, HashMap())
        assertEquals("Welcome to fallback property", templated)

        templated = SwrveTextTemplating.apply(text, null)
        assertEquals("Welcome to fallback property", templated)
    }
}