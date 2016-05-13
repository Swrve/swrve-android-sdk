package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveMessageFormat;

import java.io.File;
import java.util.Date;
import java.util.Set;

/**
 * Used internally to provide common campaign functionality between between different classes.
 */
public interface ISwrveCampaignManager {
    Date getNow(); // for testing

    Date getInitialisedTime();

    File getCacheDir();

    Set<String> getAssetsOnDisk();

    SwrveConfigBase getConfig();

    String getAppStoreURLForApp(int appId);

    void buttonWasPressedByUser(SwrveButton button);

    void messageWasShownToUser(SwrveMessageFormat messageFormat);

}
