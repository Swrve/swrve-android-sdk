package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import com.amazon.device.messaging.ADM;
import com.amazon.device.messaging.development.ADMManifest;

public class SwrvePushSDK implements ISwrvePushSDK {
    private static final String TAG = "SwrveAdm";

    private ISwrvePushSDKListener listener;
    private static SwrvePushSDK instance;

    public SwrvePushSDK() {
    }

    public boolean isInitialised() {
        return listener != null;
    }

    @Override
    public String initialisePushSDK(Context context, ISwrvePushSDKListener listener, String senderId) {
        if ((context == null) || (listener == null)) {
            SwrveLogger.e(TAG, "Unable to initalise push sdk. Context or listener are null");
            return null;
        }

        //Check for ADM class.
        boolean admAvailable = false;
        try {
            Class.forName( "com.amazon.device.messaging.ADM" );
            admAvailable = true ;
        } catch (ClassNotFoundException e) {
            // Log the exception.
            SwrveLogger.e(TAG, "ADM message class not found.", e);
        }

        this.listener = listener;
        String pushToken = "";

        if (admAvailable == true) {
            //TODO remove this.
            try {
                ADMManifest.checkManifestAuthoredProperly(context);
            } catch(Exception ex) {
                ex.printStackTrace();
            }

            try {
                final ADM adm = new ADM(context);
                //Adm stored value.
                pushToken = adm.getRegistrationId();

                //Always trigger if adm is null
                if (SwrveHelper.isNullOrEmpty(pushToken)) {
                    //This will call back and set value eventually.
                    adm.startRegister();
                }
            } catch (Throwable exp) {
                // Don't trust Amazon and all the moving parts to work as expected
                SwrveLogger.e(TAG, "Couldn't obtain the registration key for the device.", exp);
            }
        }
        return pushToken;
    }

    //Beware called from message handler on background thread
    void onPushTokenUpdated(String pushToken) {
        if (listener != null) {
            listener.onPushTokenUpdated(pushToken);
        } else {
            SwrveLogger.e(TAG, "listener is null.");
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

