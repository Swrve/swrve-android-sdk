package com.swrve.sdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import com.swrve.sdk.config.SwrveConfig
import com.swrve.sdk.config.SwrveConfigBase
import com.swrve.sdk.localstorage.LocalStorageTestUtils
import com.swrve.sdk.messaging.SwrveInAppMessageFragment
import com.swrve.sdk.messaging.SwrveMessageView
import com.swrve.sdk.rest.IRESTClient
import com.swrve.sdk.rest.IRESTResponseListener
import com.swrve.sdk.rest.RESTResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.internal.TlsUtil.localhost
import org.awaitility.Awaitility
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.argumentCaptor
import org.mockito.stubbing.Answer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Scanner
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLSocketFactory

object SwrveTestUtils {
    @JvmStatic
    @Throws(Exception::class)
    fun shutdownAndRemoveSwrveSDKSingletonInstance() {
        SwrveLogger.i("SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance() start")
        try {
            val swrve = SwrveSDK.getInstance()
            if (swrve != null) {
                val details = Mockito.mockingDetails(swrve)
                if (details.isSpy || !details.isMock) {
                    swrve.shutdown()
                }
            }
        } catch (e: Exception) {
            SwrveLogger.e("Error shutting down SwrveSDK instance", e)
        }

        try {
            removeSingleton(SwrveSDKBase::class.java, "instance")
        } catch (e: Exception) {
            SwrveLogger.e("Error removing singleton instance", e)
        }

        try {
            LocalStorageTestUtils.closeSQLiteOpenHelperInstance()
        } catch (e: Exception) {
            SwrveLogger.e("Error closing SQLite helper", e)
        }
        SwrveLogger.i("SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance() finish")
    }

    @JvmStatic
    @Throws(Exception::class)
    fun removeSwrveSDKSingletonInstance() {
        removeSingleton(SwrveSDKBase::class.java, "instance")
    }

    @JvmStatic
    @JvmOverloads
    @Throws(Exception::class)
    fun removeSingleton(clazz: Class<*>, fieldName: String, value: Any? = null) {
        val instance = clazz.getDeclaredField(fieldName)
        instance.isAccessible = true
        instance[null] = value
    }

    @JvmStatic
    @Throws(Exception::class)
    fun setSDKInstance(instance: ISwrveBase<*, *>?) {
        val hack = SwrveSDKBase::class.java.getDeclaredField("instance")
        hack.isAccessible = true
        hack[null] = instance
    }

