package com.swrve.sdk;

import android.os.Bundle;

import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTClient;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.swrve.sdk.ISwrveCommon.CACHE_QA;
import static com.swrve.sdk.ISwrveCommon.CACHE_QA_RESET_DEVICE;

class QaUser {

    private static final Object LOCK = new Object();
    protected static final int REST_CLIENT_TIMEOUT_MILLIS = 15000;
    private static final String LOG_SOURCE_GEO = "geo-sdk";
    private static final String LOG_SOURCE_PUSH = "push-sdk";

    protected static QaUser instance;
    protected static IRESTClient restClient = new RESTClient(REST_CLIENT_TIMEOUT_MILLIS); // exposed for testing
    protected int appId;
    protected String apiKey;
    protected String userId;
    protected String endpoint;
    protected String appVersion;
    protected String sessionToken;
    protected String deviceId;
    protected boolean loggingEnabled;
    protected boolean resetDevice;
    protected ExecutorService restClientExecutor;

    protected static QaUser getInstance() {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new QaUser();
                instance.init();
            }
        }
        return instance;
    }

    static void update() {
        try {
            synchronized (LOCK) {
                if (instance != null && instance.restClientExecutor != null) {
                    instance.restClientExecutor.shutdown();
                }
                instance = new QaUser();
                instance.init();
            }
        } catch (Exception e) {
            SwrveLogger.e("Error updating qauser singleton", e);
        }
    }

    private void init() {
        try {
            ISwrveCommon swrveCommon = SwrveCommon.getInstance();
            userId = swrveCommon.getUserId();
            String qaString = swrveCommon.getCachedData(userId, CACHE_QA);
            loggingEnabled = Boolean.parseBoolean(qaString);
            String resetDeviceString = swrveCommon.getCachedData(userId, CACHE_QA_RESET_DEVICE);
            resetDevice = Boolean.parseBoolean(resetDeviceString);
            if (loggingEnabled) {
                appId = swrveCommon.getAppId();
                apiKey = swrveCommon.getApiKey();
                endpoint = swrveCommon.getBatchURL();
                appVersion = swrveCommon.getAppVersion();
                restClientExecutor = Executors.newSingleThreadExecutor();
                sessionToken = SwrveHelper.generateSessionToken(this.apiKey, this.appId, this.userId);
                deviceId = swrveCommon.getDeviceId();
            }
        } catch (Exception e) {
            SwrveLogger.e("Error trying to init QaUser.", e);
        }
    }

    static boolean isLoggingEnabled() {
        boolean isLoggingEnabled = false;
        try {
            isLoggingEnabled = QaUser.getInstance().loggingEnabled;
        } catch (Exception e) {
            SwrveLogger.e("Error calling QaUser.isLoggingEnabled", e);
        }
        return isLoggingEnabled;
    }

    static boolean isResetDevice() {
        boolean isResetDevice = false;
        try {
            isResetDevice = QaUser.getInstance().resetDevice;
        } catch (Exception e) {
            SwrveLogger.e("Error calling QaUser.isResetDevice", e);
        }
        return isResetDevice;
    }

    static void geoCampaignTriggered(long geoplaceId, long geofenceId, String actionType, Collection<QaGeoCampaignInfo> geoCampaignTriggeredList) {
        try {
            QaUser qaUser = QaUser.getInstance();
            qaUser._geoCampaignTriggered(geoplaceId, geofenceId, actionType, geoCampaignTriggeredList);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to send geo campaign triggered qa log event.", e);
        }
    }

    private void _geoCampaignTriggered(long geoplaceId, long geofenceId, String actionType, Collection<QaGeoCampaignInfo> geoCampaignInfoList) throws Exception {
        if (loggingEnabled) {
            JSONArray campaignsArray = new JSONArray();
            for (QaGeoCampaignInfo geoCampaignInfo : geoCampaignInfoList) {
                JSONObject campaignJson = new JSONObject();
                campaignJson.put("variant_id", geoCampaignInfo.variantId);
                campaignJson.put("displayed", geoCampaignInfo.displayed);
                campaignJson.put("reason", geoCampaignInfo.reason);
                campaignsArray.put(campaignJson);
            }
            if (campaignsArray.length() > 0) {
                JSONObject campaignsJson = new JSONObject();
                campaignsJson.put("geoplace_id", geoplaceId);
                campaignsJson.put("geofence_id", geofenceId);
                campaignsJson.put("action_type", actionType);
                campaignsJson.put("campaigns", campaignsArray);
                sendQaLogEvent(LOG_SOURCE_GEO, "geo-campaign-triggered", campaignsJson.toString());
            }
        }
    }

    static void geoCampaignsDownloaded(long geoplaceId, long geofenceId, String actionType, Collection<QaGeoCampaignInfo> geoCampaignInfoList) {
        try {
            QaUser qaUser = QaUser.getInstance();
            qaUser._geoCampaignsDownloaded(geoplaceId, geofenceId, actionType, geoCampaignInfoList);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to send geo campaign downloaded qa log event.", e);
        }
    }

    private void _geoCampaignsDownloaded(long geoplaceId, long geofenceId, String actionType, Collection<QaGeoCampaignInfo> geoCampaignInfoList) throws Exception {
        if (loggingEnabled) {
            JSONArray logDetailsCampaignsArray = new JSONArray();

            JSONObject logDetailsCampaignsJson = new JSONObject();
            logDetailsCampaignsJson.put("geoplace_id", geoplaceId);
            logDetailsCampaignsJson.put("geofence_id", geofenceId);
            logDetailsCampaignsJson.put("action_type", actionType);

            for (QaGeoCampaignInfo geoCampaignInfo : geoCampaignInfoList) {
                JSONObject logDetailsCampaignJson = new JSONObject();
                logDetailsCampaignJson.put("variant_id", geoCampaignInfo.variantId);
                logDetailsCampaignsArray.put(logDetailsCampaignJson);
            }
            logDetailsCampaignsJson.put("campaigns", logDetailsCampaignsArray);

            sendQaLogEvent(LOG_SOURCE_GEO, "geo-campaigns-downloaded", logDetailsCampaignsJson.toString());
        }
    }

    static void pushNotification(String trackingId, Bundle msg) {
        try {
            QaUser qaUser = QaUser.getInstance();
            qaUser._pushNotification(trackingId, msg);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to send pushNotification qa log event.", e);
        }
    }

    private void _pushNotification(String trackingId, Bundle msg) throws Exception {
        if (loggingEnabled) {
            JSONObject logDetailsJson = new JSONObject();
            logDetailsJson.put("campaign_id", trackingId);
            logDetailsJson.put("displayed", true);
            logDetailsJson.put("reason", "");
            JSONObject logDetailsPayloadJson = new JSONObject();
            logDetailsPayloadJson.put("push", "payload");
            logDetailsJson.put("payload", logDetailsPayloadJson);
            sendQaLogEvent(LOG_SOURCE_PUSH, "push-received", logDetailsJson.toString());
        }
    }

    private void sendQaLogEvent(String logSource, String logType, String logDetails) {
        try {
            int seqnum = SwrveCommon.getInstance().getNextSequenceNumber();
            long time = getTime();
            String qaLogEventAsJSON = EventHelper.qaLogEventAsJSON(seqnum, time, logSource, logType, logDetails);
            LinkedHashMap<Long, String> events = new LinkedHashMap<>();
            events.put(time, qaLogEventAsJSON);
            final String body = EventHelper.eventsAsBatch(events, userId, appVersion, sessionToken, deviceId);
            executeRestClient(endpoint, body);
        } catch (Exception ex) {
            SwrveLogger.e("Error trying to send qa log event.", ex);
        }
    }

    protected void executeRestClient(final String endpoint, final String body) {
        restClientExecutor.execute(new Runnable() {
            @Override
            public void run() {
                SwrveLogger.v("QaUser request with body:\n %s", body);
                restClient.post(endpoint, body, new RESTResponseListener(endpoint));
            }
        });
    }

    protected long getTime() {
        return System.currentTimeMillis();
    }

    private class RESTResponseListener implements IRESTResponseListener {
        private String endpoint;

        public RESTResponseListener(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void onResponse(RESTResponse response) {
            if (!SwrveHelper.successResponseCode(response.responseCode)) {
                SwrveLogger.e("QaUser request to %s failed with error code %s: %s", endpoint, response.responseCode, response.responseBody);
            }
        }

        @Override
        public void onException(Exception exp) {
            SwrveLogger.e("QaUser request to %s failed", exp, endpoint);
        }
    }
}
