package com.swrve.sdk;

import android.content.Context;

import com.swrve.sdk.rest.SwrveFilterInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;

class SwrveAssetsManagerImp implements SwrveAssetsManager {

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

        if (!storageDir.canWrite()) {
            SwrveLogger.e("Could not download assets because do not have write access to storageDir:%s", storageDir);
        } else {
            downloadAssets(assetsQueue);
        }
        if (callback != null) {
            callback.complete();
        }
    }

    protected void downloadAssets(final Set<SwrveAssetsQueueItem> assetsQueue) {
        if(assetsQueue == null) {
            return;
        }

        Set<SwrveAssetsQueueItem> assetsToDownload = filterExistingFiles(assetsQueue);
        for (SwrveAssetsQueueItem assetItem : assetsToDownload) {
            boolean success = downloadAsset(assetItem);
            if (success) {
                synchronized (assetsOnDisk) {
                    assetsOnDisk.add(assetItem.getName()); // store the font name
                }
            }
        }
    }

    protected Set<SwrveAssetsQueueItem> filterExistingFiles(Set<SwrveAssetsQueueItem> assetsQueue) {
        Iterator<SwrveAssetsQueueItem> itDownloadQueue = assetsQueue.iterator();
        while (itDownloadQueue.hasNext()) {
            SwrveAssetsQueueItem item = itDownloadQueue.next();
            File file = new File(storageDir, item.getName());
            if (file.exists()) {
                itDownloadQueue.remove();
                synchronized (assetsOnDisk) {
                    assetsOnDisk.add(item.getName()); // store the font name
                }
            }
        }
        return assetsQueue;
    }

    protected boolean downloadAsset(final SwrveAssetsQueueItem assetItem) {
        boolean success = false;
        String cdnRoot = assetItem.isImage() ? cdnImages : cdnFonts;
        if (SwrveHelper.isNullOrEmpty(cdnRoot)) {
            SwrveLogger.e("Error downloading asset. No cdn url for %s", assetItem);
            return success;
        }

        String url = cdnRoot + assetItem.getName();
        InputStream inputStream = null;
        try {
            URLConnection openConnection = new URL(url).openConnection();
            openConnection.setRequestProperty("Accept-Encoding", "gzip");

            inputStream = new SwrveFilterInputStream(openConnection.getInputStream());

            // Support gzip if possible
            String encoding = openConnection.getContentEncoding();
            if (encoding != null && encoding.toLowerCase(Locale.ENGLISH).indexOf("gzip") != -1) {
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
                FileOutputStream fileStream = new FileOutputStream(new File(storageDir, assetItem.getName()));
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
}