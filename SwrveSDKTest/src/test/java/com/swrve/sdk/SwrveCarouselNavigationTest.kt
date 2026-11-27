package com.swrve.sdk

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH
import com.swrve.sdk.test.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.Executor

class SwrveCarouselNavigationTest : SwrveBaseTest() {

    private val notificationConfig = SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, null)
        .activityClass(MainActivity::class.java)
        .build()

    private val carouselJson = """
        {
          "version": 1,
          "title": "push title",
          "notification_id": 123,
          "carousel": [
            {"title": "item 0 title", "body": "item 0 body", "image_url": "https://cdn.example.com/0.png"},
            {"title": "item 1 title", "body": "item 1 body", "image_url": "https://cdn.example.com/1.png"},
            {"title": "item 2 title", "body": "item 2 body", "image_url": "https://cdn.example.com/2.png"}
          ],
          "buttons": [
            {"title": "Précédent", "action_type": "previous"},
            {"title": "Suivant", "action_type": "next"}
          ]
        }
    """.trimIndent()

    @Before
    override fun setUp() {
        super.setUp()
        shadowApplication!!.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val swrveReal = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey") as Swrve
        val swrveSpy = spy(swrveReal)
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy)
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy)
        SwrveTestUtils.disableSwrveBackgroundEventSender(swrveSpy)
        SwrveTestUtils.setSDKInstance(swrveSpy)
        doNothing().`when`(swrveSpy).checkForCampaignAndResourcesUpdates()
        doReturn(true).`when`(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable::class.java))
        SwrveCommon.setSwrveCommon(swrveSpy)
        doReturn(notificationConfig).`when`(swrveSpy).notificationConfig
    }

    private fun carouselBundle(json: String = carouselJson): Bundle {
        val bundle = Bundle()
        bundle.putString(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "1")
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "push text")
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json)
        return bundle
    }

    private var cachedImageHeightRatio = 0f // what the image cache reports for every image

    private fun newBuilder(): SwrveNotificationBuilder {
        val builderSpy = spy(SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig))
        val mediaManagerSpy = spy(NotificationMediaManager(mActivity))
        doNothing().`when`(mediaManagerSpy).downloadImage(anyOrNull())
        doReturn(arrayListOf<String>()).`when`(mediaManagerSpy).downloadImages(any(), anyInt())
        doReturn(Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)).`when`(mediaManagerSpy).getCachedImage(anyString())
        doReturn(cachedImageHeightRatio).`when`(mediaManagerSpy).getImageHeightRatio(anyOrNull(), anyOrNull())
        builderSpy.mediaManager = mediaManagerSpy
        return builderSpy
    }

    private fun firstPost(json: String = carouselJson): Notification {
        val bundle = carouselBundle(json)
        return newBuilder().build(bundle.getString(SwrveNotificationConstants.TEXT_KEY), bundle, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null).build()
    }

    private fun buildItem(itemIndex: Int): Notification {
        return newBuilder().buildCarouselItem(carouselBundle(), GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null, 123, itemIndex).build()
    }

    private fun actionIntent(action: Notification.Action): Intent = shadowOf(action.actionIntent).savedIntent

    private fun targetIndex(action: Notification.Action): Int =
        actionIntent(action).getIntExtra(SwrveNotificationConstants.CAROUSEL_ITEM_INDEX_KEY, -1)

    @Test
    fun testNavigationActionsUseServerTitlesAndOrder() {
        val notification = firstPost()
        assertEquals(3, notification.actions.size)
        assertEquals("Précédent", notification.actions[0].title.toString())
        assertEquals("● ○ ○", notification.actions[1].title.toString())
        assertEquals("Suivant", notification.actions[2].title.toString())
    }

    private val previousButton = """{"title": "Précédent", "action_type": "previous"}"""
    private val nextButton = """{"title": "Suivant", "action_type": "next"}"""

    private fun carouselJson(vararg buttons: String): String =
        carouselJson.substringBefore("\"buttons\"") + "\"buttons\": [" + buttons.joinToString(",") + "]}"

    @Test
    fun testPreviousIsFirstWhicheverOrderThePayloadListsThem() {
        val notification = firstPost(carouselJson(nextButton, previousButton))
        assertEquals(3, notification.actions.size)
        assertEquals("Précédent", notification.actions[0].title.toString())
        assertEquals(2, targetIndex(notification.actions[0]))
        assertEquals("● ○ ○", notification.actions[1].title.toString())
        assertEquals("Suivant", notification.actions[2].title.toString())
        assertEquals(1, targetIndex(notification.actions[2]))
    }

    @Test
    fun testRepeatedButtonsShowOnlyTheFirstOfEach() {
        val otherPrevious = """{"title": "Back", "action_type": "previous"}"""
        val otherNext = """{"title": "Forward", "action_type": "next"}"""
        val notification = firstPost(carouselJson(previousButton, nextButton, otherPrevious, otherNext))
        assertEquals(3, notification.actions.size)
        assertEquals("Précédent", notification.actions[0].title.toString())
        assertEquals("Suivant", notification.actions[2].title.toString())
    }

    @Test
    fun testIndicatorComesBeforeNextWhenThereIsNoPrevious() {
        val notification = firstPost(carouselJson(nextButton))
        assertEquals(2, notification.actions.size)
        assertEquals("● ○ ○", notification.actions[0].title.toString())
        assertEquals("Suivant", notification.actions[1].title.toString())
    }

    @Test
    fun testIndicatorComesAfterPreviousWhenThereIsNoNext() {
        val notification = firstPost(carouselJson(previousButton))
        assertEquals(2, notification.actions.size)
        assertEquals("Précédent", notification.actions[0].title.toString())
        assertEquals("● ○ ○", notification.actions[1].title.toString())
    }

    @Test
    fun testNavigationIntentsTargetReceiverWithPush() {
        val intent = actionIntent(firstPost().actions[2])
        assertEquals(SwrveNotificationCarouselReceiver::class.java.name, intent.component!!.className)
        assertEquals(123, intent.getIntExtra(SwrveNotificationConstants.PUSH_NOTIFICATION_ID, -1))
        assertEquals(GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, intent.getStringExtra(SwrveNotificationConstants.CAMPAIGN_TYPE))
        val pushBundle = intent.getBundleExtra(SwrveNotificationConstants.PUSH_BUNDLE)
        assertNotNull(pushBundle)
        assertEquals("1", pushBundle!!.getString(SwrveNotificationConstants.SWRVE_TRACKING_KEY))
    }

    @Test
    fun testNavigationWrapsAroundAtTheEnds() {
        val first = firstPost()
        assertEquals(2, targetIndex(first.actions[0]))
        assertEquals(1, targetIndex(first.actions[2]))

        val last = buildItem(2)
        assertEquals(1, targetIndex(last.actions[0]))
        assertEquals(0, targetIndex(last.actions[2]))
    }

    @Test
    fun testIndicatorShowsCurrentItemAndGoesToNext() {
        val first = firstPost()
        assertEquals("● ○ ○", first.actions[1].title.toString())
        assertEquals(1, targetIndex(first.actions[1]))

        val last = buildItem(2)
        assertEquals("○ ○ ●", last.actions[1].title.toString())
        assertEquals(0, targetIndex(last.actions[1]))
    }

    @Test
    fun testCarouselDoesNotAlertAgain() {
        assertTrue(firstPost().flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        assertTrue(buildItem(1).flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
    }

    @Test
    fun testFirstPostKeepsPushTextWhenCollapsed() {
        val extras = firstPost().extras
        assertEquals("push title", extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("push text", extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals("item 0 title", extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
    }

    @Test
    fun testRepostShowsItemTextWhenCollapsedAndExpanded() {
        val extras = buildItem(1).extras
        assertEquals("item 1 title", extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("item 1 body", extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals("item 1 title", extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
        assertEquals("item 1 body", extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString())
    }

    @Test
    fun testOutOfRangeIndexShowsFirstItem() {
        val notification = buildItem(7)
        assertEquals("item 0 title", notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
        assertEquals(1, targetIndex(notification.actions[2]))
    }

    @Test
    fun testSingleItemCarouselHasNoNavigation() {
        val json = """{"version":1,"notification_id":123,"carousel":[{"title":"only","image_url":"https://cdn.example.com/0.png"}],"buttons":[{"title":"Prev","action_type":"previous"},{"title":"Next","action_type":"next"}]}"""
        val notification = firstPost(json)
        assertTrue(notification.actions == null || notification.actions.isEmpty())
    }

    private inner class TestNotificationCarouselReceiver : SwrveNotificationCarouselReceiver() {
        override fun getExecutor(): Executor = Executor { it.run() }
        override fun getSwrveNotificationBuilder(context: Context, notificationConfig: SwrveNotificationConfig) = newBuilder()
    }

    private fun newReceiver(): SwrveNotificationCarouselReceiver = TestNotificationCarouselReceiver()

    @Test
    fun testReceiverRepostsSameNotificationAtTargetItem() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val first = firstPost()
        notificationManager.notify(123, first)

        newReceiver().onReceive(context, actionIntent(first.actions[2]))

        val posted = shadowOf(notificationManager).getNotification(123)
        assertNotNull(posted)
        assertEquals("item 1 title", posted.extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
        assertEquals(1, shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun testSidSurvivesRepostForEngagement() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val bundle = carouselBundle()
        bundle.putString(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, "sid-1")
        val first = newBuilder().build(bundle.getString(SwrveNotificationConstants.TEXT_KEY), bundle, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null).build()
        notificationManager.notify(123, first)

        newReceiver().onReceive(context, actionIntent(first.actions[2]))

        val tapIntent = shadowOf(shadowOf(notificationManager).getNotification(123).contentIntent).savedIntent
        assertEquals("sid-1", tapIntent.getStringExtra(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY))
    }

    @Test
    fun testEveryItemKeepsTheShapeWorkedOutWhenPosted() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        cachedImageHeightRatio = 0.5f
        val first = firstPost()
        notificationManager.notify(123, first)
        assertEquals(100, (first.extras.get(Notification.EXTRA_PICTURE) as Bitmap).height)

        cachedImageHeightRatio = 1f // the cache changed since, eg the first image was cleared
        newReceiver().onReceive(context, actionIntent(first.actions[2]))

        val picture = shadowOf(notificationManager).getNotification(123).extras.get(Notification.EXTRA_PICTURE) as Bitmap
        assertEquals(200, picture.width)
        assertEquals(100, picture.height)
    }

    @Test
    fun testShapeIsWorkedOutAgainWhenTheIntentHasNone() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        cachedImageHeightRatio = 0.5f
        val first = firstPost()
        notificationManager.notify(123, first)

        cachedImageHeightRatio = 0.4f
        val nextIntent = Intent(actionIntent(first.actions[2])) // as posted by an SDK that didn't pass the shape
        nextIntent.removeExtra(SwrveNotificationConstants.CAROUSEL_IMAGE_HEIGHT_RATIO_KEY)
        newReceiver().onReceive(context, nextIntent)

        val picture = shadowOf(notificationManager).getNotification(123).extras.get(Notification.EXTRA_PICTURE) as Bitmap
        assertEquals(200, picture.width)
        assertEquals(80, picture.height)
    }

    @Test
    fun testReceiverDoesNotRepostDismissedNotification() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val nextIntent = actionIntent(firstPost().actions[2]) // never posted, as if the user swiped it away

        newReceiver().onReceive(context, nextIntent)

        assertEquals(0, shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun testNavigationDoesNotPrefetchAgain() {
        val builder = newBuilder()
        builder.buildCarouselItem(carouselBundle(), GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null, 123, 1).build()
        verify(builder.mediaManager, never()).downloadImage(anyOrNull())
        verify(builder.mediaManager, never()).downloadImages(any(), anyInt())
        verify(builder.mediaManager).getCachedImage("https://cdn.example.com/1.png")
    }

    @Test
    fun testReceiverIsNotExported() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = context.packageManager.getReceiverInfo(ComponentName(context, SwrveNotificationCarouselReceiver::class.java), 0)
        assertFalse(info.exported)
    }

    @Test
    fun testGifListSurvivesNavigationForCleanup() {
        val gifs = arrayListOf("content://media/external/downloads/0", null, "content://media/external/downloads/2")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = newBuilder()
        doNothing().`when`(builder.mediaManager).downloadImage(anyOrNull())
        doReturn(gifs).`when`(builder.mediaManager).downloadImages(any(), anyInt())
        val bundle = carouselBundle()
        val first = builder.build(bundle.getString(SwrveNotificationConstants.TEXT_KEY), bundle, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null).build()
        notificationManager.notify(123, first)

        newReceiver().onReceive(context, actionIntent(first.actions[2]))

        val posted = shadowOf(notificationManager).getNotification(123)
        assertEquals(gifs, shadowOf(posted.deleteIntent).savedIntent.getStringArrayListExtra(NotificationMediaManager.EXTRA_GIF_URIS))
        assertEquals(gifs, shadowOf(posted.contentIntent).savedIntent.getStringArrayListExtra(NotificationMediaManager.EXTRA_GIF_URIS))
        assertEquals(gifs, actionIntent(posted.actions[2]).getStringArrayListExtra(NotificationMediaManager.EXTRA_GIF_URIS))
    }
}
