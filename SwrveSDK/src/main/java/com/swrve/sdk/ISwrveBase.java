package com.swrve.sdk;

import android.app.Activity;
import android.os.Bundle;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
import com.swrve.sdk.messaging.SwrveOrientation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface ISwrveBase<T, C extends SwrveConfigBase> {

    void sessionStart();

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

    void getRealTimeUserProperties(final SwrveRealTimeUserPropertiesListener listener);

    void sendQueuedEvents();

    void flushToDisk();

    void shutdown();

    void stopTracking();

    void setLanguage(Locale locale);

    String getLanguage();

    String getApiKey();

    String getUserId();

    JSONObject getDeviceInfo() throws JSONException;

    void refreshCampaignsAndResources();

    void embeddedMessageButtonWasPressed(SwrveEmbeddedMessage message, String buttonName);

    void embeddedMessageWasShownToUser(SwrveEmbeddedMessage message);

    String getPersonalizedEmbeddedMessageData(SwrveEmbeddedMessage message, Map<String, String> personalizationProperties);

    String getPersonalizedText(String text, Map<String, String> personalizationProperties);

    String getAppStoreURLForApp(int appId);

    File getCacheDir();

    Date getInitialisedTime();

    C getConfig();

    List<SwrveBaseCampaign> getMessageCenterCampaigns();

    List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation);

    List<SwrveBaseCampaign> getMessageCenterCampaigns(Map<String, String> properties);

    List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation, Map<String, String> properties);

    SwrveBaseCampaign getMessageCenterCampaign(int campaignId, Map<String, String> properties);

    boolean showMessageCenterCampaign(SwrveBaseCampaign campaign);

    boolean showMessageCenterCampaign(SwrveBaseCampaign campaign, Map<String, String> properties);

    void removeMessageCenterCampaign(SwrveBaseCampaign campaign);

    void markMessageCenterCampaignAsSeen(SwrveBaseCampaign campaign);

    void handleDeferredDeeplink(Bundle bundle);

    void handleDeeplink(Bundle bundle);

    void identify(final String userId, final SwrveIdentityResponse identityResponse);

    String getExternalUserId();

    void setCustomPayloadForConversationInput(Map payload);

    void start(Activity context);

    void start(Activity context, String userId);

    boolean isStarted();

}
