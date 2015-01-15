package com.swrve.sdk.demo;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.SwrveInstance;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";
    private static int YOUR_APP_ID = 123;
    private static String YOUR_API_KEY = "YOUR_API_KEY";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Initialize SDK
            SwrveInstance.getInstance().init(this, YOUR_APP_ID, YOUR_API_KEY);
        } catch (IllegalArgumentException exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}