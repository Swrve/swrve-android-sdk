package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.iid.InstanceID;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.ISwrvePushNotificationListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main implementation of the Google Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {
    protected static final String REGISTRATION_ID_CATEGORY = "RegistrationId";
    protected static final String SWRVE_GCM_TOKEN = "swrve.gcm_token";
    protected static final String SWRVE_GOOGLE_ADVERTISING_ID = "swrve.GAID";

    protected String registrationId;
    protected String advertisingId;
    protected boolean isAdvertisingLimitAdTrackingEnabled;
    protected ISwrvePushNotificationListener pushNotificationListener;
    protected String lastProcessedMessage;

    protected Swrve(Context context, int appId, String apiKey, SwrveConfig config) {
        super(context, appId, apiKey, config);
    }

    @Override
    public void onTokenRefreshed() {
        registerInBackground(getContext());
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        // Push notification configured for this app
        if (config.isPushEnabled()) {
            try {
                // Check device for Play Services APK.
                if (isGooglePlayServicesAvailable()) {
                    String newRegistrationId = getRegistrationId();
                    if (SwrveHelper.isNullOrEmpty(newRegistrationId)) {
                        registerInBackground(getContext());
                    } else {
                        registrationId = newRegistrationId;
                    }
                }
            } catch (Throwable exp) {
                // Don't trust GCM and all the moving parts to work as expected
                SwrveLogger.e(LOG_TAG, "Couldn't obtain the registration id for the device", exp);
            }
        }

        // Google Advertising Id logging enabled and Google Play services ready
        if (config.isGAIDLoggingEnabled() && isGooglePlayServicesAvailable()) {
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
    protected void afterInit() {
    }

    @Override
    protected void afterBind() {
        // empty
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        if (config.isPushEnabled() && !SwrveHelper.isNullOrEmpty(registrationId)) {
            deviceInfo.put(SWRVE_GCM_TOKEN, registrationId);
        }
        if (config.isGAIDLoggingEnabled() && !SwrveHelper.isNullOrEmpty(advertisingId)) {
            deviceInfo.put(SWRVE_GOOGLE_ADVERTISING_ID, advertisingId);
        }
    }

    @Override
    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    protected boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(context.get());
        if (resultCode == ConnectionResult.SUCCESS) {
            return true;
        }
        boolean resolveable = googleAPI.isUserResolvableError(resultCode);
        if (resolveable) {
            SwrveLogger.e(LOG_TAG, "Google Play Services are not available, resolveable error code: " + resultCode + ". You can use getErrorDialog in your app to try to address this issue at runtime.");
        } else {
            SwrveLogger.e(LOG_TAG, "Google Play Services are not available. Error code: " + resultCode);
        }

        return false;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     *
     * Stores the registration ID and app version
     */
    protected void registerInBackground(final Context context) {
        new AsyncTask<Void, Integer, Void>() {
            private void setRegistrationId(String regId) {
                try {
                    registrationId = regId;
                    if (qaUser != null) {
                        qaUser.updateDeviceInfo();
                    }

                    // Store registration id and app version
                    cachedLocalStorage.setAndFlushSharedEntry(REGISTRATION_ID_CATEGORY, registrationId);
                    cachedLocalStorage.setAndFlushSharedEntry(APP_VERSION_CATEGORY, appVersion);
                    // Re-send data now
                    queueDeviceInfoNow(true);
                } catch (Exception ex) {
                    SwrveLogger.e(LOG_TAG, "Couldn't save the GCM registration id for the device", ex);
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                // Try to obtain the GCM registration id from Google Play
                try {
                    InstanceID instanceID = InstanceID.getInstance(context);
                    String gcmRegistrationId = instanceID.getToken(config.getSenderId(), null);
                    if (!SwrveHelper.isNullOrEmpty(gcmRegistrationId)) {
                        setRegistrationId(gcmRegistrationId);
                    }
                } catch (Exception ex) {
                    SwrveLogger.e(LOG_TAG, "Couldn't obtain the GCM registration id for the device", ex);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
            }
        }.execute(null, null, null);
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    protected String getRegistrationId() {
        // Try to get registration id from storage
        String registrationIdRaw = cachedLocalStorage.getSharedCacheEntry(REGISTRATION_ID_CATEGORY);
        if (SwrveHelper.isNullOrEmpty(registrationIdRaw)) {
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        String registeredVersion = cachedLocalStorage.getSharedCacheEntry(APP_VERSION_CATEGORY);
        if (!SwrveHelper.isNullOrEmpty(registeredVersion) && !registeredVersion.equals(appVersion)) {
            return "";
        }
        return registrationIdRaw;
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

    @Override
    public void onResume(Activity ctx) {
        super.onResume(ctx);
        try {
            if (config.isPushEnabled()) {
                if (qaUser != null) {
                    qaUser.bindToServices(); // todo is this needed?
                }
            }
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, "onResume failed", exp);
        }
    }

    @Deprecated
    public void processIntent(Intent intent) {
        SwrveLogger.e(LOG_TAG, "The processIntent method is Deprecated and should not be used anymore");
    }
}
