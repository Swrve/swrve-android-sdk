package com.swrve.sdk

import com.swrve.sdk.messaging.SwrveInAppCampaign
import com.swrve.sdk.messaging.model.Arg
import com.swrve.sdk.messaging.model.Conditions
import com.swrve.sdk.messaging.model.Trigger
import junit.framework.TestCase
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.Date

class TriggerTest : SwrveBaseTest() {
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
    fun testTriggerModelV5ExceptionHandled() {
        val json = """["song1.played", "song2.played", "song3.played"]"""
        val triggers = Trigger.fromJson(json, 1)
        Assert.assertNull(triggers) // asserting that the invalid json exception is caught
    }

    @Test
    @Throws(Exception::class)
    fun testTriggerModelWithConditions() {
        val json = SwrveTestUtils.getAssetAsText(mActivity!!, "triggers.json")
        val triggers = Trigger.fromJson(json, 1)

        Assert.assertNotNull(triggers)
        Assert.assertEquals(3, triggers.size.toLong())

        val trigger1 = triggers[0]
        Assert.assertEquals("music.condition1", trigger1.eventName)
        Assert.assertNotNull(trigger1.conditions)
        val conditions = trigger1.conditions
        Assert.assertEquals(Conditions.Op.AND, conditions.op)
        Assert.assertEquals(2, conditions.args.size.toLong())
        Assert.assertEquals("artist", conditions.args[0].key)
        Assert.assertEquals(Arg.Op.EQ, conditions.args[0].op)
        Assert.assertEquals("prince", conditions.args[0].value)
        Assert.assertEquals("song", conditions.args[1].key)
        Assert.assertEquals(Arg.Op.EQ, conditions.args[1].op)
        Assert.assertEquals("purple rain", conditions.args[1].value)

        val trigger2 = triggers[1]
        Assert.assertEquals("music.condition2", trigger2.eventName)
        Assert.assertNotNull(trigger2.conditions)
        Assert.assertNull(trigger2.conditions.args)
        Assert.assertEquals(Conditions.Op.EQ, trigger2.conditions.op)
        Assert.assertEquals("artist", trigger2.conditions.key)
        Assert.assertEquals("queen", trigger2.conditions.value)

        val trigger3 = triggers[2]
        Assert.assertEquals("music.condition3", trigger3.eventName)
        Assert.assertNull(trigger3.conditions.args)
        Assert.assertNull(trigger3.conditions.op)
    }

    @Test
    fun testTriggerModelWithInvalidConditions() {
        // if any trigger is invalid, then all triggers are null'ed.

        val unsupportedOp = """
            [{
                "event_name": "invalid.trigger",
                "conditions": {
                    "key": "artist",
                    "op": "unsupported_op",
                    "value": "queen"
                }
            }]
            """
        var triggers = Trigger.fromJson(unsupportedOp, 1)
        Assert.assertNull(triggers)

        val nullKey = """
            [{
                "event_name": "invalid.trigger",
                "conditions": {
                    "key": null,
                    "op": "eq",
                    "value": "queen"
                }
            }]
            """
        triggers = Trigger.fromJson(nullKey, 1)
        Assert.assertNull(triggers)

        val nullValue = """
            [{
                "event_name": "invalid.trigger",
                "conditions": {
                    "key": "artist",
                    "op": "eq",
                    "value": null
                }
            }]
            """
        triggers = Trigger.fromJson(nullValue, 1)
        Assert.assertNull(triggers)

        val missingArgs = """
            [{
                "event_name": "invalid.trigger",
                "conditions": {
                    "key": "artist",
                    "op": "and",
                    "value": "queen"
                }
            }]
            """
        triggers = Trigger.fromJson(missingArgs, 1)
        Assert.assertNull(triggers)

        val nullArg = """
            [{
                "event_name": "invalid.trigger",
                "conditions": {
                    "op": "and",
                    "args": null    }
            }]
            """
        triggers = Trigger.fromJson(nullArg, 1)
        Assert.assertNull(triggers)

        val nullArgKey = """
            [{
                "event_name": "invalid.trigger",
                "conditions": {
                    "op": "and",
                    "args": [{
                        "key": null,
                        "op": "eq",
                        "value": "prince"
                    }]    }
            }]
            """
        triggers = Trigger.fromJson(nullArgKey, 1)
        Assert.assertNull(triggers)

        val nullArgOp = """
            [{
                "event_name": "invalid.trigger",
                "conditions": {
                    "op": "and",
                    "args": [{
                        "key": "artist",
                        "op": null,
                        "value": "prince"
                    }]    }
            }]
            """
        triggers = Trigger.fromJson(nullArgOp, 1)
        Assert.assertNull(triggers)

        val nullArgValue = """
            [{
                "event_name": "invalid.trigger",
                "conditions": {
                    "op": "and",
                    "args": [{
                        "key": "artist",
                        "op": "eq",
                        "value": null
                    }]    }
            }]
            """
        triggers = Trigger.fromJson(nullArgValue, 1)
        Assert.assertNull(triggers)

        val unsupportedArgOp = """
            [{
                "event_name": "invalid.trigger",
                "conditions": {
                    "op": "and",
                    "args": [{
                        "key": "artist",
                        "op": "unsupported_op",
                        "value": null
                    }]    }
            }]
            """
        triggers = Trigger.fromJson(unsupportedArgOp, 1)
        Assert.assertNull(triggers)
    }

