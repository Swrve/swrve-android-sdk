package com.swrve.sdk;

import android.content.Context;

/**
 * Use this class to get static access to the singleton instance the Swrve Google SDK.
 */
public class SwrveInstance {
    private static ISwrve instance;

    // TODO DOM do we need to create a method that takes an additonal config?

    /**
     * Get a singleton Swrve Google SDK.
     * @param context your activity or application context // TODO DOM review this comment
     * @param appId   your app id in the Swrve dashboard
     * @param apiKey  your app api_key in the Swrve dashboard
     * @return singleton SDK instance.
     */
    public static synchronized ISwrve createInstance(Context context, int appId, String apiKey) {
        instance = SwrveFactory.createInstance(context, appId, apiKey);
        return instance;
    }

    /**
     * Get a singleton Swrve SDK.
     * @return singleton SDK instance.
     */
    public static synchronized ISwrve getInstance() {
        if (instance == null) {
            throw new RuntimeException("Must call SwrveInstance.createInstance first."); // TODO DOM should we do throw this? Probably not!
            //instance = SwrveFactory.createInstance();
        }
        return instance;
    }
}
