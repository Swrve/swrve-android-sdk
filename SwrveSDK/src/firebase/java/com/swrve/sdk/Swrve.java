package com.swrve.sdk;

import static com.swrve.sdk.SwrveFlavour.FIREBASE;

import android.app.Application;
import android.content.Context;

import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main implementation of the Firebase Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {

    protected static final SwrveFlavour FLAVOUR = FIREBASE;

    protected SwrveGoogleUtil googleUtil;

    protected Swrve(Application application, int appId, String apiKey, SwrveConfig config) {
        super(application, appId, apiKey, config);
        googleUtil = new SwrveGoogleUtil(application, profileManager.getTrackingState());
    }

    @Override
    public void onTokenRefresh() {
        if (!isStarted()) return;

        final String userId = getUserId();
        googleUtil.registerForTokenInBackground(multiLayerLocalStorage, userId);
    }

    @Override
    public void setRegistrationId(String regId) {
        final String userId = getUserId();
        storageExecutorExecute(() -> googleUtil.saveAndSendRegistrationId(multiLayerLocalStorage, userId, regId));
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        String userId = getUserId();
        googleUtil.init(multiLayerLocalStorage, userId, config.isPushRegistrationAutomatic(), config.isGAIDLoggingEnabled());
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        googleUtil.appendDeviceUpdate(deviceInfo);
    }

    @Override
    protected String getPlatformOS(Context context) {
        return SwrveHelper.getPlatformOS(context, FLAVOUR);
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
        if (!isStarted()) return;

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
