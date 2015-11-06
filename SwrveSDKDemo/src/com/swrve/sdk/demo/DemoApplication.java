package com.swrve.sdk.demo;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY);
        } catch (IllegalArgumentException exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}