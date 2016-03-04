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

    boolean isDebug();

    String getCachedLocationData();

    void setLocationVersion(int locationVersion);

    void userUpdate(Map<String, String> attributes);

    ISwrveConversationsSDK getConversationSDK();

    void sendEventsWakefully(Context context, ArrayList<String> events);

    String getEventsServer();

    int getHttpTimeout();

    int getMaxEventsPerFlush();
}