    @Test
    @Throws(Exception::class)
    fun testCampaignTriggerConditionAND() {
        val qaUserMock = Mockito.mock(QaUser::class.java)
        qaUserMock.loggingEnabled = true
        QaUser.instance = qaUserMock

        val text = SwrveTestUtils.getAssetAsText(mActivity!!, "campaign_trigger_condition.json")
        Assert.assertNotNull(text)
        val jsonObject = JSONObject(text)
        val campaign = SwrveInAppCampaign(
            SwrveTestUtils.testSwrveCampaignManager,
            SwrveCampaignDisplayer(),
            jsonObject,
            HashSet(),
            null
        )
        Assert.assertNotNull(campaign)

        var qaCampaignInfoMap: Map<Int?, QaCampaignInfo> = HashMap()
        var payload: MutableMap<String?, String?> = HashMap()
        payload["artist"] = "prince" // just 1
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        payload = HashMap()
        payload["artist"] = "prince"
        payload["song"] = "PuRpLe RaIn" // mixed case on purpose
        payload["extra"] = "unused"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "this should not match"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "queen"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition2",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "this should not match"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition2",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["extra"] = "unused"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition3",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        Assert.assertNull(
            campaign.getMessageForEvent(
                "random.event",
                null,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        // match the event name but null payload
        qaCampaignInfoMap = HashMap()
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition1",
                null,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        // match the event name but null payload
        qaCampaignInfoMap = HashMap()
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition2",
                null,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)
    }

    @Test
    @Throws(Exception::class)
    fun testCampaignTriggerConditionOR() {
        val qaUserMock = Mockito.mock(QaUser::class.java)
        qaUserMock.loggingEnabled = true
        QaUser.instance = qaUserMock

        val text = SwrveTestUtils.getAssetAsText(mActivity!!, "campaign_trigger_conditionOR.json")
        Assert.assertNotNull(text)
        val jsonObject = JSONObject(text)
        val campaign = SwrveInAppCampaign(
            SwrveTestUtils.testSwrveCampaignManager,
            SwrveCampaignDisplayer(),
            jsonObject,
            HashSet(),
            null
        )
        Assert.assertNotNull(campaign)

        var qaCampaignInfoMap: Map<Int?, QaCampaignInfo> = HashMap()
        var payload: MutableMap<String?, String?> = HashMap()
        payload["artist"] = "prince" // match just 1
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        payload = HashMap()
        payload["song"] = "PuRpLe RaIn" // mixed case on purpose
        payload["extra"] = "unused"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "this should not match"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "queen"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition2",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "this should not match"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition2",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["extra"] = "unused"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition3",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        Assert.assertNull(
            campaign.getMessageForEvent(
                "random.event",
                null,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        // match the event name but null payload
        qaCampaignInfoMap = HashMap()
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition1",
                null,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        // match the event name but null payload
        qaCampaignInfoMap = HashMap()
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition2",
                null,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)
    }

