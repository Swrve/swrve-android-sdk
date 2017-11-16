package com.swrve.sdk.config;

import com.swrve.sdk.SwrveAppStore;

/**
 * Configuration for the Swrve Amazon SDK.
 */
public class SwrveConfig extends SwrveConfigBase {
    public SwrveConfig() {
        setAppStore(SwrveAppStore.Amazon);
    }
}
