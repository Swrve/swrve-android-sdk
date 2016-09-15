package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.ISwrveNotificationListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main implementation of the Amazon Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve, ISwrveNotificationListener {
    protected static final String SWRVE_AMAZON_TOKEN = "swrve.adm_token";

    protected String admRegistrationId;
    protected ISwrvePushNotificationListener pushNotificationListener;
    protected String lastProcessedMessage;

    protected Swrve(Context context, int appId, String apiKey, SwrveConfig config) {
        super(context, appId, apiKey, config);
    }

    @Override
    public void onRegistrationIdReceived(String registrationId) {
        //TODO remove not needed.
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        try {
            ISwrveNotificationSDK notificationSDK = SwrveNotificationSDK.getInstance();
            if (notificationSDK != null) {
                notificationSDK.setNotificationListener(this);
                notificationSDK.initialiseNotificationSDK(context);
            } else {
                SwrveLogger.e(LOG_TAG, "SwrveNotificationSDK is null");
            }
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "Unable to initialise notification sdk: " + ex.toString());
        }
    }

    @Override
    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        if (config.isPushEnabled() && !SwrveHelper.isNullOrEmpty(admRegistrationId)) {
            deviceInfo.put(SWRVE_AMAZON_TOKEN, admRegistrationId);
        }
    }

    public void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature) {
        //TODO
        SwrveLogger.e(LOG_TAG, "iapPlay TODO");
    }

    /**
     * @deprecated Swrve engaged events are automatically sent, so this is no longer needed.
     */
    @Deprecated
    public void processIntent(Intent intent) {
        SwrveLogger.e(LOG_TAG, "The processIntent method is Deprecated and should not be used anymore");
    }

    @Override
    public void onRegistrationIdUpdated(String registrationId) {
        try {
            admRegistrationId = registrationId;
            if (qaUser != null) {
                qaUser.updateDeviceInfo();
            }

            // Re-send data now
            queueDeviceInfoNow(true);
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "Couldn't update the ADM registration id for the device", ex);
        }
    }

    @Override
    public void onMessageReceived(Bundle msg) {
        //Just call back into the notification sdk to show it for now.
        ISwrveNotificationSDK notificationSDK = SwrveNotificationSDK.getInstance();
        if (notificationSDK != null) {
            notificationSDK.showNotification(getContext(), msg);
        }
    }

    @Override
    public void onPushEngaged(Bundle msg) {
        //Push has been engaged, let listener know
        if (pushNotificationListener != null) {
            pushNotificationListener.onPushNotification(msg);
        }
    }
}
