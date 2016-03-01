package com.swrve.sdk;


public class SwrveCommon {
    private static final String LOG_TAG = "SwrveCommon";
    private static ISwrveCommon swrveCommon;
    private static Runnable toRunIfNull;

    public static void setRunnable(Runnable toRunIfNull) {
        SwrveCommon.toRunIfNull = toRunIfNull;
    }

    protected static void checkInstanceCreated() throws RuntimeException {
        if((swrveCommon == null) && (toRunIfNull != null)) {
            toRunIfNull.run();
        }

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
