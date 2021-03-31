package com.swrve.sdk.config;

import com.swrve.sdk.SwrveAppStore;

/**
 * Configuration for the Swrve Huawei SDK.
 */
public class SwrveConfig extends SwrveConfigBase {
    public SwrveConfig() {
        setAppStore(SwrveAppStore.Huawei);
    }

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
}
