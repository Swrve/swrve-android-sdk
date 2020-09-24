package com.swrve.sdk;

import android.app.Application;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.firebase.SwrveFirebaseConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.UI_MODE_SERVICE;
import static com.swrve.sdk.ISwrveCommon.OS_ANDROID;
import static com.swrve.sdk.ISwrveCommon.OS_ANDROID_TV;

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
    }

    @Override
    public void onTokenRefresh() {
        if (!started) return;

        registerInBackground();
    }

    protected void registerInBackground() {
        FirebaseInstanceId firebaseInstanceId = getFirebaseInstanceId();
        if (firebaseInstanceId != null) {
            final String userId = getUserId(); // user can change so retrieve now as a final String for thread safeness
            Task<InstanceIdResult> task = firebaseInstanceId.getInstanceId();
            task.addOnSuccessListener(instanceIdResult -> {
                try {
                    String newRegistrationId = instanceIdResult.getToken();
                    if (!SwrveHelper.isNullOrEmpty(newRegistrationId)) {
                        _setRegistrationId(userId, newRegistrationId);
                    }
                } catch (Exception ex) {
                    SwrveLogger.e("Couldn't obtain the Firebase registration id for the device", ex);
                }
            }).addOnFailureListener(e -> SwrveLogger.e("Couldn't obtain the Firebase registration id for the device", e));
        }
    }

    protected FirebaseInstanceId getFirebaseInstanceId() {
        FirebaseInstanceId firebaseInstanceId = null;
        try {
            firebaseInstanceId = FirebaseInstanceId.getInstance();
        } catch (IllegalStateException e) {
            SwrveLogger.e("Swrve cannot get instance of FirebaseInstanceId and therefore cannot get token registration id.", e);
        }
        return firebaseInstanceId;
    }

    private void obtainGAID() {
        // Load previous value for Advertising ID
        advertisingId = multiLayerLocalStorage.getCacheEntry(getUserId(), CACHE_GOOGLE_ADVERTISING_ID);
        String isAdvertisingLimitAdTrackingEnabledString = multiLayerLocalStorage.getCacheEntry(getUserId(), CACHE_GOOGLE_ADVERTISING_AD_TRACK_LIMIT);
        isAdvertisingLimitAdTrackingEnabled = Boolean.parseBoolean(isAdvertisingLimitAdTrackingEnabledString);
        final String userId = getUserId(); // user can change so retrieve now as a final String for thread safeness
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
                } catch (IOException ex) {
                    // Unrecoverable error connecting to Google Play services (e.g.,
                    // the old version of the service doesn't support getting AdvertisingId).
                    SwrveLogger.e("Couldn't obtain Advertising Id: Unrecoverable error connecting to Google Play services", ex);
                } catch (GooglePlayServicesNotAvailableException ex) {
                    // Google Play services is not available entirely.
                    SwrveLogger.e("Couldn't obtain Advertising Id: Google Play services is not available", ex);
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

    @Override
    public void setRegistrationId(String regId) {
        try {
            final String userId = getUserId(); // user can change so retrieve now as a final String for thread safeness
            _setRegistrationId(userId, regId);
        } catch (Exception ex) {
            SwrveLogger.e("Couldn't save the GCM registration id for the device", ex);
        }
    }

    private void _setRegistrationId(String userId, String regId) {
        if (registrationId == null || !registrationId.equals(regId)) {
            registrationId = regId;
            multiLayerLocalStorage.setCacheEntry(profileManager.getUserId(), CACHE_REGISTRATION_ID, registrationId); // Store registration id
            sendDeviceTokenUpdateNow(userId); // Send token now
        }
    }

    // Send device token in the background without affecting DAU/sessions (user_initiated = false)
    protected void sendDeviceTokenUpdateNow(final String userId) {
        final SwrveBase<ISwrve, SwrveConfig> swrve = this;
        storageExecutorExecute(() -> {
            try {
                JSONObject firebaseDeviceInfo = new JSONObject();
                extraDeviceInfo(firebaseDeviceInfo);

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("attributes", firebaseDeviceInfo);
                parameters.put("user_initiated", "false");

                int seqnum = swrve.getNextSequenceNumber();
                String event = EventHelper.eventAsJSON("device_update", parameters, null, seqnum, System.currentTimeMillis());
                ArrayList<String> events = new ArrayList<>();
                events.add(event);
                swrve.sendEventsInBackground(context.get(), userId, events);
            } catch (JSONException e) {
                SwrveLogger.e("Couldn't construct token update event", e);
            }
        });
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        if (config.isPushRegistrationAutomatic()) {
            try {
                String newRegistrationId = getRegistrationId();
                if (SwrveHelper.isNullOrEmpty(newRegistrationId)) {
                    registerInBackground();
                } else {
                    registrationId = newRegistrationId;
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
    protected String getPlatformOS(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return OS_ANDROID_TV;
        }
        return OS_ANDROID;
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
        if (!started) return;

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
