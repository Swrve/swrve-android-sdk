package com.swrve.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.swrve.sdk.ISwrveCommon.SDK_PREFS_KEY_USER_ID;
import static com.swrve.sdk.ISwrveCommon.SDK_PREFS_NAME;

class SwrveProfileManager<C extends SwrveConfigBase> {

    private final Context context;
    private final int appId;
    private final String apiKey;
    private final SwrveConfigBase config;
    protected IRESTClient restclient;
    private String userId;
    private String sessionToken;
    private SwrveTrackingState trackingState;

    protected SwrveProfileManager(Context context, int appId, String apiKey, C config, IRESTClient restClient) {
        this.context = context;
        this.appId = appId;
        this.apiKey = apiKey;
        this.config = config;
        this.restclient = restClient;
    }

    // This method will not persist the userId to enable control of when tracking the anonymous userId begins in MANAGED mode
    synchronized void initUserId() {
        if (userId == null) { // double-checked lock
            String savedUserIdFromPrefs = getSavedUserIdFromPrefs();
            if (SwrveHelper.isNullOrEmpty(savedUserIdFromPrefs)) {
                userId = UUID.randomUUID().toString(); // Create a random UUID
            } else {
                userId = savedUserIdFromPrefs;
            }
            SwrveLogger.i("SwrveSDK: userId is: %s", userId);
        }
    }

    void persistUser() {
        String userIdToSave = getUserId(); // ensure userId has been initialised by calling getUserId()
        SharedPreferences settings = context.getSharedPreferences(SDK_PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit(); // Save new user id
        editor.putString(SDK_PREFS_KEY_USER_ID, userIdToSave).commit();
    }

    String getUserId() {
        if (userId == null) {
            initUserId();
        }
        return userId;
    }

    void setUserId(String userId) {
        this.userId = userId;
        persistUser();
        SwrveLogger.i("SwrveSDK: userId is set to: %s", userId);
    }

    String getSavedUserIdFromPrefs() {
        SharedPreferences settings = context.getSharedPreferences(SDK_PREFS_NAME, 0);
        return settings.getString(SDK_PREFS_KEY_USER_ID, null);
    }

    synchronized void initTrackingState() {
        if (trackingState == null) { // double-checked lock
            trackingState = SwrveTrackingState.getTrackingState(context);
            SwrveLogger.i("SwrveSDK: trackingState:%s", trackingState);
        }
    }

    SwrveTrackingState getTrackingState() {
        if (trackingState == null) {
            initTrackingState();
        }
        return trackingState;
    }

    void setTrackingState(SwrveTrackingState trackingState) {
        this.trackingState = trackingState;
        SwrveTrackingState.saveTrackingState(context, trackingState);
        SwrveLogger.i("SwrveSDK: trackingState is set to: %s", trackingState);
    }

    protected String getSessionToken() {
        if (sessionToken == null) {
            sessionToken = SwrveHelper.generateSessionToken(apiKey, appId, userId);
        }
        return sessionToken;
    }

    void updateSessionToken() {
        this.sessionToken = SwrveHelper.generateSessionToken(apiKey, appId, userId);
    }

    protected String getIdentityUrl() {
        return config.getIdentityUrl() + SwrveBase.IDENTITY_ACTION;
    }

    protected String getIdentityBody(final String externalUserId, final String userId, final String deviceId) {
        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();
        map.put("swrve_id", userId);
        map.put("external_user_id", externalUserId);
        map.put("unique_device_id", deviceId);
        map.put("api_key", apiKey);
        return gson.toJson(map);
    }

    protected void identify(final String externalUserId, final String userId, final String deviceId, final SwrveIdentityResponse identityResponse) {

        final IdentifyIRESTResponseListener callback = new IdentifyIRESTResponseListener(identityResponse);
        final String postString = getIdentityBody(externalUserId, userId, deviceId);
        final String identityUrl = getIdentityUrl();
        SwrveLogger.d("Identity call: %s  body:  %s ", identityUrl, postString);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Runnable runnable = () -> restclient.post(identityUrl, postString, callback);
            executorService.execute(SwrveRunnables.withoutExceptions(runnable));
        } finally {
            executorService.shutdown();
        }
    }

    private class IdentifyIRESTResponseListener implements IRESTResponseListener {

        final SwrveIdentityResponse identityResponse;

        public IdentifyIRESTResponseListener(SwrveIdentityResponse identityResponse) {
            this.identityResponse = identityResponse;
        }

        @Override
        public void onResponse(RESTResponse response) {
            String status = null;
            String swrveId = null;
            String errorMessage = null;

            try {
                JSONObject responseJson = new JSONObject(response.responseBody);
                if (responseJson.has("status")) {
                    status = responseJson.getString("status");
                }

                if (responseJson.has("swrve_id")) {
                    swrveId = responseJson.getString("swrve_id");
                }

                if (responseJson.has("message")) {
                    errorMessage = responseJson.getString("message");
                } else {
                    errorMessage = response.responseBody;
                }

            } catch (Exception e) {
                SwrveLogger.e("SwrveSDK unable to decode identity JSON : \"%s\".", response.responseBody);
            }

            if (response.responseCode == HttpURLConnection.HTTP_OK) {

                if (SwrveHelper.isNullOrEmpty(swrveId)) {
                    identityResponse.onError(response.responseCode, "Swrve Id was missing from json payload");
                    return;
                }

                identityResponse.onSuccess(status, swrveId);
            } else if (response.responseCode < HttpURLConnection.HTTP_INTERNAL_ERROR) {
                // client, redirect error may contain a message decoded above from the server
                identityResponse.onError(response.responseCode, errorMessage);
            }
            // 500 exception range will be processed in the onException below so do nothing here
        }

        @Override
        public void onException(Exception ex) {
            SwrveLogger.e("Error calling identity service", ex);
            String errorMessage = ex.getMessage() == null ? "Unknown error" : ex.getMessage();
            identityResponse.onError(503, errorMessage);
        }
    }
}
