package com.swrve.sdk.config;

/**
 * Configuration for the Swrve Amazon SDK.
 */
public class SwrveConfig extends SwrveConfigBase {

    /**
     * Enable push notifications.
     */
    private boolean pushEnabled;

    /**
     * Let the SDK ask for push registration ID.
     */
    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    /**
     * @return if push is enabled.
     */
    public boolean isPushEnabled() {
        return pushEnabled;
    }

}
