package com.swrve.sdk;

import android.app.NotificationChannel;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

interface ISwrveCommon {

    // cache constants
    String CACHE_DEVICE_ID = "device_id";
    String CACHE_CAMPAIGNS = "CMCC2";
    String CACHE_RESOURCES = "srcngt2";
    String CACHE_REALTIME_USER_PROPERTIES = "cmrp2s";
    String CACHE_RESOURCES_DIFF = "rsdfngt2";
    String CACHE_AD_CAMPAIGNS_DEBUG = "AdCampaign";
    String CACHE_NOTIFICATION_CAMPAIGNS_DEBUG = "NotificationCampaign";
    String CACHE_USER_JOINED_TIME = "SwrveSDK.userJoinedTime";
    String CACHE_CAMPAIGNS_STATE = "SwrveCampaignSettings";
    String CACHE_SEQNUM = "seqnum";
    String CACHE_QA = "swrve.q1";
    String CACHE_ETAG = "swrve.etag";

    // Blank key for saving device properties that are userId agnostic.
    String CACHE_DEVICE_PROP_KEY = "";

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
    String SWRVE_CAN_RECEIVE_AUTH_PUSH      = "swrve.can_receive_authenticated_push";
    String SWRVE_INIT_MODE                  = "swrve.sdk_init_mode";
    String SWRVE_DEVICE_TYPE                = "swrve.device_type";

    // batch event
    String BATCH_EVENT_KEY_USER                     = "user";
    String BATCH_EVENT_KEY_SESSION_TOKEN            = "session_token";
    String BATCH_EVENT_KEY_VERSION                  = "version";
    String BATCH_EVENT_KEY_APP_VERSION              = "app_version";
    String BATCH_EVENT_KEY_UNIQUE_DEVICE_ID         = "unique_device_id";
    String BATCH_EVENT_KEY_DATA                     = "data";

    // events
    String EVENT_ID_KEY                             = "id";
    String EVENT_TYPE_KEY                           = "type";
    String EVENT_PAYLOAD_KEY                        = "payload";
    String EVENT_TYPE_GENERIC_CAMPAIGN              = "generic_campaign_event";
    String GENERIC_EVENT_CAMPAIGN_TYPE_KEY          = "campaignType";
    String GENERIC_EVENT_CAMPAIGN_TYPE_GEO          = "geo";
    String GENERIC_EVENT_CAMPAIGN_TYPE_PUSH         = "push";
    String GENERIC_EVENT_ACTION_TYPE_KEY            = "actionType";
    String GENERIC_EVENT_ACTION_TYPE_IMPRESSION     = "impression";
    String GENERIC_EVENT_ACTION_TYPE_DELIVERED      = "delivered";
    String GENERIC_EVENT_ACTION_TYPE_ENGAGED        = "engaged";
    String GENERIC_EVENT_ACTION_TYPE_BUTTON_CLICK   = "button_click";
    String GENERIC_EVENT_ACTION_TYPE_INFLUENCED     = "influenced";
    String GENERIC_EVENT_CONTEXT_ID_KEY             = "contextId";
    String GENERIC_EVENT_CAMPAIGN_ID_KEY            = "campaignId";
    String GENERIC_EVENT_PAYLOAD_BUTTON_TEXT        = "buttonText";
    String GENERIC_EVENT_PAYLOAD_RUN_NUMBER         = "runNumber";
    String EVENT_FIRST_SESSION                      = "Swrve.first_session";

    // platform information
    String OS_ANDROID                         = "android";
    String OS_ANDROID_TV                      = "android-tv";
    String OS_AMAZON                          = "amazon-android";
    String OS_AMAZON_TV                       = "amazon-android-tv";
    String OS_HUAWEI                          = "huawei-android";
    String DEVICE_TYPE_MOBILE                 = "mobile";
    String DEVICE_TYPE_TV                     = "tv";

    enum SupportedUIMode {
        MOBILE,
        TV
    }

    String getApiKey();

    String getSessionKey();

    String getDeviceId();

    int getAppId();

    String getUserId();

    String getAppVersion();

    String getUniqueKey(String userId);

    String getBatchURL();

    String getContentURL();

    String getCachedData(String userId, String key);

    String getSwrveSDKVersion();

    void userUpdate(Map<String, String> attributes);

    void sendEventsInBackground(Context context, String userId, ArrayList<String> events);

    String getEventsServer();

    int getHttpTimeout();

    JSONObject getDeviceInfo() throws JSONException;

    int getNextSequenceNumber();

    NotificationChannel getDefaultNotificationChannel();

    SwrveNotificationConfig getNotificationConfig();

    SwrvePushNotificationListener getNotificationListener();

    SwrveSilentPushListener getSilentPushListener();

    String getJoined();

    String getLanguage();

    void setNotificationSwrveCampaignId(String swrveCampaignId);

    void saveNotificationAuthenticated(int notificationId);

    int getFlushRefreshDelay();

    void setSessionListener(SwrveSessionListener sessionListener);

    void fetchNotificationCampaigns(Set<Long> campaignIds);

    File getCacheDir(Context context);

    void saveEvent(String event);
}
