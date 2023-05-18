package com.swrve.sdk;

import android.content.Context;

import com.swrve.sdk.rest.SwrveFilterInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

class SwrveAssetsManagerImp implements SwrveAssetsManager {

    private static final String SHA1_TEST_STRING = "Lorem ipsum dolor sit amet";
    private static final String SHA1_TEST_STRING_RESULT = "38f00f8738e241daea6f37f6f55ae8414d7b0219";

    private static int WORKER_THREAD_POOL_SIZE = 10;
    private static int TIMEOUT = 60000;

    protected Set<String> assetsOnDisk = new HashSet<>();

    protected final Context context;
    protected String cdnImages;
    protected String cdnFonts;
    protected File storageDir;

    protected SwrveAssetsManagerImp(Context context) {
        this.context = context;
    }

    @Override
    public void setCdnImages(String cdnImages) {
        this.cdnImages = cdnImages;
    }

    @Override
    public void setCdnFonts(String cdnFonts) {
        this.cdnFonts = cdnFonts;
    }

    @Override
    public void setStorageDir(File storageDir) {
        this.storageDir = storageDir;
    }

    @Override
    public File getStorageDir() {
        return storageDir;
    }

    @Override
    public Set<String> getAssetsOnDisk() {
        synchronized (assetsOnDisk) {
            return this.assetsOnDisk;
        }
    }

    @Override
    public void downloadAssets(Set<SwrveAssetsQueueItem> assetsQueue, SwrveAssetsCompleteCallback callback) {

        boolean sha1Verified = true;
        Set<String> assetsDownloaded = null;
        if (!storageDir.canWrite()) {
            SwrveLogger.e("Could not download assets because do not have write access to storageDir:%s", storageDir);
        } else if (!verifySha1()) {
            SwrveLogger.e("The SwrveAssetsManager failed sha1 verification so assets will not be downloaded.");
            sha1Verified = false;
        } else {
            assetsDownloaded = downloadAssets(assetsQueue);
        }
        if (callback != null) {
            callback.complete(assetsDownloaded, sha1Verified);
        }
    }

    private boolean verifySha1() {
        if (SwrveCommon.getInstance() == null || SwrveCommon.getInstance().getAppId() != ASSET_DOWNLOAD_LIMITS_APP_ID) {
            return true;
        } else {
            String testSha1 = SwrveHelper.sha1(SHA1_TEST_STRING.getBytes(StandardCharsets.UTF_8));
            return SHA1_TEST_STRING_RESULT.equals(testSha1);
        }
    }

