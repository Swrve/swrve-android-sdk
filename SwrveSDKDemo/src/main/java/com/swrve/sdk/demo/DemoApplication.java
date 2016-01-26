package com.swrve.sdk.demo;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

import java.net.MalformedURLException;
import java.net.URL;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";

    @Override
    public void onCreate() {
        super.onCreate();

        SwrveConfig config = new SwrveConfig();
        try {
            config.setContentUrl(new URL("https://featurestack17-content.swrve.com"));
            config.setEventsUrl(new URL("https://featurestack17-api.swrve.com"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            SwrveSDK.createInstance(this, 2132, "AYgy6V2uk6f3d46ShfJ", config);
        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}