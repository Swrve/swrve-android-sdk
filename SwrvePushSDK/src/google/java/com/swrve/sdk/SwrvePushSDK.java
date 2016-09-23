package com.swrve.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.iid.InstanceID;

public class SwrvePushSDK implements ISwrvePushSDK {
    private static final String TAG = "SwrveGcm";

    private final static String PUSH_TOKEN = "push_token";
    private final static String APP_VERSION = "app_version";
    private final static String GOOGLE_PREFERENCES = "swrve_google_push_pref";

    private static SwrvePushSDK instance;

    private String pushToken;
    private Context context;
    private ISwrvePushSDKListener listener;
    boolean init = false;
    String senderId;

    public SwrvePushSDK() {
    }

    public void onPushTokenRefreshed() {
        if (this.context == null) {
            SwrveLogger.e("Context is null");
            return;
        }
        registerInBackground(context, senderId);
    }

    public boolean isInitialised() {
        return init = true;
    }

    @Override
    public String initialisePushSDK(Context context, ISwrvePushSDKListener listener, String senderId) {
        if ((context == null) || (listener == null)) {
            SwrveLogger.e(TAG, "Unable to initalise push sdk. Context or listener are null");
            return null;
        }

        if (SwrveHelper.isNullOrEmpty(senderId)) {
            SwrveLogger.e(TAG, "Unable to initalise push sdk. SenderId is null.");
            return null;
        }

        this.context = context;
        this.listener = listener;
        this.senderId = senderId;

        try {
            // Check device for Play Services APK.
            if (SwrvePushGcmHelper.isGooglePlayServicesAvailable(context)) {
                String storedPushToken = getStoredPushToken();
                if (SwrveHelper.isNullOrEmpty(storedPushToken)) {
                    registerInBackground(this.context, this.senderId);
                } else {
                    pushToken = storedPushToken;
                }
            }
        } catch (Throwable exp) {
            // Don't trust GCM and all the moving parts to work as expected
            SwrveLogger.e(TAG, "Couldn't obtain the push token (registration id) for the device", exp);
        }
        init = true;
        return pushToken;
    }

    String getCurrentAppVersion() {
        String currentAppVersion = "";
        try {
            //Get the package info
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            currentAppVersion = pInfo.versionName;
        } catch (Exception ex) {
            SwrveLogger.e(TAG, "Couldn't get app version from PackageManager", ex);
        }
        return currentAppVersion;
    }

    /**
     * Gets the current stored push token (registration ID) for application on GCM service.
     * If result is empty, the app needs to register.
     *
     * @return Push token, or empty string if there is no existing push token.
     */
    protected String getStoredPushToken() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(GOOGLE_PREFERENCES, Context.MODE_PRIVATE);
        String pushTokenRaw = sharedPreferences.getString(PUSH_TOKEN, "");

        // Try to get push token from storage
        if (SwrveHelper.isNullOrEmpty(pushTokenRaw)) {
            return "";
        }
        // Check if app was updated; if so, it must clear the push token
        // since the existing push token is not guaranteed to work with the new
        // app version.
        String currentAppVersion = getCurrentAppVersion();
        String storedAppVersion = sharedPreferences.getString(APP_VERSION, "");
        if (!SwrveHelper.isNullOrEmpty(storedAppVersion) && !storedAppVersion.equals(currentAppVersion)) {
            return "";
        }
        return pushTokenRaw;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     *
     * Stores the push token (registration ID) and app version
     */
    protected void registerInBackground(final Context context, final String gcmSenderId) {
        new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Try to obtain the GCM push token (registration id) from Google Play
                try {
                    InstanceID instanceID = InstanceID.getInstance(context);
                    String pushToken = instanceID.getToken(gcmSenderId, null);
                    if (!SwrveHelper.isNullOrEmpty(pushToken)) {
                        setPushToken(pushToken);
                    }
                } catch (Exception ex) {
                    SwrveLogger.e(TAG, "Couldn't obtain the GCM push token (registration id) for the device", ex);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
            }
        }.execute(null, null, null);
    }

    void setPushToken(String newPushToken) {
        try {
            if (pushToken == null || !pushToken.equals(newPushToken)) {
                pushToken = newPushToken;
                String currentAppVersion = getCurrentAppVersion();

                SharedPreferences sharedPreferences = context.getSharedPreferences(GOOGLE_PREFERENCES, Context.MODE_PRIVATE);

                // Store push token and app version
                sharedPreferences.edit().putString(PUSH_TOKEN, pushToken).apply();
                sharedPreferences.edit().putString(APP_VERSION, currentAppVersion).apply();

                if (listener != null) {
                    listener.onPushTokenUpdated(newPushToken);
                } else {
                    SwrveLogger.e(TAG, "listener is null.");
                }
            }
        } catch (Exception ex) {
            SwrveLogger.e(TAG, "Couldn't save the GCM push token (registration id) for the device", ex);
        }
    }

    //Beware called from message handler on background thread
    public void onMessage(String msgId, Bundle msg) {
        if (listener != null) {
            listener.onMessageReceived(msgId, msg);
        } else {
            SwrveLogger.e(TAG, "listener is null.");
        }
    }

    //Called from notification engage receiver handling intent
    public void onNotificationEngaged(Bundle bundle) {
        if (listener != null) {
            listener.onNotificationEngaged(bundle);
        } else {
            SwrveLogger.e(TAG, "listener is null.");
        }
    }

    public static SwrvePushSDK getInstance() throws RuntimeException {
        if (instance == null) {
            instance = new SwrvePushSDK();
        }
        return instance;
    }
}
