package com.swrve.sdk.firebase;

import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * @deprecated This class will be removed in next major version release
 */
public class SwrveFirebaseInstanceIdService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        // do nothing as SwrveFirebaseMessagingService.onNewToken is now used
    }
}