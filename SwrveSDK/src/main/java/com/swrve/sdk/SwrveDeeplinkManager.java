package com.swrve.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.SwrveConversationListener;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveConversationCampaign;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageListener;
import com.swrve.sdk.messaging.ui.SwrveInAppMessageActivity;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.swrve.sdk.ISwrveCommon.CACHE_AD_CAMPAIGNS_DEBUG;
import static com.swrve.sdk.ISwrveCommon.CACHE_NOTIFICATION_CAMPAIGNS_DEBUG;
import static com.swrve.sdk.SwrveImp.CAMPAIGN_RESPONSE_VERSION;
import static com.swrve.sdk.SwrveImp.SUPPORTED_REQUIREMENTS;

class SwrveDeeplinkManager {

    private static final String SWRVE_AD_CAMPAIGN_URL = "/api/1/ad_journey_campaign";
    private static final String SWRVE_AD_CONTENT = "ad_content";
    public static  final String SWRVE_AD_MESSAGE = "ad_message_key";
    private static final String SWRVE_AD_INSTALL = "install";
    private static final String SWRVE_AD_REENGAGE = "reengage";
    private static final String SWRVE_AD_SOURCE = "ad_source";
    private static final String SWRVE_AD_CAMPAIGN = "ad_campaign";
    private static final String SWRVE_FB_TARGET_URL = "target_url";
    private static final String SWRVE_NOTIFICATION_TO_CAMPAIGN = "notification_to_campaign";

    private Map<String, String> standardParams;
    private IRESTClient restClient;
    private Context context;
    private SwrveConfig config;
    private SwrveAssetsManager swrveAssetsManager;
    private SwrveBaseCampaign campaign;
    public SwrveMessageListener swrveMessageListener;
    public SwrveConversationListener swrveConversationListener;
    private SwrveMessage swrveMessage;
    private SwrveCampaignDisplayer swrveCampaignDisplayer;
    private String alreadySeenCampaignId;

    public SwrveMessage getSwrveMessage() {
        return swrveMessage;
    }

    public void setSwrveMessage(SwrveMessage swrveMessage) {
        this.swrveMessage = swrveMessage;
    }

    public IRESTClient getRestClient() {
        return restClient;
    }

    public void setRestClient(IRESTClient restClient) {
        this.restClient = restClient;
    }

    public SwrveDeeplinkManager(Map<String, String> standardParams, final SwrveConfig config, final Context context, SwrveAssetsManager swrveAssetsManager, IRESTClient restClient) {
        this.standardParams = standardParams;
        this.config = config;
        this.context = context;
        this.swrveAssetsManager = swrveAssetsManager;
        this.swrveCampaignDisplayer = new SwrveCampaignDisplayer(null);
        setRestClient(restClient);
    }

    protected static boolean isSwrveDeeplink(Bundle bundle) {
        if (bundle != null) {
            String targetURL = bundle.getString(SWRVE_FB_TARGET_URL);
            if (SwrveHelper.isNotNullOrEmpty(targetURL)) {
                Uri data = Uri.parse(targetURL);
                if (data != null) {
                    String campaignID = data.getQueryParameter(SWRVE_AD_CONTENT);
                    if (SwrveHelper.isNotNullOrEmpty(campaignID)) return true;
                }
            }
        }
        return false;
    }

