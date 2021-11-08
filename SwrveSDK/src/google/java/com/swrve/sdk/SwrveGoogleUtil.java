package com.swrve.sdk;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// This class executes google specific functions such as getting fcm token and advertising id. It is
// common to both the firebase and huawei flavours. See build.gradle where it is included  as an extra
// sourceSet to both those flavours.
class SwrveGoogleUtil implements SwrvePlatformUtil {

    protected static final String SWRVE_FIREBASE_TOKEN = "swrve.gcm_token";
    protected static final String SWRVE_GOOGLE_ADVERTISING_ID = "swrve.GAID";
    protected static final String CACHE_GOOGLE_ADVERTISING_ID = "GoogleAdvertisingId";
    protected static final String CACHE_REGISTRATION_ID = "RegistrationId";

    protected final Context context;
    protected String registrationId;
    protected String advertisingId;
    protected boolean logAdvertisingId;

    public SwrveGoogleUtil(Context context) {
        this.context = context;
    }

    @Override
    public void init(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, boolean automaticPushRegistration, boolean logAdvertisingId) {
        if (automaticPushRegistration) {
            obtainRegistrationId(multiLayerLocalStorage, userId);
        }

        this.logAdvertisingId = logAdvertisingId;
        if (logAdvertisingId) {
            obtainGAID(multiLayerLocalStorage, userId);
        }
    }

    void obtainRegistrationId(SwrveMultiLayerLocalStorage multiLayerLocalStorage, final String userId) {
        try {
            String newRegistrationId = getRegistrationId(multiLayerLocalStorage, userId);
            if (SwrveHelper.isNullOrEmpty(newRegistrationId)) {
                registerForTokenInBackground(multiLayerLocalStorage, userId);
            } else {
                registrationId = newRegistrationId;
            }
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: Couldn't obtain the registration token id", ex);
        }
    }

    void registerForTokenInBackground(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId) {

        FirebaseMessaging firebaseMessaging = getFirebaseMessagingInstance();
        if (firebaseMessaging != null) {
            Task<String> task = firebaseMessaging.getToken();
            task.addOnSuccessListener(newRegistrationId -> registerForTokenOnSuccessListener(multiLayerLocalStorage, userId, newRegistrationId))
                    .addOnFailureListener(e -> SwrveLogger.e("SwrveSDK Couldn't obtain the Firebase registration id for the device", e));
        }
    }

    FirebaseMessaging getFirebaseMessagingInstance() {
        FirebaseMessaging firebaseInstanceId = null;
        try {
            firebaseInstanceId = FirebaseMessaging.getInstance();
        } catch (IllegalStateException e) {
            SwrveLogger.e("SwrveSDK cannot get instance of FirebaseMessaging and therefore cannot get token registration id.", e);
        }
        return firebaseInstanceId;
    }

    void registerForTokenOnSuccessListener(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, String newRegistrationId) {
        if (!SwrveHelper.isNullOrEmpty(newRegistrationId)) {
            // Execute off the main thread
            final ExecutorService executorService = Executors.newSingleThreadExecutor();
            try {
                Runnable runnable = () -> saveAndSendRegistrationId(multiLayerLocalStorage, userId, newRegistrationId);
                executorService.execute(runnable);
            } finally {
                executorService.shutdown();
            }
        }
    }

    String getRegistrationId(SwrveMultiLayerLocalStorage multiLayerLocalStorage, final String userId) {
        String registrationIdRaw = multiLayerLocalStorage.getCacheEntry(userId, CACHE_REGISTRATION_ID);
        if (SwrveHelper.isNullOrEmpty(registrationIdRaw)) {
            return "";
        }
        return registrationIdRaw;
    }

