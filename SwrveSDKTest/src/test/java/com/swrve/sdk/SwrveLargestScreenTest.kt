package com.swrve.sdk

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.graphics.Rect
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import com.swrve.sdk.ISwrveCommon.CACHE_DEVICE_PROP_KEY
import com.swrve.sdk.ISwrveCommon.CACHE_LARGEST_SCREEN
import com.swrve.sdk.localstorage.InMemoryLocalStorage
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage
import com.swrve.sdk.rest.IRESTClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.robolectric.RuntimeEnvironment

class SwrveLargestScreenTest : SwrveBaseTest() {

    private lateinit var swrveSpy: Swrve
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    override fun setUp() {
        super.setUp()
        swrveSpy = SwrveTestUtils.createSpyInstance()
        SwrveTestUtils.initSwrve(swrveSpy, mActivity)
    }

    private fun screen(width: Int, height: Int, dpi: Float = 420f) = SwrveLargestScreen(width, height, dpi, dpi + 1, dpi + 2)

    @Test
    fun testLargestScreenOnlyGrows() {
        val storage = SwrveMultiLayerLocalStorage(InMemoryLocalStorage())
        val phone = screen(1080, 2424)
        assertEquals(phone, SwrveLargestScreen.update(storage, phone))
        assertEquals(phone, SwrveLargestScreen.update(storage, screen(960, 2142))) // smaller
        assertEquals(phone, SwrveLargestScreen.update(storage, screen(1080, 2448))) // 1% larger

        val foldableInner = screen(2076, 2152, 360f)
        assertEquals(foldableInner, SwrveLargestScreen.update(storage, foldableInner))
        assertEquals(foldableInner, SwrveLargestScreen.update(storage, phone)) // folded again
        assertEquals(foldableInner, SwrveLargestScreen.update(storage, null)) // kept, even if the screen can't be measured
    }

    @Test
    fun testLargestScreenIsNullUntilAScreenIsMeasured() {
        assertNull(SwrveLargestScreen.update(SwrveMultiLayerLocalStorage(InMemoryLocalStorage()), null))
    }

    @Test
    fun testLargestScreenIsStoredForTheDeviceNotTheUser() {
        val storage = SwrveMultiLayerLocalStorage(InMemoryLocalStorage())
        SwrveLargestScreen.update(storage, screen(1080, 2424))
        assertTrue(storage.getCacheEntry(CACHE_DEVICE_PROP_KEY, CACHE_LARGEST_SCREEN)!!.contains("\"width\":1080"))
    }

    @Test
    fun testUnreadableLargestScreenIsReplaced() {
        val storage = SwrveMultiLayerLocalStorage(InMemoryLocalStorage())
        storage.setCacheEntry(CACHE_DEVICE_PROP_KEY, CACHE_LARGEST_SCREEN, "not json")
        val phone = screen(1080, 2424)
        assertEquals(phone, SwrveLargestScreen.update(storage, phone))
    }

    @Test
    fun testMeasureUsesPortraitDimensions() {
        RuntimeEnvironment.setQualifiers("w800dp-h400dp-land")
        val measured = SwrveLargestScreen.measure(context)!!
        assertTrue(measured.width < measured.height)
    }

