package com.swrve.sdk.demo;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;

public class DemoApplication extends Application {

    private static final String LOG_TAG = "SwrveDemo";
    private static int YOUR_APP_ID = 123;
    private static String YOUR_API_KEY = "abc";

    @Override
    public void onCreate() {
        super.onCreate();

        // --------------------------------------------------------------------
        // todo.gg - Add sandbox and production api_keys.  Show an if block
        // that chooses the sandbox or a production api_key depending on the
        // build config
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // todo.gg - Add button listener to handle in-app message button
        // presses. Just call the processDeeplink() method.
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // todo.gg - Add push listener to handle push notification engagement
        // Just call the processDeeplink() method.
        // --------------------------------------------------------------------

        try {
            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY); // TODO DOM rake build script looks for appid/appkey in MainActivity. Change this.
        } catch (IllegalArgumentException exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }

    // --------------------------------------------------------------------
    // todo.gg - Add processDeepLink method that handles all of our
    // recommended deeplinks including:
    //      uri based
    //          event
    //          user property
    //          trigger (same as event but with delay + flush events for push to in-app)
    //          permissions
    //          purchase SKU
    //          goto (activity by name in Android, seque in iOS, nothing in Unity)
    //      non uri based
    //          gift SKU (non uri based)
    //          gift currency (non uri based)
    // --------------------------------------------------------------------
}