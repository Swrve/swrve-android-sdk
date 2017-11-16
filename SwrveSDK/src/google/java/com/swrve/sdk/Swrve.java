package com.swrve.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.ads.identifier.AdvertisingIdClient.Info;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.iid.InstanceID;
import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main implementation of the Google Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {
    protected static final String FLAVOUR_NAME = "google";
    protected static final String SWRVE_GCM_TOKEN = "swrve.gcm_token";
    protected static final String SWRVE_GOOGLE_ADVERTISING_ID = "swrve.GAID";

    protected String registrationId;
    protected String advertisingId;
    protected boolean isAdvertisingLimitAdTrackingEnabled;

    protected Swrve(Application application, int appId, String apiKey, SwrveConfig config) {
        super(application, appId, apiKey, config);
        SwrvePushSDK.createInstance(application.getApplicationContext());
    }

    @Override
    protected void _onResume(Activity ctx) {
        super._onResume(ctx);

        // Detect if user is influenced by a push notification
        SwrvePushSDK pushSDK = SwrvePushSDK.getInstance();
        if (pushSDK != null) {
            pushSDK.processInfluenceData(this);
        }
    }

    @Override
    public void onTokenRefreshed() {
        registerInBackground(getContext());
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        // Push notification configured for this app
        if (config.isPushEnabled() && config.isPushRegistrationAutomatic()) {
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
                SwrveLogger.e("Couldn't obtain the registration id for the device", exp);
            }
        }

        // Google Advertising Id logging enabled and Google Play services ready
        if (config.isGAIDLoggingEnabled() && isGooglePlayServicesAvailable()) {
            // Load previous value for Advertising ID
            advertisingId = multiLayerLocalStorage.getCacheEntry(getUserId(), CACHE_GOOGLE_ADVERTISING_ID);
            String isAdvertisingLimitAdTrackingEnabledString = multiLayerLocalStorage.getCacheEntry(getUserId(), CACHE_GOOGLE_ADVERTISING_AD_TRACK_LIMIT);
            isAdvertisingLimitAdTrackingEnabled = Boolean.parseBoolean(isAdvertisingLimitAdTrackingEnabledString);
            final String userId = getUserId(); // user can logout or change so retrieve now as a final String for thread safeness
            new AsyncTask<Void, Integer, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        // Obtain and save the new Google Advertising Id
                        Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                        advertisingId = adInfo.getId();
                        isAdvertisingLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled();

                        multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_GOOGLE_ADVERTISING_ID, advertisingId, getUniqueKey(userId));
                        multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_GOOGLE_ADVERTISING_AD_TRACK_LIMIT, Boolean.toString(isAdvertisingLimitAdTrackingEnabled), getUniqueKey(userId));
                    } catch (Exception ex) {
                        SwrveLogger.e("Couldn't obtain Advertising Id", ex);
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
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        if (config.isPushEnabled() && !SwrveHelper.isNullOrEmpty(registrationId)) {
            deviceInfo.put(SWRVE_GCM_TOKEN, registrationId);
        }
        if (config.isGAIDLoggingEnabled() && !SwrveHelper.isNullOrEmpty(advertisingId)) {
            deviceInfo.put(SWRVE_GOOGLE_ADVERTISING_ID, advertisingId);
        }
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
            SwrveLogger.e("Google Play Services are not available, resolveable error code: " + resultCode + ". You can use getErrorDialog in your app to try to address this issue at runtime.");
        } else {
            SwrveLogger.e("Google Play Services are not available. Error code: " + resultCode);
        }

        return false;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     *
     * Stores the registration ID and app version
     */
    protected void registerInBackground(final Context context) {
        final String userId = getUserId(); // user can logout or change so retrieve now as a final String for thread safeness
        final String sessionToken = profileManager.getSessionToken();
        new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Try to obtain the GCM registration id from Google Play
                try {
                    InstanceID instanceID = InstanceID.getInstance(context);
                    String gcmRegistrationId = instanceID.getToken(config.getSenderId(), null);
                    if (!SwrveHelper.isNullOrEmpty(gcmRegistrationId)) {
                        _setRegistrationId(userId, sessionToken, gcmRegistrationId);
                    }
                } catch (Exception ex) {
                    SwrveLogger.e("Couldn't obtain the GCM registration id for the device", ex);
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
        String registrationIdRaw = multiLayerLocalStorage.getCacheEntry(profileManager.getUserId(), CACHE_REGISTRATION_ID);
        if (SwrveHelper.isNullOrEmpty(registrationIdRaw)) {
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        String registeredVersion = multiLayerLocalStorage.getCacheEntry(profileManager.getUserId(), CACHE_APP_VERSION);
        if (!SwrveHelper.isNullOrEmpty(registeredVersion) && !registeredVersion.equals(appVersion)) {
            return "";
        }
        return registrationIdRaw;
    }

    @Override
    public void setRegistrationId(String regId) {
        try {
            if(profileManager != null && profileManager.isLoggedIn()) {
                final String userId = getUserId(); // user can logout or change so retrieve now as a final String for thread safeness
                final String sessionToken = profileManager.getSessionToken();
                _setRegistrationId(userId, sessionToken, regId);
            }
        } catch (Exception ex) {
            SwrveLogger.e("Couldn't save the GCM registration id for the device", ex);
        }
    }

    private void _setRegistrationId(String userId, String sessionToken, String regId) throws Exception {
        if (registrationId == null || !registrationId.equals(regId)) {
            registrationId = regId;
            if (qaUser != null) {
                qaUser.logDeviceInfo(getDeviceInfo());
            }

            // Store registration id and app version
            multiLayerLocalStorage.setCacheEntry(userId, CACHE_REGISTRATION_ID, registrationId);
            multiLayerLocalStorage.setCacheEntry(userId, CACHE_APP_VERSION, appVersion);
            // Re-send data now
            queueDeviceInfoNow(userId, sessionToken, true);
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
            SwrveLogger.e("IAP Play event failed", exp);
        }
    }

    protected boolean checkPlayStoreSpecificArguments(String purchaseData, String receiptSignature) throws IllegalArgumentException {
        if (SwrveHelper.isNullOrEmpty(purchaseData)) {
            SwrveLogger.e("IAP event illegal argument: receipt cannot be empty for Google Play store event");
            return false;
        }
        if (SwrveHelper.isNullOrEmpty(receiptSignature)) {
            SwrveLogger.e("IAP event illegal argument: receiptSignature cannot be empty for Google Play store event");
            return false;
        }
        return true;
    }
}
