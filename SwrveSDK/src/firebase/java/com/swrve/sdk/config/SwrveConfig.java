package com.swrve.sdk.config;

/**
 * Configuration for the Swrve Firebase SDK.
 */
public class SwrveConfig extends SwrveConfigPushBase {

    /**
     * Automatically get the Firebase Registration ID.
     */
    private boolean pushRegistrationAutomatic = true;

    /**
     * Automatically log Google's Advertising Id as "swrve.GAID".
     */
    private boolean gAIDLoggingEnabled;

    /**
     * @return if it will automatically log Google's Advertising Id as "swrve.GAID".
     */
    public boolean isGAIDLoggingEnabled() {
            return gAIDLoggingEnabled;
    }

    /**
     * @param enabled to enable automatic logging of Google's Advertising Id as "swrve.GAID".
     */
    public void setGAIDLoggingEnabled(boolean enabled) {
        this.gAIDLoggingEnabled = enabled;
    }

    /**
     * @return if it will automatically obtain the Firebase Registration ID.
     */
    public boolean isPushRegistrationAutomatic() {
        return pushRegistrationAutomatic;
    }

    /**
     * @param enabled to automatically obtain the Firebase Registration ID.
     */
    public void setPushRegistrationAutomatic(boolean enabled) {
        this.pushRegistrationAutomatic = enabled;
    }
}
