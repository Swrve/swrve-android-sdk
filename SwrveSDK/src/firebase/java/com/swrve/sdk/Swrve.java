package com.swrve.sdk;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.firebase.SwrveFirebaseConstants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main implementation of the Firebase Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {
    protected static final String FLAVOUR_NAME = "firebase";
    protected static final String SWRVE_GOOGLE_ADVERTISING_ID = "swrve.GAID";

    protected String registrationId;
    protected String advertisingId;
    protected boolean isAdvertisingLimitAdTrackingEnabled;

    protected Swrve(Application application, int appId, String apiKey, SwrveConfig config) {
        super(application, appId, apiKey, config);
        SwrvePushSDK.createInstance(application.getApplicationContext());
    }

    @Override
    public void onTokenRefresh() {
        registerInBackground();
    }

    protected void registerInBackground() {
        final String userId = getUserId(); // user can logout or change so retrieve now as a final String for thread safeness
        final String sessionToken = profileManager.getSessionToken();
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                try {
                    String newRegistrationId = instanceIdResult.getToken();
                    if (!SwrveHelper.isNullOrEmpty(newRegistrationId)) {
                        _setRegistrationId(userId, sessionToken, newRegistrationId);
                    }
                } catch (Exception ex) {
                    SwrveLogger.e("Couldn't obtain the Firebase registration id for the device", ex);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                SwrveLogger.e("Couldn't obtain the Firebase registration id for the device", e);
            }
        });
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
            SwrveLogger.e("Google Play Services are not available, resolvable error code: %s. You can use getErrorDialog in your app to try to address this issue at runtime.", resultCode);
        } else {
            SwrveLogger.e("Google Play Services are not available. Error code: %s", resultCode);
        }

        return false;
    }

    private void obtainGAID() {
        if (isGooglePlayServicesAvailable()) {
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
                        AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getContext());
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
            // Store registration id
            multiLayerLocalStorage.setCacheEntry(profileManager.getUserId(), CACHE_REGISTRATION_ID, registrationId);
            // Re-send data now
            queueDeviceInfoNow(userId, sessionToken, true);
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
        String registrationIdRaw = multiLayerLocalStorage.getCacheEntry(profileManager.getUserId(), CACHE_REGISTRATION_ID);
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
}
