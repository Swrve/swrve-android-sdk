package com.swrve.sdk;

import android.content.Context;

import java.util.ArrayList;
import java.util.Map;

interface ISwrveCommon
{
    String LOCATION_CAMPAIGN_CATEGORY = "LocationCampaign";
    String SWRVE_GOOGLE_ADVERTISING_ID_CATEGORY = "GoogleAdvertisingId";
    String SWRVE_GOOGLE_ADVERTISING_LIMIT_AD_TRACKING_CATEGORY = "GoogleAdvertisingLimitAdTrackingEnabled";

    String getApiKey();

    String getSessionKey();

    short getDeviceId();

    int getAppId();

    String getUserId();

    String getAppVersion();

    String getUniqueKey();

    String getBatchURL();

    String getCachedData(String userId, String key);

    void setLocationSegmentVersion(int locationSegmentVersion);

    void setLocationSDKVersion(String locationVersion);

    String getSwrveSDKVersion();

    void userUpdate(Map<String, String> attributes);

    void sendEventsWakefully(Context context, ArrayList<String> events);

    String getEventsServer();

    int getHttpTimeout();

    int getMaxEventsPerFlush();
}
