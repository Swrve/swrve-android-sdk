package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfig;

/**
 * Swrve Huawei SDK interface.
 */
public interface ISwrve extends ISwrveBase<ISwrve, SwrveConfig> {

    void setRegistrationId(String registrationId);

}
