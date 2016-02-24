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
     * Whether the Swrve SDK should handle GCM pushes
     */
    private boolean gGcmPushEnabled;

    /**
     * Whether the Swrve SDK should register to GCM (true) or let the app handles it (false)
     */
    private boolean performGcmRegistrationInternally;

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
        this.gGcmPushEnabled = !SwrveHelper.isNullOrEmpty(senderId);
        this.performGcmRegistrationInternally = true;
        return this;
    }

    /**
     * Let your app handle GCM registration instead of Swrve
     * <p>
     * When calling this method, the Swrve SDK expects your app to register to
     * GCM and then send Swrve the registration token via
     * {@code SwrveSDK.setGcmRegistrationId(registrationId);}
     */
    public SwrveConfig provideOwnGcmRegistrationId() {
        this.gGcmPushEnabled = true;
        this.performGcmRegistrationInternally = false;
        return this;
    }

    /**
     * @return if push is enabled.
     */
    public boolean isPushEnabled() {
        return gGcmPushEnabled;
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

    /**
     * @return if the Swrve SDK registers for gcm pushes by itself.
     */
    public boolean isPushRegistrationDoneBySwrve() {
        return performGcmRegistrationInternally;
    }
}
