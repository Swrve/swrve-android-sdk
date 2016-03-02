package com.swrve.sdk;

import android.content.Context;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

public interface ISwrveCommon
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

    boolean isDebug();

    void setLocationVersion(int locationVersion);

    void userUpdate(Map<String, String> attributes);

    void sendEventWakefully(Context context, String event);

    void sendEventsWakefully(Context context, ArrayList<String> events);

    /***
     * Config area
     */

    String getEventsServer();

    int getHttpTimeout();

    int getMaxEventsPerFlush();

    /***
     * eo Config
     */
}
