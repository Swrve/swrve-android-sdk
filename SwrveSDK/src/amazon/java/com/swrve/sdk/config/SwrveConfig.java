package com.swrve.sdk.config;

/**
 * Configuration for the Swrve Amazon SDK.
 */
public class SwrveConfig extends SwrveConfigBase {
    /**
     * @return Push is always enabled in the ADM flavour.
     */
    public boolean isPushEnabled() {
        return true;
    }
}
