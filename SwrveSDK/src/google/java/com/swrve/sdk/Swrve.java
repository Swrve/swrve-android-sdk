package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.qa.SwrveQAUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Main implementation of the Google Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve, ISwrvePushSDKListener {
    protected static final String SWRVE_GCM_TOKEN = "swrve.gcm_token";
    protected static final String SWRVE_GOOGLE_ADVERTISING_ID = "swrve.GAID";

    protected String pushToken;
    protected String advertisingId;
    protected boolean isAdvertisingLimitAdTrackingEnabled;
    protected ISwrvePushNotificationListener pushNotificationListener;
    protected String lastProcessedMessage;

    protected Swrve(Context context, int appId, String apiKey, SwrveConfig config) {
        super(context, appId, apiKey, config);
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        if (config.isPushEnabled()) {
            try {
                ISwrvePushSDK pushSDK = SwrvePushSDK.getInstance();
                if (pushSDK != null) {
                    pushToken = pushSDK.initialisePushSDK(context, this, config.getSenderId());
                } else {
                    SwrveLogger.e(LOG_TAG, "SwrvePushSDK is null");
                }
            } catch (Exception ex) {
                SwrveLogger.e(LOG_TAG, "Unable to initialise push sdk: " + ex.toString());
            }
        }

        // Google Advertising Id logging enabled and Google Play services ready
        if (config.isGAIDLoggingEnabled() &&
            SwrvePushGcmHelper.isGooglePlayServicesAvailable(context)) {
            // Load previous value for Advertising ID
            advertisingId = cachedLocalStorage.getCacheEntryForUser(getUserId(), SWRVE_GOOGLE_ADVERTISING_ID_CATEGORY);
            String isAdvertisingLimitAdTrackingEnabledString = cachedLocalStorage.getCacheEntryForUser(getUserId(), SWRVE_GOOGLE_ADVERTISING_LIMIT_AD_TRACKING_CATEGORY);
            isAdvertisingLimitAdTrackingEnabled = Boolean.parseBoolean(isAdvertisingLimitAdTrackingEnabledString);
            new AsyncTask<Void, Integer, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        // Obtain and save the new Google Advertising Id
                        Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                        advertisingId = adInfo.getId();
                        isAdvertisingLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled();

                        cachedLocalStorage.setAndFlushSecureSharedEntryForUser(getUserId(), SWRVE_GOOGLE_ADVERTISING_ID_CATEGORY, advertisingId, getUniqueKey());
                        cachedLocalStorage.setAndFlushSecureSharedEntryForUser(getUserId(), SWRVE_GOOGLE_ADVERTISING_LIMIT_AD_TRACKING_CATEGORY, Boolean.toString(isAdvertisingLimitAdTrackingEnabled), getUniqueKey());
                    } catch (Exception ex) {
                        SwrveLogger.e(LOG_TAG, "Couldn't obtain Advertising Id", ex);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void v) {
                }
            }.execute(null, null, null);
        }
    }

    @Override
    public void onPushTokenUpdated(String pushToken) {
        try {
            this.pushToken = pushToken;
            if (qaUser != null) {
                qaUser.updateDeviceInfo();
            }

            // Re-send data now
            queueDeviceInfoNow(true);
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "Couldn't handle the GCM push token.", ex);
        }
    }

    @Override
    public void onMessageReceived(String msgId, Bundle msg) {
        //// Notify bound qa clients
        Iterator<SwrveQAUser> iter = SwrveQAUser.getBindedListeners().iterator();
        while (iter.hasNext()) {
            SwrveQAUser sdkListener = iter.next();
            sdkListener.pushNotification(msgId, msg);
        }
    }

    @Override
    public void onNotificationEngaged(Bundle msg) {
        //Push has been engaged, let customer listener know
        if (pushNotificationListener != null) {
            pushNotificationListener.onPushNotification(msg);
        }
    }

    @Override
    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        if (config.isPushEnabled() && !SwrveHelper.isNullOrEmpty(pushToken)) {
            deviceInfo.put(SWRVE_GCM_TOKEN, pushToken);
        }
        if (config.isGAIDLoggingEnabled() && !SwrveHelper.isNullOrEmpty(advertisingId)) {
            deviceInfo.put(SWRVE_GOOGLE_ADVERTISING_ID, advertisingId);
        }
    }

    @Override
    public void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature) {
        SwrveIAPRewards rewards = new SwrveIAPRewards();
        this.iapPlay(productId, productPrice, currency, rewards, purchaseData, dataSignature);
    }

    @Override
    public void iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String purchaseData, String dataSignature) {
        this._iapPlay(productId, productPrice, currency, rewards, purchaseData, dataSignature);
    }

    protected void _iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String purchaseData, String dataSignature) {
        try {
            if (checkPlayStoreSpecificArguments(purchaseData, dataSignature)) {
                this._iap(1, productId, productPrice, currency, rewards, purchaseData, dataSignature, "Google");
            }
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, "IAP Play event failed", exp);
        }
    }

    protected boolean checkPlayStoreSpecificArguments(String purchaseData, String receiptSignature) throws IllegalArgumentException {
        if (SwrveHelper.isNullOrEmpty(purchaseData)) {
            SwrveLogger.e(LOG_TAG, "IAP event illegal argument: receipt cannot be empty for Google Play store event");
            return false;
        }
        if (SwrveHelper.isNullOrEmpty(receiptSignature)) {
            SwrveLogger.e(LOG_TAG, "IAP event illegal argument: receiptSignature cannot be empty for Google Play store event");
            return false;
        }
        return true;
    }

    /**
     * @deprecated Swrve engaged events are automatically sent, so this is no longer needed.
     */
    @Deprecated
    public void processIntent(Intent intent) {
        SwrveLogger.e(LOG_TAG, "The processIntent method is Deprecated and should not be used anymore");
    }
}
