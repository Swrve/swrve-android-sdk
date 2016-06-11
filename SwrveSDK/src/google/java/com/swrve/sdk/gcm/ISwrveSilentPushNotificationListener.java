package com.swrve.sdk.gcm;

import android.os.Bundle;

/**
 * Implement this interface to be notified of any Swrve silent push notification
 * to your app.
 */
public interface ISwrveSilentPushNotificationListener {

    /**
     * This method will be called when a silent push notification is received by your app.
     * @param bundle push notification information including custom payloads.
     */
    void onSilentPushNotification(Bundle bundle);
}
