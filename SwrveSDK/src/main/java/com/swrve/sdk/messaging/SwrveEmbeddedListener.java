package com.swrve.sdk.messaging;

import android.content.Context;

import java.util.Map;

/**
 * Implement this interface if you plan on using embedded campaigns
 * This will provide an object with information
 *
 * Note: the methods in this interface will be invoked from the UI thread.
 */
public interface SwrveEmbeddedListener {
    /**
     * This method is invoked when an Embedded Campaign was triggered
     *
     * @param context context
     * @param message message contents
     * @param personalizationProperties string map of personalization key / value pairs.
     * @param isControl flag to determine if control or treatment Campaign, Control campaigns should not be shown to users.
     */
    void onMessage(Context context, SwrveEmbeddedMessage message, Map<String, String> personalizationProperties, boolean isControl);
}
