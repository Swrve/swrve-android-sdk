package com.swrve.sdk.qa;

import android.os.Bundle;
import android.util.Log;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTClient;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Used internally to offer QA user functionality.
 */
public class SwrveQAUser {
    public static final int QA_API_VERSION = 1;
    protected static final String LOG_TAG = "SwrveSDK";
    protected static final long REST_SESSION_INTERVAL = 1000;
    protected static final long REST_TRIGGER_INTERVAL = 500;
    private static Set<WeakReference<SwrveQAUser>> bindedObjects = new HashSet<WeakReference<SwrveQAUser>>();
    protected final SimpleDateFormat deviceTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
    protected ExecutorService restClientExecutor;
    private SwrveBase<?, ?> swrve;
    private IRESTClient restClient;
    private boolean resetDevice;
    private boolean logging;
    private String loggingUrl;
    private long lastSessionRequestTime;
    private long lastTriggerRequestTime;

    public SwrveQAUser(SwrveBase<?, ?> swrve, JSONObject jsonQa) {
        this.swrve = swrve;
        this.resetDevice = jsonQa.optBoolean("reset_device_state", false);
        this.logging = jsonQa.optBoolean("logging", false);
        if (logging) {
            restClientExecutor = Executors.newSingleThreadExecutor();
            restClient = new RESTClient();
            this.loggingUrl = jsonQa.optString("logging_url", null);
        }
    }

    public static Set<SwrveQAUser> getBindedListeners() {
        HashSet<SwrveQAUser> result = new HashSet<SwrveQAUser>();
        Iterator<WeakReference<SwrveQAUser>> iter = bindedObjects.iterator();
        while (iter.hasNext()) {
            SwrveQAUser sdkListener = iter.next().get();
            if (sdkListener == null) {
                iter.remove();
            } else {
                result.add(sdkListener);
            }
        }

        return result;
    }

    public boolean isResetDevice() {
        return resetDevice;
    }

    public boolean isLogging() {
        return logging && (loggingUrl != null);
    }

    public void talkSession(Map<Integer, String> campaignsDownloaded) {
        try {
            if (canMakeSessionRequest()) {
                String endpoint = loggingUrl + "/talk/game/" + swrve.getApiKey() + "/user/" + swrve.getUserId() + "/session";
                JSONObject talkSessionJson = new JSONObject();

                // Add campaigns (downloaded or not) to request
                JSONArray campaignsJson = new JSONArray();
                Iterator<Integer> campaignIt = campaignsDownloaded.keySet().iterator();
                while (campaignIt.hasNext()) {
                    int campaignId = campaignIt.next();
                    String reason = campaignsDownloaded.get(campaignId);

                    JSONObject campaignInfo = new JSONObject();
                    campaignInfo.put("id", campaignId);
                    campaignInfo.put("reason", (reason == null) ? "" : reason);
                    campaignInfo.put("loaded", (reason == null));
                    campaignsJson.put(campaignInfo);
                }
                talkSessionJson.put("campaigns", campaignsJson);
                // Add device info to request
                JSONObject deviceJson = swrve.getDeviceInfo();
                talkSessionJson.put("device", deviceJson);

                makeRequest(endpoint, talkSessionJson);
            }
        } catch (Exception exp) {
            Log.e(LOG_TAG, "QA request talk session failed", exp);
        }
    }

    public void triggerFailure(String event, String globalReason) {
        try {
            if (canMakeTriggerRequest()) {
                String endpoint = loggingUrl + "/talk/game/" + swrve.getApiKey() + "/user/" + swrve.getUserId() + "/trigger";
                JSONObject triggerJson = new JSONObject();
                triggerJson.put("trigger_name", event);
                triggerJson.put("displayed", false);
                triggerJson.put("reason", globalReason);
                triggerJson.put("campaigns", new JSONArray());

                makeRequest(endpoint, triggerJson);
            }
        } catch (Exception exp) {
            Log.e(LOG_TAG, "QA request talk session failed", exp);
        }
    }

