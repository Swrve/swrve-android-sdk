package com.swrve.sdk;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class SwrveAssetsManager {
    private final String LOG_TAG = "SwrveAssetsManager";
    private final String cdnRoot;
    protected Set<String> assetsOnDisk;
    private Set<String> failedAssets;
    protected boolean assetsCurrentlyDownloading;
    protected File imageCacheDir;


    public SwrveAssetsManager(File imageCacheDir, String cdnRoot) {
        this.imageCacheDir = imageCacheDir;
        this.cdnRoot = cdnRoot;
        this.assetsOnDisk = new HashSet<String>();
        this.failedAssets = new HashSet<String>();
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
        failedAssets.remove(asset);
        if (containsAsset(asset)) {
            SwrveLogger.w(LOG_TAG, "Asset already present in SwrveAssetManager");
            return;
        } else {
            assetsOnDisk.add(asset);
        }
    }

    public void addFailedAsset(String asset) {
        failedAssets.add(asset);
    }

    public boolean containsAsset(String asset) {
        return assetsOnDisk.contains(asset);
    }

    public boolean hasFailedAssets() {
        return failedAssets.size() > 0;
    }

    public Set<String> getFailedAssets(){
        return this.failedAssets;
    }

    public Set<String> getAssetsOnDisk() {
        return this.assetsOnDisk;
    }
}
