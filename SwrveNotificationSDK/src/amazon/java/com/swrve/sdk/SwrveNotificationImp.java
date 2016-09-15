package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import com.amazon.device.messaging.ADM;
import com.amazon.device.messaging.development.ADMManifest;

public class SwrveNotificationImp {
    private static final String TAG = "SwrveAdm";

    boolean admAvailable = false;

    private ISwrveNotificationListener listener;

    private static SwrveNotificationImp instance;

    public SwrveNotificationImp() {
    }

    public boolean isPushEnabled() {
        return true;
    }

    public String initialiseNotificationSDK(Context context) {
        String admRegistrationId = "";

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

    public void setNotificationListener(ISwrveNotificationListener listener) {
        this.listener = listener;
    }

    //Beware called from background thread
    void onMessage(Bundle msg) {
        listener.onMessageReceived(msg);
    }

    //Beware called from background thread
    void onRegistered(String registrationId) {
        listener.onRegistrationIdUpdated(registrationId);
    }

    public void onPushEngaged(Bundle bundle) {
        listener.onPushEngaged(bundle);
    }

    public static SwrveNotificationImp getInstance() throws RuntimeException {
        if (instance == null) {
            instance = new SwrveNotificationImp();
        }
        return instance;
    }
}
