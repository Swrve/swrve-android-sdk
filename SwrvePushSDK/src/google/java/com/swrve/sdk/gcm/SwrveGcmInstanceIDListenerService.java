package com.swrve.sdk.gcm;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrvePushSDK;

public class SwrveGcmInstanceIDListenerService extends InstanceIDListenerService {

    protected static final String TAG = "SwrveGcm";

    @Override
    public void onTokenRefresh() {
        SwrvePushSDK sdk = SwrvePushSDK.getInstance();
        if (sdk != null) {
            sdk.onPushTokenRefreshed();
        } else {
            SwrveLogger.e(TAG, "Could not notify Push SDK of a new token.");
        }
    }
}