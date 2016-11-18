package com.swrve.sdk;

import android.content.Intent;

import com.swrve.sdk.config.SwrveConfig;

/**
 * Swrve Amazon SDK interface.
 */
public interface ISwrve extends ISwrveBase<ISwrve, SwrveConfig> {

    void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener);

    void onRegistrationIdReceived(String registrationId);
}
