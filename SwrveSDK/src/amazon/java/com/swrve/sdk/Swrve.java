package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;

import com.amazon.device.messaging.ADM;
import com.amazon.device.messaging.development.ADMManifest;
import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main implementation of the Amazon Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {
    protected static final String SWRVE_AMAZON_TOKEN = "swrve.adm_token";

    protected String admRegistrationId;
    protected ISwrvePushNotificationListener pushNotificationListener;
    protected String lastProcessedMessage;
    protected boolean admAvailable = false;

    protected Swrve(Context context, int appId, String apiKey, SwrveConfig config) {
        super(context, appId, apiKey, config);
    }

    //ADM callbacks
    @Override
    public void onRegistrationIdReceived(String registrationId) {
        try {
            admRegistrationId = registrationId;
            if (qaUser != null) {
                qaUser.updateDeviceInfo();
            }

            // Re-send data now
            queueDeviceInfoNow(true);
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "Couldn't update the ADM registration id for the device", ex);
        }
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        //Check for class.
        try {
            Class.forName( "com.amazon.device.messaging.ADM" );
            admAvailable = true ;
        } catch (ClassNotFoundException e) {
            // Log the exception.
            SwrveLogger.w(LOG_TAG, "ADM message class not found.", e);
        }

        if (config.isPushEnabled()) {
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
                    SwrveLogger.e(LOG_TAG, "Couldn't obtain the registration key for the device.", exp);
                }
            }
        }
    }

    @Override
    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        if (config.isPushEnabled() && !SwrveHelper.isNullOrEmpty(admRegistrationId)) {
            deviceInfo.put(SWRVE_AMAZON_TOKEN, admRegistrationId);
        }
    }


    public void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature) {
        //TODO
        SwrveLogger.e(LOG_TAG, "iapPlay TODO");
    }

    /**
     * @deprecated Swrve engaged events are automatically sent, so this is no longer needed.
     */
    @Deprecated
    public void processIntent(Intent intent) {
        SwrveLogger.e(LOG_TAG, "The processIntent method is Deprecated and should not be used anymore");
    }
}
