package com.swrve.sdk;

import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.Display;
import android.view.WindowManager;

import com.swrve.sdk.ISwrveCommon.SupportedUIMode;

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
import java.text.SimpleDateFormat;
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

import static android.content.Context.UI_MODE_SERVICE;
import static com.swrve.sdk.ISwrveCommon.DEVICE_TYPE_MOBILE;
import static com.swrve.sdk.ISwrveCommon.DEVICE_TYPE_TV;
import static com.swrve.sdk.ISwrveCommon.OS_AMAZON;
import static com.swrve.sdk.ISwrveCommon.OS_AMAZON_TV;
import static com.swrve.sdk.ISwrveCommon.OS_ANDROID;
import static com.swrve.sdk.ISwrveCommon.OS_ANDROID_TV;
import static com.swrve.sdk.ISwrveCommon.OS_HUAWEI;
import static com.swrve.sdk.SwrveFlavour.AMAZON;
import static com.swrve.sdk.SwrveFlavour.HUAWEI;

/**
 * Used internally to provide MD5, token generation and other helper methods.
 */
public final class SwrveHelper {

    private static final String CHARSET = "UTF-8";

    protected static String buildModel = Build.MODEL;

    private static final SimpleDateFormat appInstallTimeFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private static String appInstallTime;

    public static boolean isNullOrEmpty(String val) {
        return (val == null || val.length() == 0);
    }

    public static boolean isNotNullOrEmpty(String val) {
        return !isNullOrEmpty(val);
    }

