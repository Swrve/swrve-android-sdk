package com.swrve.sdk;

import android.content.Context;

import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main object used to implement the Swrve SDK.  You can obtain an instance of this class using the SwrveFactory or
 * SwrveInstance that creates a singleton Swrve object.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {

    protected Swrve() {
    }

    @Override
    protected SwrveConfig defaultConfig() {
        return new SwrveConfig();
    }

    @Override
    protected void beforeSendDeviceInfo(Context context) {
    }

    @Override
    protected void afterInit() {
    }

    @Override
    protected void afterBind() {
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
    }
}
