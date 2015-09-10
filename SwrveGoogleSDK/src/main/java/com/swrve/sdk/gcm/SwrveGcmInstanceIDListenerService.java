package com.swrve.sdk.gcm;

import android.util.Log;
import com.google.android.gms.iid.InstanceIDListenerService;
import com.swrve.sdk.SwrveSDK;

public class SwrveGcmInstanceIDListenerService extends InstanceIDListenerService {

	protected static final String TAG = "SwrveGcm";

    @Override
    public void onTokenRefresh() {
    	SwrveSDK sdk = (SwrveSDK)SwrveSDK.getInstance();
    	if (sdk != null) {
        	sdk.onTokenRefreshed();
        } else {
        	Log.e(TAG, "Could not notify the SDK of a new token. Consider using the shared instance.");
        }
    }
}
