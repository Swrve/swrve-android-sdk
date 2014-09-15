package com.swrve.sdk;

/**
 * Used internally to determine if the Android runtime version is supported.
 */
public abstract class SwrveFactoryBase {

    public static boolean sdkAvailable() {
        // Returns true if current SDK is higher or equal than 2.3.3 (API 10)
        return (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1);
    }
}
