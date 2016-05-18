package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.ISwrveCustomButtonListener;
import com.swrve.sdk.messaging.ISwrveDialogListener;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.ISwrveInstallButtonListener;
import com.swrve.sdk.messaging.ISwrveMessageListener;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface ISwrveBase<T, C extends SwrveConfigBase> {

    T onCreate(Activity activity) throws IllegalArgumentException;

    void sessionStart();

    void sessionEnd();

    void event(String name);

    void event(String name, Map<String, String> payload);

    void purchase(String item, String currency, int cost, int quantity);

    void currencyGiven(String givenCurrency, double givenAmount);

    void userUpdate(Map<String, String> attributes);

    void iap(int quantity, String productId, double productPrice, String currency);

    void iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards);

    SwrveResourceManager getResourceManager();

    void setResourcesListener(ISwrveResourcesListener resourcesListener);

    void getUserResources(final ISwrveUserResourcesListener listener);

    void getUserResourcesDiff(final ISwrveUserResourcesDiffListener listener);

    void sendQueuedEvents();

    void flushToDisk();

    void onPause();

    void onResume(Activity activity);

    void onLowMemory();

    void onNewIntent(Intent intent);

    void onDestroy(Activity activity);

    void shutdown();

    void setLanguage(Locale locale);

    String getLanguage();

    @Deprecated
    void setLanguage(String language);

    String getApiKey();

    String getUserId();

    JSONObject getDeviceInfo() throws JSONException;

    void refreshCampaignsAndResources();

    @Deprecated
    SwrveMessage getMessageForEvent(String event);

    @Deprecated
    SwrveMessage getMessageForId(int messageId);

    void buttonWasPressedByUser(SwrveButton button);

    void messageWasShownToUser(SwrveMessageFormat messageFormat);

    String getAppStoreURLForApp(int appId);

    File getCacheDir();

    void setMessageListener(ISwrveMessageListener messageListener);

    Date getInitialisedTime();

    ISwrveInstallButtonListener getInstallButtonListener();

    void setInstallButtonListener(ISwrveInstallButtonListener installButtonListener);

    ISwrveCustomButtonListener getCustomButtonListener();

    void setCustomButtonListener(ISwrveCustomButtonListener customButtonListener);

    ISwrveDialogListener getDialogListener();

    void setDialogListener(ISwrveDialogListener dialogListener);

    Context getContext();

    C getConfig();

    List<SwrveBaseCampaign> getMessageCenterCampaigns();

    List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation);

    boolean showMessageCenterCampaign(SwrveBaseCampaign campaign);

    void removeMessageCenterCampaign(SwrveBaseCampaign campaign);
}
