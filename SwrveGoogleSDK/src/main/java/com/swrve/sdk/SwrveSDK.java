package com.swrve.sdk;


import android.content.Context;
import android.content.Intent;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.ISwrvePushNotificationListener;

public class SwrveSDK extends SwrveSDKBase {

    // TODO DOM review all javadoc comments here

    /**
     * Create a single Swrve SDK instance.
     * @param context your activity or application context
     * @param appId   your app id in the Swrve dashboard
     * @param apiKey  your app api_key in the Swrve dashboard
     * @return singleton SDK instance.
     */
    public static synchronized ISwrve createInstance(final Context context, final int appId, final String apiKey) {
        return createInstance(context, appId, apiKey, new SwrveConfig());
    }

    /**
     * Create a single Swrve SDK instance.
     * @param context your activity or application context
     * @param appId   your app id in the Swrve dashboard
     * @param apiKey  your app api_key in the Swrve dashboard
     * @param config  your SwrveConfig options
     * @return singleton SDK instance.
     */
    public static synchronized ISwrve createInstance(final Context context, final int appId, final String apiKey, final SwrveConfig config) {
        if (context == null) {
            SwrveHelper.logAndThrowException("Context not specified");
        } else if (SwrveHelper.isNullOrEmpty(apiKey)) {
            SwrveHelper.logAndThrowException("Api key not specified");
        }

        if (!sdkAvailable()) {
            return new SwrveEmpty(context, apiKey);
        }
        if (instance == null) {
            instance = new Swrve(context, appId, apiKey, config);
        }
        return (ISwrve) instance;
    }

    public static SwrveConfig getConfig() {
        checkInstanceCreated();
        return (SwrveConfig) instance.getConfig();
    }

    public static void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
        checkInstanceCreated();
        ((ISwrve) instance).setPushNotificationListener(pushNotificationListener);
    }

    public static void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature) {
        checkInstanceCreated();
        ((ISwrve) instance).iapPlay(productId, productPrice, currency, purchaseData, dataSignature);
    }

    public static void iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String purchaseData, String dataSignature) {
        checkInstanceCreated();
        ((ISwrve) instance).iapPlay(productId, productPrice, currency, rewards, purchaseData, dataSignature);
    }

    public static void processIntent(Intent intent) {
        checkInstanceCreated();
        ((ISwrve) instance).processIntent(intent);
    }
}
