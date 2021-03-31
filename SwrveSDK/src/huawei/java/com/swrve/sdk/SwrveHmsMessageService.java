package com.swrve.sdk;

import android.os.Bundle;

import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

public class SwrveHmsMessageService extends HmsMessageService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        SwrveLogger.i("SwrveHmsMessageService: Received HMS token: %s", token);
        ISwrveBase sdk = SwrveSDK.getInstance();
        if (sdk instanceof Swrve) {
            ((Swrve) sdk).setRegistrationId(token);
        } else {
            SwrveLogger.e("SwrveHmsMessageService: Could not notify the SDK of a new token.");
        }
    }

    @Override
    public void onTokenError(Exception e) {
        super.onTokenError(e);
        SwrveLogger.e("SwrveHmsMessageService token error.", e);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        try {
            if (remoteMessage.getData() != null) {
                SwrveLogger.i("Received Huawei data: %s", remoteMessage.getData());

                Bundle extras = new Bundle();
                for (String key : remoteMessage.getDataOfMap().keySet()) { // Convert from string to Bundle
                    extras.putString(key, remoteMessage.getDataOfMap().get(key));
                }

                if (!SwrveHelper.isSwrvePush(extras)) {
                    SwrveLogger.i("SwrveHmsMessageService: Received Push: but not processing as it doesn't contain: %s or %s", SwrveNotificationConstants.SWRVE_TRACKING_KEY, SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY);
                    return;
                }

                handle(extras);
            }
        } catch (Exception ex) {
            SwrveLogger.e("SwrveHmsMessageService exception: ", ex);
        }
    }

    protected void handle(Bundle extras) {
        // HmsMessageService is a Service that runs on the main thread. Network i/o is prohibited on this thread so use a worker thread to continue
        SwrvePushServiceDefault.handle(this, extras);
    }
}
