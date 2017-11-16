package com.swrve.sdk;

import android.content.Intent;

import com.swrve.sdk.config.SwrveConfig;

/**
 * Swrve Google SDK interface.
 */
public interface ISwrve extends ISwrveBase<ISwrve, SwrveConfig> {

    void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature);

    void iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String purchaseData, String dataSignature);

    void setRegistrationId(String registrationId);

    void onTokenRefreshed();
}