    public static boolean isNullOrEmpty(Map<String, String> val) {
        return (val == null || val.isEmpty());
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
                for (byte b : hash) {
                    if ((0xFF & b) < 0x10) {
                        hexDigest.append("0");
                    }
                    hexDigest.append(Integer.toHexString(0xFF & b));
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
                for (byte b : hash) {
                    if ((0xFF & b) < 0x10) {
                        hexDigest.append("0");
                    }
                    hexDigest.append(Integer.toHexString(0xFF & b));
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
        Map<String, String> map = new HashMap<>();
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
            body.append(URLEncoder.encode(pair.getKey(), CHARSET) + "="
                    + ((pair.getValue() == null) ? "null" : URLEncoder.encode(pair.getValue(), CHARSET)));
        }
        return body.toString();
    }

    public static int getDisplayWidth(final Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            SwrveLogger.i("Current device does not have a Window Service active");
            return 0;
        }
        Display display = windowManager.getDefaultDisplay();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return display.getWidth();
        } else {
            Point size = new Point();
            display.getSize(size);
            return size.x;
        }
    }

    public static int getDisplayHeight(final Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            SwrveLogger.i("Current device does not have a Window Service active");
            return 0;
        }

        Display display = windowManager.getDefaultDisplay();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return display.getHeight();
        } else {
            Point size = new Point();
            display.getSize(size);
            return size.y;
        }
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

    public static String createHMACWithMD5(String source, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {
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
        // Returns true if current SDK is higher or equal than 4.X (API 16)
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Ignore the Calypso
            if (modelBlackList == null || !modelBlackList.contains(buildModel)) {
                return true;
            } else {
                SwrveLogger.i("Current device is part of the model blacklist");
            }
        } else {
            SwrveLogger.i("SDK not initialised as it is under API 16");
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
            ApplicationInfo applicationInfo = pm.getApplicationInfo(context.getPackageName(), 0);
            if (applicationInfo != null) {
                targetSdkVersion = applicationInfo.targetSdkVersion;
            }
        } catch (Exception ex) {
            SwrveLogger.e("", ex);
        }
        return targetSdkVersion;
    }

    public static String getPlatformDeviceType(Context context) {
        switch (SwrveHelper.getSupportedUIMode(context)) {
            case TV:
                return DEVICE_TYPE_TV;
            case MOBILE:
            default:
                return DEVICE_TYPE_MOBILE;
        }
    }

    // Used by Unity / Core Flavour
    public static String getPlatformOS(Context context) {
        return SwrveHelper.getPlatformOS(context, null);
    }

    public static String getPlatformOS(Context context, SwrveFlavour sdkFlavour) {
        if (sdkFlavour == null) {
            if (SwrveHelper.isNullOrEmpty(Build.MANUFACTURER)) {
                // if MANUFACTURER is empty or null, default to 'android'
                return OS_ANDROID;
            }
            if ((android.os.Build.MANUFACTURER.equalsIgnoreCase("amazon"))) {
                switch (SwrveHelper.getSupportedUIMode(context)) {
                    case TV:
                        return OS_AMAZON_TV;
                    case MOBILE:
                    default:
                        return OS_AMAZON;
                }
            } else {
                switch (SwrveHelper.getSupportedUIMode(context)) {
                    case TV:
                        return OS_ANDROID_TV;
                    case MOBILE:
                    default:
                        return OS_ANDROID;
                }
            }
        }
        // We can infer the OS based flavour
        if (sdkFlavour == AMAZON) {
            switch (SwrveHelper.getSupportedUIMode(context)) {
                case TV:
                    return OS_AMAZON_TV;
                case MOBILE:
                default:
                    return OS_AMAZON;
            }
        } else if (sdkFlavour == HUAWEI) {
            return OS_HUAWEI;
        } else {
            switch (SwrveHelper.getSupportedUIMode(context)) {
                case TV:
                    return OS_ANDROID_TV;
                case MOBILE:
                default:
                    return OS_ANDROID;
            }
        }
    }

    public static SupportedUIMode getSupportedUIMode(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            return SupportedUIMode.TV;
        }

        return SupportedUIMode.MOBILE;
    }

    // Used by Unity
    public static boolean isSwrvePush(final Bundle msg) {
        String pushId = SwrveHelper.getRemotePushId(msg);
        if (SwrveHelper.isNullOrEmpty(pushId)) {
            pushId = SwrveHelper.getSilentPushId(msg);
        }
        return SwrveHelper.isNotNullOrEmpty(pushId);
    }

    // Obtains the normal push or silent push tracking id
    public static String getRemotePushId(final Bundle msg) {
        String pushId = null;
        if (msg == null) {
            return pushId;
        }
        Object rawId = msg.get(SwrveNotificationConstants.SWRVE_TRACKING_KEY);
        pushId = (rawId != null) ? rawId.toString() : null;
        return pushId;
    }

    // Obtains the silent push tracking id
    // Used by Unity
    public static String getSilentPushId(final Bundle msg) {
        if (msg == null) {
            return null;
        }
        Object rawId = msg.get(SwrveNotificationConstants.SWRVE_SILENT_TRACKING_KEY);
        return (rawId != null) ? rawId.toString() : null;
    }

    public static Map<String, String> getBundleAsMap(Bundle propertyBundle) {
        Map<String, String> map = new HashMap<>();
        if (propertyBundle != null) {
            for (String key : propertyBundle.keySet()) {
                map.put(key, propertyBundle.getString(key));
            }
        }
        return map;
    }

    public static String getAppInstallTime(Context context) {
        if (appInstallTime == null) {
            long appInstallTimeMillis = System.currentTimeMillis();
            try {
                appInstallTimeMillis = context.getPackageManager().getPackageInfo(context.getPackageName(),
                        0).firstInstallTime;
            } catch (PackageManager.NameNotFoundException e) {
                SwrveLogger.e("Could not get package info to retrieve appInstallTime so using current date.", e);
            }
            appInstallTime = appInstallTimeFormat.format(new Date(appInstallTimeMillis));
        }
        return appInstallTime;
    }

    // Convert Bundle root keys to JSONObject and promote the _s.JsonPayload ones to
    // root level
    public static JSONObject convertPayloadToJSONObject(Bundle bundle) {

        JSONObject payload = new JSONObject();

        // the "_s.JsonPayload" is only included if the payload has json more than one
        // level deep. So
        // promote its contents to root level if its there. (Not ideal)
        if (bundle.containsKey(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY)) {
            try {
                payload = new JSONObject(bundle.getString(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY));
            } catch (Exception ex) {
                SwrveLogger.e("SwrveNotificationEngageReceiver. Could not parse deep Json", ex);
            }
        }

        // One level deep payload is mixed with swrve keys so include all keys. (Again,
        // not ideal).
        // Exclude the "_s.JsonPayload" key as already added from above.
        for (String key : bundle.keySet()) {
            if (!key.equals(SwrveNotificationConstants.SWRVE_NESTED_JSON_PAYLOAD_KEY)) {
                try {
                    payload.put(key, bundle.get(key));
                } catch (Exception e) {
                    SwrveLogger.e("SwrveNotificationEngageReceiver. Could not add key to payload %s", key, e);
                }
            }
        }

        return payload;
    }


    // Simple function to combine two maps, this will be used primarily around the Real Time User Property Personalization Logic
    // Utilizes Hashmap conversion to achieve this. baseMap will be the initial map added, then overriding will override any duplicates when
    // putAll is called.
    public static Map<String, String> combineTwoStringMaps(Map<String, String> baseMap, Map<String, String> overridingMap) {

        if (isNullOrEmpty(baseMap) && isNullOrEmpty(overridingMap)) {
            return null;
        }

        HashMap<String, String> result = new HashMap<>();
        if (baseMap != null) {
            result.putAll(baseMap);
        }
        if (overridingMap != null) {
            result.putAll(overridingMap);
        }

        return result;
    }
}