    @Test
    fun testMeasureUsesTheDisplayNotTheWindow() {
        // A freeform window on a folded 1080x2364 display
        val windowManager = Mockito.spy(context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        Mockito.doReturn(WindowMetrics(Rect(0, 0, 1080, 2364), WindowInsets.CONSUMED)).`when`(windowManager).maximumWindowMetrics
        Mockito.doReturn(WindowMetrics(Rect(270, 688, 811, 1768), WindowInsets.CONSUMED)).`when`(windowManager).currentWindowMetrics
        val windowContext = Mockito.spy(context)
        Mockito.doReturn(windowManager).`when`(windowContext).getSystemService(Context.WINDOW_SERVICE)

        val measured = SwrveLargestScreen.measure(windowContext)!!
        assertEquals(1080, measured.width)
        assertEquals(2364, measured.height)
    }

    @Test
    fun testContentRequestAndDeviceInfoReportTheLargestScreen() {
        RuntimeEnvironment.setQualifiers("w400dp-h800dp")
        val phone = SwrveLargestScreen.measure(context)!!
        assertEquals(phone.width.toString(), swrveSpy.getContentRequestParams(swrveSpy.userId)["device_width"])

        RuntimeEnvironment.setQualifiers("w800dp-h900dp") // unfolded
        val inner = SwrveLargestScreen.measure(context)!!
        assertTrue(inner.width > phone.width)
        assertReported(swrveSpy, inner)

        RuntimeEnvironment.setQualifiers("w400dp-h800dp") // folded again
        assertReported(swrveSpy, inner)
    }

    @Test
    fun testDeeplinkCampaignRequestsReportTheLargestScreen() {
        RuntimeEnvironment.setQualifiers("w400dp-h800dp")
        swrveSpy.initSwrveDeepLinkManager() // created while folded
        val restClient = Mockito.mock(IRESTClient::class.java)
        swrveSpy.swrveDeeplinkManager.setRestClient(restClient)

        RuntimeEnvironment.setQualifiers("w800dp-h900dp") // unfolded
        val inner = SwrveLargestScreen.measure(context)!!
        swrveSpy.initSwrveDeepLinkManager() // as each deeplink request path does first
        swrveSpy.swrveDeeplinkManager.fetchOfflineCampaigns(setOf(1L))

        val params = ArgumentCaptor.forClass(Map::class.java)
        Mockito.verify(restClient).get(anyString(), params.capture() as Map<String, String>?, any())
        assertEquals(inner.width.toString(), params.value["device_width"])
        assertEquals(inner.height.toString(), params.value["device_height"])
    }

    @Test
    fun testDeviceInfoBeforeInitReportsTheStoredLargestScreen() {
        val stored = screen(4000, 6000) // larger than the test device
        SwrveLargestScreen.update(swrveSpy.multiLayerLocalStorage, stored)
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance()

        // A new launch, before an Activity has initialised the sdk and opened its database
        val relaunched = SwrveTestUtils.createSpyInstance()
        val deviceInfo = relaunched._getDeviceInfo()
        assertEquals(stored.width, deviceInfo.getInt("swrve.device_width"))
        assertEquals(stored.height, deviceInfo.getInt("swrve.device_height"))
    }

    @Test
    fun testContentRequestBeforeInitReportsTheStoredLargestScreen() {
        val stored = screen(4000, 6000) // larger than the test device
        SwrveLargestScreen.update(swrveSpy.multiLayerLocalStorage, stored)
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance()

        // A new launch where a content request is built before init, such as for a Geo campaign
        val relaunched = SwrveTestUtils.createSpyInstance()
        val params = relaunched.getContentRequestParams(relaunched.userId)
        assertEquals(stored.width.toString(), params["device_width"])
        assertEquals(stored.height.toString(), params["device_height"])
    }

    @Test
    fun testDeeplinkRequestsEachHaveTheirOwnCampaignId() {
        swrveSpy.initSwrveDeepLinkManager()
        val restClient = Mockito.mock(IRESTClient::class.java)
        swrveSpy.swrveDeeplinkManager.setRestClient(restClient)

        swrveSpy.swrveDeeplinkManager.fetchOfflineCampaigns(linkedSetOf(1L, 2L))

        val params = ArgumentCaptor.forClass(Map::class.java)
        Mockito.verify(restClient, Mockito.times(2)).get(anyString(), params.capture() as Map<String, String>?, any())
        assertEquals(listOf("1", "2"), params.allValues.map { it["in_app_campaign_id"] })
    }

    private fun assertReported(swrve: Swrve, screen: SwrveLargestScreen) {
        val params = swrve.getContentRequestParams(swrve.userId)
        assertEquals(screen.width.toString(), params["device_width"])
        assertEquals(screen.height.toString(), params["device_height"])
        assertEquals(screen.dpi.toString(), params["device_dpi"])

        val deviceInfo = swrve._getDeviceInfo()
        assertEquals(screen.width, deviceInfo.getInt("swrve.device_width"))
        assertEquals(screen.height, deviceInfo.getInt("swrve.device_height"))
    }
}
