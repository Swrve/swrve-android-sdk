package com.swrve.sdk;


public class SwrveCommon {
    private static final String LOG_TAG = "SwrveCommon";
    private static ISwrveCommon swrveCommon;

    protected static void checkInstanceCreated() throws RuntimeException {
        if (swrveCommon == null) {
            SwrveLogger.e(LOG_TAG, "Please call SwrveSDK.createInstance first in your Application class.");
            throw new RuntimeException("Please call SwrveSDK.createInstance first in your Application class.");
        }
    }

    public static void setSwrveCommon(ISwrveCommon swrveCommon) {
        SwrveCommon.swrveCommon = swrveCommon;
    }

    public static ISwrveCommon getSwrveCommon() {
        return swrveCommon;
    }
}
