package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.SDK_PREFS_KEY_USER_ID;
import static com.swrve.sdk.ISwrveCommon.SDK_PREFS_NAME;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

class SwrveProfileManager<C extends SwrveConfigBase> {

    private final Context context;
    private final int appId;
    private final String apiKey;
    private final SwrveConfigBase config;
    protected IRESTClient restclient;
    private String userId;
    private String sessionToken;
    private SwrveTrackingState trackingState;
    private final Set<String> disabledUserIds = new HashSet<>();

    private final SwrveMultiLayerLocalStorage multiLayerLocalStorage;

    protected SwrveProfileManager(Context context, int appId, String apiKey, C config, IRESTClient restClient, SwrveMultiLayerLocalStorage multiLayerLocalStorage) {
        this.context = context;
        this.appId = appId;
        this.apiKey = apiKey;
        this.config = config;
        this.restclient = restClient;
        this.multiLayerLocalStorage = multiLayerLocalStorage;
    }

    // This method will not persist the userId to enable control of when tracking the anonymous userId begins in MANAGED mode
    synchronized void initUserId() {
        if (userId == null) { // double-checked lock
            String savedUserIdFromPrefs = getSavedUserIdFromPrefs();
            if (SwrveHelper.isNullOrEmpty(savedUserIdFromPrefs)) {
                userId = generateSwrveUserId(); // Create a random UUID
            } else {
                userId = savedUserIdFromPrefs;
            }
            SwrveLogger.i("SwrveSDK: userId is: %s", userId);
        }
    }

    String generateSwrveUserId() {
        return UUID.randomUUID().toString(); // Create a random UUID
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

    synchronized boolean addNewDisabledUserId(String userId) {
        if (disabledUserIds.contains(userId)) {
            SwrveLogger.i("SwrveSDK: ignoring duplicate disabled user for userId:%s", userId);
            return false;
        }
        disabledUserIds.add(userId);
        return true;
    }

    synchronized boolean isCurrentUserDisabled() {
        return this.userId != null && disabledUserIds.contains(this.userId);
    }

    String generateNewUser() {
        String newUserId = generateSwrveUserId();
        setUserId(newUserId);
        updateSessionToken();
        return newUserId;
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

    protected void handleDisabledUser(String responseBody, String disabledUserId) {
        if (SwrveHelper.isNullOrEmpty(responseBody) || SwrveHelper.isNullOrEmpty(disabledUserId)) {
            return;
        }

        try {
            JSONObject responseJson = new JSONObject(responseBody);
            String message = responseJson.optString("message", null);
            if (!"User access has been disabled".equals(message)) {
                return;
            }

            // If already disabled and we receive another 401 for the same user, ignore it as the user is already disabled
            // and we don't want to trigger the listener twice for the same user.
            if (!addNewDisabledUserId(disabledUserId)) {
                return;
            }

            // Before deleting all user data, get external Id to use in the listener onUserDisabled,
            SwrveUser disabledUser = multiLayerLocalStorage.getUserBySwrveUserId(disabledUserId);
            String externalUserId = disabledUser == null || disabledUser.getExternalUserId() == null ? "" : disabledUser.getExternalUserId();
            multiLayerLocalStorage.deleteAllDataForUserId(disabledUserId);
            multiLayerLocalStorage.deleteUser(disabledUserId);

            // Ensure the user has not changed to another user before calling stopTracking
            if (isCurrentUserDisabled()) {
                SwrveLogger.w("User access has been disabled, SDK will stop tracking.");
                SwrveSDK.stopTracking();
                generateNewUser();
            }

            if (config.getUserDisabledListener() != null) {
                config.getUserDisabledListener().onUserDisabled(context, disabledUserId, externalUserId);
            }

        } catch (Exception e) {
            SwrveLogger.e("Error parsing 401 response body while handling potential disabled user. Response body: " + responseBody, e);
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
