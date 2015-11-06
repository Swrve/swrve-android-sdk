package com.swrve.sdk.demo;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveConfigBase;

import java.net.MalformedURLException;
import java.net.URL;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";

    @Override
    public void onCreate() {
        super.onCreate();

        SwrveConfig config = new SwrveConfig();

        try {
            config.useEuStack();
            config.setStack(SwrveConfigBase.SwrveStack.US);
            config.setAppIdPrefix(2132);
            config.setUseHttpsForEventsUrl(true);
            config.setEventsUrl(new URL("https://featurestack12-api.swrve.com")); // Calling these directly should lock them
            config.setContentUrl(new URL("https://featurestack12-content.swrve.com"));
            config.setUseHttpsForEventsUrl(false); // Should still be http
            config.setStack(SwrveConfigBase.SwrveStack.US); // Should still be featurestack 12
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            SwrveSDK.createInstance(this, 2132, "AYgy6V2uk6f3d46ShfJ", config);
        } catch (IllegalArgumentException exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}