package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_SENT_TIME;

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
                SwrveLogger.i("SwrveHmsMessageService Received Huawei data: %s", remoteMessage.getData());

                Bundle extras = new Bundle();
                for (String key : remoteMessage.getDataOfMap().keySet()) { // Convert from string to Bundle
                    extras.putString(key, remoteMessage.getDataOfMap().get(key));
                }
                extras.putString(GENERIC_EVENT_PAYLOAD_MSG_ID, remoteMessage.getMessageId());
                extras.putString(GENERIC_EVENT_PAYLOAD_SENT_TIME, String.valueOf(remoteMessage.getSentTime()));

                if (!SwrveHelper.isSwrvePush(extras)) {
                    SwrveLogger.i("SwrveHmsMessageService: Received Push: but not processing as it doesn't contain: %s or %s", SwrveNotificationConstants.SWRVE_TRACKING_KEY, SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY);
                    return;
                }
                // HmsMessageService is a Service that runs on the main thread. Network i/o is prohibited on this thread so use a worker thread (SwrvePushServiceDefault) to continue.
                // The SwrvePushServiceDefault has de-duping logic so we can just call handle and don't need to worry about de-duping here.

                handle(extras);
            }
        } catch (Exception ex) {
            SwrveLogger.e("SwrveHmsMessageService exception: ", ex);
        }
    }

    protected void handle(Bundle extras) {
        SwrvePushServiceDefault.handle(this, extras);
    }
}
