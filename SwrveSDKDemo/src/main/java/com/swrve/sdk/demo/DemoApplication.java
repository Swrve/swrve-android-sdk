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
            SwrveSDK.createInstance(this, 2132, "AYgy6V2uk6f3d46ShfJ");
        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}