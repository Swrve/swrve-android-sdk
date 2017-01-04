package com.swrve.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.conversations.ISwrveConversationListener;
import com.swrve.sdk.conversations.ISwrveConversationSDKProvider;
import com.swrve.sdk.conversations.ISwrveConversationsSDK;
import com.swrve.sdk.conversations.SwrveCommonConversation;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.model.ChoiceInputResponse;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.exceptions.NoUserIdSwrveException;
import com.swrve.sdk.localstorage.ILocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.messaging.ISwrveCustomButtonListener;
import com.swrve.sdk.messaging.ISwrveDialogListener;
import com.swrve.sdk.messaging.ISwrveInstallButtonListener;
import com.swrve.sdk.messaging.ISwrveMessageListener;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveCampaign;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveConversationCampaign;
import com.swrve.sdk.messaging.SwrveEventListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main base class implementation of the Swrve SDK.
 */
public abstract class SwrveBase<T, C extends SwrveConfigBase> extends SwrveImp<T, C> implements ISwrveBase<T, C>, ISwrveCommon, ISwrveConversationsSDK {

    protected SwrveBase(Context context, int appId, String apiKey, C config) {
        super(context, appId, apiKey, config);
        SwrveCommon.setSwrveCommon(this);
    }

    protected static String _getVersion() {
        return version;
    }

