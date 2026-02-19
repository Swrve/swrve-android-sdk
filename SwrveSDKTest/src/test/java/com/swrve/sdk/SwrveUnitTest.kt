package com.swrve.sdk

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.collect.Lists
import com.swrve.sdk.SwrveTestUtils.assertQueueEvent
import com.swrve.sdk.SwrveTestUtils.createSpyInstance
import com.swrve.sdk.SwrveTestUtils.flushLifecycleExecutorQueue
import com.swrve.sdk.SwrveTestUtils.runSingleThreaded
import com.swrve.sdk.SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance
import com.swrve.sdk.config.SwrveConfig
import com.swrve.sdk.config.SwrveInAppMessageConfig
import com.swrve.sdk.device.ITelephonyManager
import com.swrve.sdk.messaging.SwrveMessagePersonalizationProvider
import com.swrve.sdk.rest.IRESTClient
import com.swrve.sdk.rest.IRESTResponseListener
import com.swrve.sdk.rest.RESTResponse
import com.swrve.sdk.test.R
import com.swrve.sdk.test.SplashActivity
import org.awaitility.Awaitility
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.atMost
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.robolectric.Robolectric
import org.robolectric.Shadows
import java.lang.ref.WeakReference
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

class SwrveUnitTest : SwrveBaseTest() {
    private var swrveSpy: Swrve? = null
    private var backgroundEventSenderMock: SwrveBackgroundEventSender? = null

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        swrveSpy = createSpyInstance()

        backgroundEventSenderMock = mock(SwrveBackgroundEventSender::class.java)
        doNothing().`when`(backgroundEventSenderMock)?.send(anyString(), anyList())
        doReturn(backgroundEventSenderMock).`when`(swrveSpy)?.getSwrveBackgroundEventSender(any(Context::class.java))

