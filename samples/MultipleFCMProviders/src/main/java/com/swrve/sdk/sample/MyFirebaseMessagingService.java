package com.swrve.sdk.sample;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
//import com.salesforce.marketingcloud.messages.push.PushMessageManager;
import com.swrve.sdk.SwrveSDK;

/**
 * Class that receives the FCM messages
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        SwrveSDK.setRegistrationId(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (SwrveSDK.isSwrvePush(remoteMessage.getData())) {
            Log.v("MyFirebaseMessagingService", "Swrve SDK handled fcm push message");
            boolean handled = SwrveSDK.handleSwrvePush(this, remoteMessage.getData(), remoteMessage.getMessageId(), remoteMessage.getSentTime());
            if (!handled) {
                Log.v("MyFirebaseMessagingService", "Swrve SDK did not handle fcm push message");
            }
//        } else if (PushMessageManager.isMarketingCloudPush(remoteMessage)) {
//            Log.v("MyFirebaseMessagingService", "Marketing Cloud SDK handled fcm push message");
//            // Execute code for Marketing Cloud SDK
        } else {
            // Check if message contains a data payload for other push provider and pass it along
        }
    }
}
