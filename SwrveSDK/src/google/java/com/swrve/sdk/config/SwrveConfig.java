package com.swrve.sdk.config;

import com.swrve.sdk.SwrveHelper;

/**
 * Configuration for the Swrve Google SDK.
 */
public class SwrveConfig extends SwrveConfigBase {

    /**
     * Android Google Cloud Messaging Sender id.
     */
    private String senderId;

    /**
     * Automatically log Google's Advertising Id as "swrve.GAID".
     */
    private boolean gAIDLoggingEnabled;

    /**
     * Returns an instance of SwrveConfig with the Sender id.
     *
     * @param senderId
     * @return configuration object with the given Sender id.
     */
    public static SwrveConfig withPush(String senderId) {
        return new SwrveConfig().setSenderId(senderId);
    }

    /**
     * @return the sender id.
     */
    public String getSenderId() {
        return this.senderId;
    }

    /**
     * @param senderId the Google Cloud Messaging sender id to set
     */
    public SwrveConfig setSenderId(String senderId) {
        this.senderId = senderId;
        return this;
    }

    /**
     * @return if push is enabled.
     */
    public boolean isPushEnabled() {
        return !SwrveHelper.isNullOrEmpty(this.senderId);
    }

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
}
