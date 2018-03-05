package com.swrve.sdk.sample;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

public class SampleApplication extends Application {

    private static final String LOG_TAG = "SwrveSample";
    private int YOUR_APP_ID = -1;
    private String YOUR_API_KEY = "api_key";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SwrveConfig config = new SwrveConfig();
            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);
        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}
