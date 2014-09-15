package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfig;

/**
 * Empty implementation of the Swrve SDK. Will be returned when the SDK is used from an unsupported runtime version (< 2.3.3).
 */
public class SwrveEmpty extends SwrveBaseEmpty<ISwrve, SwrveConfig> implements ISwrve {
}