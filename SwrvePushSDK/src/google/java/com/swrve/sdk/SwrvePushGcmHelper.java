package com.swrve.sdk;

import android.content.Context;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class SwrvePushGcmHelper {
    protected static final String TAG = "SwrveGcmHelper";

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(context);
        if (resultCode == ConnectionResult.SUCCESS) {
            return true;
        }
        boolean resolveable = googleAPI.isUserResolvableError(resultCode);
        if (resolveable) {
            SwrveLogger.e(TAG, "Google Play Services are not available, resolveable error code: " + resultCode + ". You can use getErrorDialog in your app to try to address this issue at runtime.");
        } else {
            SwrveLogger.e(TAG, "Google Play Services are not available. Error code: " + resultCode);
        }
        return false;
    }
}
