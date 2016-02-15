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

    String getBatchEventsAction();

    String getCachedLocationData();

    boolean isDebug();

    void setLocationVersion(int locationVersion);

    void userUpdate(Map<String, String> attributes);

    void sendEventsWakefully(Context context, ArrayList<String> events);

    /***
     * Config area
     */

    URL getEventsUrl();

    String getDbName();

    long getMaxSqliteDbSize();

    int getHttpTimeout();

    int getMaxEventsPerFlush();

    /***
     * eo Config
     */
}
