package com.swrve.sdk;

import android.app.NotificationChannel;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

interface ISwrveCommon {

    // cache constants
    String CACHE_DEVICE_ID = "device_id";
    String CACHE_CAMPAIGNS = "CMCC2";
    String CACHE_RESOURCES = "srcngt2";
    String CACHE_RESOURCES_DIFF = "rsdfngt2";
    String CACHE_LOCATION_CAMPAIGNS = "LocationCampaign";
    String CACHE_APP_VERSION = "AppVersion";
    String CACHE_INSTALL_TIME = "SwrveSDK.installTime";
    String CACHE_CAMPAIGNS_STATE = "SwrveCampaignSettings";
    String CACHE_SEQNUM = "seqnum";
    String CACHE_GOOGLE_ADVERTISING_ID = "GoogleAdvertisingId";
    String CACHE_GOOGLE_ADVERTISING_AD_TRACK_LIMIT = "GoogleAdvertisingLimitAdTrackingEnabled";
    String CACHE_REGISTRATION_ID = "RegistrationId";

    // device info
    String SWRVE_DEVICE_NAME                = "swrve.device_name";
    String SWRVE_OS                         = "swrve.os";
    String SWRVE_OS_VERSION                 = "swrve.os_version";
    String SWRVE_DEVICE_WIDTH               = "swrve.device_width";
    String SWRVE_DEVICE_HEIGHT              = "swrve.device_height";
    String SWRVE_DEVICE_DPI                 = "swrve.device_dpi";
    String SWRVE_CONVERSATION_VERSION       = "swrve.conversation_version";
    String SWRVE_ANDROID_DEVICE_XDPI        = "swrve.android_device_xdpi";
    String SWRVE_ANDROID_DEVICE_YDPI        = "swrve.android_device_ydpi";
    String SWRVE_LANGUAGE                   = "swrve.language";
    String SWRVE_UTC_OFFSET_SECONDS         = "swrve.utc_offset_seconds";
    String SWRVE_TIMEZONE_NAME              = "swrve.timezone_name";
    String SWRVE_SDK_VERSION                = "swrve.sdk_version";
    String SWRVE_SDK_FLAVOUR                = "swrve.sdk_flavour";
    String SWRVE_APP_STORE                  = "swrve.app_store";
    String SWRVE_INSTALL_DATE               = "swrve.install_date";
    String SWRVE_SIM_OPERATOR_NAME          = "swrve.sim_operator.name";
    String SWRVE_SIM_OPERATOR_ISO_COUNTRY   = "swrve.sim_operator.iso_country_code";
    String SWRVE_SIM_OPERATOR_CODE          = "swrve.sim_operator.code";
    String SWRVE_DEVICE_REGION              = "swrve.device_region";
    String SWRVE_ANDROID_ID                 = "swrve.android_id";
    String SWRVE_NOTIFICATIONS_ENABLED      = "swrve.permission.notifications_enabled";
    String SWRVE_NOTIFICATIONS_IMPORTANCE   = "swrve.permission.notifications_importance";
    String SWRVE_NOTIFICATIONS_BUTTONS      = "swrve.support.rich_buttons";
    String SWRVE_NOTIFICATIONS_ATTACHMENT   = "swrve.support.rich_attachment";

    String getApiKey();

    String getSessionKey();

    short getDeviceId();

    int getAppId();

    String getUserId();

    String getAppVersion();

    String getUniqueKey(String userId);

    String getBatchURL();

    String getCachedData(String userId, String key);

    void setLocationSegmentVersion(int locationSegmentVersion);

    String getSwrveSDKVersion();

    void userUpdate(Map<String, String> attributes);

    void sendEventsInBackground(Context context, String userId, ArrayList<String> events);

    String getEventsServer();

    int getHttpTimeout();

    int getMaxEventsPerFlush();

    JSONObject getDeviceInfo() throws JSONException;

    int getNextSequenceNumber();

    NotificationChannel getDefaultNotificationChannel();
}
