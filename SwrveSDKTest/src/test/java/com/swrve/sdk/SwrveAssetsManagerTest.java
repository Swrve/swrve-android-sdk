package com.swrve.sdk;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwrveAssetsManagerTest extends SwrveBaseTest {

    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testFilesAlreadyDownloaded() throws Exception {
        SwrveAssetsManagerImp assetsManager = new SwrveAssetsManagerImp(mActivity, "http://www.fakecdn.com/");
        assetsManager.setStorageDir(mActivity.getCacheDir());
        SwrveAssetsManagerImp assetsManagerSpy = Mockito.spy(assetsManager);

        writeFileToCache("asset1");
        writeFileToCache("asset2");

        Set<String> assetsQueue = new HashSet<>();
        assetsQueue.add("asset1");
        assetsQueue.add("asset2");

        assetsManagerSpy.downloadAssets(assetsQueue, null);

        ArgumentCaptor<String> assetPathCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(assetsManagerSpy, Mockito.never()).downloadAsset(assetPathCaptor.capture());
    }

    @Test
    public void testSomeFilesAlreadyDownloaded() throws Exception {

        final String asset1 = SwrveHelper.sha1("asset1".getBytes()); // this should already exist (as part of this setup)
        final String asset2 = SwrveHelper.sha1("asset2".getBytes()); // this does not exist in cache at start and should be downloaded
        final String asset3 = SwrveHelper.sha1("asset3".getBytes()); // this does not exist in cache at start and should be downloaded

        server = new MockWebServer();
        final Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().contains(asset2)){
                    return new MockResponse().setResponseCode(200).setBody("asset2");
                } else if (request.getPath().contains(asset3)){
                    return new MockResponse().setResponseCode(200).setBody("asset3");
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        server.setDispatcher(dispatcher);
        server.start();
        String cdnPath = server.url("/").toString();

        SwrveAssetsManagerImp assetsManager = new SwrveAssetsManagerImp(mActivity, cdnPath);
        assetsManager.setStorageDir(mActivity.getCacheDir());
        SwrveAssetsManagerImp assetsManagerSpy = Mockito.spy(assetsManager);

        writeFileToCache(asset1);

        Set<String> assetsQueue = new HashSet<>();
        assetsQueue.add(asset1);
        assetsQueue.add(asset2);
        assetsQueue.add(asset3);

        assertCacheFileExists(asset1);
        assertCacheFileDoesNotExist(asset2);
        assertCacheFileDoesNotExist(asset3);

        assetsManagerSpy.downloadAssets(assetsQueue, null); // null callback on purpose

        ArgumentCaptor<String> assetPathCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(assetsManagerSpy, Mockito.atLeastOnce()).downloadAsset(assetPathCaptor.capture());
        assertEquals(2, assetPathCaptor.getAllValues().size());
        assertTrue("An attempt to download asset2 did not occur", assetPathCaptor.getAllValues().contains(asset2));
        assertTrue("An attempt to download asset3 did not occur", assetPathCaptor.getAllValues().contains(asset3));

        assertCacheFileExists(asset1);
        assertCacheFileExists(asset2);
        assertCacheFileExists(asset3);
    }

    @Test
    public void testCallback() throws Exception {

        server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("asset2"));
        server.start();
        String cdnPath = server.url("/").toString();

        SwrveAssetsManagerImp assetsManager = new SwrveAssetsManagerImp(mActivity, cdnPath);
        assetsManager.setStorageDir(mActivity.getCacheDir());

        Set<String> assetsQueue = new HashSet<>();
        assetsQueue.add("someAsset");

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

    private void writeFileToCache(String filename) throws Exception {
        File file = new File(mActivity.getCacheDir(), filename);
        FileWriter fileWriter = new FileWriter(file, false);
        fileWriter.write("empty");
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