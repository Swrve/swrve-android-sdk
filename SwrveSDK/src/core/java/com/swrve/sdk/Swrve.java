package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;

import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Main object used to implement the Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {

    protected Swrve(Context context, int appId, String apiKey, SwrveConfig config) {
        super();
        if (context instanceof Activity) {
            this.context = new WeakReference<Context>(context.getApplicationContext());
            this.activityContext = new WeakReference<Activity>((Activity) context);
        } else {
            this.context = new WeakReference<Context>(context);
        }
        this.appId = appId;
        this.apiKey = apiKey;
        this.config = config;
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
