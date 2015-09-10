package com.swrve.sdk.gcm;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.swrve.sdk.SwrveSDK;

public class SwrveGcmInstanceIDListenerService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        ((SwrveSDK)SwrveSDK.getInstance()).onTokenRefreshed();
    }
}
