package com.swrve.sdk;

import java.util.Set;

/**
 * * User internally to execute code when assets are finished downloading.
 */
interface SwrveAssetsCompleteCallback {
    void complete(Set<String> assetsDownloaded, boolean sha1Verified);
}
