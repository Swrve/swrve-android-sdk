package com.swrve.sdk.sample;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfig;

public class SampleApplication extends Application {

    private static final String LOG_TAG = "SwrveSample";
    private int YOUR_APP_ID = -1;
    private String YOUR_API_KEY = "api_key";

    /**
     * A utility to manage user identity with the Swrve SDK.  Use it to initialize the
     * Swrve SDK if you have a user ID or not and to change user identities if users log
     * into or out of your app.
     * @return
     */
    public SwrveIdentityUtils getSwrveIdentityUtility() {
        return identityUtility;
    }
    protected SwrveIdentityUtils identityUtility;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            // First, initialize the identity utility.  See documentation in SwrveIdentityUtils to
            // learn about what this utility does.
            identityUtility = new SwrveIdentityUtils();

            // Next, retrieve your user ID
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String userID = preferences.getString("userID", null);

            // Next, configure SwrveConfig as normal.  The most common steps include setting the
            // Swrve stack you'll send data to and the GCM sender ID for push notifications.
            SwrveConfig config = new SwrveConfig();

            // Finally, use the identity utility to initialize Swrve.  If userID is set to null
            // the utility will initialize and 'empty' Swrve SDK which does nothing.  If userID is
            // set to a value it'll initialize the SDK as normal.
            identityUtility.createSDKInstance(this, YOUR_APP_ID, YOUR_API_KEY, config, userID);

        } catch (Exception exp) {
            Log.e(LOG_TAG, "Could not initialize the Swrve SDK", exp);
        }
    }

}
