package com.swrve.sdk;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

public class SwrvePushSDKImp {
    private static final String TAG = "SwrveGcm";
    protected static final String REGISTRATION_ID_CATEGORY = "RegistrationId";
    protected static final String APP_VERSION_CATEGORY = "AppVersion";

    private ISwrvePushSDKListener listener;

    private static SwrvePushSDKImp instance;

    private String gcmRegistrationId;
    Context context;

    public SwrvePushSDKImp() {
    }

    public boolean isPushEnabled() {
        return true;
    }

    public void onGcmRegistrationIdRefreshed() {
        if (this.context == null) {
            SwrveLogger.e("Context is null");
            return;
        }
        registerInBackground(context);
    }

    public String initialisePushSDK(Context context) {
        this.context = context;

        if (!isPushEnabled()) {
            SwrveLogger.i(TAG, "isPushEnabled returned false.");
            return null;
        }

        try {
            // Check device for Play Services APK.
            if (isGooglePlayServicesAvailable()) {
                String newRegistrationId = getRegistrationId();
                if (SwrveHelper.isNullOrEmpty(newRegistrationId)) {
                    registerInBackground(this.context);
                } else {
                    gcmRegistrationId = newRegistrationId;
                }
            }
        } catch (Throwable exp) {
            // Don't trust GCM and all the moving parts to work as expected
            SwrveLogger.e(TAG, "Couldn't obtain the registration id for the device", exp);
        }
        return gcmRegistrationId;
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

    private void setRegistrationId(String regId) {
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
                    SwrveLogger.e(TAG, "Couldn't obtain the GCM registration id for the device", ex);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
            }
        }.execute(null, null, null);
    }

    public void setPushSDKListener(ISwrvePushSDKListener listener) {
        this.listener = listener;
    }

    //Beware called from message handler on background thread
    void onMessage(String msgId, Bundle msg) {
        listener.onMessageReceived(msgId, msg);
    }

    //Beware called from message handler on background thread
    void onRegistered(String registrationId) {
        try {
            if (gcmRegistrationId == null || !gcmRegistrationId.equals(regId)) {
                gcmRegistrationId = regId;

                // Store registration id and app version
                cachedLocalStorage.setAndFlushSharedEntry(REGISTRATION_ID_CATEGORY, registrationId);
                cachedLocalStorage.setAndFlushSharedEntry(APP_VERSION_CATEGORY, appVersion);
                // Re-send data now
            }
        } catch (Exception ex) {
            SwrveLogger.e(TAG, "Couldn't save the GCM registration id for the device", ex);
        }

        if (qaUser != null) {
            qaUser.updateDeviceInfo();
        }
        queueDeviceInfoNow(true);

        listener.onRegistrationIdUpdated(registrationId);
    }

    //Called from notification engage receiver handling intent
    public void onNotifcationEnaged(Bundle bundle) {
        listener.onNotificationEngaged(bundle);
    }

    public static SwrvePushSDKImp getInstance() throws RuntimeException {
        if (instance == null) {
            instance = new SwrvePushSDKImp();
        }
        return instance;
    }
}

