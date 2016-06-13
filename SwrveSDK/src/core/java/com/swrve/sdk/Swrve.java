package com.swrve.sdk;

import android.content.Context;

import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main object used to implement the Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {

    protected Swrve(Context context, int appId, String apiKey, SwrveConfig config) {
        super(context, appId, apiKey, config);
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
