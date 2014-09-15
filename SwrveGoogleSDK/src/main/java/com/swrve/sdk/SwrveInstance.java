package com.swrve.sdk;

/**
 * Use this class to get static access to the singleton instance the Swrve Google SDK.
 */
public class SwrveInstance {
    private static ISwrve instance;

    /**
     * Get a singleton Swrve Google SDK.
     * @return singleton SDK instance.
     */
    public static synchronized ISwrve getInstance() {
        if (instance == null) {
            instance = SwrveFactory.createInstance();
        }
        return instance;
    }
}
