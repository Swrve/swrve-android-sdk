package com.swrve.sdk

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.NotificationMediaManager.NATIVE_GIF_SUPPORT_MIN_API
import com.swrve.sdk.SwrveTestUtils.getAssetAsBuffer
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.robolectric.annotation.Config
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class NotificationMediaManagerTest : SwrveBaseTest() {
    private lateinit var mediaManager: NotificationMediaManager
    private lateinit var server: MockWebServer
    private var swrveCommon: ISwrveCommon? = null
    private lateinit var contextSpy: Context
    private lateinit var resolverSpy: ContentResolver

    @Before
    override fun setUp() {
        super.setUp()
        server = MockWebServer()
        server.start()

        swrveCommon = Mockito.mock(ISwrveCommon::class.java)
        doReturn(1000).`when`<ISwrveCommon>(swrveCommon).getHttpTimeout()
        val cacheDir = ApplicationProvider.getApplicationContext<Context>().cacheDir
        doReturn(cacheDir).`when`<ISwrveCommon>(swrveCommon).getCacheDir(ApplicationProvider.getApplicationContext())
        SwrveCommon.setSwrveCommon(swrveCommon)

        contextSpy = spy(ApplicationProvider.getApplicationContext<Context>())
        resolverSpy = spy(contextSpy.contentResolver)
        mediaManager = NotificationMediaManager(ApplicationProvider.getApplicationContext())
    }

    @After
    override fun tearDown() {
        super.tearDown()
        server.shutdown()
    }

    @Test
    fun testDownloadBitmap() {
        val pngFile = "mg_icon.png"
        val mockResponse = MockResponse()
            .setBody(getAssetAsBuffer(pngFile))
        server.enqueue(mockResponse)

        val url = server.url(pngFile)
        val bitmap = mediaManager.downloadBitmap(url.toString())
        assertNotNull(bitmap)

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Expected a request to the mock server", recorded)
        if (recorded != null) {
            assertEquals("GET", recorded.method)
            assertEquals("/${pngFile}", recorded.path)
            assertEquals("gzip", recorded.getHeader("Accept-Encoding"))
            assertEquals(0, recorded.bodySize)
        }
    }

    @Test
    fun testDownloadBigPictureImage_png() {
        val pngFile = "mg_icon.png"
        val mockResponse = MockResponse()
            .setBody(getAssetAsBuffer(pngFile))
        server.enqueue(mockResponse)

        val url = server.url(pngFile)
        val result = mediaManager.downloadBigPictureImage(url.toString(), 123)

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("Expected a request to the mock server", recorded)
        if (recorded != null) {
            assertEquals("GET", recorded.method)
            assertEquals("/${pngFile}", recorded.path)
            assertEquals("gzip", recorded.getHeader("Accept-Encoding"))
            assertEquals(0, recorded.bodySize)
        }

        assertNotNull(result)
        assertNotNull(result.bitmap)
        assertNull(result.mediaUri)
    }

    @Test
    fun testDownloadBigPictureImage_gif_withContentType() {
        val gifFile = "next_arrow.gif"
        val mockResponse = MockResponse()
            .setBody(getAssetAsBuffer(gifFile))
            .addHeader("Content-Type", "image/gif")
        testGif(gifFile, mockResponse)
    }

    @Test
    fun testDownloadBigPictureImage_gifMagic() {
        val gifFile = "next_arrow.gif"
        val mockResponse = MockResponse()
            .setBody(getAssetAsBuffer(gifFile)) // leave out header content-type so magic gif marker is used
        testGif(gifFile, mockResponse)
    }

    private fun testGif(gifFile: String, mockResponse: MockResponse) {
        server.enqueue(mockResponse)

        val url = server.url(gifFile)
        val result = mediaManager.downloadBigPictureImage(url.toString(), 456)
        assertNotNull(result)
        assertNull(result.bitmap)
        assertNotNull(result.mediaUri)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.putExtra(NotificationMediaManager.EXTRA_GIF_URI, result.mediaUri.toString())
        val rowsDeleted = mediaManager.deleteGifUri(intent)
        assertEquals(1, rowsDeleted)
    }

    @Test
    fun testDownloadBigPictureUri_FailsDuringWriteAndCleansUp() {

        val testUri = Uri.parse("content://media/external/downloads/123")

        // mock insert uri to return our test uri
        val mockResolver = mock(ContentResolver::class.java)
        `when`(mockResolver.insert(any(), any())).thenReturn(testUri)

        // mock the outstream to throw an exception
        val mockOutputStream = mock(OutputStream::class.java)
        `when`(mockResolver.openOutputStream(any())).thenReturn(mockOutputStream)
        `when`(mockOutputStream.write(any(), anyInt(), anyInt()))
            .thenThrow(IOException("Simulated write failure!"))

        // recreate the media manager with the mocked resolver
        `when`(contextSpy.contentResolver).thenReturn(mockResolver)
        mediaManager = NotificationMediaManager(contextSpy)

        // simulate issue while downloading and make sure the uri is cleaned up
        val fakeGifData = "this is a fake gif".byteInputStream()
        val result = mediaManager.downloadBigPictureUri(fakeGifData, 123)

        assertNull(result)
        verify(mockResolver, times(1)).delete(testUri, null, null)
    }

    @Config(sdk = [NATIVE_GIF_SUPPORT_MIN_API - 1]) // one below min API
    @Test
    fun testDownloadBigPictureImage_gif_unsupported() {
        // see @config where this test is running on unsupported device
        val gifFile = "next_arrow.gif"
        val mockResponse = MockResponse()
            .setBody(getAssetAsBuffer(gifFile))
            .addHeader("Content-Type", "image/gif")
        server.enqueue(mockResponse)

        val url = server.url(gifFile)
        val result = mediaManager.downloadBigPictureImage(url.toString(), 789)
        assertNotNull(result)
        assertNotNull(result.bitmap) // for unsupported device, should fall back to bitmap
        assertNull(result.mediaUri)
    }

    @Test
    fun testDeleteGifUri() {
        val uri = Uri.parse("content://media/external/downloads/123")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.putExtra(NotificationMediaManager.EXTRA_GIF_URI, uri.toString())
        val rowsDeleted = mediaManager.deleteGifUri(intent)
        assertEquals(1, rowsDeleted)
    }

    @Test
    fun testCleanupOrphanedGifs_BulkDelete() {

        val mockResolver = mock(ContentResolver::class.java)
        val realContext = ApplicationProvider.getApplicationContext<Context>()
        val ctx = spy(realContext)
        `when`(ctx.contentResolver).thenReturn(mockResolver)

        mediaManager = NotificationMediaManager(ctx)

        // Expect a single bulk delete of all orphaned GIFs in our relative path. Stub the delete: return 2 rows deleted
        val expectedRelativePath = Environment.DIRECTORY_DOWNLOADS + "/" + ctx.packageName + "/animated_gifs/"
        val expectedSelection = MediaStore.MediaColumns.RELATIVE_PATH + " = ?"
        val expectedArgs = arrayOf(expectedRelativePath)
        `when`(
            mockResolver.delete(
                Mockito.eq(MediaStore.Downloads.EXTERNAL_CONTENT_URI),
                Mockito.eq(expectedSelection),
                Mockito.argThat { arg: Array<String>? ->
                    arg != null && arg.contentEquals(expectedArgs)
                }
            )
        ).thenReturn(2)

        val rowsDeleted = mediaManager.cleanupOrphanedGifs()
        assertEquals(2, rowsDeleted)
    }

    @Test
    fun testCleanupOrphanedGifs_activeNotifications_skipsBulkDelete() {

        // Mock an active notification so cleanup path is skipped
        val mockNotificationManager = mock(NotificationManager::class.java)
        val mockSbn = mock(StatusBarNotification::class.java)
        `when`(mockSbn.id).thenReturn(999)
        `when`(mockNotificationManager.activeNotifications).thenReturn(arrayOf(mockSbn))
        `when`(contextSpy.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager)

        mediaManager = NotificationMediaManager(contextSpy)

        val rowsDeleted = mediaManager.cleanupOrphanedGifs()
        assertEquals(0, rowsDeleted)

        // Since there are active notifications, deleteAllOrphanedGifs should NOT be invoked
        verify(resolverSpy, Mockito.never()).delete(Mockito.any(), Mockito.any(), Mockito.any())
    }

}
