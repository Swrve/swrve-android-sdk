package com.swrve.sdk

import android.Manifest
import android.app.Notification
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH
import com.swrve.sdk.notifications.model.SwrveNotification
import com.swrve.sdk.notifications.model.SwrveNotificationButton
import com.swrve.sdk.test.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.robolectric.Shadows.shadowOf

class SwrveNotificationBuilderCarouselTest : SwrveBaseTest() {

    private val notificationConfig = SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, null)
        .activityClass(MainActivity::class.java)
        .build()

    private lateinit var lastBuilder: SwrveNotificationBuilder
    private lateinit var lastMediaManager: NotificationMediaManager
    private lateinit var swrveSpy: Swrve

    private val carouselJson = """
        {
          "version": 1,
          "title": "base title",
          "subtitle": "base subtitle",
          "notification_id": 123,
          "carousel": [
            {"title": "${'$'}110 off", "body": "55-inch 4K LED television", "image_url": "https://cdn.example.com/item1.png", "action_type": "open_url", "action": "https://www.example.com/"},
            {"title": "${'$'}150 off", "body": "65-inch OLED television", "image_url": "https://cdn.example.com/item2.png", "action_type": "open_app", "action": ""}
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
        swrveSpy = spy(swrveReal)
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
        bundle.putString(SwrveNotificationConstants.TEXT_KEY, "plain text fallback")
        bundle.putString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, json)
        return bundle
    }

    // imageDownloads: whether the item images were pre-fetched successfully. gifUris: published GIF per item.
    // imageHeightRatios: the shape of each downloaded image by url, none by default
    private fun buildNotification(
        bundle: Bundle,
        imageDownloads: Boolean = true,
        gifUris: List<String?> = listOf(),
        imageHeightRatios: Map<String, Float> = mapOf(),
        cachedImage: Bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
    ): Notification {
        val builderSpy = spy(SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig))
        val mediaManagerSpy = spy(NotificationMediaManager(mActivity))
        val bitmap = if (imageDownloads) cachedImage else null
        doNothing().`when`(mediaManagerSpy).downloadImage(anyOrNull())
        doReturn(ArrayList(gifUris)).`when`(mediaManagerSpy).downloadImages(any(), anyInt())
        doReturn(bitmap).`when`(mediaManagerSpy).getCachedImage(anyString())
        doAnswer { imageHeightRatios[it.getArgument<String?>(0)] ?: 0f }.`when`(mediaManagerSpy).getImageHeightRatio(anyOrNull(), anyOrNull())
        builderSpy.mediaManager = mediaManagerSpy
        lastBuilder = builderSpy
        lastMediaManager = mediaManagerSpy
        val text = bundle.getString(SwrveNotificationConstants.TEXT_KEY)
        return builderSpy.build(text, bundle, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null).build()
    }

    private fun fixture(name: String): JsonObject {
        val json = SwrveTestUtils.getAssetAsText(mActivity!!, "carousel_payloads.json")
        return JsonParser.parseString(json).asJsonObject.getAsJsonObject(name)
    }

    @Test
    fun testCanonicalFixtureShowsCarouselWithoutItsCustomButton() {
        val notification = buildNotification(carouselBundle(fixture("canonical").toString()))

        assertEquals("\$110 off", notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
        assertTrue(notification.actions == null || notification.actions.isEmpty())
    }

    @Test
    fun testNotAnArrayFixtureRendersAsPlainTextPush() {
        val notification = buildNotification(carouselBundle(fixture("not_an_array").toString()))
        val extras = notification.extras

        assertEquals("plain text fallback", extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertNull(extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
        assertNull(extras.get(Notification.EXTRA_PICTURE))
        assertTrue(notification.actions == null || notification.actions.isEmpty())
    }

    @Test
    fun testIndicatorShowsDespiteStraySettingsKeys() {
        val payload = fixture("stray_settings_keys_are_ignored")
        val items = payload.getAsJsonArray("carousel")
        items.add(JsonParser.parseString("""{"title": "item 1", "image_url": "https://cdn.example.com/carousel/1.png"}"""))
        payload.add("buttons", JsonParser.parseString("""[{"title": "Previous", "action_type": "previous"}, {"title": "Next", "action_type": "next"}]"""))

        val notification = buildNotification(carouselBundle(payload.toString()))

        assertEquals(3, notification.actions.size)
        assertEquals("● ○", notification.actions[1].title.toString())
    }

    @Test
    fun testFirstItemShowsInExpandedViewAndPushTextWhenCollapsed() {
        val notification = buildNotification(carouselBundle())
        val extras = notification.extras

        // Collapsed and heads-up keep the push's own title and text
        assertEquals("base title", extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("plain text fallback", extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals("base subtitle", extras.getCharSequence(Notification.EXTRA_SUB_TEXT).toString())

        // Expanded shows the item
        assertEquals("\$110 off", extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
        assertEquals("55-inch 4K LED television", extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString())
        assertNotNull(extras.get(Notification.EXTRA_PICTURE))
        verify(lastMediaManager).getCachedImage("https://cdn.example.com/item1.png")
    }

    @Test
    fun testNavigationButtonsParseWithLocalisedTitles() {
        val buttons = SwrveNotification.fromJson(carouselJson).buttons
        assertEquals(2, buttons.size)
        assertEquals(SwrveNotificationButton.ActionType.PREVIOUS, buttons[0].actionType)
        assertEquals("Précédent", buttons[0].title)
        assertEquals(SwrveNotificationButton.ActionType.NEXT, buttons[1].actionType)
        assertEquals("Suivant", buttons[1].title)
    }

    @Test
    fun testNavigationButtonsAreBuiltAsCarouselNavigation() {
        val notification = buildNotification(carouselBundle())
        assertEquals(3, notification.actions.size)
        for (action in notification.actions) {
            val intent = shadowOf(action.actionIntent).savedIntent
            assertEquals(SwrveNotificationCarouselReceiver::class.java.name, intent.component!!.className)
        }
    }

    @Test
    fun testFailedImageDownloadShowsPlaceholderAndKeepsItem() {
        val notification = buildNotification(carouselBundle(), imageDownloads = false)
        val extras = notification.extras

        assertEquals(2f / 3f, heightRatio(extras.get(Notification.EXTRA_PICTURE) as Bitmap), 0.01f) // 3:2 with no image downloaded
        assertEquals("base title", extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("\$110 off", extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
        assertEquals("55-inch 4K LED television", extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString())
        assertNull(lastBuilder.notificationDetails.mediaUrl) // no image was shown
        // a custom filter rebuilding the notification from its details gets the placeholder, not text only
        assertEquals(2f / 3f, heightRatio(lastBuilder.notificationDetails.mediaBitmap), 0.01f)
    }

    @Test
    fun testPlaceholderTakesTheShapeOfTheFirstImage() {
        val notification = buildNotification(carouselBundle(), imageDownloads = false,
            imageHeightRatios = mapOf("https://cdn.example.com/item1.png" to 0.5f))

        assertEquals(0.5f, heightRatio(notification.extras.get(Notification.EXTRA_PICTURE) as Bitmap), 0.01f)
    }

    // Notification.Builder scales a large picture down when it builds, keeping its shape, so compare shapes not sizes
    private fun heightRatio(bitmap: Bitmap): Float = bitmap.height.toFloat() / bitmap.width

    @Test
    fun testImageIsCroppedToTheShapeOfTheFirstImage() {
        val notification = buildNotification(carouselBundle(),
            imageHeightRatios = mapOf("https://cdn.example.com/item1.png" to 0.25f), // kept to 3:1
            cachedImage = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888))

        val picture = notification.extras.get(Notification.EXTRA_PICTURE) as Bitmap
        assertEquals(300, picture.width)
        assertEquals(100, picture.height)
    }

    @Test
    fun testShapeComesFromTheNextImageWhenTheFirstDidNotDownload() {
        val notification = buildNotification(carouselBundle(),
            imageHeightRatios = mapOf("https://cdn.example.com/item2.png" to 0.4f),
            cachedImage = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888))

        val picture = notification.extras.get(Notification.EXTRA_PICTURE) as Bitmap
        assertEquals(200, picture.width)
        assertEquals(80, picture.height)
    }

    @Test
    fun testShapeIsCarriedInTheNavigationIntents() {
        val notification = buildNotification(carouselBundle(), imageHeightRatios = mapOf("https://cdn.example.com/item1.png" to 0.5f))

        for (action in notification.actions) {
            assertEquals(0.5f, shadowOf(action.actionIntent).savedIntent.getFloatExtra(SwrveNotificationConstants.CAROUSEL_IMAGE_HEIGHT_RATIO_KEY, 0f))
        }
    }

    @Test
    fun testItemWithoutTitleShowsBodyOnly() {
        val json = """{"version":1,"title":"base title","notification_id":123,"carousel":[{"body":"only body","image_url":"https://cdn.example.com/item1.png"}]}"""
        val notification = buildNotification(carouselBundle(json))
        val extras = notification.extras

        assertEquals("base title", extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertNull(extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
        assertEquals("only body", extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString())
    }

    @Test
    fun testLockScreenMessage() {
        val json = """{"version":1,"notification_id":123,"lock_screen_msg":"hidden","carousel":[{"title":"item title","body":"item body","image_url":"https://cdn.example.com/item1.png"}]}"""
        val notification = buildNotification(carouselBundle(json))

        assertEquals("hidden", notification.publicVersion.tickerText.toString())
        assertEquals("item title", notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
    }

    @Test
    fun testEmptyCarouselRendersAsNormalPushWithButtons() {
        val json = """{"version":1,"title":"base title","carousel":[],"buttons":[{"title":"Shop now","action_type":"open_app"}]}"""
        val notification = buildNotification(carouselBundle(json))
        val extras = notification.extras

        assertEquals("base title", extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertNull(extras.getCharSequence(Notification.EXTRA_TITLE_BIG))
        assertEquals(1, notification.actions.size)
        assertEquals("Shop now", notification.actions[0].title.toString())
    }

    @Test
    fun testItemMissingImageUrlShowsPlaceholder() {
        val json = """{"version":1,"title":"base title","notification_id":123,"carousel":[{"title":"item title","body":"item body"}]}"""
        val notification = buildNotification(carouselBundle(json))
        val extras = notification.extras

        // as for an image that failed to download, so the notification keeps its height
        assertEquals(2f / 3f, heightRatio(extras.get(Notification.EXTRA_PICTURE) as Bitmap), 0.01f)
        assertEquals("item title", extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
        assertEquals("item body", extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT).toString())
    }

    @Test
    fun testCarouselReplacesMediaAndExpanded() {
        val json = """
            {
              "version": 1,
              "title": "base title",
              "notification_id": 123,
              "media": {"title": "media title", "body": "media body", "type": "image", "url": "https://media.example.com/media.png"},
              "expanded": {"title": "expanded title", "body": "expanded body"},
              "carousel": [{"title": "item title", "image_url": "https://cdn.example.com/item1.png"}]
            }
        """.trimIndent()
        val notification = buildNotification(carouselBundle(json))
        val extras = notification.extras

        assertEquals("base title", extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("plain text fallback", extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals("item title", extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
        assertNull(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        verify(lastMediaManager).getCachedImage("https://cdn.example.com/item1.png")
        verify(lastMediaManager, never()).downloadBigPictureImage(anyString(), anyInt())

        val details = lastBuilder.notificationDetails
        assertEquals("https://cdn.example.com/item1.png", details.mediaUrl)
        assertEquals("item title", details.expandedTitle)
        assertNull(details.expandedBody)
    }

    @Test
    fun testAllItemImagesArePrefetchedAndShownFromCache() {
        buildNotification(carouselBundle())

        verify(lastMediaManager).downloadImage(null)
        verify(lastMediaManager).downloadImages(listOf("https://cdn.example.com/item1.png", "https://cdn.example.com/item2.png"), 123)
        verify(lastMediaManager, never()).downloadBigPictureImage(anyString(), anyInt())
        assertNull(lastBuilder.notificationDetails.mediaContentUri)
    }

    @Test
    fun testGifItemIsShownFromItsPublishedUri() {
        val gif = "content://media/external/downloads/0"
        val notification = buildNotification(carouselBundle(), gifUris = listOf(gif, null))

        assertEquals(gif, lastBuilder.notificationDetails.mediaContentUri)
        assertNull(notification.extras.get(Notification.EXTRA_PICTURE)) // GIF is set as an Icon, not a bitmap, and keeps its own shape
        verify(lastMediaManager, never()).getCachedImage("https://cdn.example.com/item1.png")
    }

    @Test
    fun testEveryCarouselGifIsCarriedForCleanupAndNavigation() {
        val gifs = listOf("content://media/external/downloads/0", "content://media/external/downloads/1")
        val notification = buildNotification(carouselBundle(), gifUris = gifs)

        assertEquals(gifs, shadowOf(notification.deleteIntent).savedIntent.getStringArrayListExtra(NotificationMediaManager.EXTRA_GIF_URIS))
        assertEquals(gifs, shadowOf(notification.contentIntent).savedIntent.getStringArrayListExtra(NotificationMediaManager.EXTRA_GIF_URIS))
        for (action in notification.actions) {
            assertEquals(gifs, shadowOf(action.actionIntent).savedIntent.getStringArrayListExtra(NotificationMediaManager.EXTRA_GIF_URIS))
        }
    }

    @Test
    fun testNoGifsMeansNoDeleteIntent() {
        val notification = buildNotification(carouselBundle(), gifUris = listOf(null, null))
        assertNull(notification.deleteIntent)
    }

    @Test
    fun testDeletePublishedGifsDeletesEveryGif() {
        val gifs = listOf("content://media/external/downloads/0", null, "content://media/external/downloads/2")
        buildNotification(carouselBundle(), gifUris = gifs)
        doReturn(1).`when`(lastMediaManager).deleteGifUri(any<Uri>())

        lastBuilder.deletePublishedGifs()

        verify(lastMediaManager).deleteGifUri(Uri.parse(gifs[0]))
        verify(lastMediaManager).deleteGifUri(Uri.parse(gifs[2]))
    }

    @Test
    fun testGifsAreDeletedWhenFilterSuppressesNotification() {
        val suppressingConfig = SwrveNotificationConfig.Builder(com.swrve.sdk.test.R.drawable.ic_launcher, null)
            .activityClass(MainActivity::class.java)
            .notificationFilter { _, _, _, _ -> null }
            .build()
        doReturn(suppressingConfig).`when`(swrveSpy).notificationConfig
        val builderSpy = spy(SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), suppressingConfig))
        val mediaManagerSpy = spy(NotificationMediaManager(mActivity))
        doNothing().`when`(mediaManagerSpy).downloadImage(anyOrNull())
        doReturn(arrayListOf("content://media/external/downloads/0", null)).`when`(mediaManagerSpy).downloadImages(any(), anyInt())
        doReturn(Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)).`when`(mediaManagerSpy).getCachedImage(anyString())
        doReturn(1).`when`(mediaManagerSpy).deleteGifUri(any<Uri>())
        builderSpy.mediaManager = mediaManagerSpy
        val pushManagerSpy = spy(SwrvePushManagerImp(mActivity))
        doReturn(builderSpy).`when`(pushManagerSpy).getSwrveNotificationBuilder()

        pushManagerSpy.processNotification(carouselBundle(), "1")

        verify(builderSpy).deletePublishedGifs()
        verify(mediaManagerSpy).deleteGifUri(Uri.parse("content://media/external/downloads/0"))
    }

    private fun processWithGif(json: String): Pair<SwrveNotificationBuilder, NotificationMediaManager> {
        val builderSpy = spy(SwrveNotificationBuilder(ApplicationProvider.getApplicationContext(), notificationConfig))
        val mediaManagerSpy = spy(NotificationMediaManager(mActivity))
        doNothing().`when`(mediaManagerSpy).downloadImage(anyOrNull())
        doReturn(arrayListOf("content://media/external/downloads/0", null)).`when`(mediaManagerSpy).downloadImages(any(), anyInt())
        doReturn(Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)).`when`(mediaManagerSpy).getCachedImage(anyString())
        doReturn(1).`when`(mediaManagerSpy).deleteGifUri(any<Uri>())
        builderSpy.mediaManager = mediaManagerSpy
        val pushManagerSpy = spy(SwrvePushManagerImp(mActivity))
        doReturn(builderSpy).`when`(pushManagerSpy).getSwrveNotificationBuilder()
        pushManagerSpy.processNotification(carouselBundle(json), "1")
        return Pair(builderSpy, mediaManagerSpy)
    }

    @Test
    fun testGifsAreDeletedWhenBuildingFails() {
        val json = carouselJson.replace("\"version\": 1,", "\"version\": 1, \"accent\": \"not-a-colour\",")
        val (builderSpy, mediaManagerSpy) = processWithGif(json)

        verify(builderSpy).deletePublishedGifs()
        verify(mediaManagerSpy).deleteGifUri(Uri.parse("content://media/external/downloads/0"))
    }

    @Test
    fun testGifsAreKeptWhenNotificationIsPosted() {
        val (builderSpy, _) = processWithGif(carouselJson)
        verify(builderSpy, never()).deletePublishedGifs()
    }

    @Test
    fun testIconIsDownloadedBeforeItems() {
        val json = carouselJson.replace("\"version\": 1,", "\"version\": 1, \"icon_url\": \"https://cdn.example.com/icon.png\",")
        buildNotification(carouselBundle(json))

        val inOrder = Mockito.inOrder(lastMediaManager)
        inOrder.verify(lastMediaManager).downloadImage("https://cdn.example.com/icon.png")
        inOrder.verify(lastMediaManager).downloadImages(any(), anyInt())
    }
}
