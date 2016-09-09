package com.swrve.sdk;

import android.content.Intent;

import com.swrve.sdk.config.SwrveConfig;

/**
 * Swrve Amazon SDK interface.
 */
public interface ISwrve extends ISwrveBase<ISwrve, SwrveConfig> {

    void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener);

    void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature);

    //ADM callbacks
    void onRegistrationIdReceived(String registrationId);

    /**
     * @deprecated Swrve engaged events are automatically sent, so this is no longer needed.
     */
    //@Deprecated
    void processIntent(Intent intent);

}
