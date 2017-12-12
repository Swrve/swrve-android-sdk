package com.swrve.sdk;

import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTClient;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.swrve.sdk.ISwrveCommon.CACHE_LOCATION_CAMPAIGNS;
import static com.swrve.sdk.ISwrveCommon.CACHE_QA;

class QaUser {

    private static final Object LOCK = new Object();
    protected static final int REST_CLIENT_TIMEOUT_MILLIS = 15000;
    private static final String LOG_SOURCE_LOCATION = "location-sdk";
    protected static final int RATE_LIMIT_PERIOD_MILLIS = 1000 * 15;
    protected static final int RATE_LIMIT_MAX_ALLOWED_REQUESTS = 3;
    protected static final int RATE_LIMIT_COOLOFF_TIME_MILLS = 1000 * 60;

    protected static QaUser instance;
    protected static IRESTClient restClient = new RESTClient(REST_CLIENT_TIMEOUT_MILLIS); // exposed for testing
    protected static Queue<Long> campaignTriggeredLogTimeQueue = new ArrayBlockingQueue(RATE_LIMIT_MAX_ALLOWED_REQUESTS);
    protected static long rateLimitCooloffUntilTime;

    protected int appId;
    protected String apiKey;
    protected String userId;
    protected String endpoint;
    protected String appVersion;
    protected String sessionToken;
    protected short deviceId;
    protected boolean loggingEnabled;
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
            QaUser qaUser = QaUser.getInstance();
            isLoggingEnabled = qaUser.loggingEnabled;
        } catch (Exception e) {
            SwrveLogger.e("Error calling QaUser.isLoggingEnabled", e);
        }
        return isLoggingEnabled;
    }

    static void locationCampaignTriggered(Collection<QaLocationCampaignTriggered> locationCampignTriggeredList) {
        try {
            QaUser qaUser = QaUser.getInstance();
            qaUser._locationCampaignTriggered(locationCampignTriggeredList);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to send location campaign triggered qa log event.", e);
        }
    }

    private void _locationCampaignTriggered(Collection<QaLocationCampaignTriggered> locationCampignTriggeredList) throws Exception {
        if (loggingEnabled && !isRateLimited(getTime())) {
            JSONArray campaignsArray = new JSONArray();
            for (QaLocationCampaignTriggered triggered : locationCampignTriggeredList) {
                JSONObject campaignJson = new JSONObject();
                campaignJson.put("id", triggered.id);
                campaignJson.put("variant_id", triggered.variantId);
                campaignJson.put("plot_id", triggered.plotId);
                campaignJson.put("displayed", triggered.displayed);
                campaignJson.put("reason", triggered.reason);
                campaignsArray.put(campaignJson);
            }
            if (campaignsArray.length() > 0) {
                JSONObject campaignsJson = new JSONObject();
                campaignsJson.put("campaigns", campaignsArray);
                sendQaLogEvent(LOG_SOURCE_LOCATION, "location-campaign-triggered", campaignsJson.toString());
            }
        }
    }

    static void locationCampaignEngaged(String plotId, int campaignId, int variantId, String payload) {
        try {
            QaUser qaUser = QaUser.getInstance();
            qaUser._locationCampaignEngaged(plotId, campaignId, variantId, payload);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to send location campaign engaged qa log event.", e);
        }
    }

    private void _locationCampaignEngaged(String plotId, long campaignId, long variantId, String payload) throws Exception {
        if (loggingEnabled) {
            JSONObject engagedJson = new JSONObject();
            engagedJson.put("plot_campaign_id", plotId);
            engagedJson.put("campaign_id", campaignId);
            engagedJson.put("variant_id", variantId);
            JSONObject payloadJson = new JSONObject();
            if(SwrveHelper.isNotNullOrEmpty(payload)) {
                payloadJson = new JSONObject(payload);
            }
            engagedJson.put("variant_payload", payloadJson);
            sendQaLogEvent(LOG_SOURCE_LOCATION, "location-campaign-engaged", engagedJson.toString());
        }
    }

    static void locationCampaignsDownloaded() {
        try {
            QaUser qaUser = QaUser.getInstance();
            qaUser._locationCampaignsDownloaded();
        } catch (Exception e) {
            SwrveLogger.e("Error trying to send location campaign downloaded qa log event.", e);
        }
    }

    private void _locationCampaignsDownloaded() throws Exception {
        // only execute if locationsdk is installed
        if (loggingEnabled) {
            JSONArray logDetailsCampaignsArray = new JSONArray();

            ISwrveCommon swrveCommon = SwrveCommon.getInstance();
            String locationCampaignsFromCache = swrveCommon.getCachedData(swrveCommon.getUserId(), CACHE_LOCATION_CAMPAIGNS);
            if(SwrveHelper.isNotNullOrEmpty(locationCampaignsFromCache)) {
                JSONObject locationCampaignsJson = new JSONObject(locationCampaignsFromCache);
                JSONObject campaignsJson = locationCampaignsJson.getJSONObject("campaigns");

                Iterator iterator = campaignsJson.keys();
                while (iterator.hasNext()) {
                    String campaignId = (String) iterator.next();
                    JSONObject campaignJson = campaignsJson.getJSONObject(campaignId);
                    long version = campaignJson.getLong("version");
                    if (version <= 1) {
                        JSONObject messageJson = campaignJson.getJSONObject("message");
                        long variantId = messageJson.getLong("id");

                        JSONObject logDetailsCampaignJson = new JSONObject();
                        logDetailsCampaignJson.put("id", campaignId);
                        logDetailsCampaignJson.put("variant_id", variantId);
                        logDetailsCampaignsArray.put(logDetailsCampaignJson);
                    }
                }
            }

            JSONObject logDetailsCampaignsJson = new JSONObject();
            logDetailsCampaignsJson.put("campaigns", logDetailsCampaignsArray);
            sendQaLogEvent(LOG_SOURCE_LOCATION, "location-campaigns-downloaded", logDetailsCampaignsJson.toString());
        }
    }

    protected boolean isRateLimited(long mostRecentTime) {

        boolean isRateLimited = false;
        synchronized (LOCK) {

            long oldestTime = campaignTriggeredLogTimeQueue.size() >= 1 ? campaignTriggeredLogTimeQueue.element() : 0;
            long timeDiff = mostRecentTime - oldestTime;
            if (mostRecentTime < rateLimitCooloffUntilTime) {
                // currently in a cooloff so don't add to queue
                isRateLimited = true;
            } else if (campaignTriggeredLogTimeQueue.size() == RATE_LIMIT_MAX_ALLOWED_REQUESTS && (timeDiff <= RATE_LIMIT_PERIOD_MILLIS)) {
                // number of requests in period has exceeded limit so apply a cooloff and don't add to queue
                isRateLimited = true;
                rateLimitCooloffUntilTime = mostRecentTime + RATE_LIMIT_COOLOFF_TIME_MILLS;
                campaignTriggeredLogTimeQueue.clear();
            } else {
                campaignTriggeredLogTimeQueue.offer(mostRecentTime);
            }
        }

        return isRateLimited;
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