    @Override
    public void saveAndSendRegistrationId(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, String regId) {
        if (registrationId == null || !registrationId.equals(regId)) {
            registrationId = regId;
            multiLayerLocalStorage.setCacheEntry(userId, CACHE_REGISTRATION_ID, registrationId);
        }

        try {
            JSONObject deviceUpdate = new JSONObject();
            appendDeviceUpdate(deviceUpdate);
            EventHelper.sendUninitiatedDeviceUpdateEvent(context, userId, deviceUpdate);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK exception in saveAndSendRegistrationId", e);
        }
    }

    @Override
    public void appendDeviceUpdate(JSONObject deviceUpdate) throws JSONException {
        if (!SwrveHelper.isNullOrEmpty(registrationId)) {
            deviceUpdate.put(SWRVE_FIREBASE_TOKEN, registrationId);
        }
        if (!SwrveHelper.isNullOrEmpty(advertisingId)) {
            deviceUpdate.put(SWRVE_GOOGLE_ADVERTISING_ID, advertisingId);
        }
    }

    void obtainGAID(SwrveMultiLayerLocalStorage multiLayerLocalStorage, final String userId) {
        // Load previous value for Advertising ID
        advertisingId = multiLayerLocalStorage.getCacheEntry(userId, CACHE_GOOGLE_ADVERTISING_ID);
        fetchGAIDAsync(multiLayerLocalStorage, userId);
    }

    void fetchGAIDAsync(SwrveMultiLayerLocalStorage multiLayerLocalStorage, final String userId) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Runnable runnable = () -> {
                try {
                    // Obtain and save the new Google Advertising Id
                    advertisingId = getAdvertisingId();
                    if (SwrveHelper.isNotNullOrEmpty(advertisingId)) {
                        if (advertisingId.equals("00000000-0000-0000-0000-000000000000")) {
                            SwrveLogger.e("SwrveSDK: Advertising Id has been redacted. Please check permissions.");
                        }
                        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
                        String uniqueKey = swrveCommon.getUniqueKey(userId);
                        multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_GOOGLE_ADVERTISING_ID, advertisingId, uniqueKey);
                    }
                } catch (Exception e) {
                    SwrveLogger.e("SwrveSDK: Couldn't obtain Advertising Id.", e);
                }
            };
            executorService.execute(runnable);
        } finally {
            executorService.shutdown();
        }
    }

    // The play-services-ads-identifier library adds a permission that may cause play store rejection for family rated apps.
    // This library needs to be added by the customer to build.gradle if they want access to advertising id. Eg:
    // api 'com.google.android.gms:play-services-ads-identifier:17.1.0'
    String getAdvertisingId() {
        String advertisingId = null;
        try {
            // Use reflection to access api: AdvertisingIdClient.getAdvertisingIdInfo(context).getId();
            Class advertisingIdClientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            if (advertisingIdClientClass != null) {
                Class<?> params[] = new Class[]{Context.class};
                Method getAdvertisingIdInfoMethod = advertisingIdClientClass.getMethod("getAdvertisingIdInfo", params);
                if (getAdvertisingIdInfoMethod != null) {
                    Object advertisingIdClientIInfoObject = getAdvertisingIdInfoMethod.invoke(null, context);
                    if (advertisingIdClientIInfoObject != null) {
                        Class infoClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info");
                        Method getIdMethod = infoClass.getMethod("getId");
                        if (getIdMethod != null) {
                            advertisingId = (String) getIdMethod.invoke(advertisingIdClientIInfoObject);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            if (logAdvertisingId) {
                SwrveLogger.e("SwrveSDK: Error getting Advertising Id. The play-services-ads-identifier library may not be a dependency or may be incorrect version. See docs.", ex);
            } else {
                SwrveLogger.v("SwrveSDK: Not getting Advertising Id. The play-services-ads-identifier library may not be a dependency.");
            }
        } finally {
            if (logAdvertisingId && SwrveHelper.isNullOrEmpty(advertisingId)) {
                SwrveLogger.e("SwrveSDK: Error getting Advertising Id. The play-services-ads-identifier library may not be a dependency or may be incorrect version. See docs.");
            }
        }
        return advertisingId;
    }
}
