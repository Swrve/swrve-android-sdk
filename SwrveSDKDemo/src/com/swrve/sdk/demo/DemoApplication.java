package com.swrve.sdk.demo;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

import java.net.MalformedURLException;
import java.net.URL;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";
    private static int YOUR_APP_ID = 2374;
    private static String YOUR_API_KEY = "Xu0TQ8zwSdP3ASE06TqB";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            SwrveConfig config = new SwrveConfig();
            // TODO: Converser setup, remove
            config.setEventsUrl(new URL("http://featurestack7-api.swrve.com"));
            config.setContentUrl(new URL("http://featurestack7-content.swrve.com"));
            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);
            // SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY); // TODO DOM rake build script looks for appid/appkey in MainActivity. Change this.
        } catch (IllegalArgumentException exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}