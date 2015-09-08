package com.swrve.sdk.config;

import com.swrve.sdk.SwrveHelper;

/**
 * Configuration for the Swrve Google SDK.
 */
public class SwrveConfig extends SwrveConfigBase {

    /**
     * Android Google Cloud Messaging sender id.
     */
    private String senderId;

    /**
     * Returns an instance of SwrveConfig with the sender id.
     *
     * @param senderId
     * @return
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
     * @return if push is enabled
     */
    public boolean isPushEnabled() {
        return !SwrveHelper.isNullOrEmpty(this.senderId);
    }
}
