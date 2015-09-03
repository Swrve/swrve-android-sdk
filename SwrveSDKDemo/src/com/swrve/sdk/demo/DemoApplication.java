package com.swrve.sdk.demo;

import android.app.Application;
import android.util.Log;

import com.plotprojects.retail.android.Plot;
import com.swrve.sdk.SwrveSDK;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";
    private static int YOUR_APP_ID = 123;
    private static String YOUR_API_KEY = "abc";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY);
            Plot.init(this);
        } catch (IllegalArgumentException exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}