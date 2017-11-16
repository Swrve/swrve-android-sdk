package com.swrve.sdk;

import org.json.JSONObject;

/**
 * Implement this interface to be notified of any Swrve push notification
 * to your app.
 */
public interface SwrvePushNotificationListener {

    /**
     * This method will be called when a push notification is received by your app,
     * after the user has reacted to it. This is executed in a broadcastreceiver and should not
     * perform any long-running operations.
     * @param payload push notification information including custom payloads.
     */
    void onPushNotification(JSONObject payload);
}
