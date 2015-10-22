package com.swrve.sdk.gcm;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.swrve.sdk.ISwrveBase;
import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveSDK;

public class SwrveGcmInstanceIDListenerService extends InstanceIDListenerService {

    protected static final String TAG = "SwrveGcm";

    @Override
    public void onTokenRefresh() {
        ISwrveBase sdk = SwrveSDK.getInstance();
        if (sdk != null && sdk instanceof Swrve) {
            ((Swrve)sdk).onTokenRefreshed();
        } else {
            SwrveLogger.e(TAG, "Could not notify the SDK of a new token. Consider using the shared instance.");
        }
    }
}