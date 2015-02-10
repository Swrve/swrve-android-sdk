package com.swrve.sdk.demo;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

import java.net.MalformedURLException;
import java.net.URL;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";
    private static int YOUR_APP_ID = 123;
    private static String YOUR_API_KEY = "abc";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            SwrveConfig config = new SwrveConfig();
            // TODO: Converser setup, remove
            config.setEventsUrl(new URL("http://converser-api.swrve.com"));
            config.setContentUrl(new URL("http://converser-content.swrve.com"));
            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config); // TODO DOM rake build script looks for appid/appkey in MainActivity. Change this.
        } catch (IllegalArgumentException exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}