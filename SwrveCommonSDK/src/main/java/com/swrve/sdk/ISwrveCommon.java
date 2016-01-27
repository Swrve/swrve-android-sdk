package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.localstorage.ILocalStorage;

import java.util.Map;

public interface ISwrveCommon
{
    String getApiKey();

    int getAppId();

    String getUserId();

    String getAppVersion();

    String getUniqueKey();

    SwrveConfigBase getConfig();

    String getBatchEventsAction();

    String getLocationCampaignCategory();

    ILocalStorage createLocalStorage();

    boolean isDebug();

    void setLocationVersion(int locationVersion);

    void userUpdate(Map<String, String> attributes);
}
