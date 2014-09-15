package com.swrve.sdk.messaging;

/**
 * Implement this interface to handle the rendering of in-app messages
 * completely from your app. You will have to render and manage these
 * messages yourself.
 */
public interface ISwrveMessageListener {
    /**
     * This method is invoked when a message should be shown in your app.
     *
     * @param message   message to be shown.
     * @param firstTime indicates if this message was already showing and the app
     *                  rotated.
     */
    void onMessage(SwrveMessage message, boolean firstTime);
}
