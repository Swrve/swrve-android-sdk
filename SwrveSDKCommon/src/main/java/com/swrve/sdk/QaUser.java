package com.swrve.sdk;

import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTClient;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.swrve.sdk.ISwrveCommon.CACHE_QA;

public class QaUser {

    private static final Object LOCK = new Object();
    protected static final int LOG_QUEUE_FLUSH_INTERVAL_MILLIS = 4000;
    protected static final int REST_CLIENT_TIMEOUT_MILLIS = 15000;
    private static final String LOG_SOURCE_GEO = "geo-sdk";
    private static final String LOG_SOURCE_PUSH = "push-sdk";
    private static final String LOG_SOURCE_SDK = "sdk";

    protected static QaUser instance;
    protected static IRESTClient restClient = new RESTClient(REST_CLIENT_TIMEOUT_MILLIS); // exposed for testing
    protected List<String> qaLogQueue = Collections.synchronizedList(new ArrayList<String>());
    private ScheduledExecutorService flushLogQueueExecutorService;
    private boolean startFlushLogQueueService = false;
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

    private Runnable flushLogQueueRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                flushQaLogQueue();
            } catch (Exception ex) {
                SwrveLogger.e("QaUser error in runnable trying to flush log queue.", ex);
            }
        }
    };

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
                    instance.flushQaLogQueue(); // flush any remaining logs before shutting down
                    instance.restClientExecutor.shutdown();
                    instance.flushLogQueueExecutorService.shutdown();
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
            String qaJson = swrveCommon.getCachedData(userId, CACHE_QA);
            if (SwrveHelper.isNullOrEmpty(qaJson)) {
                loggingEnabled = false;
                resetDevice = false;
            } else {
                try {
                    JSONObject qaJsonObject = new JSONObject(qaJson);
                    if (qaJsonObject.has("logging")) {
                        loggingEnabled = qaJsonObject.optBoolean("logging", false);
                    }
                    if (qaJsonObject.has("reset_device_state")) {
                        resetDevice = qaJsonObject.optBoolean("reset_device_state", false);
                    }
                } catch (Exception e) {
                    SwrveLogger.e("SwrveSDK problem with decoding qauser json: %s", e, qaJson);
                }
            }
            if (loggingEnabled) {
                appId = swrveCommon.getAppId();
                apiKey = swrveCommon.getApiKey();
                endpoint = swrveCommon.getBatchURL();
                appVersion = swrveCommon.getAppVersion();
                restClientExecutor = Executors.newSingleThreadExecutor();
                sessionToken = SwrveHelper.generateSessionToken(this.apiKey, this.appId, this.userId);
                deviceId = swrveCommon.getDeviceId();

                scheduleRepeatingQueueFlush(LOG_QUEUE_FLUSH_INTERVAL_MILLIS);
            } else {
                if (flushLogQueueExecutorService != null) {
                    flushLogQueueExecutorService.shutdown();
                }
            }
        } catch (Exception e) {
            SwrveLogger.e("Error trying to init QaUser.", e);
        }
    }

    protected void scheduleRepeatingQueueFlush(long interval) {
        try {
            flushLogQueueExecutorService = Executors.newScheduledThreadPool(5);
            flushLogQueueExecutorService.scheduleAtFixedRate(flushLogQueueRunnable, 0, interval, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to schedule repeating qalogqueue flush.", e);
        }
    }

    public static boolean isLoggingEnabled() {
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

    // TODO Remove this later when all users have moved off version 4.0.0 of geosdk
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
                queueQaLogEvent(LOG_SOURCE_GEO, "geo-campaign-triggered", campaignsJson.toString());
            }
        }
    }

    // TODO Remove this later when all users have moved off version 4.0.0 of geosdk
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

            queueQaLogEvent(LOG_SOURCE_GEO, "geo-campaigns-downloaded", logDetailsCampaignsJson.toString());
        }
    }

    static void campaignsDownloaded(List<QaCampaignInfo> campaignInfoList) {
        try {
            QaUser qaUser = QaUser.getInstance();
            qaUser._campaignsDownloaded(campaignInfoList);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to queue campaigns-downloaded qalogevent.", e);
        }
    }

    private void _campaignsDownloaded(List<QaCampaignInfo> campaignInfoList) throws Exception {
        if (loggingEnabled) {

            JSONArray logDetailsCampaignsArray = new JSONArray();
            JSONObject logDetailsCampaignsJson = new JSONObject();
            for (QaCampaignInfo campaignInfo : campaignInfoList) {
                JSONObject logDetailsCampaignJson = new JSONObject();
                logDetailsCampaignJson.put("id", campaignInfo.id);
                logDetailsCampaignJson.put("variant_id", campaignInfo.variantId);
                logDetailsCampaignJson.put("type", campaignInfo.type);
                logDetailsCampaignsArray.put(logDetailsCampaignJson);
            }
            logDetailsCampaignsJson.put("campaigns", logDetailsCampaignsArray);

            queueQaLogEvent(LOG_SOURCE_SDK, "campaigns-downloaded", logDetailsCampaignsJson.toString());
        }
    }

    static void campaignsAppRuleTriggered(String eventName, Map<String, String> eventPayload, String appRuleReason) {
        try {
            QaUser qaUser = QaUser.getInstance();
            qaUser._campaignsAppRuleTriggered(eventName, eventPayload, appRuleReason);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to queue campaign-triggered qalogevent.", e);
        }
    }

    private void _campaignsAppRuleTriggered(String eventName, Map<String, String> eventPayload, String appRuleReason) throws Exception {
        if (loggingEnabled) {

            JSONObject logDetailsCampaignsJson = new JSONObject();
            logDetailsCampaignsJson.put("event_name", eventName);
            JSONObject eventPayloadJson = eventPayload == null ? new JSONObject() : new JSONObject(eventPayload);
            logDetailsCampaignsJson.put("event_payload", eventPayloadJson);
            logDetailsCampaignsJson.put("displayed", false);
            logDetailsCampaignsJson.put("reason", appRuleReason);

            JSONArray logDetailsCampaignsArray = new JSONArray();
            logDetailsCampaignsJson.put("campaigns", logDetailsCampaignsArray);

            queueQaLogEvent(LOG_SOURCE_SDK, "campaign-triggered", logDetailsCampaignsJson.toString());
        }
    }

    static void campaignTriggeredConversation(String eventName, Map<String, String> eventPayload, boolean displayed, Map<Integer, QaCampaignInfo> qaCampaignInfoMap) {
        try {
            QaUser qaUser = QaUser.getInstance();
            String noCampaignTriggeredReason = displayed ? "" : "The loaded campaigns returned no conversation";
            qaUser._campaignTriggered(eventName, eventPayload, displayed, noCampaignTriggeredReason, qaCampaignInfoMap);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to queue campaign-triggered qalogevent.", e);
        }
    }

    static void campaignTriggeredMessage(String eventName, Map<String, String> eventPayload, boolean displayed, Map<Integer, QaCampaignInfo> qaCampaignInfoMap) {
        try {
            QaUser qaUser = QaUser.getInstance();
            String noCampaignTriggeredReason = displayed ? "" : "The loaded campaigns returned no message";
            qaUser._campaignTriggered(eventName, eventPayload, displayed, noCampaignTriggeredReason, qaCampaignInfoMap);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to queue campaign-triggered qalogevent.", e);
        }
    }

    static void campaignTriggeredMessageNoDisplay(String eventName, Map<String, String> eventPayload) {
        try {
            QaUser qaUser = QaUser.getInstance();
            String noDisplayReason = "No In App Message triggered because Conversation displayed";
            qaUser._campaignTriggered(eventName, eventPayload, false, noDisplayReason, new HashMap<Integer, QaCampaignInfo>());
        } catch (Exception e) {
            SwrveLogger.e("Error trying to queue campaign-triggered qalogevent.", e);
        }
    }

    private void _campaignTriggered(String eventName, Map<String, String> eventPayload, boolean displayed, String reason, Map<Integer, QaCampaignInfo> qaCampaignInfoMap) throws Exception {
        if (loggingEnabled) {

            JSONObject logDetailsCampaignsJson = new JSONObject();
            logDetailsCampaignsJson.put("event_name", eventName);
            JSONObject eventPayloadJson = eventPayload == null ? new JSONObject() : new JSONObject(eventPayload);
            logDetailsCampaignsJson.put("event_payload", eventPayloadJson);
            logDetailsCampaignsJson.put("displayed", displayed);
            logDetailsCampaignsJson.put("reason", reason);

            JSONArray logDetailsCampaignsArray = new JSONArray();
            if (qaCampaignInfoMap != null) {
                for (Map.Entry<Integer, QaCampaignInfo> entry : qaCampaignInfoMap.entrySet()) {
                    QaCampaignInfo campaignInfo = entry.getValue();
                    JSONObject logDetailsCampaignJson = new JSONObject();
                    logDetailsCampaignJson.put("id", campaignInfo.id);
                    logDetailsCampaignJson.put("variant_id", campaignInfo.variantId);
                    logDetailsCampaignJson.put("type", campaignInfo.type);
                    logDetailsCampaignJson.put("displayed", campaignInfo.displayed);
                    logDetailsCampaignJson.put("reason", campaignInfo.reason);
                    logDetailsCampaignsArray.put(logDetailsCampaignJson);
                }
            }
            logDetailsCampaignsJson.put("campaigns", logDetailsCampaignsArray);

            queueQaLogEvent(LOG_SOURCE_SDK, "campaign-triggered", logDetailsCampaignsJson.toString());
        }
    }

    public static void campaignButtonClicked(int campaignId, int variantId, String buttonName, String actionType, String actionValue) {
        try {
            QaUser qaUser = QaUser.getInstance();
            qaUser._campaignButtonClicked(campaignId, variantId, buttonName, actionType, actionValue);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to queue campaign-button-clicked qalogevent.", e);
        }
    }

    private void _campaignButtonClicked(int campaignId, int variantId, String buttonName, String actionType, String actionValue) throws Exception {
        if (loggingEnabled) {
            JSONObject logDetailsCampaignsJson = new JSONObject();
            logDetailsCampaignsJson.put("campaign_id", campaignId);
            logDetailsCampaignsJson.put("variant_id", variantId);
            logDetailsCampaignsJson.put("button_name", buttonName);
            logDetailsCampaignsJson.put("action_type", actionType);
            logDetailsCampaignsJson.put("action_value", actionValue);

            queueQaLogEvent(LOG_SOURCE_SDK, "campaign-button-clicked", logDetailsCampaignsJson.toString());
        }
    }

    static void wrappedEvents(List<String> events) {
        try {
            QaUser qaUser = QaUser.getInstance();
            qaUser._wrappedEvents(events);
        } catch (Exception e) {
            SwrveLogger.e("Error trying to queue wrapped event qalogevent.", e);
        }
    }

    protected void _wrappedEvents(List<String> events) throws Exception {
        if (loggingEnabled) {

            for(String eventJson: events) {

                JSONObject event = new JSONObject(eventJson);

                // Build
                JSONObject logDetailsJson = new JSONObject();
                if(event.has("type")){
                    logDetailsJson.put("type", event.getString("type"));
                    event.remove("type");
                }
                if(event.has("seqnum")){
                    logDetailsJson.put("seqnum", event.getLong("seqnum"));
                    event.remove("seqnum");
                }
                if(event.has("time")){
                    logDetailsJson.put("client_time", event.getLong("time"));
                    event.remove("time");
                }
                String payloadString = "{}"; // babble currently only accepting payload jsonobject as a string, and not a proper jsonobject
                if (event.has("payload")) {
                    payloadString = event.getJSONObject("payload").toString();
                    event.remove("payload");
                }
                logDetailsJson.put("payload", payloadString);

                // add remaining details as parameters
                logDetailsJson.put("parameters", event); // parameters required even if empty

                queueQaLogEvent(LOG_SOURCE_SDK, "event", logDetailsJson.toString());
            }
        }
    }

    private void queueQaLogEvent(String logSource, String logType, String logDetails) {
        try {
            long time = getTime();
            String qaLogEventAsJSON = EventHelper.qaLogEventAsJSON(time, logSource, logType, logDetails);
            qaLogQueue.add(qaLogEventAsJSON);
            synchronized (qaLogQueue) {
                if (startFlushLogQueueService) {
                    scheduleRepeatingQueueFlush(LOG_QUEUE_FLUSH_INTERVAL_MILLIS);
                    startFlushLogQueueService = false;
                }
            }
        } catch (Exception ex) {
            SwrveLogger.e("Error trying to queue qalogevent.", ex);
        }
    }

    private void flushQaLogQueue() throws Exception {
        LinkedHashMap<Long, String> events = new LinkedHashMap<>();
        synchronized (qaLogQueue) {
            if (qaLogQueue.size() > 0) {
                long i = 0;
                for (String log : qaLogQueue) {
                    events.put(i++, log);
                }
                qaLogQueue.clear();
            } else {
                startFlushLogQueueService = true;
                if (flushLogQueueExecutorService != null) {
                    flushLogQueueExecutorService.shutdown();
                }
            }
        }
        if (events.size() > 0) {
            String body = EventHelper.eventsAsBatch(events, userId, appVersion, sessionToken, deviceId);
            executeRestClient(endpoint, body);
        }
    }

    protected synchronized void executeRestClient(final String endpoint, final String body) {
        restClientExecutor.execute(new Runnable() {
            @Override
            public void run() {
                SwrveLogger.v("QaUser request with body:\n %s", body);
                IRESTClient restClient = new RESTClient(REST_CLIENT_TIMEOUT_MILLIS);
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
            if (SwrveHelper.successResponseCode(response.responseCode)) {
                SwrveLogger.v("QaUser request to %s sent with response code %s: %s", endpoint, response.responseCode, response.responseBody);
            } else {
                SwrveLogger.e("QaUser request to %s failed with error code %s: %s", endpoint, response.responseCode, response.responseBody);
            }
        }

        @Override
        public void onException(Exception exp) {
            SwrveLogger.e("QaUser request to %s failed", exp, endpoint);
        }
    }

}