    public void trigger(String event, SwrveMessage messageShown, Map<Integer, String> campaignReasons, Map<Integer, Integer> campaignMessages) {
        try {
            if (canMakeTriggerRequest()) {
                String endpoint = loggingUrl + "/talk/game/" + swrve.getApiKey() + "/user/" + swrve.getUserId() + "/trigger";
                JSONObject triggerJson = new JSONObject();
                triggerJson.put("trigger_name", event);
                triggerJson.put("displayed", (messageShown != null));
                triggerJson.put("reason", (messageShown == null) ? "The loaded campaigns returned no message" : "");

                // Add campaigns that were not displayed
                JSONArray campaignsJson = new JSONArray();
                Iterator<Integer> campaignIt = campaignReasons.keySet().iterator();
                while (campaignIt.hasNext()) {
                    int campaignId = campaignIt.next();
                    String reason = campaignReasons.get(campaignId);
                    Integer messageId = campaignMessages.get(campaignId);

                    JSONObject campaignInfo = new JSONObject();
                    campaignInfo.put("id", campaignId);
                    campaignInfo.put("displayed", false);
                    campaignInfo.put("message_id", (messageId == null) ? -1 : messageId);
                    campaignInfo.put("reason", (reason == null) ? "" : reason);
                    campaignsJson.put(campaignInfo);
                }

                // Add campaign that was shown, if available
                if (messageShown != null) {
                    JSONObject campaignInfo = new JSONObject();
                    campaignInfo.put("id", messageShown.getCampaign().getId());
                    campaignInfo.put("displayed", true);
                    campaignInfo.put("message_id", messageShown.getId());
                    campaignInfo.put("reason", "");
                    campaignsJson.put(campaignInfo);
                }
                triggerJson.put("campaigns", campaignsJson);

                makeRequest(endpoint, triggerJson);
            }
        } catch (Exception exp) {
            Log.e(LOG_TAG, "QA request talk session failed", exp);
        }
    }

    public void updateDeviceInfo() {
        try {
            if (canMakeRequest()) {
                String endpoint = loggingUrl + "/talk/game/" + swrve.getApiKey() + "/user/" + swrve.getUserId() + "/device_info";
                JSONObject deviceJson = swrve.getDeviceInfo();
                makeRequest(endpoint, deviceJson);
            }
        } catch (Exception exp) {
            Log.e(LOG_TAG, "QA request device info failed", exp);
        }
    }

    public void pushNotification(Bundle msg) {
        try {
            if (canMakeTriggerRequest()) {
                Object rawId = msg.get("_p");
                String msgId = (rawId != null) ? rawId.toString() : null;
                if (!SwrveHelper.isNullOrEmpty(msgId)) {
                    String endpoint = loggingUrl + "/talk/game/" + swrve.getApiKey() + "/user/" + swrve.getUserId() + "/push";
                    JSONObject pushJson = new JSONObject();
                    pushJson.put("id", msgId);
                    pushJson.put("alert", msg.getString("text"));
                    pushJson.put("sound", msg.getString("sound"));
                    pushJson.put("badge", "");
                    makeRequest(endpoint, pushJson);
                } else {
                    Log.e(LOG_TAG, "Push notification does not have a proper _p value");
                }
            }
        } catch (Exception exp) {
            Log.e(LOG_TAG, "QA request talk session failed", exp);
        }
    }

    public void bindToServices() {
        Iterator<WeakReference<SwrveQAUser>> iter = bindedObjects.iterator();
        while (iter.hasNext()) {
            SwrveQAUser sdkListener = iter.next().get();
            if (sdkListener == this) {
                return;
            }
        }
        bindedObjects.add(new WeakReference<SwrveQAUser>(this));
    }

    public void unbindToServices() {
        // Remove the weak reference to the listener
        Iterator<WeakReference<SwrveQAUser>> iter = bindedObjects.iterator();
        while (iter.hasNext()) {
            SwrveQAUser sdkListener = iter.next().get();
            if (sdkListener == this) {
                iter.remove();
                break;
            }
        }
    }

    private void makeRequest(final String endpoint, JSONObject json) throws JSONException {
        json.put("version", QA_API_VERSION);
        json.put("client_time", deviceTimeFormat.format(new Date()));
        final String body = json.toString();

        restClientExecutor.execute(new Runnable() {
            @Override
            public void run() {
                restClient.post(endpoint, body, new RESTResponseListener(endpoint));
            }
        });
    }

    private boolean canMakeRequest() {
        return (swrve != null && isLogging());
    }

    private boolean canMakeSessionRequest() {
        if (canMakeRequest()) {
            long currentTime = (new Date()).getTime();
            if (lastSessionRequestTime == 0 || (currentTime - lastSessionRequestTime) > REST_SESSION_INTERVAL) {
                lastSessionRequestTime = currentTime;
                return true;
            }
        }

        return false;
    }

    private boolean canMakeTriggerRequest() {
        if (canMakeRequest()) {
            long currentTime = (new Date()).getTime();
            if (lastTriggerRequestTime == 0 || (currentTime - lastTriggerRequestTime) > REST_TRIGGER_INTERVAL) {
                lastTriggerRequestTime = currentTime;
                return true;
            }
        }

        return false;
    }

    private class RESTResponseListener implements IRESTResponseListener {
        private String endpoint;

        public RESTResponseListener(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void onResponse(RESTResponse response) {
            if (!SwrveHelper.successResponseCode(response.responseCode)) {
                Log.e(LOG_TAG, "QA request to " + endpoint + " failed with error code " + response.responseCode + ": " + response.responseBody);
            }
        }

        @Override
        public void onException(Exception exp) {
            Log.e(LOG_TAG, "QA request to " + endpoint + " failed", exp);
        }
    }
}
