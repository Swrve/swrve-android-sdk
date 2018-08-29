package com.swrve.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import com.swrve.sdk.config.SwrveConfigBase;

import java.util.UUID;

import static com.swrve.sdk.SwrveImp.SDK_PREFS_NAME;

class SwrveProfileManager<C extends SwrveConfigBase> {

    private Context context;
    private String userId;
    private String sessionToken;

    protected SwrveProfileManager(Context context, C config, String apiKey, int appId) {
        this.context = context;
        this.userId = getUserId(config);
        this.sessionToken = SwrveHelper.generateSessionToken(apiKey, appId, userId);
    }

    private String getUserId(C config) {
        String userId = config.getUserId();
        if (SwrveHelper.isNullOrEmpty(userId)) {
            String savedUserIdFromPrefs = getSavedUserIdFromPrefs();
            if (SwrveHelper.isNullOrEmpty(savedUserIdFromPrefs)) {
                userId = UUID.randomUUID().toString(); // Create a random UUID
            } else {
                userId = savedUserIdFromPrefs;
            }
        }
        saveUserIdToPrefs(userId);
        SwrveLogger.i("Your user id is: %s", userId);
        return userId;
    }

    protected String getUserId() {
        return userId;
    }

    protected boolean isLoggedIn() {
        return userId != null;
    }

    private void saveUserIdToPrefs(String userId) {
        SharedPreferences settings = context.getSharedPreferences(SDK_PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit(); // Save new user id
        editor.putString("userId", userId).commit();
    }

    private String getSavedUserIdFromPrefs() {
        SharedPreferences settings = context.getSharedPreferences(SDK_PREFS_NAME, 0);
        String userId = settings.getString("userId", null);
        return userId;
    }

    protected String getSessionToken() {
        return sessionToken;
    }
}
