package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import com.amazon.device.messaging.ADM;
import com.amazon.device.messaging.development.ADMManifest;

public class SwrvePushSDKImp {
    private static final String TAG = "SwrveAdm";

    boolean admAvailable = false;

    private ISwrvePushSDKListener listener;

    private static SwrvePushSDKImp instance;

    public SwrvePushSDKImp() {
    }

    public boolean isPushEnabled() {
        return true;
    }

    public String initialisePushSDK(Context context) {
        String admRegistrationId = "";

        if (!isPushEnabled()) {
            SwrveLogger.i(TAG, "isPushEnabled returned false.");
            return null;
        }

        //Check for class.
        try {
            Class.forName( "com.amazon.device.messaging.ADM" );
            admAvailable = true ;
        } catch (ClassNotFoundException e) {
            // Log the exception.
            SwrveLogger.w(TAG, "ADM message class not found.", e);
        }

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
                admRegistrationId = adm.getRegistrationId();

                //Always trigger if adm is null
                if (SwrveHelper.isNullOrEmpty(admRegistrationId)) {
                    //This will call back and set value eventually.
                    adm.startRegister();
                }
            } catch (Throwable exp) {
                // Don't trust Amazon and all the moving parts to work as expected
                SwrveLogger.e(TAG, "Couldn't obtain the registration key for the device.", exp);
            }
        }
        return admRegistrationId;
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

