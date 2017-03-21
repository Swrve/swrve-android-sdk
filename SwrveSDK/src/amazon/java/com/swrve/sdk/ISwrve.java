package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfig;

/**
 * Swrve Amazon SDK interface.
 */
public interface ISwrve extends ISwrveBase<ISwrve, SwrveConfig> {

    void onRegistrationIdReceived(String registrationId);
}
