package com.swrve.sdk;

import java.io.File;
import java.util.Set;

/**
 * Used internally to download assets to a file directory (usually the internal cache dir).
 */
interface SwrveAssetsManager {

    /**
     * The root url of the cdn to download image assets. Should end in a forward slash.
     * @param cdnImages The image cdn url
     */
    void setCdnImages(String cdnImages);

    /**
     * The root url of the cdn to download font assets. Should end in a forward slash.
     * @param cdnFonts The font cdn url
     */
    void setCdnFonts(String cdnFonts);

    /**
     * The directory to store the asset when its downloaded. Assumes permission has been granted.
     * @param storageDir The file dir to store assets.
     */
    void setStorageDir(File storageDir);

    /**
     * The directory where assets are downloaded. Assumes permission has been granted.
     * @return the file dir where assets are stored.
     */
    File getStorageDir();

    /**
     * Gets the current set of downloaded assets. (contains both image and font assets)
     * @return a set of assets strings.
     */
    Set<String> getAssetsOnDisk();

    /**
     * Download a set of assets from the configured cdn. Checks if asset is already downloaded first.
     * @param assetsImages A collection of assets to download.
     * @param callback Executed when assets are downloaded. Not executed if there's an error.
     */
    void downloadAssets(final Set<SwrveAssetsQueueItem> assetsImages, final SwrveAssetsCompleteCallback callback);

}