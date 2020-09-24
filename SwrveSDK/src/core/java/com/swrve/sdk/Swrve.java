package com.swrve.sdk;

import android.app.Application;
import android.content.Context;
import android.app.UiModeManager;
import android.content.res.Configuration;

import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.UI_MODE_SERVICE;

/**
 * Main object used to implement the Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {
    protected static final String FLAVOUR_NAME = "core";

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
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return OS_ANDROID_TV;
        }
        return OS_ANDROID;
    }
}
