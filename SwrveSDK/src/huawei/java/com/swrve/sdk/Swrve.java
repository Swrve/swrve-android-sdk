package com.swrve.sdk;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

import static com.swrve.sdk.SwrveFlavour.HUAWEI;

/**
 * Main implementation of the Huawei Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {

    protected static final SwrveFlavour FLAVOUR = HUAWEI;
    protected SwrvePlatformUtil platformUtil;

    protected Swrve(Application application, int appId, String apiKey, SwrveConfig config) {
        super(application, appId, apiKey, config);
        if (isGooglePlayServicesEnabled(application)) {
            platformUtil = new SwrveGoogleUtil(application, profileManager.getTrackingState());
        } else {
            platformUtil = new SwrveHuaweiUtil(application, profileManager.getTrackingState());
        }
    }

    boolean isGooglePlayServicesEnabled(Context context) {
        boolean enabled = false;
        try {
            int isGooglePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
            enabled = isGooglePlayServicesAvailable == ConnectionResult.SUCCESS;
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK isGooglePlayServicesEnabled exception.", e);
        }
        return enabled;
    }

    @Override
    public void setRegistrationId(String regId) {
        final String userId = getUserId();
        storageExecutorExecute(() -> platformUtil.saveAndSendRegistrationId(multiLayerLocalStorage, userId, regId));
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        String userId = getUserId();
        platformUtil.init(multiLayerLocalStorage, userId, true, false);
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        platformUtil.appendDeviceUpdate(deviceInfo);
    }

    @Override
    protected String getPlatformOS(Context context) {
        return SwrveHelper.getPlatformOS(context, FLAVOUR);
    }
}
