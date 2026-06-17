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
        var properties: MutableMap<String, String> = HashMap()
        properties["campaignId"] = "1"
        properties["item.label"] = "some_label"
        properties["key1"] = "value1"
        properties["key2"] = "value2"

        var text = "Welcome to \${item.label}. And another \${key1}/\${key2}"
        var templated = SwrveTextTemplating.apply(text, properties, false)
        assertEquals("Welcome to some_label. And another value1/value2", templated)

        try {
            text =
                "THIS SHOULD throw SwrveSDKTextTemplatingException: Welcome to \${item.label}. And another \${key3}/\${key4}"
            templated = SwrveTextTemplating.apply(text, properties, false)
            assertEquals("", templated)
            fail("This line should not be reached. An exception should have been thrown.")
        } catch (e: SwrveSDKTextTemplatingException) {
            // expected exception
        } catch (e: Exception) {
            fail("This line should not be reached. An exception should have been thrown. " + e.message)
        }

        text = "http://someurl.com/\${item.label}/key1=\${key1}&blah=\${key2}&key1=\${key1}"
        templated = SwrveTextTemplating.apply(text, properties, false)
        assertEquals("http://someurl.com/some_label/key1=value1&blah=value2&key1=value1", templated)

        // Test with missing property (no customFields entry)
        properties = HashMap()
        properties["campaignId"] = "1"
        properties["itemlabel"] = "somelabel"

        try {
            text = "THIS SHOULD throw SwrveSDKTextTemplatingException: And another \${missing}"
            templated = SwrveTextTemplating.apply(text, properties, false)
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
        var properties: MutableMap<String, String> = HashMap()
        properties["campaignId"] = "1"
        properties["item.label"] = "some_label"
        properties["key1"] = "value1"
        properties["key2"] = "value2"

        var text =
            "Welcome to \${item.label}. And another \${key1}/\${key2} \${item.label|fallback=\"fallback property\"}"
        var templated = SwrveTextTemplating.apply(text, properties, false)
        assertEquals("Welcome to some_label. And another value1/value2 some_label", templated)

        properties = HashMap()
        properties["campaignId"] = "1"
        properties["item.label"] = ""
        properties["key1"] = "value1"
        properties["key2"] = "value2"

        text =
            "Welcome to \${item.label|fallback=\"hello\"}. And another \${key1}/\${key2}/\${key3|fallback=\"ola\"} \${item.label|fallback=\"bye\"}"
        templated = SwrveTextTemplating.apply(text, properties, false)
        assertEquals("Welcome to hello. And another value1/value2/ola bye", templated)

        properties = HashMap()
        properties["campaignId"] = "1"
        properties["item.label"] = ""

        text =
            "http://www.deeplink.com/param1=\${param1|fallback=\"1\"}&param2=\${param2|fallback=\"2\"}"
        templated = SwrveTextTemplating.apply(text, properties, false)
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
        var templated = SwrveTextTemplating.applytoJSON(text, properties, false)
        assertEquals("{\"swrve\": \"value1/value2\", \"keys\": \"value1/value2\"}", templated)

        try {
            text = "{\"\${item.label}\": \"\${key1}/\${key2}\", \"keys\": \"\${key3}/\${key4}\"}"
            templated = SwrveTextTemplating.applytoJSON(text, properties, false)
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
        var properties: Map<String, String> = HashMap()
        var text = "{\"key\":\"\${user.firstname|fallback=\\\"working\\\"}\"}"
        var templated = SwrveTextTemplating.applytoJSON(text, properties, false)
        assertEquals("{\"key\":\"working\"}", templated)

        properties = HashMap()
        text =
            "{\"\${user.firstname|fallback=\\\"key\\\"}\":\"\${user.firstname|fallback=\\\"working\\\"}\"}"
        templated = SwrveTextTemplating.applytoJSON(text, properties, false)
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
    @Throws(Exception::class)
    fun testApplyFreemarkerEnabled() {
        val properties = mapOf("user.name" to "Alice")
        val text = "Hello \${user.name}"
        val result = SwrveTextTemplating.apply(text, properties, freemarkerEnabled = true)
        assertEquals("Hello Alice", result)
    }

    @Test
    @Throws(Exception::class)
    fun testApplyFreemarkerEnabledThrowsOnMissingProperty() {
        val properties = emptyMap<String, String>()
        val text = "Hello \${user.name}"
        try {
            SwrveTextTemplating.apply(text, properties, freemarkerEnabled = true)
            fail("Expected SwrveSDKTextTemplatingException")
        } catch (e: SwrveSDKTextTemplatingException) {
            // expected
        }
    }

    @Test
    @Throws(Exception::class)
    fun testApplyToJSONFreemarkerEnabled() {
        val properties = mapOf("key1" to "value1", "key2" to "value2")
        val json = "{\"a\": \"\${key1}\", \"b\": \"\${key2}\"}"
        val result = SwrveTextTemplating.applytoJSON(json, properties, freemarkerEnabled = true)
        val resultJson = org.json.JSONObject(result!!)
        assertEquals("value1", resultJson.getString("a"))
        assertEquals("value2", resultJson.getString("b"))
    }

    @Test
    @Throws(Exception::class)
    fun testApplyToJSONFreemarkerEnabledWithTemplatedKey() {
        val properties = mapOf("key_name" to "myKey", "key_value" to "myValue")
        val json = "{\"\${key_name}\": \"\${key_value}\"}"
        val result = SwrveTextTemplating.applytoJSON(json, properties, freemarkerEnabled = true)
        val resultJson = org.json.JSONObject(result!!)
        assertEquals("myValue", resultJson.getString("myKey"))
    }

    @Test
    @Throws(Exception::class)
    fun testApplyToJSONFreemarkerEnabledThrowsOnMissingProperty() {
        val properties = emptyMap<String, String>()
        val json = "{\"a\": \"\${key1}\"}"
        try {
            SwrveTextTemplating.applytoJSON(json, properties, freemarkerEnabled = true)
            fail("Expected SwrveSDKTextTemplatingException")
        } catch (e: SwrveSDKTextTemplatingException) {
            // expected
        }
    }

    @Test
    @Throws(Exception::class)
    fun testApplyToJSONFreemarkerEnabledWithDoubleQuotedStringLiteral() {
        // Simulates the server payload: the data field value contains \"gold\" (one level of JSON unescaping
        // from the server's double-escaping). The parse-first approach unescapes this a second time before
        // passing to FreeMarker, so the engine sees "gold" and evaluates correctly.
        val properties = mapOf("Recipient.tier" to "gold")
        val json = "{\"tier_message\": \"<#if Recipient.tier == \\\"gold\\\">Gold Member<#else>Standard Member</#if>\"}"
        val result = SwrveTextTemplating.applytoJSON(json, properties, freemarkerEnabled = true)
        val resultJson = org.json.JSONObject(result!!)
        assertEquals("Gold Member", resultJson.getString("tier_message"))
    }

    @Test
    @Throws(Exception::class)
    fun testApplyToJSONFreemarkerEnabledWithDoubleQuotedStringLiteralElseBranch() {
        val properties = mapOf("Recipient.tier" to "bronze")
        val json = "{\"tier_message\": \"<#if Recipient.tier == \\\"gold\\\">Gold Member<#else>Standard Member</#if>\"}"
        val result = SwrveTextTemplating.applytoJSON(json, properties, freemarkerEnabled = true)
        val resultJson = org.json.JSONObject(result!!)
        assertEquals("Standard Member", resultJson.getString("tier_message"))
    }

    @Test
    @Throws(Exception::class)
    fun testApplyFreemarkerEnabledOtherTypeWithStructuralConditional() {
        // "other" type passes the raw string to FreeMarker, allowing structural JSON manipulation
        // such as conditionally including entire keys — not possible with "json" type parse-first approach.
        val propertiesPremium = mapOf("Recipient.first_name" to "Alice", "Recipient.isPremium" to "true")
        val template = """{"name": "${"\$"}{Recipient.first_name}"<#if Recipient.isPremium == "true">,"premium_feature": true</#if>}"""
        val resultPremium = SwrveTextTemplating.apply(template, propertiesPremium, freemarkerEnabled = true)
        val resultPremiumJson = org.json.JSONObject(resultPremium!!)
        assertEquals("Alice", resultPremiumJson.getString("name"))
        assertTrue(resultPremiumJson.getBoolean("premium_feature"))

        val propertiesStandard = mapOf("Recipient.first_name" to "Bob", "Recipient.isPremium" to "false")
        val resultStandard = SwrveTextTemplating.apply(template, propertiesStandard, freemarkerEnabled = true)
        val resultStandardJson = org.json.JSONObject(resultStandard!!)
        assertEquals("Bob", resultStandardJson.getString("name"))
        assertFalse(resultStandardJson.has("premium_feature"))
    }

    @Test
    @Throws(Exception::class)
    fun testApplyToJSONFreemarkerEnabledWithNestedObject() {
        val properties = mapOf("Recipient.first_name" to "Alice", "Recipient.tier" to "gold")
        val json = "{\"user\": {\"name\": \"\${Recipient.first_name}\", \"tier\": \"\${Recipient.tier}\"}, \"message\": \"Hello \${Recipient.first_name}\"}"
        val result = SwrveTextTemplating.applytoJSON(json, properties, freemarkerEnabled = true)
        val resultJson = org.json.JSONObject(result!!)
        val userJson = resultJson.getJSONObject("user")
        assertEquals("Alice", userJson.getString("name"))
        assertEquals("gold", userJson.getString("tier"))
        assertEquals("Hello Alice", resultJson.getString("message"))
    }

    @Test
    @Throws(Exception::class)
    fun testApplyToJSONFreemarkerEnabledWithArray() {
        val properties = mapOf("Recipient.first_name" to "Alice", "Recipient.tier" to "gold")
        val json = "{\"tags\": [\"\${Recipient.first_name}\", \"\${Recipient.tier}\"]}"
        val result = SwrveTextTemplating.applytoJSON(json, properties, freemarkerEnabled = true)
        val resultJson = org.json.JSONObject(result!!)
        val tagsArray = resultJson.getJSONArray("tags")
        assertEquals("Alice", tagsArray.getString(0))
        assertEquals("gold", tagsArray.getString(1))
    }

    @Test
    @Throws(Exception::class)
    fun testApplyToJSONFreemarkerEnabledWithTopLevelArray() {
        val properties = mapOf("Recipient.first_name" to "Alice", "Recipient.tier" to "gold")
        val json = "[\"\${Recipient.first_name}\", \"\${Recipient.tier}\"]"
        val result = SwrveTextTemplating.applytoJSON(json, properties, freemarkerEnabled = true)
        val resultArray = org.json.JSONArray(result!!)
        assertEquals("Alice", resultArray.getString(0))
        assertEquals("gold", resultArray.getString(1))
    }

    @Test
    @Throws(SwrveSDKTextTemplatingException::class)
    fun testFallbackWorksWithoutProperties() {
        val text = "Welcome to \${item.label|fallback=\"fallback property\"}"

        var templated = SwrveTextTemplating.apply(text, HashMap(), false)
        assertEquals("Welcome to fallback property", templated)

        templated = SwrveTextTemplating.apply(text, null, false)
        assertEquals("Welcome to fallback property", templated)
    }
}