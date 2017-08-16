package com.swrve.sdk.config;

import android.app.NotificationChannel;

/**
 * Base push configuration for the Swrve SDK.
 */
public abstract class SwrveConfigPushBase extends SwrveConfigBase {

    /**
     * Default notification channel used to display notifications. This is required if you target Android O (API 26) or higher.
     * We recommend that the channel is created before setting it in our config. Our SDK will attempt to create it if it doesn't exist.
     */
    private NotificationChannel defaultNotificationChannel;

    /**
     * Set the default notification channel used to display notifications. This is required if you target Android O (API 26) or higher.
     * We recommend that the channel is created before setting it in our config. Our SDK will attempt to create it if it doesn't exist.
     * @param defaultNotificationChannel Default notification channel
     */
    public void setDefaultNotificationChannel(NotificationChannel defaultNotificationChannel) {
        this.defaultNotificationChannel = defaultNotificationChannel;
    }

    /**
     * @return default channel used to display notifications. This is required if you target Android O (API 26) or higher.
     */
    public NotificationChannel getDefaultNotificationChannel() {
        return defaultNotificationChannel;
    }
}
