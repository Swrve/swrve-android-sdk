package com.swrve.sdk.sample;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.swrve.sdk.firebase.SwrveFirebaseMessagingService;

/**
 * Class that receives the FCM messages
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Do something with remoteMessage in your app here

        // Let the Swrve SDK know about the message too
        SwrveFirebaseMessagingService swrveService = new SwrveFirebaseMessagingService();
        swrveService.onCreate();;
        swrveService.onMessageReceived(remoteMessage);
    }
}
