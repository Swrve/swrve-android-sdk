package com.swrve.sdk

import android.app.Activity
import com.swrve.sdk.SwrveTestUtils.mockCommonSocketFactory
import com.swrve.sdk.SwrveTestUtils.setupLocalSllSocketFactory
import com.swrve.sdk.test.MainActivity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.awaitility.Awaitility
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPOutputStream

class SwrveAssetsManagerTest : SwrveBaseTest() {
    private var server: MockWebServer? = null

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        ShadowLog.stream = System.out
        mActivity = Robolectric.buildActivity(MainActivity::class.java).create().get()
    }

    @After
    @Throws(Exception::class)
    override fun tearDown() {
        if (server != null) {
            server!!.shutdown()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFilesAlreadyDownloaded() {
        val assetsManager = SwrveAssetsManagerImp(mActivity)
        assetsManager.setStorageDir(mActivity!!.cacheDir)
        val assetsManagerSpy = Mockito.spy(assetsManager)

        writeFileToCache("asset1", "digest1")
        writeFileToCache("asset2", "digest2")

        val assetsQueueImages: MutableSet<SwrveAssetsQueueItem> = HashSet()
        assetsQueueImages.add(SwrveAssetsQueueItem(1, "asset1", "digest1", true, false))
        assetsQueueImages.add(SwrveAssetsQueueItem(1, "asset2", "digest2", true, false))

        assetsManagerSpy.downloadAssets(assetsQueueImages, null)

        Mockito.verify(assetsManagerSpy, Mockito.never()).downloadAsset(
            Mockito.any(
                SwrveAssetsQueueItem::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testExternallySourcedFilesAlreadyDownloaded() {
        val assetsManager = SwrveAssetsManagerImp(mActivity)
        assetsManager.setStorageDir(mActivity!!.cacheDir)
        val assetsManagerSpy = Mockito.spy(assetsManager)

        val assetUrl1 =
            SwrveHelper.sha1("https://www.testitem/asset1.png".toByteArray()) // this should already exist (as part of this setup)
        val assetUrl2 =
            SwrveHelper.sha1("https://www.testitem/asset2.png".toByteArray()) // this should already exist (as part of this setup)

        writeFileToCache(assetUrl1!!, "https://www.testitem/asset1.png")
        writeFileToCache(assetUrl2!!, "https://www.testitem/asset2.png")

        val assetsQueueImages: MutableSet<SwrveAssetsQueueItem> = HashSet()
        assetsQueueImages.add(
            SwrveAssetsQueueItem(
                1,
                assetUrl1,
                "https://www.testitem/asset1.png",
                true,
                true
            )
        )
        assetsQueueImages.add(
            SwrveAssetsQueueItem(
                1,
                assetUrl2,
                "https://www.testitem/asset2.png",
                true,
                true
            )
        )

        assetsManagerSpy.downloadAssets(assetsQueueImages, null)

        Mockito.verify(assetsManagerSpy, Mockito.never()).downloadAssetFromExternalSource(
            Mockito.any(
                SwrveAssetsQueueItem::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSomeFilesAlreadyDownloaded() {
        val digest1 =
            SwrveHelper.sha1("digest1".toByteArray()) // this should already exist (as part of this setup)
        val digest2 =
            SwrveHelper.sha1("digest2".toByteArray()) // this does not exist in cache at start and should be downloaded
        val digest3 =
            SwrveHelper.sha1("digest3".toByteArray()) // this does not exist in cache at start and should be downloaded
        val digest4 =
            SwrveHelper.sha1("digest4".toByteArray()) // this does not exist in cache at start and should be downloaded

        server = MockWebServer()
        val dispatcher: Dispatcher = object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path!!.contains("asset2")) {
                    return MockResponse().setResponseCode(200).setBody("digest2")
                } else if (request.path!!.contains("asset3")) {
                    return MockResponse().setResponseCode(200).setBody("digest3")
                } else if (request.path!!.contains("asset4")) {
                    return MockResponse().setResponseCode(200).setBody("digest4")
                        .setHeader("Content-Type", "image/gif")
                } else if (request.path!!.contains("externalAsset1")) {
                    return MockResponse().setResponseCode(200)
                        .setBody("externalAsset1") // do not set the content type
                } else if (request.path!!.contains("externalAsset2")) {
                    return MockResponse().setResponseCode(200).setBody("externalAsset2")
                        .setHeader("Content-Type", "image/gif")
                } else if (request.path!!.contains("externalAsset3")) {
                    return MockResponse().setResponseCode(200).setBody("externalAsset3")
                        .setHeader("Content-Type", "image/jpeg")
                } else if (request.path!!.contains("externalAsset4")) {
                    return MockResponse().setResponseCode(200).setBody("externalAsset4")
                        .setHeader("Content-Type", "image/png")
                } else if (request.path!!.contains("externalAsset5")) {
                    return MockResponse().setResponseCode(200).setBody("externalAsset5")
                        .setHeader("Content-Type", "image/bmp")
                } else if (request.path!!.contains("externalAsset6")) {
                    return MockResponse().setResponseCode(200).setBody("externalAsset6")
                        .setHeader("Content-Type", "image/jpg")
                }
                return MockResponse().setResponseCode(404)
            }
        }
        server!!.dispatcher = dispatcher

        val socketFactory = setupLocalSllSocketFactory(
            server!!
        )
        mockCommonSocketFactory(socketFactory)

        server!!.start()
        val cdnPath = server!!.url("/").toString()
        val externalAsset1Sha1 = SwrveHelper.sha1((cdnPath + "externalAsset1").toByteArray())
        val externalAsset2Sha1 = SwrveHelper.sha1((cdnPath + "externalAsset2").toByteArray())
        val externalAsset3Sha1 = SwrveHelper.sha1((cdnPath + "externalAsset3").toByteArray())
        val externalAsset4Sha1 = SwrveHelper.sha1((cdnPath + "externalAsset4").toByteArray())
        val externalAsset5Sha1 = SwrveHelper.sha1((cdnPath + "externalAsset5").toByteArray())
        val externalAsset6Sha1 = SwrveHelper.sha1((cdnPath + "externalAsset6").toByteArray())

        val assetsManager = SwrveAssetsManagerImp(mActivity)
        assetsManager.setCdnImages(cdnPath)
        assetsManager.setCdnFonts(cdnPath)
        assetsManager.setStorageDir(mActivity!!.cacheDir)
        val assetsManagerSpy = Mockito.spy(assetsManager)

        writeFileToCache("asset1", digest1) // simulate that asset1 exists already

        val assetsQueue: MutableSet<SwrveAssetsQueueItem> = HashSet()
        val item1 = SwrveAssetsQueueItem(1, "asset1", digest1, true, false)
        val item2 = SwrveAssetsQueueItem(1, "asset2", digest2, true, false)
        val item3 = SwrveAssetsQueueItem(1, "asset3", digest3, true, false)
        val item4 = SwrveAssetsQueueItem(1, "asset4_which_is_a_gif", digest4, true, false)
        val item5 =
            SwrveAssetsQueueItem(1, externalAsset1Sha1, (cdnPath + "externalAsset1"), true, true)
        val item6 =
            SwrveAssetsQueueItem(1, externalAsset2Sha1, (cdnPath + "externalAsset2"), true, true)
        val item7 =
            SwrveAssetsQueueItem(1, externalAsset3Sha1, (cdnPath + "externalAsset3"), true, true)
        val item8 =
            SwrveAssetsQueueItem(1, externalAsset4Sha1, (cdnPath + "externalAsset4"), true, true)
        val item9 =
            SwrveAssetsQueueItem(1, externalAsset5Sha1, (cdnPath + "externalAsset5"), true, true)
        val item10 =
            SwrveAssetsQueueItem(1, externalAsset6Sha1, (cdnPath + "externalAsset6"), true, true)

        assetsQueue.add(item1)
        assetsQueue.add(item2)
        assetsQueue.add(item3)
        assetsQueue.add(item4)
        assetsQueue.add(item5)
        assetsQueue.add(item6)
        assetsQueue.add(item7)
        assetsQueue.add(item8)
        assetsQueue.add(item9)
        assetsQueue.add(item10)

        assertCacheFileExists("asset1")
        assertCacheFileDoesNotExist(digest2!!)
        assertCacheFileDoesNotExist(digest3!!)
        assertCacheFileDoesNotExist("$digest4.gif")
        assertCacheFileDoesNotExist(externalAsset1Sha1!!)
        assertCacheFileDoesNotExist("$externalAsset2Sha1.gif")
        assertCacheFileDoesNotExist(externalAsset6Sha1!!)

        assetsManagerSpy.downloadAssets(assetsQueue, null) // null callback on purpose

        val assetPathCaptor = argumentCaptor<SwrveAssetsQueueItem>()
        Mockito.verify(assetsManagerSpy, Mockito.atLeastOnce())
            .downloadAsset(assetPathCaptor.capture())
        Mockito.verify(assetsManagerSpy, Mockito.atLeastOnce())
            .downloadAssetFromExternalSource(assetPathCaptor.capture())
        Assert.assertEquals(9, assetPathCaptor.allValues.size.toLong())
        Assert.assertTrue(
            "An attempt to download asset2 did not occur",
            assetPathCaptor.allValues.contains(item2)
        )
        Assert.assertTrue(
            "An attempt to download asset3 did not occur",
            assetPathCaptor.allValues.contains(item3)
        )
        Assert.assertTrue(
            "An attempt to download asset4 did not occur",
            assetPathCaptor.allValues.contains(item4)
        )
        Assert.assertTrue(
            "An attempt to download externalAsset1 did not occur",
            assetPathCaptor.allValues.contains(item5)
        )
        Assert.assertTrue(
            "An attempt to download externalAsset2 did not occur",
            assetPathCaptor.allValues.contains(item6)
        )
        Assert.assertTrue(
            "An attempt to download externalAsset3 did not occur",
            assetPathCaptor.allValues.contains(item7)
        )
        Assert.assertTrue(
            "An attempt to download externalAsset4 did not occur",
            assetPathCaptor.allValues.contains(item8)
        )
        Assert.assertTrue(
            "An attempt to download externalAsset5 did not occur",
            assetPathCaptor.allValues.contains(item9)
        )
        Assert.assertTrue(
            "An attempt to download externalAsset6 did not occur",
            assetPathCaptor.allValues.contains(item10)
        )

        assertCacheFileExists("asset1")
        assertCacheFileExists("asset2")
        assertCacheFileExists("asset3")
        assertCacheFileExists("asset4_which_is_a_gif" + ".gif") // Note the gif extension is used in the filename
        assertCacheFileDoesNotExist(externalAsset1Sha1) // No content type is set
        assertCacheFileExists("$externalAsset2Sha1.gif") // Note the gif extension is used in the filename
        assertCacheFileExists(externalAsset3Sha1!!) // jpeg
        assertCacheFileExists(externalAsset4Sha1!!) // png
        assertCacheFileExists(externalAsset5Sha1!!) // bmp
        assertCacheFileExists(externalAsset6Sha1!!) // jpg
    }

    @Test
    @Throws(Exception::class)
    fun testCallback() {
        server = MockWebServer()
        server!!.enqueue(MockResponse().setBody("asset2"))
        server!!.start()
        val cdnPath = server!!.url("/").toString()

        val assetsManager = SwrveAssetsManagerImp(mActivity)
        assetsManager.setCdnImages(cdnPath)
        assetsManager.setCdnFonts(cdnPath)
        assetsManager.setStorageDir(mActivity!!.cacheDir)

        val assetsQueue: MutableSet<SwrveAssetsQueueItem> = HashSet()
        assetsQueue.add(SwrveAssetsQueueItem(1, "someAsset", "someAsset", true, false))

        val callbackExecuted = AtomicBoolean(false)
        val callback =
            SwrveAssetsCompleteCallback { assetsDownloaded: Set<String?>?, sha1Verified: Boolean ->
                callbackExecuted.set(
                    true
                )
            }
        assetsManager.downloadAssets(assetsQueue, callback)

        Awaitility.await().untilTrue(callbackExecuted)
    }

    @Test
    @Throws(Exception::class)
    fun testGZIPSupport() {
        val body2 = "digest2"
        val digest2 =
            SwrveHelper.sha1(body2.toByteArray()) // this does not exist in cache at start and should be downloaded

        // Compress text sample into gzip
        val bodyBytes = ByteArrayOutputStream()
        val body = OutputStreamWriter(
            GZIPOutputStream(bodyBytes),
            Charset.forName("UTF-8")
        )
        body.write(body2)
        body.close()
        val responseBuffer = Buffer().write(bodyBytes.toByteArray())

        server = MockWebServer()
        val dispatcher: Dispatcher = object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path!!.contains("asset2")) {
                    return MockResponse().setResponseCode(200).setBody(responseBuffer)
                        .setHeader("Content-Encoding", "gzip")
                }
                return MockResponse().setResponseCode(404)
            }
        }
        server!!.dispatcher = dispatcher

        val socketFactory = setupLocalSllSocketFactory(
            server!!
        )
        mockCommonSocketFactory(socketFactory)

        server!!.start()
        val cdnPath = server!!.url("/").toString()

        val assetsManager = SwrveAssetsManagerImp(mActivity)
        assetsManager.setCdnImages(cdnPath)
        assetsManager.setCdnFonts(cdnPath)
        assetsManager.setStorageDir(mActivity!!.cacheDir)
        val assetsManagerSpy = Mockito.spy(assetsManager)

        val assetsQueue: MutableSet<SwrveAssetsQueueItem> = HashSet()
        val item2 = SwrveAssetsQueueItem(1, "asset2", digest2, true, false)
        assetsQueue.add(item2)

        assertCacheFileDoesNotExist(digest2!!)

        assetsManagerSpy.downloadAssets(assetsQueue, null) // null callback on purpose

        val assetPathCaptor = argumentCaptor<SwrveAssetsQueueItem>()
        Mockito.verify(assetsManagerSpy, Mockito.atLeastOnce())
            .downloadAsset(assetPathCaptor.capture())
        Assert.assertEquals(1, assetPathCaptor.allValues.size.toLong())
        Assert.assertTrue(
            "An attempt to download asset2 did not occur",
            assetPathCaptor.allValues.contains(item2)
        )
        assertCacheFileExists("asset2")
    }

    @Throws(Exception::class)
    private fun writeFileToCache(filename: String, text: String?) {
        val file = File(mActivity!!.cacheDir, filename)
        val fileWriter = FileWriter(file, false)
        fileWriter.write(text)
        fileWriter.close()
    }

    private fun assertCacheFileExists(fileName: String) {
        val file = File(mActivity!!.cacheDir, fileName)
        Assert.assertTrue(
            "Asset " + fileName + " should now exist in the cache at location:" + file.absolutePath,
            file.exists()
        )
    }

    private fun assertCacheFileDoesNotExist(fileName: String) {
        val file = File(mActivity!!.cacheDir, fileName)
        Assert.assertFalse(
            "Asset " + fileName + " should NOT exist in the cache at location:" + file.absolutePath,
            file.exists()
        )
    }
}