        swrveSpy!!.init(mActivity)
        swrveSpy!!.activityContext = WeakReference(mActivity)
        flushLifecycleExecutorQueue(swrveSpy!!)
    }

    @After
    @Throws(Exception::class)
    override fun tearDown() {
        super.tearDown()
        SwrveHelper.buildModel = Build.MODEL
    }

    @Test
    @Throws(Exception::class)
    fun testInitWithAppVersion() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val appVersion = "my_version"
        val config = SwrveConfig()
        config.setAppVersion(appVersion)
        val swrve = createSpyInstance(config)
        assertEquals(appVersion, swrve.appVersion)
    }

    @Test
    @Throws(Exception::class)
    fun testInitWithoutAppVersion() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val config = SwrveConfig()
        config.setAppVersion(null)
        val swrve = createSpyInstance(config)
        assertNotNull(swrve.appVersion) // // Check generated app version
    }

    @Test
    @Throws(Exception::class)
    fun testLanguage() {
        shutdownAndRemoveSwrveSDKSingletonInstance()

        val language1 = Locale.JAPANESE
        val language2 = Locale.CHINESE
        val config = SwrveConfig()
        config.setLanguage(language1)
        val swrve = createSpyInstance(config)
        assertEquals("ja", swrve.getLanguage())
        swrve.setLanguage(language2)
        assertEquals("zh", swrve.getLanguage())
    }

    @Test
    @Throws(Exception::class)
    fun testSwitchAppId() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val swrve1 = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey") as Swrve
        flushLifecycleExecutorQueue(swrve1) // wait until swrve2 instance is fully created
        assertTrue(swrve1.isStarted)

        shutdownAndRemoveSwrveSDKSingletonInstance()
        val swrve2 = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 2, "apiKey_different") as Swrve
        flushLifecycleExecutorQueue(swrve2) // wait until swrve2 instance is fully created
        assertTrue(swrve2.isStarted) // after switching appId, the sdk will still be started.
    }

    @Test
    @Throws(Exception::class)
    fun testInitialisationAndUserIdGenerated() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        swrveSpy = createSpyInstance()
        val userId = SwrveSDK.getUserId()
        assertNotNull(userId)
    }

    @Test
    @Throws(Exception::class)
    fun testDeviceInfoQueued() {
        shutdownAndRemoveSwrveSDKSingletonInstance()

        swrveSpy = createSpyInstance()

        verify(swrveSpy, atMost(0))?.queueDeviceUpdateNow(anyString(), anyString(), anyBoolean()) // device info not queued
        verify(swrveSpy, atMost(0))?.deviceUpdate(anyString(), any(JSONObject::class.java))
        swrveSpy!!.onCreate(mActivity)
        verify(swrveSpy, atMost(1))?.queueDeviceUpdateNow(anyString(), anyString(), anyBoolean()) // device info queued once upon init
        verify(swrveSpy, atMost(1))?.deviceUpdate(anyString(), any(JSONObject::class.java))

        swrveSpy!!.onCreate(mActivity)
        verify(swrveSpy, atMost(1))?.queueDeviceUpdateNow(anyString(), anyString(), anyBoolean()) // device info not queued, because sdk already initialised
        verify(swrveSpy, atMost(1))?.deviceUpdate(anyString(), any(JSONObject::class.java))

        swrveSpy!!.onResume(mActivity)
        verify(swrveSpy, atMost(1))?.queueDeviceUpdateNow(anyString(), anyString(), anyBoolean()) // device info not queued, because sdk already initialised
        verify(swrveSpy, atMost(1))?.deviceUpdate(anyString(), any(JSONObject::class.java))
    }

    @Test
    fun testQueueEvent() {
        SwrveSDK.event("this_name")
        val parameters: MutableMap<String, Any> = HashMap()
        parameters["name"] = "this_name"
        assertQueueEvent(swrveSpy!!, "event", parameters, null)
    }

    @Test
    fun testQueueEventAndPayload() {
        val payload: MutableMap<String, String> = HashMap()
        payload["k1"] = "v1"
        SwrveSDK.event("this_name", payload)

        val expectedParameters: MutableMap<String, Any> = HashMap()
        expectedParameters["name"] = "this_name"
        val expectedPayload: Map<String, Any> = HashMap()
        payload["k1"] = "v1"
        assertQueueEvent(swrveSpy!!, "event", expectedParameters, expectedPayload)
    }

    @Test
    fun testQueueEventAndInvalidPayload() {
        reset(swrveSpy) // reset the setup init calls on swrveSpy so the never() test can be done below
        val payloadInvalid: MutableMap<String?, String?> = HashMap()
        payloadInvalid[null] = null
        SwrveSDK.event("this_name", payloadInvalid)
        verify(swrveSpy, never())?.queueEvent(anyString(), anyString(), anyMap(), anyMap(), anyBoolean())

        val payloadValid: MutableMap<String, String?> = HashMap()
        payloadValid["valid"] = null
        SwrveSDK.event("this_name", payloadValid)
        verify(swrveSpy, times(1))?.queueEvent(anyString(), anyString(), anyMap(), anyMap(), anyBoolean())
    }

    @Test
    fun testPurchase() {
        SwrveSDK.purchase("item_purchase", "€", 99, 5)

        val parameters: MutableMap<String, Any> = HashMap()
        parameters["item"] = "item_purchase"
        parameters["cost"] = "99"
        parameters["quantity"] = "5"
        parameters["currency"] = "€"
        assertQueueEvent(swrveSpy!!, "purchase", parameters, null)
    }

    @Test
    fun testIAP() {
        SwrveSDK.iap(1, "com.swrve.product1", 0.99, "USD")

        val parameters: MutableMap<String, Any> = HashMap()
        parameters["app_store"] = "unknown_store"
        parameters["cost"] = 0.99
        parameters["quantity"] = 1
        parameters["product_id"] = "com.swrve.product1"
        parameters["local_currency"] = "USD"
        assertQueueEvent(swrveSpy!!, "iap", parameters, null)
    }

    @Test
    fun testIAPRewards() {
        val rewards = SwrveIAPRewards()
        rewards.addCurrency("gold", 203)
        rewards.addCurrency("coins", 105)
        rewards.addItem("sword", 59)
        SwrveSDK.iap(2, "com.swrve.product2", 1.99, "EUR", rewards)

        val parameters: MutableMap<String, Any> = HashMap()
        parameters["app_store"] = "unknown_store"
        parameters["cost"] = 1.99
        parameters["quantity"] = 2
        parameters["product_id"] = "com.swrve.product2"
        parameters["local_currency"] = "EUR"
        parameters["rewards"] = rewards.rewardsJSON.toString()
        assertQueueEvent(swrveSpy!!, "iap", parameters, null)
    }

    @Test
    fun testUserUpdate() {
        val attributes: MutableMap<String, String> = HashMap()
        attributes["a0"] = "b0"
        SwrveSDK.userUpdate(attributes)

        val parameters: MutableMap<String, Any> = HashMap()
        val attributesJSON: MutableMap<String?, Any?> = HashMap()
        attributesJSON["a0"] = "b0"
        parameters["attributes"] = JSONObject(attributesJSON).toString()
        assertQueueEvent(swrveSpy!!, "user", parameters, null)
    }

    @Test
    @Throws(Exception::class)
    fun testDeviceUpdate_AuthPushConstantSet() {
        val deviceInfo = swrveSpy!!.deviceInfo
        assertEquals(deviceInfo.getBoolean("swrve.can_receive_authenticated_push"), true)
    }

    @Test
    @Throws(Exception::class)
    fun testModelBlacklist() {
        // Test default blacklist
        shutdownAndRemoveSwrveSDKSingletonInstance()
        var config = SwrveConfig()
        SwrveHelper.buildModel = "Calypso AppCrawler"
        var sdk = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config)
        assertTrue(sdk is SwrveEmpty)
        assertNotNull(SwrveSDK.getInstance())

        // Test custom blacklist
        shutdownAndRemoveSwrveSDKSingletonInstance()
        config = SwrveConfig()
        config.modelBlackList = Lists.newArrayList("custom_model")
        SwrveHelper.buildModel = "custom_model"
        sdk = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config)
        assertTrue(sdk is SwrveEmpty)
        assertNotNull(SwrveSDK.getInstance())

        shutdownAndRemoveSwrveSDKSingletonInstance()
        SwrveHelper.buildModel = "not_custom_model"
        sdk = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config)
        assertTrue(sdk is Swrve)
        assertNotNull(SwrveSDK.getInstance())
    }

    @Test
    @Throws(Exception::class)
    fun testAutoShowMessagesDelay() {
        shutdownAndRemoveSwrveSDKSingletonInstance()

        // configure sdk to disable autoShowMessagesEnabled after 1 second
        val config = SwrveConfig()

        config.inAppMessageConfig = SwrveInAppMessageConfig.Builder().autoShowMessagesMaxDelay(500L).build()
        swrveSpy = createSpyInstance(config)

        // create instance should not call disableAutoShowAfterDelay and the default for autoShowMessagesEnabled should be false
        assertEquals("AutoDisplayMessages should be true upon sdk init.", false, swrveSpy!!.autoShowMessagesEnabled)
        verify(swrveSpy, atMost(0))?.disableAutoShowAfterDelay()

        swrveSpy!!.onCreate(mActivity)

        // After init of sdk the disableAutoShowAfterDelay should be called
        verify(swrveSpy, atMost(1))?.disableAutoShowAfterDelay()

        // sleep 2 seconds and test autoShowMessagesEnabled has been disabled.
        Thread.sleep(1000L)
        assertEquals("AutoDisplayMessages should be true upon sdk init.", false, swrveSpy!!.autoShowMessagesEnabled)
    }

    @Test
    fun testSessionStartUponInit() {
        // verify that sessionStart is first and then startCampaignsAndResourcesTimer.
        val inOrder = inOrder(swrveSpy, swrveSpy)
        inOrder.verify(swrveSpy, times(1))?.sessionStart()
        inOrder.verify(swrveSpy, times(1))?.startCampaignsAndResourcesTimer(true)
    }

    @Test
    fun testSessionStart() {
        val sessionListenerMock = mock(SwrveSessionListener::class.java)
        swrveSpy!!.setSessionListener(sessionListenerMock)
        SwrveSDK.sessionStart()
        verify(swrveSpy, atLeastOnce())?.restClientExecutorExecute(any(Runnable::class.java))
        verify(sessionListenerMock, atLeastOnce()).sessionStarted()
    }

    @Test
    fun testSessionListeners() {
        //Verify that when multiple sessionListeners are set, they all get called
        val sessionListenerMock1 = mock(SwrveSessionListener::class.java)
        val sessionListenerMock2 = mock(SwrveSessionListener::class.java)
        swrveSpy!!.setSessionListener(sessionListenerMock1)
        swrveSpy!!.setSessionListener(sessionListenerMock2)
        SwrveSDK.sessionStart()
        verify(swrveSpy, atLeastOnce())?.restClientExecutorExecute(any(Runnable::class.java))
        verify(sessionListenerMock1, atLeastOnce()).sessionStarted()
        verify(sessionListenerMock2, atLeastOnce()).sessionStarted()

        swrveSpy!!.removeSessionListener(sessionListenerMock2)
        SwrveSDK.sessionStart()
        verify(swrveSpy, atLeastOnce())?.restClientExecutorExecute(any(Runnable::class.java))
        verify(sessionListenerMock1, times(2)).sessionStarted()
        verify(sessionListenerMock2, atMost(1)).sessionStarted()

        //Ensure setSessionListener with value of null has the effect of removing all session listeners
        swrveSpy!!.setSessionListener(null)
        verify(swrveSpy, atLeastOnce())?.restClientExecutorExecute(any(Runnable::class.java))
        verify(sessionListenerMock1, times(2)).sessionStarted()
        verify(sessionListenerMock2, times(1)).sessionStarted()
    }

    @Test
    @Throws(Exception::class)
    fun testSendSessionStart() {
        val eventListenerMock = mock(ISwrveEventListener::class.java)
        swrveSpy!!.eventListener = eventListenerMock

        val eventsManagerMock = mock(SwrveEventsManager::class.java)
        doReturn(eventsManagerMock).`when`(swrveSpy)?.getSwrveEventsManager(anyString(), anyString(), anyString())
        doReturn(100).`when`(swrveSpy)?.nextSequenceNumber

        swrveSpy!!.sendSessionStart(123)

        val deviceId = swrveSpy!!.multiLayerLocalStorage.getCacheEntry(swrveSpy!!.userId, "device_id")
        verify(swrveSpy, atLeastOnce())?.getSwrveEventsManager(swrveSpy!!.userId, deviceId, swrveSpy!!.sessionKey)

        verify(swrveSpy, atLeastOnce())?.restClientExecutorExecute(any(Runnable::class.java))
        val list: MutableList<String> = ArrayList()
        list.add("{\"type\":\"session_start\",\"time\":123,\"seqnum\":100}")
        verify(eventsManagerMock, atLeastOnce()).storeAndSendEvents(list, swrveSpy!!.multiLayerLocalStorage.secondaryStorage)

        // verify eventlistener triggered for auto showing new session campaigns
        verify(eventListenerMock, atLeastOnce()).onEvent("Swrve.session.start", null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnResumeNewSession() {
        verify(swrveSpy, times(1))?.sessionStart() // init was called in setup so sessionStart already called

        // fast forward time but not far enough for a new session to be started
        var newSessionTime = swrveSpy!!.lastSessionTick - 100
        doReturn(newSessionTime).`when`(swrveSpy)?.sessionTime
        swrveSpy!!.onResume(mActivity)
        verify(swrveSpy, times(1))?.sessionStart() // session start is ill only once

        // fast forward time for a new session to be started
        newSessionTime = swrveSpy!!.lastSessionTick + 100
        doReturn(newSessionTime).`when`(swrveSpy)?.sessionTime
        swrveSpy!!.onResume(mActivity)
        // verify that new session "sessionStart" is first and then startCampaignsAndResourcesTimer.
        val inOrder = inOrder(swrveSpy, swrveSpy)
        inOrder.verify(swrveSpy, times(2))?.sessionStart()
        inOrder.verify(swrveSpy, times(1))?.generateNewSessionInterval()
        inOrder.verify(swrveSpy, times(1))?.startCampaignsAndResourcesTimer(true)
        inOrder.verify(swrveSpy, times(1))?.disableAutoShowAfterDelay()
    }

    @Test
    @Throws(Exception::class)
    fun testUserUpdateDate() {
        SwrveSDK.userUpdate("a0", Date())

        val userIdStringCaptor = ArgumentCaptor.forClass(String::class.java)
        val eventStringCaptor = ArgumentCaptor.forClass(String::class.java)
        val parametersMapCaptor = ArgumentCaptor.forClass(MutableMap::class.java) as ArgumentCaptor<Map<String, Any>>
        val payloadMapCaptor = ArgumentCaptor.forClass(MutableMap::class.java) as ArgumentCaptor<Map<String, String>>
        val eventListenerBooleanCaptor = ArgumentCaptor.forClass(Boolean::class.java)
        verify(swrveSpy, times(3))?.queueEvent(userIdStringCaptor.capture(),
            eventStringCaptor.capture(), parametersMapCaptor.capture(), payloadMapCaptor.capture(), eventListenerBooleanCaptor.capture())

        val eventType = eventStringCaptor.value
        assertEquals("user", eventType)
        val parameters: Map<String, Any> = parametersMapCaptor.getValue()
        val attributes = parameters["attributes"]
        assertNotNull(attributes)
        val jsonObject = attributes as JSONObject?
        val date = jsonObject!!.getString("a0")
        assertNotNull(date)
        val regex = Pattern.compile(iso8601regex)
        assertTrue(regex.matcher(date).matches())
    }

    @Test
    fun testiso8601Regex() {
        val regex = Pattern.compile(iso8601regex)
        val badinputs = arrayOf(
            "2015-03-16T23:59:59+00:00",
            "2015-03-16T23:59:59+00",
            "2015-03-16T23:59:59+0000",
            "2015-03-16T23:59:59.000+00",
            "2015-03-16T23:59:59.000+0000",
            "2015-03-16T23:59:59+09:00",
            "2015-17-16T23:59:59+10",
            "2015-03-16T23:59:59-0100",
            "2015-03-16T23:59:59.000+00",
            "2015-03-16T23:59:59.000+0000",
            "2015-03-16T23:59:59.500+00",
            "2015-03-16T23:59:59.600+0000",
            "2016-22-11T17:08:50.000Z"
        )
        val goodInputs =
            arrayOf("2016-03-11T09:29:33.915Z", "2016-02-11T17:08:50.000Z", "2015-03-16T23:59:59.000Z", "2015-03-16T23:59:59.000+00:00", "2015-03-16T23:59:59.999Z", "2015-03-16T23:59:59.999+00:00")
        verifyRegex(regex, badinputs, goodInputs)
    }

    protected fun verifyRegex(regex: Pattern, shouldFail: Array<String>, shouldPass: Array<String>) {
        for (candit in shouldFail) {
            assertFalse(regex.matcher(candit).matches())
        }
        for (candit in shouldPass) {
            assertTrue(regex.matcher(candit).matches())
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSendEventsInBackground() {
        // setup mocks

        val qaUserMock = mock(QaUser::class.java)
        QaUser.instance = qaUserMock

        val events = Lists.newArrayList("some_event_json")
        swrveSpy!!.sendEventsInBackground(mActivity, "userId", events)

        verify(backgroundEventSenderMock, times(1))?.send("userId", events)
        verify(qaUserMock, atLeastOnce())._wrappedEvents(events)
    }

    @Test
    fun testRestrictedEventName() {
        val userId = SwrveSDK.getUserId()

        SwrveSDK.event("valid.event.name1")
        val parameters: MutableMap<String, Any> = java.util.HashMap()
        parameters["name"] = "valid.event.name1"
        verify(swrveSpy, times(1))?.queueEvent(userId, "event", parameters, null, true)

        SwrveSDK.event("Swrve.thisEventIsRestrictedAndWillNotBeQueued")
        parameters["name"] = "Swrve.thisEventIsRestrictedAndWillNotBeQueued"
        verify(swrveSpy, never())?.queueEvent(userId, "event", parameters, null, true)

        SwrveSDK.event("valid.event.name2")
        parameters["name"] = "valid.event.name2"
        verify(swrveSpy, times(1))?.queueEvent(userId, "event", parameters, null, true)
    }

    @Test
    @Throws(Exception::class)
    fun testUserInfo() {
        Settings.Secure.putString(mActivity!!.contentResolver, Settings.Secure.ANDROID_ID, "my_android_id")

        shutdownAndRemoveSwrveSDKSingletonInstance()
        val config = SwrveConfig()
        config.isAndroidIdLoggingEnabled = true
        config.setAutoDownloadCampaignsAndResources(false)
        val swrveSpy = createSpyInstance(config)
        runSingleThreaded(swrveSpy)

        shadowApplication!!.grantPermissions(permission.POST_NOTIFICATIONS)

        val telephonyManagerMock = mock(ITelephonyManager::class.java)
        doReturn("vodafone IE").`when`(telephonyManagerMock).simOperatorName
        doReturn("ie").`when`(telephonyManagerMock).simCountryIso
        doReturn("27201").`when`(telephonyManagerMock).simOperator
        doReturn(telephonyManagerMock).`when`(swrveSpy).getTelephonyManager(any(Context::class.java))

        swrveSpy.activityContext = WeakReference(mActivity)
        swrveSpy.onCreate(mActivity)

        val userIdStringCaptor = ArgumentCaptor.forClass(String::class.java)
        val jsonObjectCaptor = ArgumentCaptor.forClass(JSONObject::class.java)
        verify(swrveSpy, atLeastOnce()).deviceUpdate(userIdStringCaptor.capture(), jsonObjectCaptor.capture())

        val attributeDevices = jsonObjectCaptor.value

        // Check that the user update event contains all the device info
        assertTrue(attributeDevices.has("swrve.device_name"))
        assertTrue(attributeDevices.has("swrve.os"))
        assertTrue(attributeDevices.has("swrve.os_version"))
        assertTrue(attributeDevices.has("swrve.os_int_version"))
        assertTrue(attributeDevices.has("swrve.app_target_version"))
        assertTrue(attributeDevices.has("swrve.device_width"))
        assertTrue(attributeDevices.has("swrve.device_height"))
        assertTrue(attributeDevices.has("swrve.device_dpi"))
        assertTrue(attributeDevices.has("swrve.android_device_xdpi"))
        assertTrue(attributeDevices.has("swrve.android_device_ydpi"))
        assertTrue(attributeDevices.has("swrve.language"))
        assertTrue(attributeDevices.has("swrve.device_region"))
        assertEquals(2, attributeDevices.getString("swrve.device_region").length.toLong())
        assertTrue(attributeDevices.has("swrve.utc_offset_seconds"))
        assertTrue(attributeDevices.has("swrve.timezone_name"))
        assertTrue(attributeDevices.has("swrve.sdk_version"))
        assertTrue(attributeDevices.has("swrve.sdk_flavour"))
        assertTrue(attributeDevices.has("swrve.sdk_init_mode"))
        assertTrue(attributeDevices.has("swrve.device_type"))
        assertEquals("auto_auto", attributeDevices["swrve.sdk_init_mode"])
        val expectedDeviceTypes: MutableList<String> = ArrayList()
        expectedDeviceTypes.add("tv")
        expectedDeviceTypes.add("mobile")
        assertTrue(expectedDeviceTypes.contains(attributeDevices["swrve.device_type"]))
        assertEquals("my_android_id", attributeDevices["swrve.android_id"])
        assertEquals(swrveSpy.getConfig().appStore, attributeDevices["swrve.app_store"])
        assertTrue(attributeDevices.has("swrve.install_date"))
        // Carrier info
        assertEquals("vodafone IE", attributeDevices["swrve.sim_operator.name"])
        assertEquals("ie", attributeDevices["swrve.sim_operator.iso_country_code"])
        assertEquals("27201", attributeDevices["swrve.sim_operator.code"])

        assertEquals(true, attributeDevices["swrve.permission.notifications_enabled"])
        assertEquals(NotificationManagerCompat.IMPORTANCE_NONE, attributeDevices["swrve.permission.notifications_importance"])

        assertEquals("granted", attributeDevices["swrve.permission.android.notification"])
        assertEquals(false, attributeDevices["swrve.permission.android.notification_show_rationale"])
        assertEquals(0, attributeDevices["swrve.permission.android.notification_answered_times"])
    }

    @Test
    @Throws(Exception::class)
    fun testUserInfoInitMode() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val config = SwrveConfig()
        config.initMode = SwrveInitMode.AUTO
        config.isAutoStartLastUser = true
        val swrveSpy = createSpyInstance(config)

        assertDeviceUpdateInitMode(swrveSpy, "auto_auto")

        config.initMode = SwrveInitMode.AUTO
        config.isAutoStartLastUser = false
        assertDeviceUpdateInitMode(swrveSpy, "auto")

        config.initMode = SwrveInitMode.MANAGED
        config.isAutoStartLastUser = true
        assertDeviceUpdateInitMode(swrveSpy, "managed_auto")

        config.initMode = SwrveInitMode.MANAGED
        config.isAutoStartLastUser = false
        assertDeviceUpdateInitMode(swrveSpy, "managed")
    }

    @Throws(Exception::class)
    private fun assertDeviceUpdateInitMode(swrveSpy: Swrve, expectedInitMode: String) {
        val attributeDevices = swrveSpy._getDeviceInfo()
        assertTrue(attributeDevices.has("swrve.sdk_init_mode"))
        assertEquals(expectedInitMode, attributeDevices["swrve.sdk_init_mode"])
    }

    @Test
    @Throws(Exception::class)
    fun testUserInfoManagedMode() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val config = SwrveConfig()
        config.initMode = SwrveInitMode.MANAGED
        val swrveSpy = createSpyInstance(config)
        runSingleThreaded(swrveSpy)

        swrveSpy.onCreate(mActivity)

        val userIdStringCaptor = ArgumentCaptor.forClass(String::class.java)
        val jsonObjectCaptor = ArgumentCaptor.forClass(JSONObject::class.java)
        verify(swrveSpy, atLeastOnce()).deviceUpdate(userIdStringCaptor.capture(), jsonObjectCaptor.capture())

        val attributeDevices = jsonObjectCaptor.value
        assertTrue(attributeDevices.has("swrve.sdk_init_mode"))
        assertEquals("managed_auto", attributeDevices["swrve.sdk_init_mode"])
    }

    @Test
    @Throws(Exception::class)
    fun testUserInfoManagedAutoMode() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val settings = mActivity!!.getSharedPreferences(ISwrveCommon.SDK_PREFS_NAME, 0)
        val editor = settings.edit()
        editor.putString("trackingState", "").commit() // blank out the state from the setup()

        val config = SwrveConfig()
        config.initMode = SwrveInitMode.MANAGED
        config.isAutoStartLastUser = false // Note the auto start false
        val swrveSpy = createSpyInstance(config)
        runSingleThreaded(swrveSpy)

        swrveSpy.start(mActivity)

        val userIdStringCaptor = ArgumentCaptor.forClass(String::class.java)
        val jsonObjectCaptor = ArgumentCaptor.forClass(JSONObject::class.java)
        verify(swrveSpy, atLeastOnce()).deviceUpdate(userIdStringCaptor.capture(), jsonObjectCaptor.capture())

        val attributeDevices = jsonObjectCaptor.value
        assertTrue(attributeDevices.has("swrve.sdk_init_mode"))
        assertEquals("managed", attributeDevices["swrve.sdk_init_mode"])
    }

    @Test
    @Throws(Exception::class)
    fun testNoCarrierInfo() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val config = SwrveConfig()
        val swrveSpy = createSpyInstance(config)
        runSingleThreaded(swrveSpy)

        val telephonyManagerMock = mock(ITelephonyManager::class.java)
        doReturn(null).`when`(telephonyManagerMock).simOperatorName
        doReturn(null).`when`(telephonyManagerMock).simCountryIso
        doReturn(null).`when`(telephonyManagerMock).simOperator
        doReturn(telephonyManagerMock).`when`(swrveSpy).getTelephonyManager(any(Context::class.java))

        swrveSpy.onCreate(mActivity)

        val userIdStringCaptor = ArgumentCaptor.forClass(String::class.java)
        val jsonObjectCaptor = ArgumentCaptor.forClass(JSONObject::class.java)
        verify(swrveSpy, atLeastOnce()).deviceUpdate(userIdStringCaptor.capture(), jsonObjectCaptor.capture())

        val attributeDevices = jsonObjectCaptor.value

        // Check that the user update event contains all the device info
        assertFalse(attributeDevices.has("swrve.sim_operator.name"))
        assertFalse(attributeDevices.has("swrve.sim_operator.iso_country_code"))
        assertFalse(attributeDevices.has("swrve.sim_operator.code"))
    }

    @Test
    @Throws(Exception::class)
    fun testSwrveResourceManager() {
        val cacheFileContents =
            "[{\"uid\": \"animal.ant\", \"name\": \"ant\", \"cost\": \"5.50\", \"quantity\": \"6\", \"tail\": \"false\"},{\"uid\": \"animal.bear\",\"name\": \"bear\", \"cost\": \"9.99\",\"quantity\": \"20\", \"tail\": \"true\"}]"
        var resourceJson: JSONArray? = JSONArray(cacheFileContents)

        swrveSpy!!.onCreate(mActivity)

        val resourceManager = swrveSpy!!.getResourceManager()
        resourceManager.setResourcesFromJSON(resourceJson)

        // Check the resources were written correctly to resource manager and functions to retrieve individual values work as expected
        assertEquals(2, resourceManager.getResources().size.toLong())

        val resource1 = resourceManager.getResource("animal.ant")
        assertNotNull(resource1)
        assertTrue(resource1.attributeKeys.contains("name"))
        assertTrue(resource1.attributeKeys.contains("cost"))
        assertTrue(resource1.attributeKeys.contains("quantity"))
        assertTrue(resource1.attributeKeys.contains("tail"))
        assertEquals("ant", resourceManager.getAttributeAsString("animal.ant", "name", "anonymous"))
        assertEquals("5.50", resourceManager.getAttributeAsString("animal.ant", "cost", "0"))
        assertEquals(6, resourceManager.getAttributeAsInt("animal.ant", "quantity", 0).toLong())
        assertFalse(resourceManager.getAttributeAsBoolean("animal.ant", "tail", true))

        val resource2 = resourceManager.getResource("animal.bear")
        assertNotNull(resource2)
        assertTrue(resource2.attributeKeys.contains("name"))
        assertTrue(resource2.attributeKeys.contains("cost"))
        assertTrue(resource2.attributeKeys.contains("quantity"))
        assertTrue(resource2.attributeKeys.contains("tail"))
        assertEquals("bear", resourceManager.getAttributeAsString("animal.bear", "name", "anonymous"))
        assertEquals("9.99", resourceManager.getAttributeAsString("animal.bear", "cost", "0"))
        assertEquals(20, resourceManager.getAttributeAsInt("animal.bear", "quantity", 0).toLong())
        assertTrue(resourceManager.getAttributeAsBoolean("animal.bear", "tail", false))

        // Test that when new resources are loaded, old ones are removed correctly
        val newCacheFileContents = "[{\"uid\": \"animal.ant\", \"name\": \"ant\", \"cost\": \"5.95\", \"quantity\": \"6\", \"tail\": \"false\"}]"
        try {
            resourceJson = JSONArray(newCacheFileContents)
        } catch (e: JSONException) {
            assertTrue(false) // Invalid JSON
        }
        resourceManager.setResourcesFromJSON(resourceJson)

        assertEquals(1, resourceManager.getResources().size.toLong())
        assertNotNull(resourceManager.getResource("animal.ant"))
        assertEquals("5.95", resourceManager.getAttributeAsString("animal.ant", "cost", "0"))

        // Check default value is used correctly for unknown resources
        assertEquals(5, resourceManager.getAttributeAsInt("unknown", "invalid", 5).toLong())
        assertFalse(resourceManager.getAttributeAsBoolean("unknown", "invalid", false))
        assertEquals("defaultvalue", resourceManager.getAttributeAsString("unknown", "invalid", "defaultvalue"))
        assertEquals("4.5", resourceManager.getAttributeAsString("unknown", "invalid", "4.5"))
    }

    @Test
    fun testGetUserResources() {
        val originalResponseBody = "[{ 'uid': 'animal.ant', 'name': 'ant', 'cost': '550', 'cost_type': 'gold'}, { 'uid': 'animal.bear', 'name': 'bear', 'cost': '999', 'cost_type': 'gold'}]"
        val userId = swrveSpy!!.userId
        swrveSpy!!.multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, ISwrveCommon.CACHE_RESOURCES, originalResponseBody, swrveSpy!!.getUniqueKey(userId))
        swrveSpy!!.getUserResources(object : SwrveUserResourcesListener {
            override fun onUserResourcesSuccess(resources: Map<String, Map<String, String>>, resourcesAsJSON: String) {
                assertEquals(2, resources.size.toLong())

                try {
                    val json = JSONArray(resourcesAsJSON)
                    assertEquals(json.length().toLong(), 2)
                    var i = 0
                    val j = json.length()
                    while (i < j) {
                        val resource = json.getJSONObject(i)
                        assert(resource.has("uid"))
                        if (resource.getString("uid") == "animal.ant") {
                            assertEquals(resource.getString("name"), "ant")
                            assertEquals(resource.getInt("cost").toLong(), 550)
                            assertEquals(resource.getString("cost_type"), "gold")
                        } else if (resource.getString("uid") == "animal.bear") {
                            assertEquals(resource.getString("name"), "bear")
                            assertEquals(resource.getInt("cost").toLong(), 999)
                            assertEquals(resource.getString("cost_type"), "gold")
                        } else {
                            assertFalse(true) // shouldn't be any other resources
                        }
                        i++
                    }
                } catch (e: Exception) {
                    SwrveLogger.e("Exception", e)
                }
            }

            override fun onUserResourcesError(exception: Exception) {
                fail(exception.toString())
            }
        })
    }

    @Ignore("MOBILE-27318 unstable test, so rewrite")
    @Test
    @Throws(Exception::class)
    fun testGetUserResourcesDiff() {
        val waitCallback = AtomicBoolean(false)
        Mockito.`when`(swrveSpy!!.restClientExecutorExecute(any(Runnable::class.java))).thenCallRealMethod()

        val originalResponseBody = "[{ 'uid': 'animal.ant', 'diff': { 'cost': { 'old': '550', 'new': '666' }}}, { 'uid': 'animal.bear', 'diff': { 'level': { 'old': '10', 'new': '9000' }}}]"
        val resourceIds: MutableSet<String> = HashSet()
        resourceIds.add("animal.ant")
        resourceIds.add("animal.bear")

        val restClientMock = mock(IRESTClient::class.java)
        doAnswer(Answer<Void?> { invocation: InvocationOnMock ->
            val callback = invocation.arguments[2] as RESTCacheResponseListener
            callback.onResponse(RESTResponse(200, originalResponseBody, null))
            null
        }).`when`<IRESTClient>(restClientMock)[anyString(), anyMap<String, String>(), any<IRESTResponseListener>(
            IRESTResponseListener::class.java
        )]
        swrveSpy!!.restClient = restClientMock

        swrveSpy!!.getUserResourcesDiff(object : SwrveUserResourcesDiffListener {
            override fun onUserResourcesDiffSuccess(oldResourcesValues: Map<String, Map<String, String>>, newResourcesValues: Map<String, Map<String, String>>, resourcesAsJSON: String) {
                assertEquals(originalResponseBody, resourcesAsJSON)
                assertEquals(resourceIds, oldResourcesValues.keys)
                assertEquals(resourceIds, newResourcesValues.keys)
                waitCallback.set(true)
            }

            override fun onUserResourcesDiffError(exception: Exception) {
            }
        })

        Awaitility.await().untilTrue(waitCallback)
    }

    @Test
    fun testGetApiKey() {
        swrveSpy!!.onCreate(mActivity)
        assertEquals("apiKey", swrveSpy!!.getApiKey())
    }

    @Test
    fun testGetContextActivity() {
        swrveSpy!!.onCreate(mActivity)
        assertEquals(mActivity, mActivity)
    }

    @Test
    @Throws(Exception::class)
    fun testGetConfig() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val config = SwrveConfig()
        val swrve = createSpyInstance(config)
        assertEquals(config, swrve.getConfig())
    }

    @Test
    @Throws(Exception::class)
    fun testDisableSendQueuedEventsOnResume() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val config = SwrveConfig()
        config.isSendQueuedEventsOnResume = false
        swrveSpy = createSpyInstance(config)
        runSingleThreaded(swrveSpy!!)
        swrveSpy!!.onCreate(mActivity)

        swrveSpy!!.event("generic_event_1", null)
        swrveSpy!!.event("generic_event_2", null)
        swrveSpy!!.event("generic_event_3", null)

        val numberOfStoredEventsBefore = getAllEventsInPrimaryStorage(swrveSpy!!)
        assertTrue(numberOfStoredEventsBefore >= 3)
        swrveSpy!!.lastSessionTick = swrveSpy!!.now.time + 200000
        swrveSpy!!.onResume(mActivity)

        // should be same amount of events stored after resume called
        val numberOfStoredEventsAfter = getAllEventsInPrimaryStorage(swrveSpy!!)
        assertEquals(numberOfStoredEventsBefore.toLong(), numberOfStoredEventsAfter.toLong())
    }

    private fun getAllEventsInPrimaryStorage(swrve: Swrve): Int {
        return swrve.multiLayerLocalStorage.primaryStorage.getFirstNEvents(Int.MAX_VALUE, swrve.userId).size
    }

    @Test
    fun testNoInvalidSignature() {
        val originalResponseBody = "[]"
        val userId = swrveSpy!!.userId
        swrveSpy!!.multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, ISwrveCommon.CACHE_RESOURCES, originalResponseBody, swrveSpy!!.getUniqueKey(userId))
        swrveSpy!!.getUserResources(object : SwrveUserResourcesListener {
            override fun onUserResourcesSuccess(resources: Map<String, Map<String, String>>, resourcesAsJSON: String) {
            }

            override fun onUserResourcesError(exception: Exception) {
            }
        })

        val userIdStringCaptor = ArgumentCaptor.forClass(String::class.java)
        val eventStringCaptor = ArgumentCaptor.forClass(String::class.java)
        val parametersMapCaptor = ArgumentCaptor.forClass(MutableMap::class.java) as ArgumentCaptor<Map<String, Any>>
        val payloadMapCaptor = ArgumentCaptor.forClass(MutableMap::class.java) as ArgumentCaptor<Map<String, String>>
        val triggerEventListenerCaptor = ArgumentCaptor.forClass(Boolean::class.java)
        verify(swrveSpy, atLeastOnce())?.queueEvent(
            userIdStringCaptor.capture(),
            eventStringCaptor.capture(),
            parametersMapCaptor.capture(),
            payloadMapCaptor.capture(),
            triggerEventListenerCaptor.capture()
        )

        assertTrue(eventStringCaptor.allValues.size > 0)
        val events1 = eventStringCaptor.allValues
        val parameters1 = parametersMapCaptor.allValues
        assertFalse(hasEventName("Swrve.signature_invalid", events1, parameters1))

        // Force a cache invalid, fake modification to resource cache signature
        swrveSpy!!.multiLayerLocalStorage.primaryStorage.setCacheEntry(userId, ISwrveCommon.CACHE_RESOURCES, "fake_new_content")

        swrveSpy!!.getUserResources(object : SwrveUserResourcesListener {
            override fun onUserResourcesSuccess(resources: Map<String, Map<String, String>>, resourcesAsJSON: String) {
            }

            override fun onUserResourcesError(exception: Exception) {
            }
        })

        verify(swrveSpy, atLeastOnce())?.queueEvent(
            userIdStringCaptor.capture(),
            eventStringCaptor.capture(), parametersMapCaptor.capture(), payloadMapCaptor.capture(), triggerEventListenerCaptor.capture()
        )
        assertTrue(eventStringCaptor.allValues.size > 0)
        val events2 = eventStringCaptor.allValues
        val parameters2 = parametersMapCaptor.allValues
        assertTrue(hasEventName("Swrve.signature_invalid", events2, parameters2))
    }

    private fun hasEventName(eventName: String, events: List<String>, parameters: List<Map<*, *>>): Boolean {
        var foundEvent = false
        for (i in events.indices) {
            val event = events[i]
            if (event == "event") {
                if (parameters[i].containsKey("name")) {
                    if (parameters[i]["name"] == eventName) {
                        foundEvent = true
                    }
                }
            }
        }
        return foundEvent
    }

    @Test
    fun testOnPause() {
        val userId = swrveSpy!!.userId
        swrveSpy!!.onPause()
        val inOrder = inOrder(swrveSpy, swrveSpy)
        inOrder.verify(swrveSpy, times(1))?.flushToDisk()
        inOrder.verify(swrveSpy, times(1))?.generateNewSessionInterval()
        inOrder.verify(swrveSpy, times(1))?.saveCampaignsState(userId)
    }

    @Test
    fun testRetrievePersonalizationProperties() {
        val testRealtimeUserProperties: MutableMap<String, String> = HashMap()
        testRealtimeUserProperties["key1"] = "value1"

        val providerResponse: MutableMap<String, String> = HashMap()
        providerResponse["key2"] = "value2"

        val messageCenterResponse: MutableMap<String, String> = HashMap()
        messageCenterResponse["key3"] = "value3"

        // verify when there's nothing return null and don't crash
        var resultProperties = swrveSpy!!.retrievePersonalizationProperties(null, null)
        assertEquals(null, resultProperties)

        swrveSpy!!.realTimeUserProperties = testRealtimeUserProperties

        var expectedProperties: MutableMap<String?, String?> = HashMap()
        expectedProperties["user.key1"] = "value1"

        // verify with no callback, just real time user properties
        resultProperties = swrveSpy!!.retrievePersonalizationProperties(null, null)
        assertEquals(expectedProperties, resultProperties)

        // verify from trigger / setting off personalization provider
        swrveSpy!!.personalizationProvider = SwrveMessagePersonalizationProvider { eventPayload: Map<String?, String?>? ->
            if (!SwrveHelper.isNullOrEmpty(eventPayload) && eventPayload!!.containsKey("change_value")) {
                val map = providerResponse
                map["key3"] = "event_payload"
                return@SwrveMessagePersonalizationProvider map
            }
            providerResponse
        }

        expectedProperties = HashMap()
        expectedProperties["user.key1"] = "value1"
        expectedProperties["key2"] = "value2"

        resultProperties = swrveSpy!!.retrievePersonalizationProperties(null, null)
        assertEquals(resultProperties, expectedProperties)

        // verify with event payload
        expectedProperties["key3"] = "event_payload"

        val eventPayload: MutableMap<String, String> = HashMap()
        eventPayload["change_value"] = "value"
        resultProperties = swrveSpy!!.retrievePersonalizationProperties(eventPayload, null)
        assertEquals(resultProperties, expectedProperties)

        // verify from message center (directly passing in properties)
        expectedProperties = HashMap()
        expectedProperties["user.key1"] = "value1"
        expectedProperties["key3"] = "value3"

        resultProperties = swrveSpy!!.retrievePersonalizationProperties(null, messageCenterResponse)
        assertEquals(resultProperties, expectedProperties)
    }

    @Test
    fun testResolveNotificationPermissionAnsweredTime() {
        swrveSpy!!.activityContext = WeakReference(mActivity)

        // default
        assertEquals(0, swrveSpy!!.resolveNotificationPermissionAnsweredTime().toLong())
        assertNull(swrveSpy!!.multiLayerLocalStorage.getCacheEntry("", "permission_rationale_was_true_android.permission.POST_NOTIFICATIONS")) // this should always stay true if it is ever set.
        assertNull(swrveSpy!!.multiLayerLocalStorage.getCacheEntry("", "permission_answered_times_android.permission.POST_NOTIFICATIONS"))

        // simulate a permission request where shouldShowRequestPermissionRationale returns true, thus resolveNotificationPermissionAnsweredTime will always be 1
        doReturn(true).`when`(swrveSpy)?.shouldShowRequestPermissionRationale(any(Activity::class.java), anyString())
        assertEquals(1, swrveSpy!!.resolveNotificationPermissionAnsweredTime().toLong())
        assertEquals("True", swrveSpy!!.multiLayerLocalStorage.getCacheEntry("", "permission_rationale_was_true_android.permission.POST_NOTIFICATIONS"))
        assertEquals("1", swrveSpy!!.multiLayerLocalStorage.getCacheEntry("", "permission_answered_times_android.permission.POST_NOTIFICATIONS"))

        // simulate a subsequent request by forcing shouldShowRequestPermissionRationale to be false
        doReturn(false).`when`(swrveSpy)?.shouldShowRequestPermissionRationale(any(Activity::class.java), anyString())
        assertEquals(2, swrveSpy!!.resolveNotificationPermissionAnsweredTime().toLong())
        assertEquals("True", swrveSpy!!.multiLayerLocalStorage.getCacheEntry("", "permission_rationale_was_true_android.permission.POST_NOTIFICATIONS")) // this should always stay true if it is ever set.
        assertEquals("2", swrveSpy!!.multiLayerLocalStorage.getCacheEntry("", "permission_answered_times_android.permission.POST_NOTIFICATIONS"))
    }

    @Test
    fun testResolveNotificationPermissionAnsweredTimeWithRequestsOutsideOfSwrveSDK() {
        swrveSpy!!.activityContext = WeakReference(mActivity)

        // simulate a permission request happened already outside of swrvesdk. This might have happened through another sdk or customer own code.
        // shouldShowRequestPermissionRationale returns false, and permission_notification_rationale_was_true is true
        doReturn(false).`when`(swrveSpy)?.shouldShowRequestPermissionRationale(any(Activity::class.java), anyString())
        swrveSpy!!.multiLayerLocalStorage.setCacheEntry("", "permission_rationale_was_true_" + permission.POST_NOTIFICATIONS, "True") // note this without userId
        assertEquals(2, swrveSpy!!.resolveNotificationPermissionAnsweredTime().toLong())
        assertEquals("True", swrveSpy!!.multiLayerLocalStorage.getCacheEntry("", "permission_rationale_was_true_android.permission.POST_NOTIFICATIONS"))
        assertEquals("2", swrveSpy!!.multiLayerLocalStorage.getCacheEntry("", "permission_answered_times_android.permission.POST_NOTIFICATIONS"))
    }

    @Test
    fun testNotificationPermissionTriggered() {
        val eventListenerSpy = spy(swrveSpy!!.eventListener as SwrveEventListener)
        doNothing().`when`(eventListenerSpy).requestNotificationPermission(any())
        swrveSpy!!.eventListener = eventListenerSpy
        val config = swrveSpy!!.getConfig()
        val notificationEvents: MutableList<String> = ArrayList()
        notificationEvents.add("test_request_notification_permission")
        val notificationConfig = SwrveNotificationConfig.Builder(R.drawable.ic_launcher, null).pushNotificationPermissionEvents(notificationEvents).build()
        config.notificationConfig = notificationConfig
        SwrveSDK.event("regular_event")
        verify(eventListenerSpy, never()).requestNotificationPermission(any())

        SwrveSDK.event("test_request_notification_permission")
        verify(eventListenerSpy, times(1)).requestNotificationPermission(any())
    }

    @Test
    @Throws(Exception::class)
    fun testSplashActivity() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val config = SwrveConfig()
        config.splashActivity = SplashActivity::class.java
        val swrve = createSpyInstance(config)

        val splashActivity = Robolectric.buildActivity(SplashActivity::class.java).create().visible().get()
        assertTrue(swrve.isSplashActivity(splashActivity))

        assertFalse(swrve.isSplashActivity(mActivity))
    }

    @Test
    fun testOpenDeepLinkWithDeeplinkListener() {
        val bundleMock = mock(Bundle::class.java)
        val deeplinkListenerMock = mock(SwrveDeeplinkListener::class.java)
        val swrveCommonMock = mock(ISwrveCommon::class.java)
        doReturn(deeplinkListenerMock).`when`(swrveCommonMock).swrveDeeplinkListener
        SwrveCommon.setSwrveCommon(swrveCommonMock)

        val uri = "www.google.com"
        SwrveIntentHelper.openDeepLink(mActivity, uri, bundleMock)
        val shadowMainActivity = Shadows.shadowOf(mActivity)
        assertNull(shadowMainActivity.peekNextStartedActivityForResult())

        verify(deeplinkListenerMock, times(1)).handleDeeplink(mActivity, uri, bundleMock)
    }

    @Test
    fun testCleanupOrphanedGifs() {
        shutdownAndRemoveSwrveSDKSingletonInstance()
        val swrveSpy = createSpyInstance(SwrveConfig())
        runSingleThreaded(swrveSpy)
        swrveSpy.init(mActivity)
        verify(swrveSpy, times(1)).cleanupOrphanedGifs()
    }

    companion object {
        private const val iso8601regex = "\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[1-2]\\d|3[0-1])T(?:[0-1]\\d|2[0-3]):[0-5]\\d:[0-5]\\d.\\d\\d\\d(Z|[+]\\d\\d:\\d\\d)"
    }
}
