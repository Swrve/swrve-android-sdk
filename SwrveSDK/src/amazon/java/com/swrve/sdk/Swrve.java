package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.qa.SwrveQAUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Main implementation of the Amazon Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve, ISwrvePushSDKListener {
    protected static final String SWRVE_AMAZON_TOKEN = "swrve.adm_token";

    protected String pushToken;
    protected ISwrvePushNotificationListener pushNotificationListener;
    protected String lastProcessedMessage;

    protected Swrve(Context context, int appId, String apiKey, SwrveConfig config) {
        super(context, appId, apiKey, config);
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        try {
            ISwrvePushSDK pushSDK = SwrvePushSDK.createInstance();
            if (pushSDK != null) {
                SwrveNotificationDetail detail = SwrveNotification.createNotificationFromMetaData(context);
                pushToken = pushSDK.initialisePushSDK(context, this, detail, "");
            } else {
                SwrveLogger.e(LOG_TAG, "SwrvePushSDK is null");
            }
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "Unable to initialise push sdk: " + ex.toString());
        }
    }

    @Override
    public void onPushTokenUpdated(String pushToken) {
        try {
            this.pushToken = pushToken;
            if (qaUser != null) {
                qaUser.updateDeviceInfo();
            }

            // Re-send data now
            queueDeviceInfoNow(true);
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "Couldn't handle the ADM push token.", ex);
        }
    }

    @Override
    public void onMessageReceived(String msgId, Bundle msg) {
        //// Notify bound qa clients
        Iterator<SwrveQAUser> iter = SwrveQAUser.getBindedListeners().iterator();
        while (iter.hasNext()) {
            SwrveQAUser sdkListener = iter.next();
            sdkListener.pushNotification(msgId, msg);
        }
    }

    @Override
    public void onNotificationEngaged(Bundle msg) {
        //Push has been engaged, let customer listener know
        if (pushNotificationListener != null) {
            pushNotificationListener.onPushNotification(msg);
        }
    }

    @Override
    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        if (!SwrveHelper.isNullOrEmpty(pushToken)) {
            deviceInfo.put(SWRVE_AMAZON_TOKEN, pushToken);
        }
    }
}
