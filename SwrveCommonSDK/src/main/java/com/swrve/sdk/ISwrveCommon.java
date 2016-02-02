package com.swrve.sdk;

import java.net.URL;
import java.util.Map;

public interface ISwrveCommon
{
    String getApiKey();

    int getAppId();

    String getUserId();

    String getAppVersion();

    String getUniqueKey();

    String getBatchEventsAction();

    String getCachedLocationData();

    boolean isDebug();

    void setLocationVersion(int locationVersion);

    void userUpdate(Map<String, String> attributes);

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
