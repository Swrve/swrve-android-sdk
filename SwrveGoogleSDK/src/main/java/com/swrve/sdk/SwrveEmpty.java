package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.ISwrvePushNotificationListener;

/**
 * Empty implementation of the Swrve Google SDK. Will be returned when the SDK is used from an unsupported runtime version (< 2.3.3).
 */
public class SwrveEmpty extends SwrveBaseEmpty<ISwrve, SwrveConfig> implements ISwrve {

    protected SwrveEmpty(Context context, String apiKey) {
        super(context, apiKey);
    }

    @Override
    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
    }

    @Override
    public void iapPlay(String productId, double productPrice, String currency, String receipt, String receiptSignature) {
    }

    @Override
    public void iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String receipt, String receiptSignature) {
    }

    @Override
    public void onTokenRefreshed() {
    }

    @Override
    public void processIntent(Intent intent) {
    }
}
