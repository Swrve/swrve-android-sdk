package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.firebase.SwrveFirebaseConstants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main implementation of the Firebase Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {
    protected static final String REGISTRATION_ID_CATEGORY = "RegistrationId";
    protected static final String SWRVE_GOOGLE_ADVERTISING_ID = "swrve.GAID";

    protected String registrationId;
    protected String advertisingId;
    protected boolean isAdvertisingLimitAdTrackingEnabled;

    protected Swrve(Context context, int appId, String apiKey, SwrveConfig config) {
        super(context, appId, apiKey, config);
        SwrvePushSDK.createInstance(context)
                .setDefaultNotificationChannel(config.getDefaultNotificationChannel());
    }

    @Override
    public void onTokenRefresh() {
        registerInBackground();
    }

    protected void registerInBackground() {
        new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Try to obtain the Firebase registration id from Google Play
                try {
                    String newRegistrationId = FirebaseInstanceId.getInstance().getToken();
                    if (!SwrveHelper.isNullOrEmpty(newRegistrationId)) {
                        setRegistrationId(newRegistrationId);
                    }
                } catch (Exception ex) {
                    SwrveLogger.e("Couldn't obtain the Firebase registration id for the device", ex);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
            }
        }.execute(null, null, null);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(getContext());
        if (resultCode == ConnectionResult.SUCCESS) {
            return true;
        }
        if (googleAPI.isUserResolvableError(resultCode)) {
            SwrveLogger.e("Google Play Services are not available, resolvable error code: " + resultCode + ". You can use getErrorDialog in your app to try to address this issue at runtime.");
        } else {
            SwrveLogger.e("Google Play Services are not available. Error code: " + resultCode);
        }

        return false;
    }

    private void obtainGAID() {
        if (isGooglePlayServicesAvailable()) {
            // Load previous value for Advertising ID
            advertisingId = cachedLocalStorage.getCacheEntryForUser(getUserId(), SWRVE_GOOGLE_ADVERTISING_ID_CATEGORY);
            String isAdvertisingLimitAdTrackingEnabledString = cachedLocalStorage.getCacheEntryForUser(getUserId(), SWRVE_GOOGLE_ADVERTISING_LIMIT_AD_TRACKING_CATEGORY);
            isAdvertisingLimitAdTrackingEnabled = Boolean.parseBoolean(isAdvertisingLimitAdTrackingEnabledString);
            new AsyncTask<Void, Integer, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        // Obtain and save the new Google Advertising Id
                        AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getContext());
                        advertisingId = adInfo.getId();
                        isAdvertisingLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled();

                        cachedLocalStorage.setAndFlushSecureSharedEntryForUser(getUserId(), SWRVE_GOOGLE_ADVERTISING_ID_CATEGORY, advertisingId, getUniqueKey());
                        cachedLocalStorage.setAndFlushSecureSharedEntryForUser(getUserId(), SWRVE_GOOGLE_ADVERTISING_LIMIT_AD_TRACKING_CATEGORY, Boolean.toString(isAdvertisingLimitAdTrackingEnabled), getUniqueKey());
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
    public void setRegistrationId(String regId) {
        if (registrationId == null || !registrationId.equals(regId)) {
            registrationId = regId;
            if (qaUser != null) {
                qaUser.logDeviceInfo(getDeviceInfo());
            }
            // Store registration id and app version
            cachedLocalStorage.setAndFlushSharedEntry(REGISTRATION_ID_CATEGORY, registrationId);
            // Re-send data now
            queueDeviceInfoNow(true);
        }
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        if (config.isPushRegistrationAutomatic()) {
            try {
                // Check device for Play Services APK.
                if (isGooglePlayServicesAvailable()) {
                    String newRegistrationId = getRegistrationId();
                    if (SwrveHelper.isNullOrEmpty(newRegistrationId)) {
                        registerInBackground();
                    } else {
                        registrationId = newRegistrationId;
                    }
                }
            } catch (Throwable exp) {
                // Don't trust Firebase and all the moving parts to work as expected
                SwrveLogger.e("Couldn't obtain the registration id for the device", exp);
            }
        }

        // Google Advertising Id logging enabled and Google Play services ready
        if (config.isGAIDLoggingEnabled()) {
            obtainGAID();
        }
    }

    protected String getRegistrationId() {
        // Try to get registration id from storage
        String registrationIdRaw = cachedLocalStorage.getSharedCacheEntry(REGISTRATION_ID_CATEGORY);
        if (SwrveHelper.isNullOrEmpty(registrationIdRaw)) {
            return "";
        }
        return registrationIdRaw;
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        if (!SwrveHelper.isNullOrEmpty(registrationId)) {
            deviceInfo.put(SwrveFirebaseConstants.SWRVE_FIREBASE_TOKEN, registrationId);
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

    private void _iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String purchaseData, String dataSignature) {
        try {
            if (checkPlayStoreSpecificArguments(purchaseData, dataSignature)) {
                this._iap(1, productId, productPrice, currency, rewards, purchaseData, dataSignature, "Google");
            }
        } catch (Exception exp) {
            SwrveLogger.e("IAP Play event failed", exp);
        }
    }

    private boolean checkPlayStoreSpecificArguments(String purchaseData, String receiptSignature) throws IllegalArgumentException {
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

    @Override
    protected void _onResume(Activity ctx) {
        super._onResume(ctx);

        // Detect if user is influenced by a push notification
        SwrvePushSDK pushSDK = SwrvePushSDK.getInstance();
        if (pushSDK != null) {
            pushSDK.processInfluenceData(this);
        }
    }
}
