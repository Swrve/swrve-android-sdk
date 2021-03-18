package com.swrve.sdk;

import android.app.Application;
import android.content.Context;

import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

import static com.swrve.sdk.SwrveFlavour.CORE;

/**
 * Main object used to implement the Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {
    protected static final SwrveFlavour FLAVOUR = CORE;

    protected Swrve(Application application, int appId, String apiKey, SwrveConfig config) {
        super(application, appId, apiKey, config);
    }

    @Override
    protected void beforeSendDeviceInfo(Context context) {
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
    }

    @Override
    protected String getPlatformOS(Context context) {
        return SwrveHelper.getPlatformOS(context);
    }
}
