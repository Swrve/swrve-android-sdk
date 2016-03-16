package com.swrve.sdk;

import android.content.Context;

import java.util.ArrayList;
import java.util.Map;

interface ISwrveCommon
{
    String getApiKey();

    String getSessionKey();

    short getDeviceId();

    int getAppId();

    String getUserId();

    String getAppVersion();

    String getUniqueKey();

    String getBatchURL();

    String getCachedLocationData();

    void setLocationVersion(int locationVersion);

    void userUpdate(Map<String, String> attributes);

    void sendEventsWakefully(Context context, ArrayList<String> events);

    String getEventsServer();

    int getHttpTimeout();

    int getMaxEventsPerFlush();
}
