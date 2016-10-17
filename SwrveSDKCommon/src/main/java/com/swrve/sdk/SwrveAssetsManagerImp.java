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
import java.util.Set;

import static com.swrve.sdk.SwrveHelper.LOG_TAG;

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
    public void downloadAssets(Set<String> assetsQueueImages, Set<String> assetsQueueFonts, SwrveAssetsCompleteCallback callback) {

        if (!storageDir.canWrite()) {
            SwrveLogger.e(LOG_TAG, "Could not download assets because do not have write access to storageDir:" + storageDir);
        } else {
            downloadAssets(assetsQueueImages, cdnImages);
            downloadAssets(assetsQueueFonts, cdnFonts);
        }
        if (callback != null) {
            callback.complete();
        }
    }

    protected void downloadAssets(final Set<String> assetsQueue, final String cdnRoot) {
        if (SwrveHelper.isNullOrEmpty(cdnRoot)) {
            SwrveLogger.e(LOG_TAG, "Error downloading assets. No cdnRoot url.");
            return;
        }
        if(assetsQueue == null) {
            return;
        }

        Set<String> assetsToDownload = filterExistingFiles(assetsQueue);
        for (String asset : assetsToDownload) {
            boolean success = downloadAsset(asset, cdnRoot);
            if (success) {
                synchronized (assetsOnDisk) {
                    assetsOnDisk.add(asset);
                }
            }
        }
    }

    protected Set<String> filterExistingFiles(Set<String> assetsQueue) {
        Iterator<String> itDownloadQueue = assetsQueue.iterator();
        while (itDownloadQueue.hasNext()) {
            String assetPath = itDownloadQueue.next();
            File file = new File(storageDir, assetPath);
            if (file.exists()) {
                itDownloadQueue.remove();
                synchronized (assetsOnDisk) {
                    assetsOnDisk.add(assetPath);
                }
            }
        }
        return assetsQueue;
    }

    protected boolean downloadAsset(final String assetPath, final String cdnRoot) {
        boolean success = false;
        String url = cdnRoot + assetPath;
        InputStream inputStream = null;
        try {
            URLConnection openConnection = new URL(url).openConnection();
            inputStream = new SwrveFilterInputStream(openConnection.getInputStream());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                stream.write(buffer, 0, bytesRead);
            }
            byte[] fileContents = stream.toByteArray();
            String sha1File = SwrveHelper.sha1(stream.toByteArray());
            if (assetPath.equals(sha1File)) {
                FileOutputStream fileStream = new FileOutputStream(new File(storageDir, assetPath));
                fileStream.write(fileContents); // Save to file
                fileStream.close();
                success = true;
            } else {
                SwrveLogger.e(LOG_TAG, "Error downloading asset:" + assetPath + ". Did not match sha:" + sha1File);
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error downloading asset:" + assetPath, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    SwrveLogger.e(LOG_TAG, "Error closing assets stream.", e);
                }
            }
        }
        return success;
    }
}