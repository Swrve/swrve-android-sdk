package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_ORIGINAL_PRIORITY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_PRIORITY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_SENT_TIME;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

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
            }

            Map<String, String> data = new HashMap<>(remoteMessage.getData());
            data.put(GENERIC_EVENT_PAYLOAD_MSG_ID, remoteMessage.getMessageId());
            data.put(GENERIC_EVENT_PAYLOAD_SENT_TIME, String.valueOf(remoteMessage.getSentTime()));
            data.put(GENERIC_EVENT_PAYLOAD_PRIORITY, String.valueOf(remoteMessage.getPriority()));
            data.put(GENERIC_EVENT_PAYLOAD_ORIGINAL_PRIORITY, String.valueOf(remoteMessage.getOriginalPriority()));

            if (SwrveHelper.isCarouselPush(remoteMessage.getData())) {
                // Carousel images are downloaded before the notification is shown, which can take longer than
                // onMessageReceived allows, so use the push worker (it does its own _sid de-duping)
                SwrvePushWorkerHelper workerHelper = getSwrvePushWorkerHelper(data);
                if (workerHelper.handle() || !workerHelper.didFailToQueue()) {
                    return;
                }
                // The worker helper has already recorded the _sid, so skip de-duping and show it inline rather than lose it
                SwrveLogger.w("SwrveSDK could not queue carousel push worker, processing inline.");
            } else if (SwrvePushSidDeDuper.isDupe(this, remoteMessage.getData())) {
                SwrveLogger.i("SwrveSDK Received Push: but not processing as _sid has been processed before.");
                return;
            }

            Bundle extras = new Bundle();
            for (String key : data.keySet()) { // Convert from map to Bundle
                extras.putString(key, data.get(key));
            }
            getSwrvePushManager().processMessage(extras);
        } catch (Exception ex) {
            SwrveLogger.e("SwrveFirebaseMessagingService exception: ", ex);
        }
    }

    protected SwrvePushManager getSwrvePushManager() {
        return new SwrvePushManagerImp(this);
    }

    protected SwrvePushWorkerHelper getSwrvePushWorkerHelper(Map<String, String> data) {
        return new SwrvePushWorkerHelper(this, SwrvePushManagerWorker.class, data);
    }
}
