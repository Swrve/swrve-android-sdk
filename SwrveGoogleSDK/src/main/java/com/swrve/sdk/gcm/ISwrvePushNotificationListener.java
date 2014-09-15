package com.swrve.sdk.gcm;

import android.os.Bundle;

/**
 * Implement this interface to be notified of any Swrve push notification
 * to your app.
 */
public interface ISwrvePushNotificationListener {

    /**
     * This method will be called when a push notification is received by your app,
     * after the user has reacted to it.
     * @param bundle push notification information including custom payloads.
     */
    void onPushNotification(Bundle bundle);
}
