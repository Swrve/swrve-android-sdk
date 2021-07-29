package com.swrve.sdk;

import android.content.Context;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;

import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.huawei.hms.push.HmsMessaging.DEFAULT_TOKEN_SCOPE;
import static com.swrve.sdk.ISwrveCommon.CACHE_DEVICE_PROP_KEY;

class SwrveHuaweiUtil implements SwrvePlatformUtil {

    protected static final String SWRVE_HMS_TOKEN = "swrve.hms_token";
    protected static final String HMS_CACHE_REGISTRATION_ID = "RegistrationIdHMS";

    protected final Context context;
    protected String registrationId;

    public SwrveHuaweiUtil(Context context) {
        this.context = context;
    }

    @Override
    public void init(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, boolean automaticPushRegistration, boolean logAdvertisingId) {
        if (automaticPushRegistration) {
            registrationId = getRegistrationId(multiLayerLocalStorage);
            String appId = getAppId();
            if (SwrveHelper.isNotNullOrEmpty(appId)) {
                executeSafeRunnable(() -> registerForTokenInBackground(multiLayerLocalStorage, userId, appId, registrationId));
            }
        }
        // logAdvertisingId is ignored for now.
    }

    protected String getRegistrationId(SwrveMultiLayerLocalStorage multiLayerLocalStorage) {
        String registrationIdRaw = multiLayerLocalStorage.getCacheEntry(CACHE_DEVICE_PROP_KEY, HMS_CACHE_REGISTRATION_ID); // regId saved with blank userId in huwaei sdk.
        if (SwrveHelper.isNullOrEmpty(registrationIdRaw)) {
            registrationIdRaw = "";
        }
        return registrationIdRaw;
    }

    @Override
    public void saveAndSendRegistrationId(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, String regId) {
        if (registrationId == null || !registrationId.equals(regId)) {
            registrationId = regId;
            multiLayerLocalStorage.setCacheEntry(CACHE_DEVICE_PROP_KEY, HMS_CACHE_REGISTRATION_ID, registrationId); // regId saved with blank userId in huwaei sdk.
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
            deviceUpdate.put(SWRVE_HMS_TOKEN, registrationId);
        }
    }

    protected void registerForTokenInBackground(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, String appId, String registrationId) {
        HmsInstanceId hmsInstanceId = getHMSInstance();
        if (hmsInstanceId != null) {
            String newRegId = getToken(hmsInstanceId, appId);
            if (SwrveHelper.isNullOrEmpty(newRegId)) {
                SwrveLogger.i("SwrveSDK getToken returned null or empty.");
            } else if (newRegId.equals(registrationId)) {
                SwrveLogger.i("SwrveSDK getToken no change to token in storage.");
            } else {
                SwrveLogger.i("SwrveSDK getToken saving and updated new token.", newRegId);
                saveAndSendRegistrationId(multiLayerLocalStorage, userId, newRegId);
            }
        }
    }

    protected String getToken(HmsInstanceId hmsInstanceId, String appId) {
        String token = null;
        try {
            token = hmsInstanceId.getToken(appId, DEFAULT_TOKEN_SCOPE);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK error getToken.", e);
        }
        return token;
    }

    HmsInstanceId getHMSInstance() {
        HmsInstanceId hmsInstanceId = null;
        try {
            hmsInstanceId = HmsInstanceId.getInstance(context);
        } catch (IllegalStateException e) {
            SwrveLogger.e("SwrveSDK cannot get instance of HmsInstanceId and therefore cannot get token registration id.", e);
        }
        return hmsInstanceId;
    }

    public String getAppId() {
        String appId = null;
        try {
            appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id");
            if (SwrveHelper.isNullOrEmpty(appId)) {
                SwrveLogger.e("SwrveSDK error project app id from AGConnectServicesConfig is null or empty");
                return null;
            }
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK error getting project app id from AGConnectServicesConfig config.", e);
        }
        return appId;
    }

    protected void executeSafeRunnable(Runnable runnable) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Runnable safeRunnable = SwrveRunnables.withoutExceptions(runnable);
            executorService.execute(safeRunnable);
        } finally {
            executorService.shutdown();
        }
    }
}
