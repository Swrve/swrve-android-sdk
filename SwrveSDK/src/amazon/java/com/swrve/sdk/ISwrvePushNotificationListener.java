package com.swrve.sdk;

import android.os.Bundle;

/**
 * Implement this interface to be notified of any Swrve push notification
 * to your app.
 */
public interface ISwrvePushNotificationListener {

    /**
     * This method will be called when a push notification is received by your app,
     * after the user has reacted to it. This is executed in a broadcastreceiver and should not
     * perform any long-running operations.
     * @param bundle push notification information including custom payloads.
     */
    void onPushNotification(Bundle bundle);
}
