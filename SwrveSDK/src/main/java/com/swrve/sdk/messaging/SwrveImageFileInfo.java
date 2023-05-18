package com.swrve.sdk.messaging;

import com.swrve.sdk.SwrveImageScaler;

class SwrveImageFileInfo {

    final String filePath;
    final boolean usingDynamic;
    final boolean isGif;
    final SwrveImageScaler.BitmapResult image;

    protected SwrveImageFileInfo(String filePath, boolean usingDynamic, boolean isGif, SwrveImageScaler.BitmapResult image) {
        this.filePath = filePath;
        this.usingDynamic = usingDynamic;
        this.isGif = isGif;
        this.image = image;
    }
}
