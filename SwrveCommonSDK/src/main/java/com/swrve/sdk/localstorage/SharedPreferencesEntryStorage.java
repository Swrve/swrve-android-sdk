package com.swrve.sdk.localstorage;

import android.content.SharedPreferences;
import android.util.Log;

import com.swrve.sdk.SwrveHelper;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Used internally to provide a volatile storage of data that may be saved later on the device.
 */
public class SharedPreferencesEntryStorage implements IEntryStorage {
    private static final String SIGNATURE_SUFFIX = "_SGT";

    private final String userId;
    private final String uniqueKey;
    private SharedPreferences prefs;
    // Global user data
    private Map<String, String> globalStringCache = new HashMap<String, String>();
    // Local user data
    private Map<String, String> userStringCache = new HashMap<String, String>();
    private Map<String, Integer> userIntegerCache = new HashMap<String, Integer>();

    public SharedPreferencesEntryStorage(String userId, String uniqueKey, SharedPreferences prefs) {
        this.userId = userId;
        this.uniqueKey = uniqueKey;
        this.prefs = prefs;
    }

    private String getUserKey(String key) {
        return userId + ":" + key;
    }

    @Override
    public void putGlobalString(String key, String value) {
        boolean writeToPrefs = true;
        if (globalStringCache.containsKey(key)) {
            writeToPrefs = (globalStringCache.get(key) != value);
        } else {
            globalStringCache.put(key, value);
        }
        if (writeToPrefs) {
            prefs.edit().putString(key, value).commit();
        }
    }

    @Override
    public String getGlobalString(String key, String defaultValue) {
        if (globalStringCache.containsKey(key)) {
            return globalStringCache.get(key);
        }

        return prefs.getString(key, defaultValue);
    }

    @Override
    public void putUserInt(String key, int value) {
        String userKey = getUserKey(key);
        boolean writeToPrefs = true;
        if (userIntegerCache.containsKey(userKey)) {
            writeToPrefs = (userIntegerCache.get(userKey) != value);
        } else {
            userIntegerCache.put(userKey, value);
        }
        if (writeToPrefs) {
            prefs.edit().putInt(userKey, value).commit();
        }
    }

    @Override
    public int getUserInt(String key, int defaultValue) {
        String userKey = getUserKey(key);
        if (userIntegerCache.containsKey(userKey)) {
            return userIntegerCache.get(userKey);
        }

        return prefs.getInt(userKey, defaultValue);
    }

    @Override
    public void putUserString(String key, String value) {
        String userKey = getUserKey(key);
        boolean writeToPrefs = true;
        if (userStringCache.containsKey(userKey)) {
            writeToPrefs = (userStringCache.get(userKey) != value);
        } else {
            userStringCache.put(userKey, value);
        }
        if (writeToPrefs) {
            prefs.edit().putString(userKey, value).commit();
        }
    }

    @Override
    public String getUserString(String key, String defaultValue) {
        String userKey = getUserKey(key);
        if (userStringCache.containsKey(userKey)) {
            return userStringCache.get(userKey);
        }

        return prefs.getString(userKey, defaultValue);
    }

    @Override
    public void removeUserString(String key) {
        String userKey = getUserKey(key);
        userStringCache.remove(userKey);
        prefs.edit().remove(userKey).commit();
    }

    @Override
    public void putUserSecureString(String key, String value) {
        String userKey = getUserKey(key);
        String signatureUserKey = getUserKey(key + SIGNATURE_SUFFIX);
        try {
            String signature = SwrveHelper.createHMACWithMD5(value, uniqueKey);
            boolean writeToPrefs = true;
            if (userStringCache.containsKey(userKey)) {
                writeToPrefs = (userStringCache.get(userKey) != value);
            } else {
                userStringCache.put(userKey, value);
                userStringCache.put(signatureUserKey, signature);
            }
            if (writeToPrefs) {
                prefs.edit().putString(userKey, value).putString(signatureUserKey, signature).commit();
            }
        } catch (NoSuchAlgorithmException e) {
            Log.i("SwrveSDK", "Computing signature failed because of invalid algorithm");
            putUserString(key, value);
        } catch (InvalidKeyException e) {
            Log.i("SwrveSDK", "Computing signature failed because of an invalid key");
        }
    }

    @Override
    public String getUserSecureString(String key, String defaultValue) throws SecurityException {
        String userKey = getUserKey(key);
        String signatureUserKey = getUserKey(key + SIGNATURE_SUFFIX);

        String savedContent = userStringCache.get(userKey);
        String savedSignature = userStringCache.get(signatureUserKey);
        if (SwrveHelper.isNullOrEmpty(savedContent) || SwrveHelper.isNullOrEmpty(savedSignature)) {
            savedContent = prefs.getString(userKey, defaultValue);
            savedSignature = prefs.getString(signatureUserKey, null);
        }

        // Check content with signature
        if (!SwrveHelper.isNullOrEmpty(savedContent)) {
            try {
                String computedSignature = SwrveHelper.createHMACWithMD5(savedContent, uniqueKey);
                if (SwrveHelper.isNullOrEmpty(computedSignature) || SwrveHelper.isNullOrEmpty(savedSignature) || !savedSignature.equals(computedSignature)) {
                    throw new SecurityException("Signature validation failed");
                }
            } catch (NoSuchAlgorithmException e) {
                Log.i("SwrveSDK", "Computing signature failed because of invalid algorithm");
            } catch (InvalidKeyException e) {
                Log.i("SwrveSDK", "Computing signature failed because of an invalid key");
            }
        }

        return savedContent;
    }

}