package com.swrve.sdk;

import android.content.Context;

/**
 * Use this class to obtain an instance of the Swrve SDK.
 */
public class SwrveFactory extends SwrveFactoryBase {

    // TODO DOM should I make this class final?
    /**
     * Create a new instance of the Swrve SDK.
     * @return new instance of the SDK.
     */
    protected static ISwrve createInstance(Context context, final int appId, final String apiKey) {
        if (sdkAvailable()) {
            return new Swrve(context, appId, apiKey);
        }

        return new SwrveEmpty(context, apiKey);
    }
}
