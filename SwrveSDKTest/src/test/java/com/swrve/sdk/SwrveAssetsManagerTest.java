package com.swrve.sdk;

import android.app.Activity;

import com.swrve.sdk.test.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwrveAssetsManagerTest extends SwrveBaseTest {

    private Activity mActivity;
    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        mActivity = Robolectric.buildActivity(MainActivity.class).create().get();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testFilesAlreadyDownloaded() throws Exception {
        SwrveAssetsManagerImp assetsManager = new SwrveAssetsManagerImp(mActivity);
        assetsManager.setStorageDir(mActivity.getCacheDir());
        SwrveAssetsManagerImp assetsManagerSpy = Mockito.spy(assetsManager);

        writeFileToCache("asset1", "digest1");
        writeFileToCache("asset2", "digest2");

        Set<SwrveAssetsQueueItem> assetsQueueImages = new HashSet<>();
        assetsQueueImages.add(new SwrveAssetsQueueItem("asset1", "digest1", true, false));
        assetsQueueImages.add(new SwrveAssetsQueueItem("asset2", "digest2", true, false));

        assetsManagerSpy.downloadAssets(assetsQueueImages, null);

        Mockito.verify(assetsManagerSpy, Mockito.never()).downloadAsset(Mockito.any(SwrveAssetsQueueItem.class));
    }

    @Test
    public void testExternallySourcedFilesAlreadyDownloaded() throws Exception {
        SwrveAssetsManagerImp assetsManager = new SwrveAssetsManagerImp(mActivity);
        assetsManager.setStorageDir(mActivity.getCacheDir());
        SwrveAssetsManagerImp assetsManagerSpy = Mockito.spy(assetsManager);

        final String assetUrl1 = SwrveHelper.sha1("https://www.testitem/asset1.png".getBytes()); // this should already exist (as part of this setup)
        final String assetUrl2 = SwrveHelper.sha1("https://www.testitem/asset2.png".getBytes()); // this should already exist (as part of this setup)

        writeFileToCache(assetUrl1, "https://www.testitem/asset1.png");
        writeFileToCache(assetUrl2, "https://www.testitem/asset2.png");

        Set<SwrveAssetsQueueItem> assetsQueueImages = new HashSet<>();
        assetsQueueImages.add(new SwrveAssetsQueueItem(assetUrl1, "https://www.testitem/asset1.png", true, true));
        assetsQueueImages.add(new SwrveAssetsQueueItem(assetUrl2, "https://www.testitem/asset2.png", true, true));

        assetsManagerSpy.downloadAssets(assetsQueueImages, null);

        Mockito.verify(assetsManagerSpy, Mockito.never()).downloadAssetFromExternalSource(Mockito.any(SwrveAssetsQueueItem.class));
    }

    @Test
    public void testSomeFilesAlreadyDownloaded() throws Exception {

        final String digest1 = SwrveHelper.sha1("digest1".getBytes()); // this should already exist (as part of this setup)
        final String digest2 = SwrveHelper.sha1("digest2".getBytes()); // this does not exist in cache at start and should be downloaded
        final String digest3 = SwrveHelper.sha1("digest3".getBytes()); // this does not exist in cache at start and should be downloaded

        server = new MockWebServer();
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("asset2")) {
                    return new MockResponse().setResponseCode(200).setBody("digest2");
                } else if (request.getPath().contains("asset3")) {
                    return new MockResponse().setResponseCode(200).setBody("digest3");
                } else if (request.getPath().contains("externalAsset1")) {
                    return new MockResponse().setResponseCode(200).setBody("externalAsset1");
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        server.setDispatcher(dispatcher);
        server.start();
        String cdnPath = server.url("/").toString();
        final String assetUrlSha1 = SwrveHelper.sha1((cdnPath + "externalAsset1").getBytes()); // made the url the same as the one expected by the mockwebserver for testing.

        SwrveAssetsManagerImp assetsManager = new SwrveAssetsManagerImp(mActivity);
        assetsManager.setCdnImages(cdnPath);
        assetsManager.setCdnFonts(cdnPath);
        assetsManager.setStorageDir(mActivity.getCacheDir());
        SwrveAssetsManagerImp assetsManagerSpy = Mockito.spy(assetsManager);

        writeFileToCache("asset1", digest1);

        Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
        SwrveAssetsQueueItem item1 = new SwrveAssetsQueueItem("asset1", digest1, true, false);
        SwrveAssetsQueueItem item2 = new SwrveAssetsQueueItem("asset2", digest2, true, false);
        SwrveAssetsQueueItem item3 = new SwrveAssetsQueueItem("asset3", digest3, true, false);
        SwrveAssetsQueueItem item4 = new SwrveAssetsQueueItem(assetUrlSha1, (cdnPath + "externalAsset1"), true, true);

        assetsQueue.add(item1);
        assetsQueue.add(item2);
        assetsQueue.add(item3);
        assetsQueue.add(item4);

        assertCacheFileExists("asset1");
        assertCacheFileDoesNotExist(digest2);
        assertCacheFileDoesNotExist(digest3);
        assertCacheFileDoesNotExist(assetUrlSha1);

        assetsManagerSpy.downloadAssets(assetsQueue, null); // null callback on purpose

        ArgumentCaptor<SwrveAssetsQueueItem> assetPathCaptor = ArgumentCaptor.forClass(SwrveAssetsQueueItem.class);
        Mockito.verify(assetsManagerSpy, Mockito.atLeastOnce()).downloadAsset(assetPathCaptor.capture());
        Mockito.verify(assetsManagerSpy, Mockito.atLeastOnce()).downloadAssetFromExternalSource(assetPathCaptor.capture());
        assertEquals(3, assetPathCaptor.getAllValues().size());
        assertTrue("An attempt to download asset2 did not occur", assetPathCaptor.getAllValues().contains(item2));
        assertTrue("An attempt to download asset3 did not occur", assetPathCaptor.getAllValues().contains(item3));
        assertTrue("An attempt to download asset4 did not occur", assetPathCaptor.getAllValues().contains(item4));


        assertCacheFileExists("asset1");
        assertCacheFileExists("asset2");
        assertCacheFileExists("asset3");
        assertCacheFileExists(assetUrlSha1);
    }

    @Test
    public void testCallback() throws Exception {

        server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("asset2"));
        server.start();
        String cdnPath = server.url("/").toString();

        SwrveAssetsManagerImp assetsManager = new SwrveAssetsManagerImp(mActivity);
        assetsManager.setCdnImages(cdnPath);
        assetsManager.setCdnFonts(cdnPath);
        assetsManager.setStorageDir(mActivity.getCacheDir());

        Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
        assetsQueue.add(new SwrveAssetsQueueItem("someAsset", "someAsset", true, false));

        SwrveAssetsCompleteCallback callback = new SwrveAssetsCompleteCallback() {
            @Override
            public void complete() {
                // empty
            }
        };
        SwrveAssetsCompleteCallback callbackSpy = Mockito.spy(callback);
        assetsManager.downloadAssets(assetsQueue, callbackSpy);

        Mockito.verify(callbackSpy, Mockito.atLeastOnce()).complete();
    }

    @Test
    public void testGZIPSupport() throws Exception {
        final String body2 = "digest2";
        final String digest2 = SwrveHelper.sha1(body2.getBytes()); // this does not exist in cache at start and should be downloaded

        // Compress text sample into gzip
        final ByteArrayOutputStream bodyBytes = new ByteArrayOutputStream();
        OutputStreamWriter body = new OutputStreamWriter(new GZIPOutputStream(bodyBytes),
                Charset.forName("UTF-8"));
        body.write(body2);
        body.close();
        final Buffer responseBuffer = new Buffer().write(bodyBytes.toByteArray());

        server = new MockWebServer();
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains("asset2")) {
                    return new MockResponse().setResponseCode(200).setBody(responseBuffer).setHeader("Content-Encoding", "gzip");
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        server.setDispatcher(dispatcher);
        server.start();
        String cdnPath = server.url("/").toString();

        SwrveAssetsManagerImp assetsManager = new SwrveAssetsManagerImp(mActivity);
        assetsManager.setCdnImages(cdnPath);
        assetsManager.setCdnFonts(cdnPath);
        assetsManager.setStorageDir(mActivity.getCacheDir());
        SwrveAssetsManagerImp assetsManagerSpy = Mockito.spy(assetsManager);

        Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
        SwrveAssetsQueueItem item2 = new SwrveAssetsQueueItem("asset2", digest2, true, false);
        assetsQueue.add(item2);

        assertCacheFileDoesNotExist(digest2);

        assetsManagerSpy.downloadAssets(assetsQueue, null); // null callback on purpose

        ArgumentCaptor<SwrveAssetsQueueItem> assetPathCaptor = ArgumentCaptor.forClass(SwrveAssetsQueueItem.class);
        Mockito.verify(assetsManagerSpy, Mockito.atLeastOnce()).downloadAsset(assetPathCaptor.capture());
        assertEquals(1, assetPathCaptor.getAllValues().size());
        assertTrue("An attempt to download asset2 did not occur", assetPathCaptor.getAllValues().contains(item2));
        assertCacheFileExists("asset2");
    }

    private void writeFileToCache(String filename, String text) throws Exception {
        File file = new File(mActivity.getCacheDir(), filename);
        FileWriter fileWriter = new FileWriter(file, false);
        fileWriter.write(text);
        fileWriter.close();
    }

    private void assertCacheFileExists(String fileName) {
        File file = new File(mActivity.getCacheDir(), fileName);
        assertTrue("Asset " + fileName + " should now exist in the cache at location:" + file.getAbsolutePath(), file.exists());
    }

    private void assertCacheFileDoesNotExist(String fileName) {
        File file = new File(mActivity.getCacheDir(), fileName);
        assertFalse("Asset " + fileName + " should NOT exist in the cache at location:" + file.getAbsolutePath(), file.exists());
    }
}