    protected Set<String> downloadAssets(final Set<SwrveAssetsQueueItem> assetsQueue) {
        Set<String> assetsDownloaded = new HashSet<>();
        if (assetsQueue == null) {
            return assetsDownloaded;
        }

        Set<SwrveAssetsQueueItem> assetsToDownload = filterExistingFiles(assetsQueue);
        try {
            CountDownLatch latch = new CountDownLatch(assetsToDownload.size());
            ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREAD_POOL_SIZE);
            for (SwrveAssetsQueueItem assetItem : assetsToDownload) {
                pool.submit(() -> {
                    boolean success;
                    if (assetItem.isExternalSource()) {
                        success = downloadAssetFromExternalSource(assetItem);
                    } else {
                        success = downloadAsset(assetItem);
                    }

                    if (success) {
                        synchronized (assetsOnDisk) {
                            assetsOnDisk.add(assetItem.getName()); // store the font name
                            assetsDownloaded.add(assetItem.getName());
                        }
                    }
                    latch.countDown();
                });
            }
            latch.await();
        } catch (Exception e) {
            SwrveLogger.e("Error downloading assets.", e);
        }
        return assetsDownloaded;
    }

    protected Set<SwrveAssetsQueueItem> filterExistingFiles(Set<SwrveAssetsQueueItem> assetsQueue) {
        Iterator<SwrveAssetsQueueItem> itDownloadQueue = assetsQueue.iterator();
        while (itDownloadQueue.hasNext()) {
            SwrveAssetsQueueItem item = itDownloadQueue.next();
            if (isDownloaded(item.getName())) {
                itDownloadQueue.remove();
                synchronized (assetsOnDisk) {
                    assetsOnDisk.add(item.getName()); // store the font name
                }
            }
        }
        return assetsQueue;
    }

    // Check if asset is already downloaded. Some assets will have additional file extension type appended to the asset name
    private boolean isDownloaded(String assetName) {
        boolean isDownloaded = false;
        File file = new File(storageDir, assetName);
        if (file.exists()) { // check file exists without any extension
            isDownloaded = true;
        } else {
            for (Map.Entry<String, String> entry : SwrveAssetsTypes.MIMETYPES.entrySet()) { // check file exists with additional supported extensions
                String assetNameWithExtension = assetName + entry.getValue();
                file = new File(storageDir, assetNameWithExtension);
                if (file.exists()) {
                    isDownloaded = true;
                    break;
                }
            }
        }
        return isDownloaded;
    }

    protected boolean downloadAsset(final SwrveAssetsQueueItem assetItem)  {
        boolean success = false;
        String cdnRoot = assetItem.isImage() ? cdnImages : cdnFonts;
        if (SwrveHelper.isNullOrEmpty(cdnRoot)) {
            SwrveLogger.e("Error downloading asset. No cdn url for %s", assetItem);
            return success;
        }

        String url;
        String additionalParams = getAdditionalParams(assetItem);
        if (additionalParams == null) {
            url = cdnRoot + assetItem.getName();
        } else {
            url = cdnRoot + assetItem.getName() + "?" + additionalParams;
        }
        InputStream inputStream = null;
        try {
            URL assetUrl = new URL(url);
            HttpsURLConnection httpsConnection = (HttpsURLConnection) assetUrl.openConnection();
            if (SwrveCommon.getInstance().getSSLSocketFactoryConfig() != null) {
                SSLSocketFactory socketFactory = SwrveCommon.getInstance().getSSLSocketFactoryConfig().getFactory(assetUrl.getHost());
                if (socketFactory != null) {
                    httpsConnection.setSSLSocketFactory(socketFactory);
                }
            }
            httpsConnection.setRequestProperty("Accept-Encoding", "gzip");

            inputStream = new SwrveFilterInputStream(httpsConnection.getInputStream());

            // Support gzip if possible
            String encoding = httpsConnection.getContentEncoding();
            if (encoding != null && encoding.toLowerCase(Locale.ENGLISH).contains("gzip")) {
                inputStream = new GZIPInputStream(inputStream);
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                stream.write(buffer, 0, bytesRead);
            }
            byte[] fileContents = stream.toByteArray();
            String sha1File = SwrveHelper.sha1(fileContents);
            if (assetItem.getDigest().equals(sha1File)) {
                String fileAssetName = getFileAssetName(assetItem.getName(), httpsConnection.getContentType());
                FileOutputStream fileStream = new FileOutputStream(new File(storageDir, fileAssetName));
                fileStream.write(fileContents); // Save to file
                fileStream.close();
                success = true;
            } else {
                SwrveLogger.e("Error downloading assetItem:%s. Did not match digest:%s", assetItem, sha1File);
            }
        } catch (Exception e) {
            SwrveLogger.e("Error downloading asset:%s", e, assetItem);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    SwrveLogger.e("Error closing assets stream.", e);
                }
            }
        }
        return success;
    }

    private String getAdditionalParams(SwrveAssetsQueueItem assetItem) {
        String additionalParams = null;
        if (SwrveCommon.getInstance() == null || SwrveCommon.getInstance().getAppId() != ASSET_DOWNLOAD_LIMITS_APP_ID) {
            return additionalParams;
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("app_id", String.valueOf(SwrveCommon.getInstance().getAppId()));
            params.put("campaign_id", String.valueOf(assetItem.getCampaignId()));
            params.put("client_time", String.valueOf(System.currentTimeMillis()));
            String userId = SwrveCommon.getInstance().getUserId();
            if (SwrveHelper.isNotNullOrEmpty(userId)) {
                params.put("user_id", userId);
            }
            String deviceId = SwrveCommon.getInstance().getDeviceId();
            if (SwrveHelper.isNotNullOrEmpty(deviceId)) {
                params.put("device_id", deviceId);
            }
            long usableSpaceBytes = new File(storageDir.getAbsoluteFile().toString()).getUsableSpace();
            params.put("usable_space", String.valueOf(usableSpaceBytes));
            params.put("dc", String.valueOf(assetItem.getDownloadCount())); // download_count

            additionalParams = SwrveHelper.encodeParameters(params);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK: Error adding additional params to SwrveAssetsManager request.", e);
        }
        return additionalParams;
    }

    protected boolean downloadAssetFromExternalSource(final SwrveAssetsQueueItem assetItem) {
        boolean success = false;

        // since this is coming from a source other than the Swrve CDN. Digest is the url
        String url = assetItem.getDigest();
        if (SwrveHelper.isNullOrEmpty(url)) {
            SwrveLogger.e("Error downloading asset. No cdn url for %s", assetItem);
            return success;
        }

        InputStream inputStream = null;
        try {
            URL assetUrl = new URL(url);
            HttpsURLConnection httpsConnection = (HttpsURLConnection) assetUrl.openConnection();
            if (SwrveCommon.getInstance().getSSLSocketFactoryConfig() != null) {
                SSLSocketFactory socketFactory = SwrveCommon.getInstance().getSSLSocketFactoryConfig().getFactory(assetUrl.getHost());
                if (socketFactory != null) {
                    httpsConnection.setSSLSocketFactory(socketFactory);
                }
            }
            httpsConnection.setRequestProperty("Accept-Encoding", "gzip");
            httpsConnection.setReadTimeout(TIMEOUT);
            httpsConnection.setConnectTimeout(TIMEOUT);
            inputStream = new SwrveFilterInputStream(httpsConnection.getInputStream());

            // Support gzip if possible
            String encoding = httpsConnection.getContentEncoding();
            if (encoding != null && encoding.toLowerCase(Locale.ENGLISH).contains("gzip")) {
                inputStream = new GZIPInputStream(inputStream);
            }

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                stream.write(buffer, 0, bytesRead);
            }

            if (isSupportedContentType(httpsConnection.getContentType())) {
                // asset name is a sha1 of the url done in constructor
                String fileAssetName = getFileAssetName(assetItem.getName(), httpsConnection.getContentType());
                FileOutputStream fileStream = new FileOutputStream(new File(storageDir, fileAssetName));
                byte[] fileContents = stream.toByteArray();
                fileStream.write(fileContents); // Save to file
                fileStream.close();
                success = true;
            } else {
                SwrveLogger.e("Asset cannot be downloaded. External url content type:%s is not supported. Asset:%s", httpsConnection.getContentType(), assetItem);
                QaUser.assetFailedToDownload(assetItem.getName(), url, "External url content type is not supported.");
            }
        } catch (MalformedURLException e) {
            SwrveLogger.e("Error downloading asset: %s", e, assetItem);
            QaUser.assetFailedToDownload(assetItem.getName(), url, "Image url was malformed");
        } catch (UnknownHostException e) {
            SwrveLogger.e("Error downloading asset: %s", e, assetItem);
            QaUser.assetFailedToDownload(assetItem.getName(), url, "Host name could not be resolved");
        } catch (IOException e) {
            SwrveLogger.e("Error downloading asset: %s", e, assetItem);
            QaUser.assetFailedToDownload(assetItem.getName(), url, "Asset file could not be retrieved");
        } catch (Exception e) {
            SwrveLogger.e("Error downloading asset: %s", e, assetItem);
            QaUser.assetFailedToDownload(assetItem.getName(), url, "Asset could not be downloaded");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    SwrveLogger.e("Error closing assets stream.", e);
                }
            }
        }
        return success;
    }

    private boolean isSupportedContentType(String mimeType) {
        boolean isSupportedContentType = true;
        if (mimeType == null || !SwrveAssetsTypes.MIMETYPES.containsKey(mimeType)) {
            isSupportedContentType = false;
        }
        return isSupportedContentType;
    }

    // Some mimeTypes are saved with file extension, but default is to just use the assetName
    private String getFileAssetName(String assetName, String mimeType) {
        String downloadedAssetName = assetName;
        if (mimeType != null && mimeType.equalsIgnoreCase("image/gif")) {
            downloadedAssetName = downloadedAssetName + SwrveAssetsTypes.MIMETYPES.get(mimeType);
        }
        return downloadedAssetName;
    }
}