    protected void handleDeeplink(Bundle bundle) {
        if (bundle != null) {
            String url = bundle.getString(SWRVE_FB_TARGET_URL);
            if (SwrveHelper.isNotNullOrEmpty(url)) {
                Uri data = Uri.parse(url);
                this.handleDeeplink(data, SWRVE_AD_REENGAGE);
                return;
            }

            String swrveCampaginId = bundle.getString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY);
            if (SwrveHelper.isNotNullOrEmpty(swrveCampaginId)) {
                loadCampaign(swrveCampaginId, SWRVE_NOTIFICATION_TO_CAMPAIGN);
                return;
            }
        }
    }

    protected void handleDeferredDeeplink(Bundle bundle) {
        if (bundle != null) {
            Uri data = Uri.parse(bundle.getString(SWRVE_FB_TARGET_URL));
            this.handleDeeplink(data,SWRVE_AD_INSTALL);
        }
    }

    private void handleDeeplink(Uri data, String actionType) {
        if (data != null) {
            String campaignId = data.getQueryParameter(SWRVE_AD_CONTENT);
            if (SwrveHelper.isNullOrEmpty(campaignId)) {
                SwrveLogger.i("SwrveDeeplinkManager: Could not queue deeplink generic event, missing campaignId");
                return;
            }
            if (campaignId.equals(alreadySeenCampaignId)) {
                SwrveLogger.i("SwrveDeeplinkManager: Not queuing deeplink generic event, alreadySeenCampaignID flag is true.");
                return;
            }

            loadCampaign(campaignId,actionType);

            String adSource = data.getQueryParameter(SWRVE_AD_SOURCE);
            String contextId = data.getQueryParameter(SWRVE_AD_CAMPAIGN);
            if (SwrveHelper.isNullOrEmpty(adSource) || SwrveHelper.isNullOrEmpty(contextId)) {
                SwrveLogger.i("SwrveDeeplinkManager: Not queuing deeplink generic event, adSource:%s contextId:%s", adSource, contextId);
                return;
            }

            try {
                queueDeeplinkGenericEvent(adSource, campaignId, contextId, actionType);
            } catch (Exception e) {
                SwrveLogger.e("SwrveDeeplinkManager: Could not queue deeplink generic event", e);
            }
        }
    }

    protected void loadCampaign(final String campaignID, final String actionType) {
        //AD Campaign ID
        standardParams.put("in_app_campaign_id", campaignID);

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            executorService.execute(SwrveRunnables.withoutExceptions(new Runnable() {
                @Override
                public void run() {

                    try {
                        restClient.get(config.getContentUrl() + SWRVE_AD_CAMPAIGN_URL, standardParams, new IRESTResponseListener() {
                            @Override
                            public void onResponse(RESTResponse response) {

                                if (response.responseCode == HttpURLConnection.HTTP_OK) {

                                    try {
                                        JSONObject responseJson;
                                        try {
                                            responseJson = new JSONObject(response.responseBody);
                                        } catch (JSONException e) {
                                            SwrveLogger.e("SwrveSDK unable to decode ad_journey_campaign JSON : \"%s\".", response.responseBody);
                                            throw e;
                                        }

                                        updateCdnPaths(responseJson);
                                        getCampaignAssets(responseJson);
                                        writeCampaignDataToCache(responseJson,actionType);

                                    } catch (JSONException e) {
                                        SwrveLogger.e("Could not parse JSON for ad campaign", e);
                                    }
                                } else {
                                    SwrveLogger.e("SwrveSDK unable to get ad_journey_campaign JSON : \"%s\".", response.responseBody);
                                }
                            }

                            @Override
                            public void onException(Exception e) {

                                SwrveLogger.e("Error downloading ad campaign", e);
                            }
                        });
                    } catch (UnsupportedEncodingException e) {
                        SwrveLogger.e("Could not update ad campaign, invalid parameters", e);
                    }
                }
            }));
        } finally {
            executorService.shutdown();
        }
    }

    protected void writeCampaignDataToCache(final JSONObject campaignContent, String actionType) {
        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        final String userId = swrveCommon.getUserId();
        final Swrve swrve = (Swrve) SwrveSDK.getInstance();
        String category = actionType.equals(SWRVE_NOTIFICATION_TO_CAMPAIGN) ? CACHE_NOTIFICATION_CAMPAIGNS_DEBUG : CACHE_AD_CAMPAIGNS_DEBUG;
        swrve.multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, category, campaignContent.toString(), swrve.getUniqueKey(userId));
    }

    @SuppressLint("UseSparseArrays")
    protected void getCampaignAssets(JSONObject json) {
        if (json == null) {
            SwrveLogger.i("NULL JSON for campaigns, aborting load.");
            return;
        }
        SwrveLogger.i("Campaign JSON data: %s", json);

        try {
            JSONObject jsonCampaign = json.getJSONObject("campaign");
            JSONObject additionalInfo = json.getJSONObject("additional_info");

            if (!additionalInfo.has("version")) {
                return;
            }
            String version = additionalInfo.getString("version");
            if (!version.equals(CAMPAIGN_RESPONSE_VERSION)) {
                SwrveLogger.i("Campaign JSON (%s) has the wrong version for this sdk (%s). No campaigns loaded.", version, CAMPAIGN_RESPONSE_VERSION);
                return;
            }

            final Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();

            if (jsonCampaign.has("conversation")) {
                JSONObject conversationJson = jsonCampaign.getJSONObject("conversation");

                // Check filters (permission requests, platform)
                boolean passesAllFilters = true;
                String lastCheckedFilter = null;

                if (conversationJson.has("filters")) {
                    JSONArray filters = conversationJson.getJSONArray("filters");
                    for (int ri = 0; ri < filters.length() && passesAllFilters; ri++) {
                        lastCheckedFilter = filters.getString(ri);
                        passesAllFilters = supportsDeviceFilter(lastCheckedFilter);
                    }
                }

                int conversationVersionDownloaded = conversationJson.optInt("conversation_version", 1);
                if (conversationVersionDownloaded <= ISwrveConversationSDK.CONVERSATION_VERSION) {
                    Swrve swrve = (Swrve) SwrveSDK.getInstance();
                    campaign = new SwrveConversationCampaign(swrve, this.swrveCampaignDisplayer, jsonCampaign, assetsQueue);

                } else {
                    SwrveLogger.i("Conversation version %s cannot be loaded with this SDK version", conversationVersionDownloaded);
                }

            } else if (jsonCampaign.has("messages")) {
                Swrve swrve = (Swrve) SwrveSDK.getInstance();
                campaign = new SwrveInAppCampaign(swrve, this.swrveCampaignDisplayer, jsonCampaign, assetsQueue);
            } else {
                SwrveLogger.e("Unknown campaign type");
            }

            downloadAssets(assetsQueue);
        } catch (JSONException exp) {
            SwrveLogger.e("Error parsing campaign JSON", exp);
        }
    }

    protected void downloadAssets(final Set<SwrveAssetsQueueItem> assetsQueue) {
        final SwrveAssetsCompleteCallback callback = new SwrveAssetsCompleteCallback() {
            @Override
            public void complete() {
                showCampaign(campaign, context, config);
            }
        };
        swrveAssetsManager.downloadAssets(assetsQueue, callback);
    }

    protected void showCampaign(SwrveBaseCampaign campaign, Context context, SwrveConfig config) {
        alreadySeenCampaignId =  String.valueOf(campaign.getId());
        if (campaign != null) {
            if (campaign instanceof SwrveConversationCampaign) {
                SwrveConversation conversation = ((SwrveConversationCampaign) campaign).getConversation();
                if (this.swrveConversationListener == null) {
                    ConversationActivity.showConversation(context, conversation, config.getOrientation());
                } else {
                    this.swrveConversationListener.onMessage(conversation);
                }

            } else if (campaign instanceof SwrveInAppCampaign) {
                SwrveMessage message = ((SwrveInAppCampaign) campaign).getMessages().get(0);
                setSwrveMessage(message);
                if (this.swrveMessageListener == null) {
                    Intent intent = new Intent(context, SwrveInAppMessageActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(SWRVE_AD_MESSAGE, true);
                    context.startActivity(intent);
                } else {
                    this.swrveMessageListener.onMessage(message);
                }
            }
        }
    }
  
    protected void queueDeeplinkGenericEvent(String adSource, String campaignId, String contextId, String actionType) throws JSONException {
        ISwrveCommon swrve = SwrveCommon.getInstance();
        if (swrve != null && SwrveHelper.isNotNullOrEmpty(adSource) && SwrveHelper.isNotNullOrEmpty(contextId)) {
            Map<String, String> payload = new HashMap<>();
            String id = "-1"; // variant id is not applicable to single ad campaigns
            String campaignType = "external_source_" + adSource.toLowerCase(Locale.ENGLISH);
            ArrayList<String> events = EventHelper.createGenericEvent(id, campaignType, actionType, contextId, campaignId, payload, swrve.getNextSequenceNumber());
            swrve.sendEventsInBackground(context, swrve.getUserId(), events);
        } else {
            SwrveLogger.e("Cannot queueDeeplinkGenericEvent: no SwrveSDK instance present or parameters were null");
        }
    }

    protected boolean supportsDeviceFilter(String requirement) {
        return SUPPORTED_REQUIREMENTS.contains(requirement.toLowerCase(Locale.ENGLISH));
    }

    private void updateCdnPaths(JSONObject json) throws JSONException {
        if(json.has("additional_info")) {
            if (json.has("cdn_root")) {
                String cdnRoot = json.getString("cdn_root");
                this.swrveAssetsManager.setCdnImages(cdnRoot);
                SwrveLogger.i("CDN URL %s", cdnRoot);
            } else if (json.has("cdn_paths")) {
                JSONObject cdnPaths = json.getJSONObject("cdn_paths");
                String cdnImages = cdnPaths.getString("message_images");
                String cdnFonts = cdnPaths.getString("message_fonts");
                this.swrveAssetsManager.setCdnImages(cdnImages);
                this.swrveAssetsManager.setCdnFonts(cdnFonts);
                SwrveLogger.i("CDN URL images:%s fonts:%s", cdnImages, cdnFonts);
            }
        }
    }
}
