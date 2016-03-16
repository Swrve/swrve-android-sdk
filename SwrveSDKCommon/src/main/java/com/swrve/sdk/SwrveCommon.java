package com.swrve.sdk;


class SwrveCommon {
    private static final String LOG_TAG = "SwrveCommon";
    private static ISwrveCommon instance;
    private static Runnable toRunIfNull;

    public static void setRunnable(Runnable toRunIfNull) {
        SwrveCommon.toRunIfNull = toRunIfNull;
    }

    protected static void checkInstanceCreated() throws RuntimeException {
        if((instance == null) && (toRunIfNull != null)) {
            toRunIfNull.run();
        }

        if (instance == null) {
            SwrveLogger.e(LOG_TAG, "Please call SwrveSDK.createInstance first in your Application class.");
            throw new RuntimeException("Please call SwrveSDK.createInstance first in your Application class.");
        }
    }

    public static void setSwrveCommon(ISwrveCommon swrveCommon) {
        instance = swrveCommon;
    }

    public static ISwrveCommon getInstance() {
        return instance;
    }
}
