package com.swrve.sdk.sample;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.swrve.sdk.SwrvePushServiceDefault;

/**
 * Class that receives the FCM messages
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (!SwrvePushServiceDefault.handle(this, remoteMessage.getData())) {
            // execute code for other push provider
        }
    }
}