    @JvmStatic
    fun getAssetAsText(context: Context, assetName: String): String {
        var `in`: InputStream? = null
        var result: String? = null
        try {
            val resource = context.classLoader.getResource(assetName)
            assertNotNull(resource)
            `in` = resource.openStream()
            assertNotNull(`in`)
            val s = Scanner(`in`).useDelimiter("\\A")
            result = if (s.hasNext()) s.next() else ""
            assertFalse(result!!.length == 0)
        } catch (ex: IOException) {
            SwrveLogger.e("Error getting asset as text:%s", ex, assetName)
            Assert.fail("Error getting asset as text:" + assetName + " ex:" + ex.message)
        } finally {
            if (`in` != null) {
                try {
                    `in`.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return result!!
    }

    /**
     * Loads the campaigns from json file into swrve sdk
     * @param swrve sdk
     * @param campaignFileName the cfile name in assets folder containing the campaign json
     * @param assets an array of downloaded assets so campaign is eligible (font or image)
     * @throws Exception If there's an error
     */
    @JvmStatic
    @Throws(Exception::class)
    fun loadCampaignsFromFile(context: Context, swrve: Swrve, campaignFileName: String, vararg assets: String?) {
        loadCampaignsFromFile(context, swrve, campaignFileName, false, false, *assets)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun loadCampaignsFromFile(context: Context, swrve: Swrve, campaignFileName: String, loadPreviousCampaignState: Boolean, saveToCache: Boolean, vararg assets: String?) {
        val json = getAssetAsText(context, campaignFileName)
        val jsonObject = JSONObject(json)
        swrve.loadCampaignsFromJSON(swrve.userId, jsonObject, swrve.campaignsState, loadPreviousCampaignState)
        assets?.let {
            if (assets.size > 0) {
                val assetsOnDisk: MutableSet<String> = HashSet()
                for (asset in assets) {
                    asset?.let {
                        assetsOnDisk.add(asset)
                        writeFileToCache(ApplicationProvider.getApplicationContext<Context>().cacheDir, asset)
                    }
                }
                (swrve.swrveAssetsManager as SwrveAssetsManagerImp).assetsOnDisk = assetsOnDisk
            }
        }
        if (saveToCache) {
            swrve.saveCampaignsInCache(jsonObject)
        }
    }

    @JvmStatic
    fun writeResourceFileToCache(resourceName: String, cacheName: String) {
        var inputStream: InputStream? = null
        try {
            val resource = ApplicationProvider.getApplicationContext<Context>().classLoader.getResource(resourceName)
            assertNotNull(resource)
            inputStream = resource.openStream()
            assertNotNull(inputStream)

            val stream = ByteArrayOutputStream()
            val buffer = ByteArray(2048)
            var bytesRead: Int
            while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                stream.write(buffer, 0, bytesRead)
            }
            val fileContents = stream.toByteArray()

            val dst = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, cacheName)
            val fileStream = FileOutputStream(dst)
            fileStream.write(fileContents) // Save to file
            fileStream.close()
        } catch (ex: Exception) {
            SwrveLogger.e("Error writing resource to cache:%s", ex, resourceName)
            Assert.fail("Error getting resource:" + resourceName + " ex:" + ex.message)
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @JvmStatic
    fun takeScreenshot(view: SwrveMessageView): String {
        view.layout(0, 0, view.format.size.x, view.format.size.y)
        return takeScreenshot(view as View)
    }

    fun takeScreenshot(view: View): String {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) bgDrawable.draw(canvas)
        else canvas.drawColor(Color.WHITE)
        view.draw(canvas)

        // Convert to base64 bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos) //bm is the bitmap object
        bitmap.recycle()
        val b = baos.toByteArray()

        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    @JvmStatic
    val testSwrveCampaignManager: ISwrveCampaignManager
        get() = object : ISwrveCampaignManager {
            override fun getNow(): Date {
                return Date()
            }

            override fun getInitialisedTime(): Date {
                return Date()
            }

            override fun getCacheDir(): File {
                return File("")
            }

            override fun getAssetsOnDisk(): Set<String> {
                val set: MutableSet<String> =
                    HashSet()
                set.add("asset1")
                return set
            }

            override fun getConfig(): SwrveConfigBase {
                return SwrveConfig()
            }
        }

    fun writeFileToCache(cache: File?, filename: String) {
        val file = File(cache, filename)
        var fileWriter: FileWriter? = null
        try {
            fileWriter = FileWriter(file, false)
            fileWriter.write("empty")
            fileWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fileWriter?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun disableAssetsManager(swrve: Swrve) {
        val swrveAssetsManagerSpy = Mockito.spy(swrve.swrveAssetsManager)
        doNothing().`when`(swrveAssetsManagerSpy).downloadAssets(anySet(), any(SwrveAssetsCompleteCallback::class.java))
        swrve.swrveAssetsManager = swrveAssetsManagerSpy
    }

    @JvmStatic
    fun disableBeforeSendDeviceInfo(swrveReal: Swrve?, swrveSpy: Swrve) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        application.unregisterActivityLifecycleCallbacks(swrveReal)
        swrveSpy.registerActivityLifecycleCallbacks()
        doNothing().`when`(swrveSpy).beforeSendDeviceInfo(any(Context::class.java))
    }

    @JvmStatic
    fun disableRestClientExecutor(swrveSpy: Swrve) {
        doReturn(true).`when`(swrveSpy).restClientExecutorExecute(any(Runnable::class.java)) // disable rest
    }

    /*
     * Using Mockito to verify events are sent to the queueEvent method.
     * Call Mockito.reset(swrveSpy) before doing the test but can't be guaranteed that this is the only event
     * captured to the queueEvent method, so a search is done.
     */
    @JvmStatic
    fun assertQueueEvent(swrveSpy: Swrve, expectedEventType: String, expectedParameters: Map<String, Any>?, expectedPayload: Map<String, Any>?) {
        val userIdCaptor = argumentCaptor<String>()
        val eventTypeCaptor = argumentCaptor<String>()
        val parametersCaptor = argumentCaptor<MutableMap<String, Any>>()
        val payloadCaptor = argumentCaptor<MutableMap<String, String>>()
        val triggerEventListenerCaptor = argumentCaptor<Boolean>()
        verify(swrveSpy, Mockito.atLeastOnce()).queueEvent(
            userIdCaptor.capture(),
            eventTypeCaptor.capture(),
            parametersCaptor.capture(),
            payloadCaptor.capture(),
            triggerEventListenerCaptor.capture()
        )

        val userIds = userIdCaptor.allValues
        val capturedEventTypes = eventTypeCaptor.allValues
        val capturedParameters = parametersCaptor.allValues
        val capturedPayload = payloadCaptor.allValues

        // assert userId
        assertTrue("Asserting userId: " + swrveSpy.userId + " in userIds:" + userIds, userIds.contains(swrveSpy.userId))

        // assert event type
        assertTrue("Asserting eventType: $expectedEventType in capturedEventTypes:$capturedEventTypes", capturedEventTypes.contains(expectedEventType))

        // find the indices at which they were found so rest of parameters and payloads can be eliminated.
        val matchedIndices: MutableList<Int> = ArrayList()
        for (i in capturedEventTypes.indices) {
            val capturedEventType = capturedEventTypes[i]
            if (capturedEventType == expectedEventType) {
                matchedIndices.add(i)
            }
        }

        // assert parameters
        if (expectedParameters != null && expectedParameters.size > 0) {
            val hasMatches = filterMatchesFromListMap(matchedIndices, expectedParameters, capturedParameters)
            assertTrue("Asserting expectedParameters:$expectedParameters in:$capturedParameters", hasMatches)
        }

        // assert payload
        if (expectedPayload != null && expectedPayload.size > 0) {
            val hasMatches = filterMatchesFromListMap(matchedIndices, expectedPayload, capturedPayload)
            assertTrue("Asserting expectedPayload:$expectedPayload in:$capturedPayload", hasMatches)
        }

        if (matchedIndices.size == 0) {
            Assert.fail("Event not queued. eventType:$expectedEventType\nparameters:$expectedParameters\npayload:$expectedPayload")
        }
    }

    private fun filterMatchesFromListMap(matchedIndices: MutableList<Int>, expected: Map<String, Any>, actual: List<Map<*, *>>): Boolean {
        val indicesToRemove: MutableList<Int> = ArrayList()
        for (index in matchedIndices) { // only iterate through the known matched indices
            val capturedParameterMap = actual[index]
            for (i in actual.indices) {
                var matchesAll = true
                for ((expectedKey, expectedValue) in expected) { // iterate through expected results
                    if (!capturedParameterMap.containsKey(expectedKey)) {
                        matchesAll = false
                        break
                    } else if (capturedParameterMap[expectedKey].toString() != expectedValue.toString()) {
                        matchesAll = false
                        break
                    }
                }
                if (!matchesAll) {
                    indicesToRemove.add(index)
                    break
                }
            }
        }
        matchedIndices.removeAll(indicesToRemove)
        return matchedIndices.size > 0
    }

    @JvmStatic
    fun createFakeRestClient(responseCode: Int): IRESTClient {
        return object : IRESTClient {
            override fun get(endpoint: String, callback: IRESTResponseListener) {
                // unused
            }

            override fun get(endpoint: String, params: Map<String, String>, callback: IRESTResponseListener) {
                // unused
            }

            override fun post(endpoint: String, encodedBody: String, callback: IRESTResponseListener) {
                val response = RESTResponse(responseCode, responseCode.toString(), createFakeResponseHeaders(responseCode))
                callback.onResponse(response)
            }

            override fun post(endpoint: String, encodedBody: String, callback: IRESTResponseListener, contentType: String) {
                val response = RESTResponse(responseCode, responseCode.toString(), createFakeResponseHeaders(responseCode))
                callback.onResponse(response)
            }
        }
    }

    private fun createFakeResponseHeaders(responseCode: Int): Map<String?, List<String>> {
        val map: MutableMap<String?, List<String>> = HashMap()
        addHeadersToMap(map, null, "HTTP/1.1 $responseCode")
        addHeadersToMap(map, "Connection", "Close")
        addHeadersToMap(map, "Content-Type", "text/plain")
        addHeadersToMap(map, "X-Android-Received-Millis", "1565687743788")
        addHeadersToMap(map, "X-Android-Response-Source", "NETWORK")
        addHeadersToMap(map, "X-Android-Selected-Protocol", "http/1.1")
        addHeadersToMap(map, "X-Android-Sent-Millis", "1565687743648")
        return map
    }

    private fun addHeadersToMap(map: MutableMap<String?, List<String>>, key: String?, value: String) {
        val headers: MutableList<String> = ArrayList()
        headers.add(value)
        map[key] = headers
    }

    @JvmStatic
    fun parseDate(date: String): Date? {
        var parsed = Date()
        try {
            val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")
            parsed = simpleDateFormat.parse(date)
        } catch (ex: Exception) {
            SwrveLogger.e("Error parseDate:$date", ex)
        }
        return parsed
    }

    @JvmStatic
    fun setRestClientWithGetResponse(swrve: Swrve, response: String?) {
        swrve.restClient = object : IRESTClient {
            override fun get(endpoint: String, callback: IRESTResponseListener) {
                callback.onResponse(RESTResponse(200, response, null))
            }

            @Throws(UnsupportedEncodingException::class)
            override fun get(endpoint: String, params: Map<String, String>, callback: IRESTResponseListener) {
                callback.onResponse(RESTResponse(200, response, null))
            }

            override fun post(endpoint: String, encodedBody: String, callback: IRESTResponseListener) {
            }

            override fun post(endpoint: String, encodedBody: String, callback: IRESTResponseListener, contentType: String) {
            }
        }
    }

    fun onCreate(swrve: ISwrve, activity: Activity?) {
        (swrve as Swrve).onCreate(activity)
    }

    @JvmStatic
    fun assertGenericEvent(
        eventJson: String?,
        expectedContextId: String?,
        expectedCampaignType: String?,
        expectedActionType: String?,
        expectedPayload: Map<String, Any?>?
    ) {
        // eg: {"type":"generic_campaign_event","time":1499179867473,"seqnum":1,"actionType":"button_click","campaignType":"push","contextId":"0","id":"4567","payload":{"buttonText":"btn1"}}
        val gson = GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create()
        val type = object : TypeToken<Map<String?, Any?>?>() {
        }.type
        val event = gson.fromJson<Map<String, Any>>(eventJson, type)
        assertTrue(event.containsKey("type"))
        assertEquals(ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN, event[ISwrveCommon.EVENT_TYPE_KEY])
        assertTrue(event.containsKey(ISwrveCommon.EVENT_ID_KEY))
        if (SwrveHelper.isNotNullOrEmpty(expectedContextId)) { 
            assertEquals(expectedContextId, event[ISwrveCommon.GENERIC_EVENT_CONTEXT_ID_KEY]) 
        }
        assertEquals(expectedCampaignType, event[ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_KEY])
        assertEquals(expectedActionType, event[ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_KEY])
        assertTrue(event.containsKey("time"))
        assertTrue(event.containsKey("seqnum"))

        if (expectedPayload != null && expectedPayload.size > 0) {
            assertTrue(event.containsKey(ISwrveCommon.EVENT_PAYLOAD_KEY))
            assertTrue(event.containsKey(ISwrveCommon.EVENT_PAYLOAD_KEY))
            val actualPayload: Map<*, *>? = event[ISwrveCommon.EVENT_PAYLOAD_KEY] as Map<*, *>?
            assertEquals(expectedPayload, actualPayload)
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun createSpyInstance(): Swrve {
        return createSpyInstance(SwrveConfig())
    }

    @JvmStatic
    @Throws(Exception::class)
    fun createSpyInstance(config: SwrveConfig?): Swrve {
        val swrveReal = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config) as Swrve
        flushLifecycleExecutorQueue(swrveReal) // wait until swrve instance is fully created before getting a mockito spy.
        val swrveSpy = Mockito.spy(swrveReal)
        disableBeforeSendDeviceInfo(swrveReal, swrveSpy) // disable token registration
        setSDKInstance(swrveSpy)
        disableAssetsManager(swrveSpy)
        doReturn(true).`when`(swrveSpy).restClientExecutorExecute(any(Runnable::class.java)) // disable rest
        doNothing().`when`(swrveSpy).checkForCampaignAndResourcesUpdates()
        return swrveSpy
    }

    @JvmStatic
    fun initSwrve(swrve: Swrve, activity: Activity?) {
        swrve.init(activity)
    }

    @JvmStatic
    fun disableSwrveBackgroundEventSender(swrveSpy: Swrve) {
        val backgroundEventSenderMock = Mockito.mock(SwrveBackgroundEventSender::class.java)
        doNothing().`when`(backgroundEventSenderMock).send(anyString(), anyList())
        doReturn(backgroundEventSenderMock).`when`(swrveSpy).getSwrveBackgroundEventSender(any(Context::class.java))
    }

    @JvmStatic
    fun runSingleThreaded(swrveSpy: Swrve) {
        val answer = Answer { invocation: InvocationOnMock ->
            val runnable = invocation.arguments[0] as Runnable
            runnable.run()
            true
        }
        // execute the rest and storage calls on same threads
        doAnswer(answer).`when`(swrveSpy).restClientExecutorExecute(any(Runnable::class.java))
        doAnswer(answer).`when`(swrveSpy).storageExecutorExecute(any(Runnable::class.java))
        doAnswer(answer).`when`(swrveSpy).lifecycleExecutorExecute(any(Runnable::class.java))
    }

    @JvmStatic
    fun flushLifecycleExecutorQueue(swrve: Swrve) {
        // flush the strictmode penaltyListener on the lifecycleExecutorQueue by adding a runnable and waiting.
        val callback = AtomicBoolean(false)
        swrve.lifecycleExecutorExecute {
            callback.set(true)
        }
        Awaitility.await().untilTrue(callback)
    }

    @JvmStatic
    fun setupLocalSllSocketFactory(server: MockWebServer): SSLSocketFactory {
        //sdk v9.1.0 enforced https connections, need to add https support to MockWebServer
        val handshakeCertificates = localhost()
        val socketFactory = handshakeCertificates.sslSocketFactory()
        server.useHttps(socketFactory, false)
        return socketFactory
    }

    @JvmStatic
    fun mockCommonSocketFactory(socketFactory: SSLSocketFactory?): SwrveSSLSocketFactoryConfig {
        val swrveCommonSpy = Mockito.mock(ISwrveCommon::class.java)
        val sslSocketFactoryConfig = Mockito.mock(SwrveSSLSocketFactoryConfig::class.java)
        doReturn(sslSocketFactoryConfig).`when`(swrveCommonSpy).sslSocketFactoryConfig
        doReturn(socketFactory).`when`(sslSocketFactoryConfig).getFactory(Mockito.anyString())
        SwrveCommon.setSwrveCommon(swrveCommonSpy)
        return sslSocketFactoryConfig
    }

    @JvmStatic
    fun copyFileFromAssetsToCache(context: Context, swrve: Swrve, filename: String) {
        try {
            val resourceUrl = context.classLoader.getResource(filename)
            val inputStream = resourceUrl.openStream()

            val file = File(context.cacheDir, filename)
            val out: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var len: Int
            while ((inputStream.read(buffer).also { len = it }) != -1) {
                out.write(buffer, 0, len)
            }
            out.close()
            inputStream.close()
            (swrve.swrveAssetsManager as SwrveAssetsManagerImp).assetsOnDisk.add(filename)
        } catch (e: Exception) {
            SwrveLogger.e("Exception", e)
        }
    }

    @JvmStatic
    fun getSwrveMessageView(activity: SwrveInAppMessageActivity): SwrveMessageView? {
        val expectedPageId = activity.currentPageId

        for (fragment in activity.supportFragmentManager.fragments) {
            if (fragment is SwrveInAppMessageFragment &&
                fragment.view is SwrveMessageView &&
                fragment.arguments != null
            ) {
                val pageId = fragment.arguments!!.getLong("PAGE_ID", -1)
                if (pageId == expectedPageId) {
                    return fragment.view as SwrveMessageView
                }
            }
        }
        return null
    }
}
