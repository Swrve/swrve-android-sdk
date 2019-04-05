package com.swrve.sdk;

import android.os.Bundle;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveDismissButtonListener;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessageListener;
import com.swrve.sdk.messaging.SwrveOrientation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface ISwrveBase<T, C extends SwrveConfigBase> {

    void sessionStart();

    void sessionEnd();

    void event(String name);

    void event(String name, Map<String, String> payload);

    void purchase(String item, String currency, int cost, int quantity);

    void currencyGiven(String givenCurrency, double givenAmount);

    void userUpdate(Map<String, String> attributes);

    void userUpdate(String name, Date date);

    void iap(int quantity, String productId, double productPrice, String currency);

    void iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards);

    SwrveResourceManager getResourceManager();

    void setResourcesListener(SwrveResourcesListener resourcesListener);

    void getUserResources(final SwrveUserResourcesListener listener);

    void getUserResourcesDiff(final SwrveUserResourcesDiffListener listener);

    void sendQueuedEvents();

    void flushToDisk();

    void shutdown();

    void setLanguage(Locale locale);

    String getLanguage();

    String getApiKey();

    String getUserId();

    JSONObject getDeviceInfo() throws JSONException;

    void refreshCampaignsAndResources();

    void buttonWasPressedByUser(SwrveButton button);

    void messageWasShownToUser(SwrveMessageFormat messageFormat);

    String getAppStoreURLForApp(int appId);

    File getCacheDir();

    void setMessageListener(SwrveMessageListener messageListener);

    Date getInitialisedTime();

    SwrveInstallButtonListener getInstallButtonListener();

    void setInstallButtonListener(SwrveInstallButtonListener installButtonListener);

    SwrveCustomButtonListener getCustomButtonListener();

    void setCustomButtonListener(SwrveCustomButtonListener customButtonListener);

    SwrveDismissButtonListener getDismissButtonListener();

    void setDismissButtonListener(SwrveDismissButtonListener inAppDismissButtonListener);

    C getConfig();

    List<SwrveBaseCampaign> getMessageCenterCampaigns();

    List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation);

    boolean showMessageCenterCampaign(SwrveBaseCampaign campaign);

    void removeMessageCenterCampaign(SwrveBaseCampaign campaign);

    void handleDeferredDeeplink(Bundle bundle);

    void handleDeeplink(Bundle bundle);

    void identify(final String userId, final SwrveIdentityResponse identityResponse);

    String getExternalUserId();

    void setCustomPayloadForConversationInput(Map payload);

}
