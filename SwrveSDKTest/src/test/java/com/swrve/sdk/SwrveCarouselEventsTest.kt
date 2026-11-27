package com.swrve.sdk

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH
import com.swrve.sdk.test.MainActivity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import java.util.UUID
import java.util.concurrent.Executor

class SwrveCarouselEventsTest : SwrveBaseTest() {

    private val notificationConfig = SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, null)
        .activityClass(MainActivity::class.java)
        .build()

    private lateinit var swrveSpy: Swrve

    private val carouselJson = """
        {
          "version": 1,
          "title": "push title",
          "notification_id": 123,
          "carousel": [
            {"title": "item 0", "image_url": "https://cdn.example.com/0.png", "action_type": "open_url", "action": "https://www.example.com/0"},
            {"title": "item 1", "image_url": "https://cdn.example.com/1.png", "action_type": "open_app", "action": ""},
            {"title": "item 2", "image_url": "https://cdn.example.com/2.png"}
          ],
          "buttons": [
            {"title": "Previous", "action_type": "previous"},
            {"title": "Next", "action_type": "next"}
          ]
        }
    """.trimIndent()

    @Before
    override fun setUp() {
        super.setUp()
        shadowApplication!!.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val swrveReal = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey") as Swrve
        swrveSpy = spy(swrveReal)
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy)
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy)
        SwrveTestUtils.disableSwrveBackgroundEventSender(swrveSpy)
        SwrveTestUtils.setSDKInstance(swrveSpy)
        doNothing().`when`(swrveSpy).checkForCampaignAndResourcesUpdates()
        doReturn(true).`when`(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable::class.java))
        doNothing().`when`(swrveSpy).sendEventsInBackground(any(), anyString(), any())
        SwrveCommon.setSwrveCommon(swrveSpy)
        doReturn(notificationConfig).`when`(swrveSpy).notificationConfig
    }

    private fun carouselBundle(json: String = carouselJson): Bundle {
        val bundle = Bundle()
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1")
        bundle.putString(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, UUID.randomUUID().toString())
        bundle.putString(SwrveNotificationConstants.PLATFORM_KEY, "mg")
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "push text")
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json)
        return bundle
    }

    private fun newBuilder(): SwrveNotificationBuilder {
        val builder = spy(SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig))
        val mediaManager = spy(NotificationMediaManager(mActivity))
        doNothing().`when`(mediaManager).downloadImage(anyOrNull())
        doReturn(arrayListOf<String>()).`when`(mediaManager).downloadImages(any(), anyInt())
        doReturn(Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)).`when`(mediaManager).getCachedImage(anyString())
        builder.mediaManager = mediaManager
        return builder
    }

    private inner class TestNotificationCarouselReceiver : SwrveNotificationCarouselReceiver() {
        override fun getExecutor(): Executor = Executor { it.run() }
        override fun getSwrveNotificationBuilder(context: Context, notificationConfig: SwrveNotificationConfig) = newBuilder()
    }

    private fun processPush(bundle: Bundle) {
        val pushManager = spy(SwrvePushManagerImp(mActivity))
        doReturn(newBuilder()).`when`(pushManager).getSwrveNotificationBuilder()
        pushManager.processNotification(bundle, "1")
    }

    private fun firstPost(json: String = carouselJson, bundle: Bundle = carouselBundle(json)): Notification {
        processPush(bundle)
        return postedNotification()
    }

    private fun filteringConfig() = SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, null)
        .activityClass(MainActivity::class.java)
        .notificationFilter { _, _, _, _ -> null }
        .build()

    private fun notificationManager() =
        ApplicationProvider.getApplicationContext<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun postedNotification(): Notification = shadowOf(notificationManager()).getNotification(123)

    private fun savedIntent(notification: Notification, actionIndex: Int): Intent = shadowOf(notification.actions[actionIndex].actionIntent).savedIntent

    private fun pressNext(notification: Notification) =
        TestNotificationCarouselReceiver().onReceive(ApplicationProvider.getApplicationContext(), savedIntent(notification, 2))

    private fun pressPrevious(notification: Notification) =
        TestNotificationCarouselReceiver().onReceive(ApplicationProvider.getApplicationContext(), savedIntent(notification, 0))

    // every event sent so far, which can be none
    private fun capturedEvents(): List<JSONObject> {
        val captor = argumentCaptor<ArrayList<String>>()
        verify(swrveSpy, Mockito.atLeast(0)).sendEventsInBackground(any(), anyString(), captor.capture())
        return captor.allValues.flatten().map { JSONObject(it) }
    }

    private fun sentEvents(): List<JSONObject> = capturedEvents().also { assertTrue("no events were sent", it.isNotEmpty()) }

    private fun slideViews(): List<JSONObject> = capturedEvents().filter { it.optString("actionType") == "slide_view" }

    private fun slideClicks(): List<JSONObject> = capturedEvents().filter { it.optString("actionType") == "slide_click" }

    private fun engagedEvents(): List<JSONObject> = capturedEvents().filter { it.optString("name").endsWith(".engaged") }

    // A push with a deeplink of its own, so tests can check an item's events never report it
    private fun carouselBundleWithPushDeeplink() = carouselBundle().apply {
        putString(SwrveNotificationConstants.DEEPLINK_KEY, "swrve://deeplink/push")
    }

    private fun tap(notification: Notification): JSONObject {
        val intent = shadowOf(notification.contentIntent).savedIntent
        SwrveNotificationEngage(mActivity).processIntent(intent)
        return sentEvents().last { it.optString("name").endsWith(".engaged") }
    }

    @Test
    fun testTapOnOpenUrlItemReportsItemDeeplinkAndSlide() {
        val engaged = tap(firstPost())
        val payload = engaged.getJSONObject("payload")

        assertEquals("Swrve.Messages.Push-1.engaged", engaged.getString("name"))
        assertEquals(true, payload.get("carousel"))
        assertEquals(0, payload.get("slide"))
        assertEquals("https://www.example.com/0", payload.getString("deeplink"))
    }

    @Test
    fun testTapOnOpenAppItemReportsSlideWithoutDeeplink() {
        pressNext(firstPost())
        val engaged = tap(postedNotification())
        val payload = engaged.getJSONObject("payload")

        assertEquals(true, payload.get("carousel"))
        assertEquals(1, payload.get("slide"))
        assertFalse(payload.has("deeplink"))
    }

    @Test
    fun testTapOnItemWithoutActionTypeOpensAppWithoutDeeplink() {
        pressPrevious(firstPost()) // wraps to item 2
        val payload = tap(postedNotification()).getJSONObject("payload")

        assertEquals(2, payload.get("slide"))
        assertFalse(payload.has("deeplink"))
    }

    @Test
    fun testTapOnOpenUrlItemSendsSlideClick() {
        tap(firstPost(bundle = carouselBundleWithPushDeeplink()))

        val clicks = slideClicks()
        assertEquals(1, clicks.size)
        assertEquals(1, engagedEvents().size)
        val event = clicks[0]
        assertEquals("generic_campaign_event", event.getString("type"))
        assertEquals("push", event.getString("campaignType"))
        assertEquals("1", event.getString("id"))
        assertEquals("0", event.getString("contextId"))
        val payload = event.getJSONObject("payload")
        assertEquals(true, payload.get("carousel"))
        assertEquals(0, payload.get("slide"))
        assertEquals("mg", payload.getString("platform"))
        assertEquals("https://www.example.com/0", payload.getString("deeplink"))
    }

    @Test
    fun testTapOnOpenAppItemSendsSlideClickWithoutDeeplink() {
        pressNext(firstPost(bundle = carouselBundleWithPushDeeplink()))
        tap(postedNotification())

        val clicks = slideClicks()
        assertEquals(listOf("1"), clicks.map { it.getString("contextId") })
        val payload = clicks[0].getJSONObject("payload")
        assertEquals(1, payload.get("slide"))
        assertFalse(payload.has("deeplink"))
    }

    @Test
    fun testNavigatingSendsNoSlideClick() {
        pressNext(firstPost())
        pressPrevious(postedNotification())

        assertTrue(slideClicks().isEmpty())
    }

    @Test
    fun testSlideViewReportsTheItemsDeeplinkNotThePushs() {
        pressNext(firstPost(bundle = carouselBundleWithPushDeeplink()))

        val views = slideViews()
        assertEquals("https://www.example.com/0", views[0].getJSONObject("payload").getString("deeplink"))
        assertFalse(views[1].getJSONObject("payload").has("deeplink"))
    }

    @Test
    fun testFirstItemIsReportedOnArrival() {
        firstPost()

        val views = slideViews()
        assertEquals(listOf("0"), views.map { it.getString("contextId") })
        val event = views[0]
        assertEquals("generic_campaign_event", event.getString("type"))
        assertEquals("slide_view", event.getString("actionType"))
        assertEquals("push", event.getString("campaignType"))
        assertEquals("1", event.getString("id"))
        val payload = event.getJSONObject("payload")
        assertEquals(true, payload.get("carousel"))
        assertEquals(0, payload.get("slide"))
        assertEquals("mg", payload.getString("platform"))
    }

    @Test
    fun testNoSlideViewWhenFilterSuppressesArrival() {
        doReturn(filteringConfig()).`when`(swrveSpy).notificationConfig
        processPush(carouselBundle())

        assertTrue(shadowOf(notificationManager()).allNotifications.isEmpty())
        assertTrue(slideViews().isEmpty())
    }

    @Test
    fun testNonCarouselPushSendsNoSlideViewOnArrival() {
        processPush(carouselBundle("""{"version":1,"title":"push title","notification_id":123}"""))

        assertEquals(1, shadowOf(notificationManager()).allNotifications.size)
        assertTrue(slideViews().isEmpty())
    }

    @Test
    fun testCarouselWithTooNewVersionSendsNoSlideViewOnArrival() {
        processPush(carouselBundle(carouselJson.replace("\"version\": 1,", "\"version\": 2,")))

        assertEquals(1, shadowOf(notificationManager()).allNotifications.size)
        assertTrue(slideViews().isEmpty())
    }

    @Test
    fun testFirstNextReportsOnlyTargetItem() {
        pressNext(firstPost())

        val views = slideViews()
        assertEquals(listOf("0", "1"), views.map { it.getString("contextId") })
        assertEquals(1, views[1].getJSONObject("payload").get("slide"))
    }

    @Test
    fun testFirstPreviousReportsOnlyLastItem() {
        pressPrevious(firstPost())

        assertEquals(listOf("0", "2"), slideViews().map { it.getString("contextId") })
    }

    @Test
    fun testEachItemIsReportedOncePerNotification() {
        pressNext(firstPost())        // 0 on arrival, then 1
        pressNext(postedNotification())  // 2
        pressNext(postedNotification())  // back to 0, already sent
        pressPrevious(postedNotification()) // 2, already sent

        assertEquals(listOf("0", "1", "2"), slideViews().map { it.getString("contextId") })
    }

    @Test
    fun testQuickSecondTapOnPreviousUpdateDoesNotRepeatSlideViews() {
        val first = firstPost()
        pressNext(first)
        pressNext(first)

        assertEquals(listOf("0", "1"), slideViews().map { it.getString("contextId") })
    }

    @Test
    fun testQuickSecondTapWithoutSidDoesNotRepeatSlideViews() {
        val first = firstPost(bundle = deliveryWithoutSid("m1"))
        pressNext(first)
        pressNext(first)

        assertEquals(listOf("0", "1"), slideViews().map { it.getString("contextId") })
    }

    @Test
    fun testNewDeliveryWithSameNotificationIdAndNoSidReportsAgain() {
        pressNext(firstPost(bundle = deliveryWithoutSid("m1")))
        pressNext(firstPost(bundle = deliveryWithoutSid("m2")))

        assertEquals(listOf("0", "1", "0", "1"), slideViews().map { it.getString("contextId") })
    }

    @Test
    fun testRecordForNotificationNoLongerShowingIsDropped() {
        val first = firstPost()
        pressNext(first)
        assertEquals(setOf(123), recordedNotificationIds())

        notificationManager().cancel(123)
        pressNext(first)
        assertTrue(recordedNotificationIds().isEmpty())
    }

    @Test
    fun testNoSlideViewWhenFilterSuppressesUpdate() {
        val first = firstPost()
        doReturn(filteringConfig()).`when`(swrveSpy).notificationConfig
        pressNext(first)

        assertEquals(listOf("0"), slideViews().map { it.getString("contextId") })
    }

    @Test
    fun testNoSlideViewWhenNotificationNoLongerShowing() {
        val first = firstPost()
        notificationManager().cancel(123)
        pressNext(first)

        assertEquals(listOf("0"), slideViews().map { it.getString("contextId") })
    }

    @Test
    fun testProxyOffTapOnInAppDeeplinkItemOpensItDirectly() {
        doReturn(proxyOffConfig()).`when`(swrveSpy).notificationConfig
        val intent = shadowOf(firstPost(carouselJson.replace("https://www.example.com/0", "swrve://deeplink/item0")).contentIntent).savedIntent

        assertEquals("swrve://deeplink/item0", intent.dataString)
        assertTrue(intent.getBooleanExtra(SwrveNotificationEngage.DO_NOT_OPEN_INTENT, false))
        assertEquals(0, intent.getIntExtra(SwrveNotificationConstants.CAROUSEL_ITEM_INDEX_KEY, -1))
    }

    @Test
    fun testProxyOffTapOnItemSendsSlideClick() {
        doReturn(proxyOffConfig()).`when`(swrveSpy).notificationConfig
        val intent = shadowOf(firstPost(carouselJson.replace("https://www.example.com/0", "swrve://deeplink/item0")).contentIntent).savedIntent
        SwrveNotificationEngage(mActivity).processIntent(intent)

        val clicks = slideClicks()
        assertEquals(listOf("0"), clicks.map { it.getString("contextId") })
        assertEquals("swrve://deeplink/item0", clicks[0].getJSONObject("payload").getString("deeplink"))
    }

    @Test
    fun testProxyOffTapOnExternalUrlItemFallsBackToProxy() {
        doReturn(proxyOffConfig()).`when`(swrveSpy).notificationConfig
        val intent = shadowOf(firstPost().contentIntent).savedIntent

        assertEquals(SwrveNotificationEngageActivity::class.java.name, intent.component?.className)
        assertFalse(intent.hasExtra(SwrveNotificationEngage.DO_NOT_OPEN_INTENT))
        assertEquals("https://www.example.com/0", intent.getStringExtra(SwrveNotificationConstants.CAROUSEL_ITEM_ACTION_URL_KEY))
    }

    @Test
    fun testProxyOffTapOnOpenAppItemOpensAppNotPushDeeplink() {
        doReturn(proxyOffConfig()).`when`(swrveSpy).notificationConfig
        val bundle = carouselBundle()
        bundle.putString(SwrveNotificationConstants.DEEPLINK_KEY, "swrve://deeplink/push")
        pressNext(firstPost(bundle = bundle))
        val intent = shadowOf(postedNotification().contentIntent).savedIntent

        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertNull(intent.data)
        assertTrue(intent.getBooleanExtra(SwrveNotificationEngage.DO_NOT_OPEN_INTENT, false))
    }

    private fun deliveryWithoutSid(providerMessageId: String) = carouselBundle().apply {
        remove(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY)
        putString(ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID, providerMessageId)
    }

    private fun recordedNotificationIds(): Set<Int> =
        ReflectionHelpers.getStaticField<Map<Int, Any>>(SwrveNotificationCarouselReceiver::class.java, "sentSlideViewsInProcess").keys

    private fun proxyOffConfig() = SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, null)
        .activityClass(MainActivity::class.java)
        .useEngagementProxy(false)
        .build()

    @Test
    fun testNonCarouselPushTapHasNoCarouselFields() {
        val json = """{"version":1,"title":"push title","notification_id":123}"""
        val bundle = carouselBundle(json)
        val notification = newBuilder().build("push text", bundle, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null).build()
        val intent = shadowOf(notification.contentIntent).savedIntent
        assertFalse(intent.hasExtra(SwrveNotificationConstants.CAROUSEL_ITEM_INDEX_KEY))

        reset(swrveSpy)
        doNothing().`when`(swrveSpy).sendEventsInBackground(any(), anyString(), any())
        SwrveNotificationEngage(mActivity).processIntent(intent)
        val engaged = sentEvents().last()
        assertTrue(!engaged.has("payload") || !engaged.getJSONObject("payload").has("carousel"))
        assertNull(engaged.optJSONObject("payload")?.opt("slide"))
        assertTrue(slideClicks().isEmpty())
    }
}
