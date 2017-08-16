package com.swrve.sdk.firebase;

import com.google.firebase.iid.FirebaseInstanceIdService;
import com.swrve.sdk.ISwrveBase;
import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveSDK;

public class SwrveFirebaseInstanceIdService extends FirebaseInstanceIdService {
    protected static final String TAG = "SwrveFirebaseInstanceIdService";

    @Override
    public void onTokenRefresh() {
        ISwrveBase sdk = SwrveSDK.getInstance();
        if (sdk != null && sdk instanceof Swrve) {
            ((Swrve)sdk).onTokenRefresh();
        } else {
            SwrveLogger.e(TAG, "Could not notify the SDK of a new token.");
        }
    }
}
