package com.swrve.sdk;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SwrveAssetsManager {
    private final String LOG_TAG = "SwrveAssetsManager";
    private final String cdnRoot;
    protected Set<String> assetsOnDisk;
    public int downloadedAssets = 0;
    protected boolean assetsCurrentlyDownloading;
    protected File imageCacheDir;


    protected SwrveAssetsManager(File imageCacheDir, String cdnRoot) {
        this.imageCacheDir = imageCacheDir;
        this.cdnRoot = cdnRoot;
        this.assetsOnDisk = new HashSet<String>();
        this.assetsCurrentlyDownloading = false;
    }

    public String getCDNUrlForAssetPath(String assetPath) {
        return cdnRoot + assetPath;
    }

    public boolean isAssetsCurrentlyDownloading() {
        return assetsCurrentlyDownloading;
    }

    public void setAssetsCurrentlyDownloading(boolean assetsCurrentlyDownloading) {
        this.assetsCurrentlyDownloading = assetsCurrentlyDownloading;
    }

    public void addAsset(String asset) {
        if (containsAsset(asset)) {
            SwrveLogger.w(LOG_TAG, "");
            return;
        } else {
            assetsOnDisk.add(asset);
            downloadedAssets++;
        }
    }

    public boolean containsAsset(String asset) {
        return assetsOnDisk.contains(asset);
    }

    public Set<String> getAssetsOnDisk() {
        return this.assetsOnDisk;
    }

    public int getDownloadedAssets() {
        return this.downloadedAssets;
    }
}