    @Test
    @Throws(Exception::class)
    fun testCampaignTriggerConditionCONTAINS() {
        val qaUserMock = Mockito.mock(QaUser::class.java)
        qaUserMock.loggingEnabled = true
        QaUser.instance = qaUserMock

        val text =
            SwrveTestUtils.getAssetAsText(mActivity!!, "campaign_trigger_conditionCONTAINS.json")
        Assert.assertNotNull(text)
        val jsonObject = JSONObject(text)
        val campaign = SwrveInAppCampaign(
            SwrveTestUtils.testSwrveCampaignManager,
            SwrveCampaignDisplayer(),
            jsonObject,
            HashSet(),
            null
        )
        Assert.assertNotNull(campaign)

        var qaCampaignInfoMap: Map<Int?, QaCampaignInfo> = HashMap()
        var payload: MutableMap<String?, String?> = HashMap()
        payload["artist"] = "david bowie"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "Gray David"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "Dave Ghrol"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "David.Bowie"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition2",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "Bobby.Brown"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition2",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "Paul.mcCartney"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition2",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "Axl.Rose"
        payload["genre"] = "RocknRoll"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition3",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "AC/DC"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition3",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "AC/DC"
        payload["genre"] = "Rock"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition4",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "AC/DC"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition4",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "Taylor Swift"
        payload["genre"] = "pop"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "music.condition5",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["artist"] = "Beatles"
        payload["genre"] = "pop"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "music.condition5",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)
    }

    @Test
    @Throws(Exception::class)
    fun testCampaignTriggerConditionNUMERIC() {
        val qaUserMock = Mockito.mock(QaUser::class.java)
        qaUserMock.loggingEnabled = true
        QaUser.instance = qaUserMock

        val text =
            SwrveTestUtils.getAssetAsText(mActivity!!, "campaign_trigger_conditionNUMERIC.json")
        Assert.assertNotNull(text)
        val jsonObject = JSONObject(text)
        val campaign = SwrveInAppCampaign(
            SwrveTestUtils.testSwrveCampaignManager,
            SwrveCampaignDisplayer(),
            jsonObject,
            HashSet(),
            null
        )
        Assert.assertNotNull(campaign)

        var qaCampaignInfoMap: Map<Int?, QaCampaignInfo> = HashMap()
        var payload: MutableMap<String?, String?> = HashMap()
        payload["flight"] = "17"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "flight_search1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "18"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "flight_search1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "16"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "flight_search1",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "5"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "flight_search2",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "11"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "flight_search2",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "11"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "flight_search3",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "5"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "flight_search3",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "15"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "flight_search4",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "23"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "flight_search4",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "8"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "flight_search4",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "12"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "flight_search5",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "9"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "flight_search5",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passengers1"] = "10"
        payload["passengers2"] = "4"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "flight_search6",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passengers2"] = "10"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "flight_search6",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "15"
        Assert.assertNull(
            campaign.getMessageForEvent(
                "flight_search7",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        Assert.assertFalse(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "23"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "flight_search7",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)

        qaCampaignInfoMap = HashMap()
        payload = HashMap()
        payload["passenger"] = "8"
        Assert.assertNotNull(
            campaign.getMessageForEvent(
                "flight_search7",
                payload,
                Date(),
                qaCampaignInfoMap
            )
        )
        Assert.assertEquals(1, qaCampaignInfoMap.size.toLong())
        TestCase.assertTrue(qaCampaignInfoMap[campaign.id]!!.displayed)
    }
}
