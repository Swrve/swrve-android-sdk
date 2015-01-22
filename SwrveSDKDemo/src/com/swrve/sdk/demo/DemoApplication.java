package com.swrve.sdk.demo;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.ISwrve;
import com.swrve.sdk.SwrveInstance;
import com.swrve.sdk.config.SwrveConfig;

import java.net.MalformedURLException;
import java.net.URL;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";
    private static int YOUR_APP_ID = 572;
    private static String YOUR_API_KEY = "gUnFFH3jFS3U8dFaN5MB";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            SwrveConfig config = new SwrveConfig();
            // TODO: Converser setup, remove
            config.setEventsUrl(new URL("http://converser-api.swrve.com"));
            config.setContentUrl(new URL("http://converser-content.swrve.com"));
            ISwrve swrve = SwrveInstance.createInstance(this, YOUR_APP_ID, YOUR_API_KEY); // TODO DOM change build rake script to look for appid/appkey in this class instead of MainActivity
            swrve.init(this, config); // TODO DOM init should really be called from activity, passing in instance of the current activity
        } catch (IllegalArgumentException exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}