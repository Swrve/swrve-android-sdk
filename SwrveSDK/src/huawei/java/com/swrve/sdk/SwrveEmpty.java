package com.swrve.sdk;

import android.content.Context;
import com.swrve.sdk.config.SwrveConfig;

/**
 * Empty implementation of the Swrve Huawei SDK. Will be returned when the SDK is used from an unsupported runtime version.
 */
public class SwrveEmpty extends SwrveBaseEmpty<ISwrve, SwrveConfig> implements ISwrve {

    protected SwrveEmpty(Context context, String apiKey) {
        super(context, apiKey);
    }

    @Override
    public void setRegistrationId(String registrationId) {
        // empty
    }
}
