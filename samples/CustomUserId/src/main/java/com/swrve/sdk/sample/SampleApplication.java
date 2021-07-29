package com.swrve.sdk.sample;

import android.app.Application;
import android.util.Log;

import com.swrve.sdk.SwrveInitMode;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

public class SampleApplication extends Application {

    private static final String LOG_TAG = "SwrveSample";
    private int YOUR_APP_ID = 0;
    private String YOUR_API_KEY = "YOUR_API_KEY";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            SwrveConfig config = new SwrveConfig();
            // On MANAGED mode the SDK will be delayed until start() or start(userId) is called.
            // On next application start the last user will be used to auto initialise the SDK.
            //
            // If you want the SDK to always delay until you provide the current user you can call
            // config.setAutoStartLastUser(false);
            // Note: with this flag to false push-to-in-app won't work.
            config.setInitMode(SwrveInitMode.MANAGED);

            SwrveSDK.createInstance(this, YOUR_APP_ID, YOUR_API_KEY, config);
        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }
}
