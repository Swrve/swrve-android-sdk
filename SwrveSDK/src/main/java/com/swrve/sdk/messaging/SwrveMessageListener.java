package com.swrve.sdk.messaging;

import java.util.Map;

/**
 * Implement this interface to handle the rendering of in-app messages
 * completely from your app. You will have to render and manage these
 * messages yourself.
 *
 * @deprecated Use embedded campaigns instead.
 */
@Deprecated
public interface SwrveMessageListener {
    /**
     * This method is invoked when a message should be shown in your app.
     *
     * @param message message to be shown.
     * @deprecated Use embedded campaigns instead.
     */
    @Deprecated
    void onMessage(SwrveMessage message);

    /**
     * This method is invoked when a message with additional personalization should be shown
     * in your app. Only available with IAMs
     *
     * @param message    message to be shown.
     * @param properties additional properties included for personalization options.
     * @deprecated Use embedded campaigns instead.
     */
    @Deprecated
    void onMessage(SwrveMessage message, Map<String, String> properties);
}
