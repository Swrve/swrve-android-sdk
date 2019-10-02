package com.swrve.sdk;

import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class SwrveFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        ISwrveBase sdk = SwrveSDK.getInstance();
        if (sdk instanceof Swrve) {
            ((Swrve) sdk).setRegistrationId(token);
        } else {
            SwrveLogger.e("Could not notify the SDK of a new token.");
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        try {
            if (remoteMessage.getData() != null) {
                SwrveLogger.i("Received Firebase notification: %s" + remoteMessage.getData().toString());

                Bundle extras = new Bundle();
                for (String key : remoteMessage.getData().keySet()) { // Convert from map to Bundle
                    extras.putString(key, remoteMessage.getData().get(key));
                }

                if (!SwrveHelper.isSwrvePush(extras)) {
                    SwrveLogger.i("Received Push: but not processing as it doesn't contain: %s or %s", SwrveNotificationConstants.SWRVE_TRACKING_KEY, SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY);
                    return;
                }

                getSwrvePushServiceManager().processMessage(extras);
            }
        } catch (Exception ex) {
            SwrveLogger.e("Swrve exception: ", ex);
        }
    }

    protected SwrvePushServiceManager getSwrvePushServiceManager() {
        return new SwrvePushServiceManager(this);
    }
}
