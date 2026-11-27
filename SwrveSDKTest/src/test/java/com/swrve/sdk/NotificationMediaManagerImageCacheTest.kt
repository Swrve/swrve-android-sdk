package com.swrve.sdk

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class NotificationMediaManagerImageCacheTest : SwrveBaseTest() {

    private lateinit var mediaManager: NotificationMediaManager

    @Before
    override fun setUp() {
        super.setUp()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val swrveReal = SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey") as Swrve
        val swrveSpy = spy(swrveReal)
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy)
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy)
        SwrveTestUtils.disableSwrveBackgroundEventSender(swrveSpy)
        SwrveTestUtils.setSDKInstance(swrveSpy)
        doNothing().`when`(swrveSpy).checkForCampaignAndResourcesUpdates()
        doReturn(true).`when`(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable::class.java))
        doReturn(context.cacheDir).`when`(swrveSpy).getCacheDir(context)
        SwrveCommon.setSwrveCommon(swrveSpy)
        mediaManager = spy(NotificationMediaManager(context))
        doReturn(Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)).`when`(mediaManager).downloadBitmap(anyString())
        doReturn(bitmapResult()).`when`(mediaManager).downloadBigPictureImage(anyString(), anyInt())
    }

    private fun bitmapResult() = NotificationMediaManager.BigPictureFetchResult().apply { bitmap = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888) }

    private fun gifResult(uri: String) = NotificationMediaManager.BigPictureFetchResult().apply { mediaUri = Uri.parse(uri) }

    private fun writeImage(url: String, width: Int = 20, height: Int = 10): File {
        val file = mediaManager.getCachedImageFile(url)
        file.parentFile!!.mkdirs()
        FileOutputStream(file).use { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun testImageHeightRatioIsReadFromTheCachedImage() {
        writeImage("https://cdn.example.com/wide.png", width = 60, height = 20)
        assertEquals(1f / 3f, mediaManager.getImageHeightRatio("https://cdn.example.com/wide.png", null), 0.001f)
    }

    @Test
    fun testImageHeightRatioIsZeroWithoutAnImage() {
        assertEquals(0f, mediaManager.getImageHeightRatio("https://cdn.example.com/missing.png", null))
        assertEquals(0f, mediaManager.getImageHeightRatio(null, null))
        assertEquals(0f, mediaManager.getImageHeightRatio("https://cdn.example.com/1.gif", "content://media/external/downloads/missing"))
    }

    @Test
    fun testCachedImageIsReturnedWithoutNetwork() {
        writeImage("https://cdn.example.com/0.png")
        assertNotNull(mediaManager.getCachedImage("https://cdn.example.com/0.png"))
    }

    @Test
    fun testMissingImageReturnsNull() {
        assertNull(mediaManager.getCachedImage("https://cdn.example.com/missing.png"))
        assertNull(mediaManager.getCachedImage(null))
    }

    @Test
    fun testDownloadImagesSkipsCachedImages() {
        writeImage("https://cdn.example.com/cached.png")
        val newFile = mediaManager.getCachedImageFile("https://cdn.example.com/new.png")

        mediaManager.downloadImages(listOf("https://cdn.example.com/cached.png", "https://cdn.example.com/new.png", null, ""), 123)

        verify(mediaManager).downloadBigPictureImage("https://cdn.example.com/new.png", 123)
        verify(mediaManager, never()).downloadBigPictureImage("https://cdn.example.com/cached.png", 123)
        assertTrue(newFile.exists())
        assertNotNull(mediaManager.getCachedImage("https://cdn.example.com/new.png"))
    }

    @Test
    fun testDownloadImageCachesStaticImageOnce() {
        mediaManager.downloadImage("https://cdn.example.com/icon.png")
        mediaManager.downloadImage("https://cdn.example.com/icon.png")

        verify(mediaManager, times(1)).downloadBitmap("https://cdn.example.com/icon.png")
        verify(mediaManager, never()).downloadBigPictureImage(anyString(), anyInt())
        assertNotNull(mediaManager.getCachedImage("https://cdn.example.com/icon.png"))
    }

    @Test
    fun testFailedDownloadIsNotCached() {
        doReturn(null).`when`(mediaManager).downloadBigPictureImage("https://cdn.example.com/broken.png", 123)

        val gifUris = mediaManager.downloadImages(listOf("https://cdn.example.com/broken.png"), 123)

        assertFalse(mediaManager.getCachedImageFile("https://cdn.example.com/broken.png").exists())
        assertEquals(listOf(null), gifUris)
    }

    @Test
    fun testGifItemReturnsItsUriAndIsNotCachedAsBitmap() {
        doReturn(gifResult("content://media/external/downloads/1")).`when`(mediaManager).downloadBigPictureImage("https://cdn.example.com/1.gif", 123)

        val gifUris = mediaManager.downloadImages(listOf("https://cdn.example.com/0.png", "https://cdn.example.com/1.gif"), 123)

        assertEquals(listOf(null, "content://media/external/downloads/1"), gifUris)
        assertFalse(mediaManager.getCachedImageFile("https://cdn.example.com/1.gif").exists())
    }

    @Test
    fun testSameGifUsedTwiceIsPublishedOnce() {
        doReturn(gifResult("content://media/external/downloads/1")).`when`(mediaManager).downloadBigPictureImage("https://cdn.example.com/a.gif", 123)

        val gifUris = mediaManager.downloadImages(listOf("https://cdn.example.com/a.gif", "https://cdn.example.com/a.gif"), 123)

        verify(mediaManager, times(1)).downloadBigPictureImage("https://cdn.example.com/a.gif", 123)
        assertEquals(listOf("content://media/external/downloads/1", "content://media/external/downloads/1"), gifUris)
    }

    @Test
    fun testSameUrlMapsToSameFile() {
        assertEquals(mediaManager.getCachedImageFile("https://cdn.example.com/a.png"), mediaManager.getCachedImageFile("https://cdn.example.com/a.png"))
        assertFalse(mediaManager.getCachedImageFile("https://cdn.example.com/a.png") == mediaManager.getCachedImageFile("https://cdn.example.com/b.png"))
    }

    @Test
    fun testSlowDownloadDoesNotBlockPastDeadline() {
        doReturn(200L).`when`(mediaManager).getDownloadImagesTimeoutMillis()
        doAnswer {
            Thread.sleep(3000)
            bitmapResult()
        }.`when`(mediaManager).downloadBigPictureImage("https://cdn.example.com/slow.png", 123)

        val start = System.currentTimeMillis()
        mediaManager.downloadImages(listOf("https://cdn.example.com/fast.png", "https://cdn.example.com/slow.png"), 123)

        assertTrue(System.currentTimeMillis() - start < 2000)
        assertTrue(mediaManager.getCachedImageFile("https://cdn.example.com/fast.png").exists())
    }

    @Test
    fun testDeleteGifUriDeletesEveryCarouselGif() {
        val first = Uri.parse("content://media/external/downloads/1")
        val second = Uri.parse("content://media/external/downloads/2")
        doReturn(1).`when`(mediaManager).deleteGifUri(first)
        doReturn(1).`when`(mediaManager).deleteGifUri(second)
        val intent = Intent().putStringArrayListExtra(NotificationMediaManager.EXTRA_GIF_URIS, arrayListOf(first.toString(), null, second.toString()))

        assertEquals(2, mediaManager.deleteGifUri(intent))
        verify(mediaManager).deleteGifUri(first)
        verify(mediaManager).deleteGifUri(second)
    }

    @Test
    fun testDecodeFromStreamDeletesTempFile() {
        val dir = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "scaler_test").apply { mkdirs() }
        val png = ByteArrayOutputStream().also { Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

        val bitmap = SwrveImageScaler.decodeSampledBitmapFromStream(ByteArrayInputStream(png), 100, 100, 1, "test", dir)

        assertNotNull(bitmap)
        assertEquals(0, dir.listFiles()!!.size)
    }

    @Test
    fun testFailedSaveLeavesNothingCached() {
        val unsavable = Mockito.mock(Bitmap::class.java)
        doReturn(false).`when`(unsavable).compress(Mockito.any(), Mockito.anyInt(), Mockito.any())
        val result = NotificationMediaManager.BigPictureFetchResult().apply { bitmap = unsavable }
        doReturn(result).`when`(mediaManager).downloadBigPictureImage("https://cdn.example.com/full-disk.png", 123)

        mediaManager.downloadImages(listOf("https://cdn.example.com/full-disk.png"), 123)

        val file = mediaManager.getCachedImageFile("https://cdn.example.com/full-disk.png")
        assertFalse(file.exists())
        assertTrue(file.parentFile!!.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    @Test
    fun testConcurrentDownloadsOfSameImageLeaveAValidFile() {
        val url = "https://cdn.example.com/shared.png"
        doAnswer {
            Thread.sleep(100)
            bitmapResult()
        }.`when`(mediaManager).downloadBigPictureImage(url, 123)

        val threads = (1..2).map { Thread { mediaManager.downloadImages(listOf(url), 123) } }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val file = mediaManager.getCachedImageFile(url)
        assertNotNull(mediaManager.getCachedImage(url))
        assertTrue(file.parentFile!!.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    @Test
    fun testOrphanedGifCleanupSkipsGifsFromTheLastTenMinutes() {
        val expected = (System.currentTimeMillis() - 10 * 60 * 1000) / 1000
        val cutoff = mediaManager.getOrphanedGifCutoffSeconds()
        assertTrue(Math.abs(cutoff - expected) <= 1)
    }
}
