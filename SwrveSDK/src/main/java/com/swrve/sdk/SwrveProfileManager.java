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

import static com.swrve.sdk.SwrveImp.SDK_PREFS_NAME;

class SwrveProfileManager<C extends SwrveConfigBase> {

    private final Context context;
    private final int appId;
    private final String apiKey;
    private final SwrveConfigBase config;
    protected IRESTClient restclient;
    private String userId;
    private String sessionToken;

    protected SwrveProfileManager(Context context, int appId, String apiKey, C config, IRESTClient restClient) {
        this.context = context;

        String savedUserIdFromPrefs = getSavedUserIdFromPrefs(context);
        if (SwrveHelper.isNullOrEmpty(savedUserIdFromPrefs)) {
            this.userId = UUID.randomUUID().toString(); // Create a random UUID
        } else {
            this.userId = savedUserIdFromPrefs;
        }

        SwrveLogger.i("Your user id is: %s", this.userId);

        this.sessionToken = SwrveHelper.generateSessionToken(apiKey, appId, userId);
        this.config = config;
        this.restclient = restClient;
        this.apiKey = apiKey;
        this.appId = appId;
    }

    void persistUser() {
        saveUserIdToPrefs(this.userId);
    }

    protected String getUserId() {
        return userId;
    }

    private void saveUserIdToPrefs(String userId) {
        SharedPreferences settings = context.getSharedPreferences(SDK_PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit(); // Save new user id
        editor.putString("userId", userId).commit();
    }

    static String getSavedUserIdFromPrefs(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SDK_PREFS_NAME, 0);
        return settings.getString("userId", null);
    }

    protected String getSessionToken() {
        return sessionToken;
    }

    void updateSessionToken() {
        this.sessionToken = SwrveHelper.generateSessionToken(apiKey, appId, userId);
    }

    String updateUserId(String userId) {
        saveUserIdToPrefs(userId);
        this.userId = userId;
        SwrveLogger.i("Your user id is: %s", userId);
        return userId;
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
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    restclient.post(identityUrl, postString, callback);
                }
            };
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
