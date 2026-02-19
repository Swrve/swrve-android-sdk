package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_ORIGINAL_PRIORITY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_PRIORITY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_SENT_TIME;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class SwrveFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        SwrveSDK.setRegistrationId(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        try {
            SwrveLogger.i("SwrveSDK Received Firebase remote message: %s", remoteMessage.getData());
            if (!SwrveHelper.isSwrvePush(remoteMessage.getData())) {
                SwrveLogger.i("SwrveSDK Received Push: but not processing as it doesn't contain: %s or %s",
                        SwrveNotificationConstants.SWRVE_TRACKING_KEY, SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY);
                return;
            } else if(SwrvePushSidDeDuper.isDupe(this, remoteMessage.getData())){
                SwrveLogger.i("SwrveSDK Received Push: but not processing as _sid has been processed before.");
                return;
            }

            Bundle extras = new Bundle();
            for (String key : remoteMessage.getData().keySet()) { // Convert from map to Bundle
                extras.putString(key, remoteMessage.getData().get(key));
            }
            extras.putString(GENERIC_EVENT_PAYLOAD_MSG_ID, remoteMessage.getMessageId());
            extras.putString(GENERIC_EVENT_PAYLOAD_SENT_TIME, String.valueOf(remoteMessage.getSentTime()));
            extras.putString(GENERIC_EVENT_PAYLOAD_PRIORITY, String.valueOf(remoteMessage.getPriority()));
            extras.putString(GENERIC_EVENT_PAYLOAD_ORIGINAL_PRIORITY, String.valueOf(remoteMessage.getOriginalPriority()));

            getSwrvePushManager().processMessage(extras);
        } catch (Exception ex) {
            SwrveLogger.e("SwrveFirebaseMessagingService exception: ", ex);
        }
    }

    protected SwrvePushManager getSwrvePushManager() {
        return new SwrvePushManagerImp(this);
    }
}