    public static String getVersion() {
        try {
            return _getVersion();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public T onCreate(final Activity activity) throws IllegalArgumentException {
        if (destroyed) {
            destroyed = initialised = false;
            bindCounter.set(0);
        }
        if (!initialised) {
            // First time it is initialized
            return init(activity);
        }
        bindToContext(activity);
        afterBind();
        showPreviousMessage();
        return (T) this;
    }

    protected T init(final Activity activity) throws IllegalArgumentException {
        // Initialization checks
        if (activity == null) {
            SwrveHelper.logAndThrowException("Activity not specified");
        }

        try {
            final Context resolvedContext = bindToContext(activity);
            final boolean preloadRandC = config.isLoadCachedCampaignsAndResourcesOnUIThread();

            // Save initialization time
            initialisedTime = getNow();
            initialised = true;
            this.lastSessionTick = getSessionTime();
            this.sessionToken = SwrveHelper.generateSessionToken(this.apiKey, this.appId, userId); // Generate session token

            cachedLocalStorage = createCachedLocalStorage();

            appStoreURLs = new SparseArray<String>();

            // Find cache folder
            findCacheFolder(activity);

            // Open access to local storage
            openLocalStorageConnection();

            // Obtain extra device info
            beforeSendDeviceInfo(resolvedContext);

            this.resourceManager = new SwrveResourceManager();
            if (preloadRandC) {
                // Initialize resources from cache
                initResources();
            }

            // Send session start
            queueSessionStart();
            generateNewSessionInterval();

            // If user is first installed
            String savedInstallTime = getSavedInstallTime();
            if (SwrveHelper.isNullOrEmpty(savedInstallTime)) {
                // First time we see this user
                _event("Swrve.first_session", null);
                Date now = getNow();
                userInstallTime = installTimeFormat.format(now);
            }
            // Get install time
            installTime.set(getInstallTime());
            installTimeLatch.countDown();

            // Android referrer information
            SharedPreferences settings = activity.getSharedPreferences(SDK_PREFS_NAME, 0);
            String referrer = settings.getString(SWRVE_REFERRER_ID, null);
            if (!SwrveHelper.isNullOrEmpty(referrer)) {
                Map<String, String> attributes = new HashMap<String, String>();
                attributes.put(SWRVE_REFERRER_ID, referrer);
                SwrveLogger.i(LOG_TAG, "Received install referrer, so sending userUpdate:" + attributes);
                userUpdate(attributes);
                settings.edit().remove(SWRVE_REFERRER_ID).apply();
            }

            // Get device info
            getDeviceInfo(resolvedContext);
            queueDeviceInfoNow(true);

            // Talk initialization
            if (config.isTalkEnabled()) {
                if (SwrveHelper.isNullOrEmpty(language)) {
                    SwrveHelper.logAndThrowException("Language needed to use Talk");
                } else if (SwrveHelper.isNullOrEmpty(config.getAppStore())) {
                    SwrveHelper.logAndThrowException("App store needed to use Talk");
                }

                if (preloadRandC) {
                    initCampaigns(); // Initialize campaigns from cache
                }

                // Add custom message listener
                if (messageListener == null) {
                    setMessageListener(new ISwrveMessageListener() {
                        public void onMessage(final SwrveMessage message, boolean firstTime) {
                            if (SwrveBase.this.activityContext != null) {
                                final Activity activity = SwrveBase.this.activityContext.get();
                                if (activity == null) {
                                    SwrveLogger.e(LOG_TAG, "Can't display a message with a non-Activity context");
                                    return;
                                }
                                // Run code on the UI thread
                                activity.runOnUiThread(new DisplayMessageRunnable(SwrveBase.this, activity, message, firstTime));
                            }
                        }
                    });
                }

                // Add custom conversation listener
                if (conversationListener == null) {
                    setConversationListener(new ISwrveConversationListener() {
                        public void onMessage(final SwrveConversation conversation) {
                            // Start a Conversation activity to display the campaign
                            if (SwrveBase.this.context != null) {
                                final Context ctx = SwrveBase.this.context.get();
                                if (ctx == null) {
                                    SwrveLogger.e(LOG_TAG, "Can't display a conversation without a context");
                                    return;
                                }

                                // Set the SDK provider so that the ConversationActivity can find
                                // the shared instance of the SDK
                                // This is also implemented by the Unity Android SDK Java layer
                                ConversationActivity.setSDKProvider(new ISwrveConversationSDKProvider() {
                                    @Override
                                    public ISwrveConversationsSDK getInstance() {
                                        return (SwrveBase)SwrveSDKBase.getInstance();
                                    }
                                });

                                Intent intent = new Intent(ctx, ConversationActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("conversation", conversation);
                                ctx.startActivity(intent);
                                // Report that the message was shown to users
                                conversation.getCampaign().messageWasShownToUser();
                            }
                        }
                    });
                }

                // Show any previous message after rotation
                showPreviousMessage();
            }

            // Retrieve values for resource/campaigns flush frequencies and ETag
            campaignsAndResourcesFlushFrequency = settings.getInt("swrve_cr_flush_frequency", SWRVE_DEFAULT_CAMPAIGN_RESOURCES_FLUSH_FREQUENCY);
            campaignsAndResourcesFlushRefreshDelay = settings.getInt("swrve_cr_flush_delay", SWRVE_DEFAULT_CAMPAIGN_RESOURCES_FLUSH_REFRESH_DELAY);
            campaignsAndResourcesLastETag = settings.getString("campaigns_and_resources_etag", null);

            startCampaignsAndResourcesTimer(true);
            disableAutoShowAfterDelay();

            if (!preloadRandC) {
                // Load campaigns and resources in a background thread
                storageExecutorExecute(new Runnable() {
                    @Override
                    public void run() {
                        initResources();
                        if (config.isTalkEnabled()) {
                            initCampaigns();
                        }
                    }
                });
            }

            sendCrashlyticsMetadata();
            afterInit();

            SwrveLogger.i(LOG_TAG, "Init finished");
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, "Swrve init failed", exp);
        }
        return (T) this;
    }

    protected abstract void beforeSendDeviceInfo(Context context);

    protected abstract void afterInit();

    protected abstract void afterBind();

    protected abstract void extraDeviceInfo(JSONObject deviceInfo) throws JSONException;

    protected void _sessionStart() {
        queueSessionStart();
        // Send event after it has been queued
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                sendQueuedEvents();
            }
        });
    }

    protected void _sessionEnd() {
        queueEvent("session_end", null, null);
    }

    protected void _event(String name) {
        _event(name, null);
    }

    protected void _event(String name, Map<String, String> payload) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("name", name);

        queueEvent("event", parameters, payload);
    }

    protected void _purchase(String item, String currency, int cost, int quantity) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("item", item);
        parameters.put("currency", currency);
        parameters.put("cost", Integer.toString(cost));
        parameters.put("quantity", Integer.toString(quantity));

        queueEvent("purchase", parameters, null);
    }

    protected void _currencyGiven(String givenCurrency, double givenAmount) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("given_currency", givenCurrency);
        parameters.put("given_amount", Double.toString(givenAmount));

        queueEvent("currency_given", parameters, null);
    }

    protected void _userUpdate(Map<String, String> attributes) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        try {
            parameters.put("attributes", new JSONObject(attributes));
        } catch (NullPointerException ex) {
            SwrveLogger.e(LOG_TAG, "JSONException when encoding user attributes", ex);
        }

        queueEvent("user", parameters, null);
    }

    protected void _iap(int quantity, String productId, double productPrice, String currency) {
        SwrveIAPRewards rewards = new SwrveIAPRewards();
        this.iap(quantity, productId, productPrice, currency, rewards);
    }

    protected void _iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards) {
        this._iap(quantity, productId, productPrice, currency, rewards, "", "", "unknown_store");
    }

    protected SwrveResourceManager _getResourceManager() {
        return this.resourceManager;
    }

    protected void _getUserResources(final ISwrveUserResourcesListener listener) {
        String cachedResources = null;

        // Read cached resources
        try {
            cachedResources = cachedLocalStorage.getSecureCacheEntryForUser(userId, RESOURCES_CACHE_CATEGORY, getUniqueKey());
        } catch (SecurityException e) {
            SwrveLogger.i(LOG_TAG, "Signature for " + RESOURCES_CACHE_CATEGORY + " invalid; could not retrieve data from cache");
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", "Swrve.signature_invalid");
            queueEvent("event", parameters, null, false);
            listener.onUserResourcesError(e);
        }

        if (!SwrveHelper.isNullOrEmpty(cachedResources)) {
            try {
                // Parse raw response
                JSONArray jsonResources = new JSONArray(cachedResources);
                // Convert to map
                Map<String, Map<String, String>> mapResources = new HashMap<String, Map<String, String>>();
                for (int i = 0, j = jsonResources.length(); i < j; i++) {
                    JSONObject resourceJSON = jsonResources.getJSONObject(i);
                    String uid = resourceJSON.getString("uid");
                    Map<String, String> resourceMap = SwrveHelper.JSONToMap(resourceJSON);
                    mapResources.put(uid, resourceMap);
                }

                // Execute callback (NOTE: Executed in same thread!)
                listener.onUserResourcesSuccess(mapResources, cachedResources);
            } catch (Exception exp) {
                // Launch exception
                listener.onUserResourcesError(exp);
            }
        }
    }

    protected void _getUserResourcesDiff(final ISwrveUserResourcesDiffListener listener) {
        final SwrveBase<?, ?> swrveReference = this;
        restClientExecutorExecute(new Runnable() {
            @Override
            public void run() {
                if (userId != null) {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("api_key", apiKey);
                    params.put("user", userId);
                    params.put("app_version", appVersion);
                    params.put("joined", String.valueOf(getOrWaitForInstallTime()));
                    try {
                        SwrveLogger.i(LOG_TAG, "Contacting AB Test server " + config.getContentUrl());
                        restClient.get(config.getContentUrl() + USER_RESOURCES_DIFF_ACTION, params, new RESTCacheResponseListener(swrveReference, cachedLocalStorage, userId, RESOURCES_DIFF_CACHE_CATEGORY, EMPTY_JSON_ARRAY) {
                            @Override
                            public void onResponseCached(int responseCode, String responseBody) {
                                SwrveLogger.i(LOG_TAG, "Got AB Test response code " + responseCode);
                                if (!SwrveHelper.isNullOrEmpty(responseBody)) {
                                    // Process data and launch listener
                                    processUserResourcesDiffData(responseBody, listener);
                                }
                            }

                            @Override
                            public void onException(Exception exp) {
                                // Launch exception
                                SwrveLogger.e(LOG_TAG, "AB Test exception", exp);
                                listener.onUserResourcesDiffError(exp);
                            }
                        });
                    } catch (Exception exp) {
                        // Launch exception
                        SwrveLogger.e(LOG_TAG, "AB Test exception", exp);
                        listener.onUserResourcesDiffError(exp);
                    }
                } else {
                    // No user specified...
                    SwrveLogger.e(LOG_TAG, "Error: No user specified");
                    listener.onUserResourcesDiffError(new NoUserIdSwrveException());
                }
            }
        });
    }

    protected void _sendQueuedEvents() {
        if (userId != null) {
            restClientExecutorExecute(new Runnable() {
                @Override
                public void run() {
                    short deviceId = EventHelper.getDeviceId(cachedLocalStorage);
                    SwrveEventsManager swrveEventsManager = new SwrveEventsManager(config, restClient, userId, appVersion, sessionToken, deviceId);
                    swrveEventsManager.sendStoredEvents(cachedLocalStorage);
                    eventsWereSent = true;
                }
            });
        }
    }

    protected void _flushToDisk() {
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                try {
                    SwrveLogger.i(LOG_TAG, "Flushing to disk");
                    cachedLocalStorage.flush();
                } catch (Exception e) {
                    SwrveLogger.e(LOG_TAG, "Flush to disk failed", e);
                }
            }
        });
    }

    protected void _onPause() {
        if (campaignsAndResourcesExecutor != null) {
            campaignsAndResourcesExecutor.shutdown();
        }
        mustCleanInstance = true;
        Activity activity = getActivityContext();
        if (activity != null) {
            mustCleanInstance = isActivityFinishing(activity);
        }

        SwrveLogger.i(LOG_TAG, "onPause");
        flushToDisk();
        // Session management
        generateNewSessionInterval();
        // Save campaign state if needed
        saveCampaignsState();
    }

    protected void _onResume(Activity ctx) {
        SwrveLogger.i(LOG_TAG, "onResume");
        if (ctx != null) {
            bindToContext(ctx);
        }
        startCampaignsAndResourcesTimer(true);
        disableAutoShowAfterDelay();

        queueDeviceInfoNow(false);
        long currentTime = getSessionTime();
        // Session management
        if (currentTime > lastSessionTick) {
            sessionStart();
        } else {
            if (config.isSendQueuedEventsOnResume()) {
                sendQueuedEvents();
            }
        }
        generateNewSessionInterval();

        Activity activity = getActivityContext();
        if (activity != null && activity.getIntent() != null && activity.getIntent().getData() != null) {
            Uri uri = activity.getIntent().getData();
            String referrer = uri.getQueryParameter(REFERRER);
            if (!SwrveHelper.isNullOrEmpty(referrer)) {
                Map<String, String> attributes = new HashMap<String, String>();
                attributes.put(SWRVE_REFERRER_ID, referrer);
                SwrveLogger.i(LOG_TAG, "Received referrer, so sending userUpdate:" + attributes);
                userUpdate(attributes);
            }
        }
    }

    protected void _onLowMemory() {
        // We should stop Talk from showing new messages
        config.setTalkEnabled(false);
    }

    protected void _onDestroy(Activity ctx) {
        unbindAndShutdown(ctx);
    }

    protected void _shutdown() throws InterruptedException {
        if (!destroyed) {
            SwrveLogger.i(LOG_TAG, "Shutting down the SDK");
            destroyed = true;

            // Forget the current displaying message
            SwrveBase.messageDisplayed = null;

            // Forget the initialised time
            initialisedTime = null;

            removeCurrentDialog(null);
            // Remove the binding to the current activity, if any
            this.activityContext = null;

            // Remove reference to previous message
            this.messageDisplayed = null;

            // Remove QA user from push notification listener
            if (qaUser != null) {
                qaUser.unbindToServices();
                qaUser = null;
            }

            // Do not accept any more jobs but try to finish sending data
            restClientExecutor.shutdown();
            storageExecutor.shutdown();
            campaignsAndResourcesExecutor.shutdown();
            storageExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            restClientExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            cachedLocalStorage.close();
        }
    }

    protected void _setLanguage(Locale locale) {
        this.language = SwrveHelper.toLanguageTag(locale);
    }

    @Deprecated
    protected void _setLanguage(String language) {
        this.language = language;
    }

    protected String _getLanguage() {
        return language;
    }

    protected String _getApiKey() {
        return this.apiKey;
    }

    protected String _getUserId() {
        return this.userId;
    }

    protected JSONObject _getDeviceInfo() throws JSONException {
        JSONObject deviceInfo = new JSONObject();

        deviceInfo.put(SWRVE_DEVICE_NAME, getDeviceName());
        deviceInfo.put(SWRVE_OS, "Android");
        deviceInfo.put(SWRVE_OS_VERSION, Build.VERSION.RELEASE);

        Context contextRef = context.get();
        if (contextRef != null) {
            try {
                Display display = ((WindowManager) contextRef.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                DisplayMetrics metrics = new DisplayMetrics();
                int width = display.getWidth();
                int height = display.getHeight();
                display.getMetrics(metrics);
                float xdpi = metrics.xdpi;
                float ydpi = metrics.ydpi;
                // Always use portrait dimensions (i.e. width < height)
                if (width > height) {
                    int tmp = width;
                    width = height;
                    height = tmp;
                    float tmpdpi = xdpi;
                    xdpi = ydpi;
                    ydpi = tmpdpi;
                }
                deviceInfo.put(SWRVE_DEVICE_WIDTH, width);
                deviceInfo.put(SWRVE_DEVICE_HEIGHT, height);
                deviceInfo.put(SWRVE_DEVICE_DPI, metrics.densityDpi);
                deviceInfo.put(SWRVE_ANDROID_DEVICE_XDPI, xdpi);
                deviceInfo.put(SWRVE_ANDROID_DEVICE_YDPI, ydpi);
                deviceInfo.put(SWRVE_CONVERSATION_VERSION, CONVERSATION_VERSION);
                // Carrier info
                if (!SwrveHelper.isNullOrEmpty(simOperatorName)) {
                    deviceInfo.put(SWRVE_SIM_OPERATOR_NAME, simOperatorName);
                }
                if (!SwrveHelper.isNullOrEmpty(simOperatorIsoCountryCode)) {
                    deviceInfo.put(SWRVE_SIM_OPERATOR_ISO_COUNTRY, simOperatorIsoCountryCode);
                }
                if (!SwrveHelper.isNullOrEmpty(simOperatorCode)) {
                    deviceInfo.put(SWRVE_SIM_OPERATOR_CODE, simOperatorCode);
                }
                // Android ID
                if (!SwrveHelper.isNullOrEmpty(androidId)) {
                    deviceInfo.put(SWRVE_ANDROID_ID, androidId);
                }
            } catch (Exception exp) {
                SwrveLogger.e(LOG_TAG, "Get device screen info failed", exp);
            }
            deviceInfo.put(SWRVE_LANGUAGE, SwrveBase.this.language);
            String deviceRegion = Locale.getDefault().getISO3Country();
            deviceInfo.put(SWRVE_DEVICE_REGION, deviceRegion);
            deviceInfo.put(SWRVE_SDK_VERSION, PLATFORM + SwrveBase.version);
            deviceInfo.put(SWRVE_APP_STORE, config.getAppStore());

            Calendar cal = new GregorianCalendar();
            deviceInfo.put(SWRVE_TIMEZONE_NAME, cal.getTimeZone().getID());
            deviceInfo.put(SWRVE_UTC_OFFSET_SECONDS, cal.getTimeZone().getOffset(System.currentTimeMillis()) / 1000);

            if (!SwrveHelper.isNullOrEmpty(userInstallTime)) {
                deviceInfo.put(SWRVE_INSTALL_DATE, userInstallTime);
            }
        }

        extraDeviceInfo(deviceInfo);
        return deviceInfo;
    }

    protected void _refreshCampaignsAndResources() {
        // When campaigns need to be downloaded manually, enforce max. flush frequency
        if (!config.isAutoDownloadCampaingsAndResources()) {
            Date now = getNow();
            if (campaignsAndResourcesLastRefreshed != null) {
                Date nextAllowedTime = new Date(campaignsAndResourcesLastRefreshed.getTime() + campaignsAndResourcesFlushFrequency);
                if (now.compareTo(nextAllowedTime) < 0) {
                    SwrveLogger.i(LOG_TAG, "Request to retrieve campaign and user resource data was rate-limited");
                    return;
                }
            }
            campaignsAndResourcesLastRefreshed = now;
        }

        restClientExecutorExecute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<String, String>();

                // General params
                params.put("api_key", apiKey);
                params.put("user", userId);
                params.put("app_version", appVersion);
                params.put("joined", String.valueOf(getOrWaitForInstallTime()));

                if (config.isTalkEnabled()) {
                    // Talk only params
                    params.put("version", String.valueOf(CAMPAIGN_ENDPOINT_VERSION));
                    params.put("conversation_version", String.valueOf(CONVERSATION_VERSION));
                    params.put("language", language);
                    params.put("app_store", config.getAppStore());

                    // Device info
                    params.put("device_width", String.valueOf(deviceWidth));
                    params.put("device_height", String.valueOf(deviceHeight));
                    params.put("device_dpi", String.valueOf(deviceDpi));
                    params.put("android_device_xdpi", String.valueOf(androidDeviceXdpi));
                    params.put("android_device_ydpi", String.valueOf(androidDeviceYdpi));
                    params.put("orientation", config.getOrientation().toString().toLowerCase(Locale.US));
                    params.put("device_name", getDeviceName());
                    params.put("os_version", Build.VERSION.RELEASE);
                }

                if(locationVersion > 0) {
                    params.put("location_version", String.valueOf(locationVersion));
                }

                // If we have a last ETag value, send that along with the request
                if (!SwrveHelper.isNullOrEmpty(campaignsAndResourcesLastETag)) {
                    params.put("etag", campaignsAndResourcesLastETag);
                }

                try {
                    restClient.get(config.getContentUrl() + CAMPAIGNS_AND_RESOURCES_ACTION, params, new IRESTResponseListener() {
                        @Override
                        public void onResponse(RESTResponse response) {
                            // Response received from server
                            if (response.responseCode == HttpURLConnection.HTTP_OK) {
                                SharedPreferences settings = context.get().getSharedPreferences(SDK_PREFS_NAME, 0);
                                SharedPreferences.Editor settingsEditor = settings.edit();

                                String etagHeader = response.getHeaderValue("ETag");
                                if (!SwrveHelper.isNullOrEmpty(etagHeader)) {
                                    campaignsAndResourcesLastETag = etagHeader;
                                    settingsEditor.putString("campaigns_and_resources_etag", campaignsAndResourcesLastETag);
                                }

                                try {
                                    JSONObject responseJson = new JSONObject(response.responseBody);

                                    if (responseJson.has("flush_frequency")) {
                                        Integer flushFrequency = responseJson.getInt("flush_frequency");
                                        if (flushFrequency != null) {
                                            campaignsAndResourcesFlushFrequency = flushFrequency;
                                            settingsEditor.putInt("swrve_cr_flush_frequency", campaignsAndResourcesFlushFrequency);
                                        }
                                    }

                                    if (responseJson.has("flush_refresh_delay")) {
                                        Integer flushDelay = responseJson.getInt("flush_refresh_delay");
                                        if (flushDelay != null) {
                                            campaignsAndResourcesFlushRefreshDelay = flushDelay;
                                            settingsEditor.putInt("swrve_cr_flush_delay", campaignsAndResourcesFlushRefreshDelay);
                                        }
                                    }

                                    if (config.isTalkEnabled()) {
                                        if (responseJson.has("campaigns")) {
                                            JSONObject campaignJson = responseJson.getJSONObject("campaigns");
                                            updateCampaigns(campaignJson, campaignsState);
                                            saveCampaignsInCache(campaignJson);
                                            autoShowMessages();

                                            // Notify campaigns have been downloaded
                                            Map<String, String> payload = new HashMap<String, String>();
                                            StringBuilder campaignIds = new StringBuilder();
                                            for (int i = 0; i < campaigns.size(); i++) {
                                                if (i != 0) {
                                                    campaignIds.append(',');
                                                }
                                                campaignIds.append(campaigns.get(i).getId());
                                            }
                                            payload.put("ids", campaignIds.toString());
                                            payload.put("count", String.valueOf(campaigns.size()));
                                            Map<String, Object> parameters = new HashMap<String, Object>();
                                            parameters.put("name", "Swrve.Messages.campaigns_downloaded");
                                            queueEvent("event", parameters, payload, false);
                                        }
                                    }

                                    // If json contains lcoation campaigns then save it to sqlite cache to be used by the location sdk
                                    if (responseJson.has("location_campaigns")) {
                                        JSONObject campaignJson = responseJson.getJSONObject("location_campaigns");
                                        saveLocationCampaignsInCache(campaignJson);
                                    }

                                    if (responseJson.has("user_resources")) {
                                        // Update resource manager
                                        JSONArray resourceJson = responseJson.getJSONArray("user_resources");
                                        resourceManager.setResourcesFromJSON(resourceJson);
                                        saveResourcesInCache(resourceJson);

                                        // Call resource listener
                                        if (campaignsAndResourcesInitialized) {
                                            invokeResourceListener();
                                        }
                                    }
                                } catch (JSONException e) {
                                    SwrveLogger.e(LOG_TAG, "Could not parse JSON for campaigns and resources", e);
                                }

                                settingsEditor.apply();
                            }

                            this.firstRefreshFinished();
                        }

                        @Override
                        public void onException(Exception e) {
                            this.firstRefreshFinished();
                            SwrveLogger.e(LOG_TAG, "Error downloading resources and campaigns", e);
                        }

                        public void firstRefreshFinished() {
                            if (!campaignsAndResourcesInitialized) {
                                campaignsAndResourcesInitialized = true;

                                // Only called first time API call returns - whether failed or successful, whether new campaigns were returned or not;
                                // this ensures that if API call fails or there are no changes, we call autoShowMessages with cached campaigns
                                autoShowMessages();

                                // Invoke listeners once to denote that the first attempt at downloading has finished
                                // independent of whether the resources or campaigns have changed from cached values
                                invokeResourceListener();
                            }
                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    SwrveLogger.e(LOG_TAG, "Could not update resources and campaigns, invalid parameters", e);
                }
            }
        });
    }

    protected SwrveMessage _getMessageForEvent(String event) {
        return getMessageForEvent(event, SwrveOrientation.Both);
    }

    private boolean checkCampaignRules(int elementCount, String elementName, String event, Date now) {
        if (elementCount == 0) {
            noMessagesWereShown(event, "No " + elementName + "s available");
            return false;
        }

        if (!event.equalsIgnoreCase(SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER) && isTooSoonToShowMessageAfterLaunch(now)) {
            noMessagesWereShown(event, "{App throttle limit} Too soon after launch. Wait until " + timestampFormat.format(showMessagesAfterLaunch));
            return false;
        }

        if (isTooSoonToShowMessageAfterDelay(now)) {
            noMessagesWereShown(event, "{App throttle limit} Too soon after last " + elementName + ". Wait until " + timestampFormat.format(showMessagesAfterDelay));
            return false;
        }

        if (hasShowTooManyMessagesAlready()) {
            noMessagesWereShown(event, "{App Throttle limit} Too many " + elementName + "s shown");
            return false;
        }

        return true;
    }


    @SuppressLint("UseSparseArrays")
    protected SwrveConversation _getConversationForEvent(String event) {
        SwrveConversation result = null;
        SwrveCampaign campaign = null;

        Date now = getNow();
        Map<Integer, String> campaignReasons = null;
        Map<Integer, Integer> campaignMessages = null;

        if (campaigns != null) {
            if (!checkCampaignRules(campaigns.size(), "conversation", event, now)) {
                return null;
            }
            if (qaUser != null) {
                campaignReasons = new HashMap<Integer, String>();
                campaignMessages = new HashMap<Integer, Integer>();
            }
            synchronized (campaigns) {
                List<SwrveConversation> availableConversations = new ArrayList<SwrveConversation>();
                Iterator<SwrveBaseCampaign> itCampaign = campaigns.iterator();
                while (itCampaign.hasNext()) {
                    SwrveBaseCampaign nextCampaign = itCampaign.next();
                    if (nextCampaign instanceof SwrveConversationCampaign) {
                        SwrveConversation nextConversation = ((SwrveConversationCampaign)nextCampaign).getConversationForEvent(event, now, campaignReasons);
                        if (nextConversation != null) {
                            // Add to list of returned messages
                            availableConversations.add(nextConversation);
                        }
                    }
                }
                if (availableConversations.size() > 0) {
                    // Select randomly
                    Collections.shuffle(availableConversations);
                    result = availableConversations.get(0);
                }
                if (qaUser != null && campaign != null && result != null) {
                    // A message was chosen, set the reason for the others
                    Iterator<SwrveConversation> itOtherConversation = availableConversations.iterator();
                    while (itOtherConversation.hasNext()) {
                        SwrveConversation otherMessage = itOtherConversation.next();
                        if (otherMessage != result) {
                            int otherCampaignId = otherMessage.getCampaign().getId();
                            if (!campaignMessages.containsKey(otherCampaignId)) {
                                campaignMessages.put(otherCampaignId, otherMessage.getId());
                                campaignReasons.put(otherCampaignId, "Campaign " + campaign.getId() + " was selected for display ahead of this campaign");
                            }
                        }
                    }
                }
            }
        }

        // If QA enabled, send message selection information
        if (qaUser != null) {
            qaUser.trigger(event, result, campaignReasons, campaignMessages);
        }

        if (result == null) {
            SwrveLogger.w(LOG_TAG, "Not showing message: no candidate messages for " + event);
        } else {
            // Notify message has been returned
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("id", String.valueOf(result.getId()));
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", "Swrve.Conversations.conversation_returned");
            queueEvent("event", parameters, payload, false);
        }

        return result;
    }

    @SuppressLint("UseSparseArrays")
    protected SwrveMessage _getMessageForEvent(String event, SwrveOrientation orientation) {
        SwrveMessage result = null;
        SwrveCampaign campaign = null;

        Date now = getNow();
        Map<Integer, String> campaignReasons = null;
        Map<Integer, Integer> campaignMessages = null;

        if (campaigns != null) {
            if (!checkCampaignRules(campaigns.size(), "message", event, now)) {
                return null;
            }
            if (qaUser != null) {
                campaignReasons = new HashMap<Integer, String>();
                campaignMessages = new HashMap<Integer, Integer>();
            }
            synchronized (campaigns) {
                List<SwrveMessage> availableMessages = new ArrayList<SwrveMessage>();
                // Select messages with higher priority
                int minPriority = Integer.MAX_VALUE;
                List<SwrveMessage> candidateMessages = new ArrayList<SwrveMessage>();
                Iterator<SwrveBaseCampaign> itCampaign = campaigns.iterator();
                while (itCampaign.hasNext()) {
                    SwrveBaseCampaign nextCampaign = itCampaign.next();
                    if (nextCampaign instanceof SwrveCampaign) {
                        SwrveMessage nextMessage = ((SwrveCampaign)nextCampaign).getMessageForEvent(event, now, campaignReasons);
                        if (nextMessage != null) {
                            // Add to list of returned messages
                            availableMessages.add(nextMessage);
                            // Check if it is a candidate to be shown
                            if (nextMessage.getPriority() <= minPriority) {
                                if (nextMessage.getPriority() < minPriority) {
                                    // If it is lower than any of the previous ones
                                    // remove those from being candidates
                                    candidateMessages.clear();
                                }
                                minPriority = nextMessage.getPriority();
                                candidateMessages.add(nextMessage);
                            }
                        }
                    }
                }

                // Select randomly from the highest messages
                Collections.shuffle(candidateMessages);
                Iterator<SwrveMessage> itCandidateMessage = candidateMessages.iterator();
                while (campaign == null && itCandidateMessage.hasNext()) {
                    SwrveMessage candidateMessage = itCandidateMessage.next();
                    // Check that the message supports the current orientation
                    if (candidateMessage.supportsOrientation(orientation)) {
                        result = candidateMessage;
                        campaign = candidateMessage.getCampaign();
                    } else {
                        if (qaUser != null) {
                            int campaignId = candidateMessage.getCampaign().getId();
                            campaignMessages.put(campaignId, candidateMessage.getId());
                            campaignReasons.put(campaignId, "Message didn't support the given orientation: " + orientation);
                        }
                    }
                }

                if (qaUser != null && campaign != null && result != null) {
                    // A message was chosen, set the reason for the others
                    Iterator<SwrveMessage> itOtherMessage = availableMessages.iterator();
                    while (itOtherMessage.hasNext()) {
                        SwrveMessage otherMessage = itOtherMessage.next();
                        if (otherMessage != result) {
                            int otherCampaignId = otherMessage.getCampaign().getId();
                            if (!campaignMessages.containsKey(otherCampaignId)) {
                                campaignMessages.put(otherCampaignId, otherMessage.getId());
                                campaignReasons.put(otherCampaignId, "Campaign " + campaign.getId() + " was selected for display ahead of this campaign");
                            }
                        }
                    }
                }
            }
        }

        // If QA enabled, send message selection information
        if (qaUser != null) {
            qaUser.trigger(event, result, campaignReasons, campaignMessages);
        }

        if (result == null) {
            SwrveLogger.w(LOG_TAG, "Not showing message: no candidate messages for " + event);
        } else {
            // Notify message has been returned
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("id", String.valueOf(result.getId()));
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", "Swrve.Messages.message_returned");
            queueEvent("event", parameters, payload, false);
        }

        return result;
    }

    protected SwrveMessage _getMessageForId(int messageId) {
        SwrveMessage result = null;

        if (campaigns != null && campaigns.size() > 0) {
            synchronized (campaigns) {
                Iterator<SwrveBaseCampaign> itCampaign = campaigns.iterator();
                while (itCampaign.hasNext() && result == null) {
                    SwrveBaseCampaign campaign = itCampaign.next();
                    if (campaign instanceof SwrveCampaign) {
                        result = ((SwrveCampaign)campaign).getMessageForId(messageId);
                    }
                }
            }
        }

        if (result == null) {
            SwrveLogger.i(LOG_TAG, "Not showing messages: no candidate messages");
        }

        return result;
    }

    protected void _buttonWasPressedByUser(SwrveButton button) {
        if (button.getActionType() != SwrveActionType.Dismiss) {
            String clickEvent = "Swrve.Messages.Message-" + button.getMessage().getId() + ".click";
            SwrveLogger.i(LOG_TAG, "Sending click event: " + clickEvent + "(" + button.getName() + ")");
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("name", button.getName());
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", clickEvent);
            queueEvent("event", parameters, payload, false);
        }
    }

    /**
     * Ensures a new message cannot be shown until now + minDelayBetweenMessage
     */
    public void setMessageMinDelayThrottle()
    {
        Date now = getNow();
        this.showMessagesAfterDelay = SwrveHelper.addTimeInterval(now, this.minDelayBetweenMessage, Calendar.SECOND);
    }

    protected String getEventForConversation(SwrveCommonConversation conversation){
        return "Swrve.Conversations.Conversation-" + conversation.getId();
    }

    protected void _messageWasShownToUser(SwrveMessageFormat messageFormat) {
        if (messageFormat != null) {
            setMessageMinDelayThrottle();
            this.messagesLeftToShow = this.messagesLeftToShow - 1;

            // Update next for round robin
            SwrveMessage message = messageFormat.getMessage();
            SwrveCampaign campaign = message.getCampaign();
            if (campaign != null) {
                campaign.messageWasShownToUser();
            }

            String viewEvent = "Swrve.Messages.Message-" + message.getId() + ".impression";
            SwrveLogger.i(LOG_TAG, "Sending view event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("format", messageFormat.getName());
            payload.put("orientation", messageFormat.getOrientation().name());
            payload.put("size", messageFormat.getSize().x + "x" + messageFormat.getSize().y);
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            queueEvent("event", parameters, payload, false);
            saveCampaignsState();
        }
    }

    protected void _queueConversationEventsCommitedByUser(SwrveCommonConversation conversation, ArrayList<UserInputResult> userInteractions){
        if (conversation != null) {
            String baseEvent = getEventForConversation(conversation)  + ".";
            for (UserInputResult userInteraction : userInteractions){
                Map<String, String> payload = new HashMap<String, String>();
                Map<String, Object> parameters = new HashMap<String, Object>();
                String viewEvent = baseEvent + userInteraction.getType();
                parameters.put("name", viewEvent);
                payload.put("page", userInteraction.getPageTag());
                payload.put("conversation", userInteraction.getConversationId());
                payload.put("event", userInteraction.getType());
                payload.put("fragment", userInteraction.getFragmentTag());

                if(userInteraction.isSingleChoice()){
                    ChoiceInputResponse response = (ChoiceInputResponse) userInteraction.getResult();
                    payload.put("result", response.getAnswerID());
                }
                queueEvent("event", parameters, payload, false);
            }
        }
    }

    protected void _conversationCallWasAccessedByUser(SwrveCommonConversation conversation, String pageTag, String controlTag){
        if (conversation != null) {
            String viewEvent = getEventForConversation(conversation) + ".call";
            SwrveLogger.d(LOG_TAG, "Sending view conversation event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            payload.put("event", "call");
            payload.put("page", pageTag);
            payload.put("control", controlTag);
            payload.put("conversation", Integer.toString(conversation.getId()));
            queueEvent("event", parameters, payload, false);
        }
    }

    protected void _conversationLinkVisitWasAccessedByUser(SwrveCommonConversation conversation, String pageTag, String controlTag){
        if (conversation != null) {
            String viewEvent = getEventForConversation(conversation) + conversation.getId()+ ".visit";
            SwrveLogger.d(LOG_TAG, "Sending view conversation event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            payload.put("event", "visit");
            payload.put("page", pageTag);
            payload.put("control", controlTag);
            payload.put("conversation", Integer.toString(conversation.getId()));
            queueEvent("event", parameters, payload, false);
        }
    }
    protected void _conversationDeeplinkWasAccessedByUser(SwrveCommonConversation conversation, String pageTag, String controlTag){
        if (conversation != null) {
            String viewEvent = getEventForConversation(conversation) + conversation.getId()+ ".deeplink";
            SwrveLogger.d(LOG_TAG, "Sending view conversation event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            payload.put("event", "deeplink");
            payload.put("page", pageTag);
            payload.put("control", controlTag);
            payload.put("conversation", Integer.toString(conversation.getId()));
            queueEvent("event", parameters, payload, false);
        }
    }


    protected void _conversationPageWasViewedByUser(SwrveCommonConversation conversation, String pageTag) {
        if (conversation != null) {
            String viewEvent = getEventForConversation(conversation) + ".impression";
            SwrveLogger.d(LOG_TAG, "Sending view conversation event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            payload.put("event", "impression");
            payload.put("page", pageTag);
            payload.put("conversation", Integer.toString(conversation.getId()));
            queueEvent("event", parameters, payload, false);
        }
    }

    protected void _conversationWasStartedByUser(SwrveCommonConversation conversation, String pageTag) {
        if (conversation != null) {
            String viewEvent = getEventForConversation(conversation) + ".start";
            SwrveLogger.d(LOG_TAG, "Sending view conversation event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            payload.put("event", "start");
            payload.put("page", pageTag);
            payload.put("conversation", Integer.toString(conversation.getId()));
            queueEvent("event", parameters, payload, false);
        }
    }

    protected void _conversationWasFinishedByUser(SwrveCommonConversation conversation, String endPageTag, String controlTag) {
        if (conversation != null) {
            String viewEvent = getEventForConversation(conversation)  + ".done";
            SwrveLogger.d(LOG_TAG, "Sending view conversation event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            payload.put("event", "done");
            payload.put("page", endPageTag); //The final page the user ended on
            payload.put("control", controlTag); //The final button the user clicked
            payload.put("conversation", Integer.toString(conversation.getId()));
            queueEvent("event", parameters, payload, false);
        }
    }

    protected void _conversationWasCancelledByUser(SwrveCommonConversation conversation, String currentPageTag) {
        if (conversation != null) {
            String viewEvent = getEventForConversation(conversation) + ".cancel";
            SwrveLogger.d(LOG_TAG, "Sending view conversation event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            payload.put("event", "cancel");
            payload.put("page", currentPageTag); //The current page the user is on when they cancelled
            payload.put("conversation", Integer.toString(conversation.getId()));
            queueEvent("event", parameters, payload, false);
        }
    }


    protected void _conversationTransitionedToOtherPage(SwrveCommonConversation conversation, String fromPageTag, String toPageTag, String controlTag) {
        if (conversation != null) {
            String viewEvent = getEventForConversation(conversation) + ".navigation";
            SwrveLogger.d(LOG_TAG, "Sending view conversation event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            payload.put("event", "navigation");
            payload.put("control", controlTag);
            payload.put("to", toPageTag); // The page the user ended on
            payload.put("page", fromPageTag); // The page the user came on
            payload.put("conversation", Integer.toString(conversation.getId()));
            queueEvent("event", parameters, payload, false);
        }
    }

    protected void _conversationEncounteredError(SwrveCommonConversation conversation, String currentPageTag,  Exception e) {
        if (conversation != null) {
            String viewEvent = getEventForConversation(conversation)  + ".error";
            if (e != null){
                SwrveLogger.e(LOG_TAG, "Sending error conversation event: " + viewEvent, e);
            }else{
                SwrveLogger.e(LOG_TAG, "Sending error conversations event: (No Exception) " + viewEvent);
            }

            SwrveLogger.d(LOG_TAG, "Sending error conversation event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            payload.put("event", "error");
            payload.put("page", currentPageTag);
            payload.put("conversation", Integer.toString(conversation.getId()));
            queueEvent("event", parameters, payload, false);
        }
    }

    protected String _getAppStoreURLForApp(int appId) {
        return appStoreURLs.get(appId);
    }

    protected File _getCacheDir() {
        return cacheDir;
    }

    protected void _setMessageListener(ISwrveMessageListener messageListener) {
        this.messageListener = messageListener;
        if (messageListener != null) {
            eventListener = new SwrveEventListener(this, messageListener, conversationListener);
        } else {
            eventListener = null;
        }
    }

    protected void _setConversationListener(ISwrveConversationListener listener) {
        this.conversationListener = listener;
        if (conversationListener != null) {
            eventListener = new SwrveEventListener(this, messageListener, conversationListener);
        } else {
            eventListener = null;
        }
    }


    protected void _setResourcesListener(ISwrveResourcesListener resourcesListener) {
        this.resourcesListener = resourcesListener;
    }

    protected Date _getInitialisedTime() {
        return initialisedTime;
    }

    protected void _setInstallButtonListener(ISwrveInstallButtonListener installButtonListener) {
        this.installButtonListener = installButtonListener;
    }

    protected ISwrveInstallButtonListener _getInstallButtonListener() {
        return installButtonListener;
    }

    protected void _setCustomButtonListener(ISwrveCustomButtonListener customButtonListener) {
        this.customButtonListener = customButtonListener;
    }

    protected ISwrveCustomButtonListener _getCustomButtonListener() {
        return customButtonListener;
    }

    protected void _setDialogListener(ISwrveDialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }

    protected ISwrveDialogListener _getDialogListener() {
        return dialogListener;
    }

    protected Context _getContext() {
        Context appCtx = context.get();
        if(appCtx == null) {
            return getActivityContext();
        }
        return appCtx;
    }

    protected C _getConfig() {
        return config;
    }

    /**
     * Public functions all wrapped in try/catch to avoid exceptions leaving the SDK and affecting the app itself.
     */
    @Override
    public void sessionStart() {
        try {
            _sessionStart();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void sessionEnd() {
        try {
            _sessionEnd();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void event(String name) {
        try {
            if(isValidEventName(name)) {
                _event(name);
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void event(String name, Map<String, String> payload) {
        try {
            if(isValidEventName(name)) {
                _event(name, payload);
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    private boolean isValidEventName(String name) {
        List<String> restrictedNamesStartWith = new ArrayList<String>() {{
            add("Swrve.");
            add("swrve.");
        }};

        for(String restricted: restrictedNamesStartWith) {
            if(name==null || name.startsWith(restricted)) {
                SwrveLogger.e(LOG_TAG, "Event names cannot begin with " + restricted + "* This event will not be sent. Eventname:" + name);
                return false;
            }
        }
        return true;
    }

    @Override
    public void purchase(String item, String currency, int cost, int quantity) {
        try {
            _purchase(item, currency, cost, quantity);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void currencyGiven(String givenCurrency, double givenAmount) {
        try {
            _currencyGiven(givenCurrency, givenAmount);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void userUpdate(Map<String, String> attributes) {
        try {
            _userUpdate(attributes);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void iap(int quantity, String productId, double productPrice, String currency) {
        try {
            _iap(quantity, productId, productPrice, currency);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards) {
        try {
            _iap(quantity, productId, productPrice, currency, rewards);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public SwrveResourceManager getResourceManager() {
        try {
            return _getResourceManager();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void getUserResources(final ISwrveUserResourcesListener listener) {
        try {
            _getUserResources(listener);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void getUserResourcesDiff(final ISwrveUserResourcesDiffListener listener) {
        try {
            _getUserResourcesDiff(listener);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void sendQueuedEvents() {
        try {
            _sendQueuedEvents();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void flushToDisk() {
        try {
            _flushToDisk();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void onPause() {
        try {
            _onPause();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void onResume(Activity ctx) {
        try {
            _onResume(ctx);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void onLowMemory() {
        try {
            _onLowMemory();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    @Override
    public void onDestroy(Activity ctx) {
        try {
            _onDestroy(ctx);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            _shutdown();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void setLanguage(Locale locale) {
        try {
            _setLanguage(locale);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public String getLanguage() {
        try {
            return _getLanguage();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    /**
     * @deprecated
     */
    @Override
    public void setLanguage(String language) {
        try {
            _setLanguage(language);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public String getApiKey() {
        try {
            return _getApiKey();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public String getUserId() {
        try {
            return _getUserId();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public JSONObject getDeviceInfo() {
        try {
            return _getDeviceInfo();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void refreshCampaignsAndResources() {
        try {
            _refreshCampaignsAndResources();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public SwrveMessage getMessageForEvent(String event) {
        try {
            return _getMessageForEvent(event);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    public SwrveMessage getMessageForEvent(String event, SwrveOrientation orientation) {
        try {
            return _getMessageForEvent(event, orientation);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public SwrveMessage getMessageForId(int messageId) {
        try {
            return _getMessageForId(messageId);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    public SwrveConversation getConversationForEvent(String event) {
        try {
            return _getConversationForEvent(event);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void buttonWasPressedByUser(SwrveButton button) {
        try {
            _buttonWasPressedByUser(button);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void messageWasShownToUser(SwrveMessageFormat messageFormat) {
        try {
            _messageWasShownToUser(messageFormat);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void conversationEventsCommitedByUser(SwrveCommonConversation conversation, ArrayList<UserInputResult> userInteractions){
        try {
            _queueConversationEventsCommitedByUser(conversation, userInteractions);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void conversationLinkVisitActionCalledByUser(SwrveCommonConversation conversation, String fromPageTag, String toActionTag){
        try {
            _conversationLinkVisitWasAccessedByUser(conversation, fromPageTag, toActionTag);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void conversationDeeplinkActionCalledByUser(SwrveCommonConversation conversation, String fromPageTag, String toActionTag){
        try {
            _conversationDeeplinkWasAccessedByUser(conversation, fromPageTag, toActionTag);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void conversationCallActionCalledByUser(SwrveCommonConversation conversation, String fromPageTag, String toActionTag){
        try {
            _conversationCallWasAccessedByUser(conversation, fromPageTag, toActionTag);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void conversationPageWasViewedByUser(SwrveCommonConversation conversation, String pageTag) {
        try {
            _conversationPageWasViewedByUser(conversation, pageTag);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void conversationWasStartedByUser(SwrveCommonConversation conversation, String pageTag) {
        try {
            _conversationWasStartedByUser(conversation, pageTag);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void conversationWasFinishedByUser(SwrveCommonConversation conversation, String endPageTag, String endControlTag) {
        try {
            _conversationWasFinishedByUser(conversation, endPageTag, endControlTag);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void conversationEncounteredError(SwrveCommonConversation conversation, String currentPageTag, Exception e) {
        try {
            _conversationEncounteredError(conversation, currentPageTag, e);
        } catch (Exception e2) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e2);
        }
    }

    @Override
    public void conversationWasCancelledByUser(SwrveCommonConversation conversation, String finalPageTag) {
        try {
            _conversationWasCancelledByUser(conversation, finalPageTag);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void conversationTransitionedToOtherPage(SwrveCommonConversation conversation, String fromPageTag, String toPageTag, String controlTag) {
        try {
            _conversationTransitionedToOtherPage(conversation, fromPageTag, toPageTag, controlTag);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public String getAppStoreURLForApp(int appId) {
        try {
            return _getAppStoreURLForApp(appId);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public File getCacheDir() {
        try {
            return _getCacheDir();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setMessageListener(ISwrveMessageListener messageListener) {
        try {
            _setMessageListener(messageListener);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    public void setConversationListener(ISwrveConversationListener listener) {
        try {
            _setConversationListener(listener);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void setResourcesListener(ISwrveResourcesListener resourcesListener) {
        try {
            _setResourcesListener(resourcesListener);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public Date getInitialisedTime() {
        try {
            return _getInitialisedTime();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public ISwrveInstallButtonListener getInstallButtonListener() {
        try {
            return _getInstallButtonListener();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setInstallButtonListener(ISwrveInstallButtonListener installButtonListener) {
        try {
            _setInstallButtonListener(installButtonListener);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public ISwrveCustomButtonListener getCustomButtonListener() {
        try {
            return _getCustomButtonListener();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setCustomButtonListener(ISwrveCustomButtonListener customButtonListener) {
        try {
            _setCustomButtonListener(customButtonListener);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public ISwrveDialogListener getDialogListener() {
        try {
            return _getDialogListener();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setDialogListener(ISwrveDialogListener dialogListener) {
        try {
            _setDialogListener(dialogListener);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public Context getContext() {
        try {
            return _getContext();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public C getConfig() {
        try {
            return _getConfig();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }


    @Override
    public List<SwrveBaseCampaign> getMessageCenterCampaigns() {
        return getMessageCenterCampaigns(getDeviceOrientation());
    }

    @Override
    public List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation) {
        List<SwrveBaseCampaign> result = new ArrayList<SwrveBaseCampaign>();
        synchronized (campaigns) {
            for (int i = 0; i < campaigns.size(); i++) {
                SwrveBaseCampaign campaign = campaigns.get(i);
                if (campaign.isMessageCenter()
                        && campaign.getStatus() != SwrveCampaignState.Status.Deleted
                        && campaign.isActive(getNow())
                        && campaign.supportsOrientation(orientation)
                        && campaign.areAssetsReady()) {
                    result.add(campaign);
                }
            }
        }
        return result;
    }

    @Override
    public boolean showMessageCenterCampaign(SwrveBaseCampaign campaign) {
        if (campaign instanceof SwrveCampaign) {
            SwrveCampaign iamCampaign = (SwrveCampaign)campaign;
            if (iamCampaign != null && iamCampaign.getMessages().size() > 0 && messageListener != null) {
                // Display first message in the in-app campaign
                messageListener.onMessage(iamCampaign.getMessages().get(0), true);
                return true;
            } else {
                Log.e(LOG_TAG, "No in-app message or message listener.");
            }
        } else if (campaign instanceof SwrveConversationCampaign) {
            SwrveConversationCampaign conversationCampaign = (SwrveConversationCampaign)campaign;
            if (conversationCampaign != null && conversationCampaign.getConversation() != null && conversationListener != null) {
                conversationListener.onMessage(conversationCampaign.getConversation());
                return true;
            } else {
                Log.e(LOG_TAG, "No conversation campaign or conversation listener.");
            }
        }
        return false;
    }

    @Override
    public void removeMessageCenterCampaign(SwrveBaseCampaign campaign) {
        campaign.setStatus(SwrveCampaignState.Status.Deleted);
        saveCampaignsState();
    }

    /***
     * Implementation of ISwrveCommon methods
     */

    public int getAppId() {
        return appId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public ILocalStorage createLocalStorage() {
        return new SQLiteLocalStorage(context.get(), config.getDbName(), config.getMaxSqliteDbSize());
    }

    @Override
    public String getBatchURL() {
        return getEventsServer() +  BATCH_EVENTS_ACTION;
    }

    public boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void setLocationVersion(int locationVersion) {
        this.locationVersion = locationVersion;
    }

    @Override
    public String getCachedLocationData() {
        ILocalStorage localStorage = null;
        try {
            localStorage = createLocalStorage();
            return localStorage.getSecureCacheEntryForUser(userId, LOCATION_CAMPAIGN_CATEGORY, getUniqueKey());
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Invalid json in cache, cannot load " + LOCATION_CAMPAIGN_CATEGORY + " campaigns", e);
        } finally {
            if (localStorage != null) localStorage.close();
        }

        return null;
    }

    @Override
    public void sendEventWakefully(Context context, final String event) {
        sendEventsWakefully(context, new ArrayList<String>() {{ add(event); }});
    }

    @Override
    public void sendEventsWakefully(Context context, ArrayList<String> events) {
        Intent intent = new Intent(context, SwrveWakefulReceiver.class);
        intent.putStringArrayListExtra(SwrveWakefulService.EXTRA_EVENTS, events);
        context.sendBroadcast(intent);
    }

    @Override
    public String getSessionKey() {
        return this.sessionToken;
    }

    @Override
    public short getDeviceId() {
        return EventHelper.getDeviceId(cachedLocalStorage);
    }

    @Override
    public String getEventsServer() {
        return config.getEventsUrl().toString();
    }

    @Override
    public int getHttpTimeout() {
        return config.getHttpTimeout();
    }

    @Override
    public int getMaxEventsPerFlush() {
        return config.getMaxEventsPerFlush();
    }

    /***
     * eo ISwrveCommon
     */
}
