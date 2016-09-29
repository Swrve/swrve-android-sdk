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

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

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

        String asset2 = SwrveHelper.sha1("asset2".getBytes());
        String asset3 = SwrveHelper.sha1("asset3".getBytes());

        server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("asset2"));
        server.enqueue(new MockResponse().setBody("asset3"));
        server.start();
        String cdnPath = server.url("/").toString();

        SwrveAssetsManagerImp assetsManager = new SwrveAssetsManagerImp(mActivity, cdnPath);
        assetsManager.setStorageDir(mActivity.getCacheDir());
        SwrveAssetsManagerImp assetsManagerSpy = Mockito.spy(assetsManager);

        writeFileToCache("asset1");

        Set<String> assetsQueue = new HashSet<>();
        assetsQueue.add("asset1");
        assetsQueue.add(asset2);
        assetsQueue.add(asset3);

        assertCacheFileExists("asset1");
        assertCacheFileDoesNotExist(asset2);
        assertCacheFileDoesNotExist(asset3);

        assetsManagerSpy.downloadAssets(assetsQueue, null); // null callback on purpose

        ArgumentCaptor<String> assetPathCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(assetsManagerSpy, Mockito.atLeastOnce()).downloadAsset(assetPathCaptor.capture());
        assertEquals(2, assetPathCaptor.getAllValues().size());
        assertEquals(asset2, assetPathCaptor.getAllValues().get(0));
        assertEquals(asset3, assetPathCaptor.getAllValues().get(1));

        assertCacheFileExists("asset1");
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
        assertTrue("Asset " + fileName + " should now exist in the cache.", file.exists());
    }

    private void assertCacheFileDoesNotExist(String fileName) {
        File file = new File(mActivity.getCacheDir(), fileName);
        assertFalse("Asset " + fileName + " should not exist in the cache.", file.exists());
    }
}
