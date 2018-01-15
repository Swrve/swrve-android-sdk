package com.swrve.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Used internally to provide MD5, token generation and other helper methods.
 */
public final class SwrveHelper {

    private static final String CHARSET = "UTF-8";

    protected static String buildModel = Build.MODEL;

    public static boolean isNullOrEmpty(String val) {
        return (val == null || val.length() == 0);
    }

    public static boolean isNotNullOrEmpty(String val) {
        return !isNullOrEmpty(val);
    }

    public static String generateSessionToken(String apiKey, int appId, String userId) {
        String timestamp = Long.toString((new Date().getTime()) / 1000);

        String hexDigest = md5(userId + timestamp + apiKey);
        return appId + "=" + userId + "=" + timestamp + "=" + hexDigest;
    }

    public static String toLanguageTag(Locale locale) {
        StringBuilder languageTag = new StringBuilder();
        languageTag.append(locale.getLanguage());
        if (!isNullOrEmpty(locale.getCountry())) {
            languageTag.append('-').append(locale.getCountry());
        }
        if (!isNullOrEmpty(locale.getVariant())) {
            languageTag.append('-').append(locale.getVariant());
        }
        return languageTag.toString();
    }

    public static String md5(String text) {
        if (text == null) {
            return null;
        } else {
            try {
                byte[] bytesOfMessage = text.getBytes();
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] hash = md5.digest(bytesOfMessage);

                StringBuilder hexDigest = new StringBuilder();
                for (int i = 0; i < hash.length; i++) {
                    if ((0xFF & hash[i]) < 0x10) {
                        hexDigest.append("0");
                    }
                    hexDigest.append(Integer.toHexString(0xFF & hash[i]));
                }
                return hexDigest.toString();
            } catch (NoSuchAlgorithmException nsae) {
                SwrveLogger.wtf("Couldn't find MD5 - what a strange JVM", nsae);
                return "";
            }
        }
    }

    public static String sha1(byte[] bytesOfMessage) {
        if (bytesOfMessage.length == 0) {
            return null;
        } else {
            try {
                MessageDigest sha1 = MessageDigest.getInstance("SHA1");
                byte[] hash = sha1.digest(bytesOfMessage);

                StringBuilder hexDigest = new StringBuilder();
                for (int i = 0; i < hash.length; i++) {
                    if ((0xFF & hash[i]) < 0x10) {
                        hexDigest.append("0");
                    }
                    hexDigest.append(Integer.toHexString(0xFF & hash[i]));
                }
                return hexDigest.toString();
            } catch (NoSuchAlgorithmException nsae) {
                SwrveLogger.wtf("Couldn't find SHA1 - what a strange JVM", nsae);
                return "";
            }
        }
    }

    /*
     * Convert from JSONObject to Map.
     */
    public static Map<String, String> JSONToMap(JSONObject obj) throws JSONException {
        Map<String, String> map = new HashMap<String, String>();
        @SuppressWarnings("unchecked")
        Iterator<String> it = obj.keys();
        while (it.hasNext()) {
            String key = it.next();
            map.put(key, obj.getString(key));
        }
        return map;
    }

    /*
     * Encode parameters for a GET request.
     */
    public static String encodeParameters(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder body = new StringBuilder();
        Iterator<Entry<String, String>> it = params.entrySet().iterator();
        boolean firstElement = true;
        while (it.hasNext()) {
            Map.Entry<String, String> pair = it.next();
            if (firstElement) {
                firstElement = false;
            } else {
                body.append("&");
            }
            body.append(URLEncoder.encode(pair.getKey(), CHARSET) + "=" + ((pair.getValue() == null) ? "null" : URLEncoder.encode(pair.getValue(), CHARSET)));
        }
        return body.toString();
    }

    public static boolean userErrorResponseCode(int responseCode) {
        return (responseCode >= 400 && responseCode < 500);
    }

    public static boolean successResponseCode(int responseCode) {
        return (responseCode >= 200 && responseCode < 300);
    }

    public static boolean serverErrorResponseCode(int responseCode) {
        return (responseCode >= 500);
    }

    public static void logAndThrowException(String reason) throws IllegalArgumentException {
        SwrveLogger.e(reason);
        throw new IllegalArgumentException(reason);
    }

    public static Date addTimeInterval(Date origin, int value, int unit) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(origin);
        cal.add(unit, value);
        return cal.getTime();
    }

    public static String readStringFromInputStream(InputStream is) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        StringBuilder body = new StringBuilder();
        String line;

        while ((line = rd.readLine()) != null) {
            body.append(line);
        }

        return body.toString();
    }

    public static String createHMACWithMD5(String source, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = Mac.getInstance("HmacMD5");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), hmac.getAlgorithm());
        hmac.init(secretKey);

        byte[] signature = hmac.doFinal(source.getBytes());
        return Base64.encodeToString(signature, Base64.DEFAULT);
    }

    public static boolean sdkAvailable() {
        return sdkAvailable(null);
    }

    public static boolean sdkAvailable(List<String> modelBlackList) {
        // Returns true if current SDK is higher or equal than 4.X (API 14)
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // Ignore the Calypso
            if (modelBlackList == null || !modelBlackList.contains(buildModel)) {
                return true;
            } else {
                SwrveLogger.i("Current device is part of the model blacklist");
            }
        } else {
            SwrveLogger.i("SDK not initialised as it is under API 14");
        }
        return false;
    }

    public static boolean hasFileAccess(String filePath) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }
        File file = new File(filePath);
        return file.canRead();
    }

    public static int getTargetSdkVersion(Context context) {
        int targetSdkVersion = 0;
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo("com.swrve.sdk.devapp.amazon", 0);
            if (applicationInfo != null) {
                targetSdkVersion = applicationInfo.targetSdkVersion;
            }
        } catch (Exception ex) {
            SwrveLogger.e("", ex);
        }
        return targetSdkVersion;
    }
}
