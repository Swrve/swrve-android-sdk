package com.swrve.sdk

import android.content.pm.PackageManager
import android.os.Bundle
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SwrveHelperTest : SwrveBaseTest() {
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
    fun testCombineTwoStringMaps() {
        val map1: MutableMap<String, String> = HashMap()
        map1["key1"] = "replace_me"
        map1["key2"] = "value2"

        val map2: MutableMap<String, String> = HashMap()
        map2["key1"] = "value1"
        map2["key3"] = "value3"

        val expectedMap: MutableMap<String, String> = HashMap()
        expectedMap["key1"] = "value1"
        expectedMap["key2"] = "value2"
        expectedMap["key3"] = "value3"

        var combinedMap = SwrveHelper.combineTwoStringMaps(map1, map2)
        Assert.assertEquals(expectedMap, combinedMap)

        combinedMap = SwrveHelper.combineTwoStringMaps(map1, null)
        Assert.assertEquals(map1, combinedMap)

        combinedMap = SwrveHelper.combineTwoStringMaps(null, map2)
        Assert.assertEquals(map2, combinedMap)

        // make sure it won't crash
        combinedMap = SwrveHelper.combineTwoStringMaps(null, null)
        Assert.assertEquals(null, combinedMap)
    }

    @Test
    @Throws(Exception::class)
    fun testConvertPushPayloadToJSONObject_OneLevelDeep() {
        // Unfortunately the "_s.JsonPayload" is only included when payload is more than one level deep.
        // This test is for when its one level deep
        val bundle = Bundle()
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123")
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, "somefakevalue")
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "456")
        bundle.putString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY, "789")
        bundle.putString("customkey1", "customvalue1")
        bundle.putString("customkey2", "customvalue2")
        bundle.putString("customkey3", "customvalue3")

        val payload = SwrveHelper.convertPayloadToJSONObject(bundle)

        Assert.assertTrue(payload.has(SwrveNotificationConstants.SWRVE_TRACKING_KEY))
        Assert.assertEquals("123", payload.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY))
        Assert.assertTrue(payload.has(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY))
        Assert.assertEquals(
            "somefakevalue",
            payload.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY)
        )
        Assert.assertTrue(payload.has(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY))
        Assert.assertEquals(
            "456",
            payload.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY)
        )
        Assert.assertTrue(payload.has(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY))
        Assert.assertEquals("789", payload.getString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY))

        // payloads
        Assert.assertTrue(payload.has("customkey1"))
        Assert.assertEquals("customvalue1", payload.getString("customkey1"))
        Assert.assertTrue(payload.has("customkey2"))
        Assert.assertEquals("customvalue2", payload.getString("customkey2"))
        Assert.assertTrue(payload.has("customkey3"))
        Assert.assertEquals("customvalue3", payload.getString("customkey3"))
    }

    @Test
    @Throws(Exception::class)
    fun testConvertPushPayloadToJSONObject_TwoLevelDeep() {
        // Unfortunately the "_s.JsonPayload" is only included when payload is more than one level deep.
        // This test is for when its one level deep
        val bundle = Bundle()
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "123")
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, "somefakevalue")
        bundle.putString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY, "456")
        bundle.putString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY, "789")
        bundle.putString("root1", "value1")
        bundle.putString("root2", "value2")

        val twoDeepJson = """
            { 
                "g1":{
                    "key1":"value1",
                    "key2":"value2"
                },
                "g2":{
                    "key3":"value3"
                },
                "root1":"value1",
                "root2":"value2"
            }
        """
        bundle.putString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY, twoDeepJson)

        val payload = SwrveHelper.convertPayloadToJSONObject(bundle)

        Assert.assertTrue(payload.has(SwrveNotificationConstants.SWRVE_TRACKING_KEY))
        Assert.assertEquals("123", payload.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY))
        Assert.assertTrue(payload.has(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY))
        Assert.assertEquals(
            "somefakevalue",
            payload.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY)
        )
        Assert.assertTrue(payload.has(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY))
        Assert.assertEquals(
            "456",
            payload.getString(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY)
        )
        Assert.assertTrue(payload.has(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY))
        Assert.assertEquals("789", payload.getString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY))

        // payloads
        Assert.assertTrue(payload.has("root1"))
        Assert.assertEquals("value1", payload.getString("root1"))
        Assert.assertTrue(payload.has("g1"))
        Assert.assertEquals("{\"key1\":\"value1\",\"key2\":\"value2\"}", payload.getString("g1"))
        Assert.assertTrue(payload.has("g2"))
        Assert.assertEquals("{\"key3\":\"value3\"}", payload.getString("g2"))
    }

    @Test
    fun testGetPermissionString() {
        Assert.assertEquals(
            "granted",
            SwrveHelper.getPermissionString(PackageManager.PERMISSION_GRANTED)
        )
        Assert.assertEquals(
            "denied",
            SwrveHelper.getPermissionString(PackageManager.PERMISSION_DENIED)
        )
        Assert.assertEquals("unknown", SwrveHelper.getPermissionString(32423542))
    }
}
