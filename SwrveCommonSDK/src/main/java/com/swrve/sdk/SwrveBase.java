package com.swrve.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.exceptions.NoUserIdSwrveException;
import com.swrve.sdk.localstorage.ILocalStorage;
import com.swrve.sdk.messaging.ISwrveCustomButtonListener;
import com.swrve.sdk.messaging.ISwrveDialogListener;
import com.swrve.sdk.messaging.ISwrveInstallButtonListener;
import com.swrve.sdk.messaging.ISwrveMessageListener;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveCampaign;
import com.swrve.sdk.messaging.SwrveEventListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main base class implementation of the Swrve SDK.
 */
public abstract class SwrveBase<T, C extends SwrveConfigBase> extends SwrveImp<T, C> implements ISwrveBase<T, C> {

    protected static String _getVersion() {
        return version;
    }

    public static String getVersion() {
        try {
            return _getVersion();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public T onCreate(final Activity activity, final int appId, final String apiKey) throws IllegalArgumentException {
        return onCreate(activity, appId, apiKey, defaultConfig());
    }

    @SuppressWarnings("unchecked")
    @Override
    public T onCreate(final Activity activity, final int appId, final String apiKey, final C config) throws IllegalArgumentException {
        if (destroyed) {
            destroyed = initialised = false;
            bindCounter.set(0);
        }
        if (!initialised) {
            // First time it is initialized
            return init(activity, appId, apiKey, config);
        }
        bindToContext(activity);
        afterBind();
        showPreviousMessage();
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    private T init(final Activity activity, final int appId, final String apiKey, final C config) throws IllegalArgumentException {
        // Initialization checks
        if (activity == null) {
            SwrveHelper.logAndThrowException("Context not specified");
        } else if (SwrveHelper.isNullOrEmpty(apiKey)) {
            SwrveHelper.logAndThrowException("Api key not specified");
        }

        try {
            // Save initialization time
            initialisedTime = getNow();
            initialised = true;
            this.lastSessionTick = getSessionTime();
            this.appId = appId;
            this.apiKey = apiKey;
            this.userId = config.getUserId();
            final Context resolvedContext = bindToContext(activity);
            final boolean preloadRandC = config.isLoadCachedCampaignsAndResourcesOnUIThread();

            // Default user id to Android ID
            if (SwrveHelper.isNullOrEmpty(userId)) {
                userId = getUniqueUserId(resolvedContext);
            }
            checkUserId(userId);
            saveUniqueUserId(resolvedContext, userId);
            Log.i(LOG_TAG, "Your user id is: " + userId);

            // Generate default urls for the given app id
            config.generateUrls(appId);

            if (SwrveHelper.isNullOrEmpty(config.getLanguage())) {
                this.language = SwrveHelper.toLanguageTag(Locale.getDefault());
            } else {
                this.language = config.getLanguage();
            }
            this.config = config;
            this.appVersion = config.getAppVersion();
            this.newSessionInterval = config.getNewSessionInterval();

            // Generate session token
            this.sessionToken = SwrveHelper.generateSessionToken(this.apiKey, this.appId, userId);

            // Default app version to android app version
            if (SwrveHelper.isNullOrEmpty(this.appVersion)) {
                try {
                    PackageInfo pInfo = resolvedContext.getPackageManager().getPackageInfo(resolvedContext.getPackageName(), 0);
                    this.appVersion = pInfo.versionName;
                } catch (Exception exp) {
                    Log.e(LOG_TAG, "Couldn't get app version from PackageManager. Please provide the app version manually through the config object.", exp);
                }
            }

            restClient = createRESTClient();
            cachedLocalStorage = createCachedLocalStorage();
            storageExecutor = createStorageExecutor();
            restClientExecutor = createRESTClientExecutor();

            appStoreURLs = new SparseArray<String>();

            // Find cache folder
            findCacheFolder(resolvedContext);

            beforeSendDeviceInfo(resolvedContext);
            // Open access to local storage
            openLocalStorageConnection();

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
                event("Swrve.first_session");
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
                Log.i(LOG_TAG, "Received install referrer, so sending userUpdate:" + attributes);
                userUpdate(attributes);
                settings.edit().remove(SWRVE_REFERRER_ID).commit();
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
                    // Initialize campaigns from cache
                    initCampaigns();
                }

                // Add custom message listener
                if (messageListener == null) {
                    setMessageListener(new ISwrveMessageListener() {
                        public void onMessage(final SwrveMessage message, boolean firstTime) {
                            if (SwrveBase.this.activityContext != null) {
                                final Activity activity = SwrveBase.this.activityContext.get();
                                if (activity == null) {
                                    Log.e(LOG_TAG, "Can't display a message with a non-Activity context");
                                    return;
                                }
                                // Run code on the UI thread
                                activity.runOnUiThread(new DisplayMessageRunnable(SwrveBase.this, activity, message, firstTime));
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

            Log.i(LOG_TAG, "Init finished");
        } catch (Exception exp) {
            Log.e(LOG_TAG, "Swrve init failed", exp);
        }
        return (T) this;
    }

    protected abstract void beforeSendDeviceInfo(Context context);

    protected abstract void afterInit();

    protected abstract void afterBind();

    protected abstract void extraDeviceInfo(JSONObject deviceInfo) throws JSONException;

    protected abstract C defaultConfig();

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
        event(name, null);
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
            Log.e(LOG_TAG, "JSONException when encoding user attributes", ex);
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
            Log.i(LOG_TAG, "Signature for " + RESOURCES_CACHE_CATEGORY + " invalid; could not retrieve data from cache");
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", "Swrve.signature_invalid");
            queueEvent("event", parameters, null);
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
                        Log.i(LOG_TAG, "Contacting AB Test server " + config.getContentUrl());
                        restClient.get(config.getContentUrl() + USER_RESOURCES_DIFF_ACTION, params, new RESTCacheResponseListener(swrveReference, cachedLocalStorage, userId, RESOURCES_DIFF_CACHE_CATEGORY, EMPTY_JSON_ARRAY) {
                            @Override
                            public void onResponseCached(int responseCode, String responseBody) {
                                Log.i(LOG_TAG, "Got AB Test response code " + responseCode);
                                if (!SwrveHelper.isNullOrEmpty(responseBody)) {
                                    // Process data and launch listener
                                    processUserResourcesDiffData(responseBody, listener);
                                }
                            }

                            @Override
                            public void onException(Exception exp) {
                                // Launch exception
                                Log.e(LOG_TAG, "AB Test exception", exp);
                                listener.onUserResourcesDiffError(exp);
                            }
                        });
                    } catch (Exception exp) {
                        // Launch exception
                        Log.e(LOG_TAG, "AB Test exception", exp);
                        listener.onUserResourcesDiffError(exp);
                    }
                } else {
                    // No user specified...
                    Log.e(LOG_TAG, "Error: No user specified");
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
                    // Get batch of events and send them
                    final LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> combinedEvents = cachedLocalStorage.getCombinedFirstNEvents(config.getMaxEventsPerFlush());
                    final LinkedHashMap<Long, String> events = new LinkedHashMap<Long, String>();
                    if (!combinedEvents.isEmpty()) {
                        Log.i(LOG_TAG, "Sending queued events");
                        try {
                            // Combine all events
                            Iterator<ILocalStorage> storageIt = combinedEvents.keySet().iterator();
                            while (storageIt.hasNext()) {
                                events.putAll(combinedEvents.get(storageIt.next()));
                            }
                            eventsWereSent = true;
                            String data = com.swrve.sdk.EventHelper.eventsAsBatch(userId, appVersion, sessionToken, events, cachedLocalStorage);
                            Log.i(LOG_TAG, "Sending " + events.size() + " events to Swrve");
                            postBatchRequest(config, data, new IPostBatchRequestListener() {
                                public void onResponse(boolean shouldDelete) {
                                    if (shouldDelete) {
                                        // Remove events from where they came from
                                        Iterator<ILocalStorage> storageIt = combinedEvents.keySet().iterator();
                                        while (storageIt.hasNext()) {
                                            ILocalStorage storage = storageIt.next();
                                            storage.removeEventsById(combinedEvents.get(storage).keySet());
                                        }
                                    } else {
                                        Log.e(LOG_TAG, "Batch of events could not be sent, retrying");
                                    }
                                }
                            });
                        } catch (JSONException je) {
                            Log.e(LOG_TAG, "Unable to generate event batch", je);
                        }
                    }
                }
            });
        }
    }

    protected void _flushToDisk() {
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(LOG_TAG, "Flushing to disk");
                    cachedLocalStorage.flush();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Flush to disk failed", e);
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

        Log.i(LOG_TAG, "onPause");
        flushToDisk();
        // Session management
        generateNewSessionInterval();
    }

    protected void _onResume(Activity ctx) {
        Log.i(LOG_TAG, "onResume");
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
                Log.i(LOG_TAG, "Received referrer, so sending userUpdate:" + attributes);
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
            Log.i(LOG_TAG, "Shutting down the SDK");
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
                // Carrier info
                if (!SwrveHelper.isNullOrEmpty(sim_operator_name)) {
                    deviceInfo.put(SWRVE_SIM_OPERATOR_NAME, sim_operator_name);
                }
                if (!SwrveHelper.isNullOrEmpty(sim_operator_iso_country_code)) {
                    deviceInfo.put(SWRVE_SIM_OPERATOR_ISO_COUNTRY, sim_operator_iso_country_code);
                }
                if (!SwrveHelper.isNullOrEmpty(sim_operator_code)) {
                    deviceInfo.put(SWRVE_SIM_OPERATOR_CODE, sim_operator_code);
                }
            } catch (Exception exp) {
                Log.e(LOG_TAG, "Get device screen info failed", exp);
            }
            deviceInfo.put(SWRVE_LANGUAGE, SwrveBase.this.language);
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
                    Log.i(LOG_TAG, "Request to retrieve campaign and user resource data was rate-limited");
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
                    params.put("language", language);
                    params.put("app_store", config.getAppStore());

                    // Device info
                    params.put("device_width", String.valueOf(device_width));
                    params.put("device_height", String.valueOf(device_height));
                    params.put("device_dpi", String.valueOf(device_dpi));
                    params.put("android_device_xdpi", String.valueOf(android_device_xdpi));
                    params.put("android_device_ydpi", String.valueOf(android_device_ydpi));
                    params.put("orientation", config.getOrientation().toString().toLowerCase(Locale.US));
                    params.put("device_name", getDeviceName());
                    params.put("os_version", Build.VERSION.RELEASE);
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
                            if (response.responseCode == HttpStatus.SC_OK) {
                                SharedPreferences settings = context.get().getSharedPreferences(SDK_PREFS_NAME, 0);
                                SharedPreferences.Editor settingsEditor = settings.edit();

                                String etagHeader = response.getHeaderValue("ETag");
                                if (!SwrveHelper.isNullOrEmpty(etagHeader)) {
                                    campaignsAndResourcesLastETag = etagHeader;
                                    settingsEditor.putString("campaigns_and_resources_etag", campaignsAndResourcesLastETag);
                                }

                                try {
                                    JSONObject resourceDict = new JSONObject(response.responseBody);

                                    if (resourceDict.has("flush_frequency")) {
                                        Integer flushFrequency = resourceDict.getInt("flush_frequency");
                                        if (flushFrequency != null) {
                                            campaignsAndResourcesFlushFrequency = flushFrequency;
                                            settingsEditor.putInt("swrve_cr_flush_frequency", campaignsAndResourcesFlushFrequency);
                                        }
                                    }

                                    if (resourceDict.has("flush_refresh_delay")) {
                                        Integer flushDelay = resourceDict.getInt("flush_refresh_delay");
                                        if (flushDelay != null) {
                                            campaignsAndResourcesFlushRefreshDelay = flushDelay;
                                            settingsEditor.putInt("swrve_cr_flush_delay", campaignsAndResourcesFlushRefreshDelay);
                                        }
                                    }

                                    if (config.isTalkEnabled()) {
                                        if (resourceDict.has("campaigns")) {
                                            JSONObject campaignJson = resourceDict.getJSONObject("campaigns");
                                            updateCampaigns(campaignJson, null);
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
                                            queueEvent("event", parameters, payload);
                                        }
                                    }

                                    if (resourceDict.has("user_resources")) {
                                        // Update resource manager
                                        JSONArray resourceJson = resourceDict.getJSONArray("user_resources");
                                        resourceManager.setResourcesFromJSON(resourceJson);
                                        saveResourcesInCache(resourceJson);

                                        // Call resource listener
                                        if (campaignsAndResourcesInitialized) {
                                            invokeResourceListener();
                                        }
                                    }
                                } catch (JSONException e) {
                                    Log.e(LOG_TAG, "Could not parse JSON for campaigns and resources", e);
                                }

                                settingsEditor.commit();
                            }

                            this.firstRefreshFinished();
                        }

                        @Override
                        public void onException(Exception e) {
                            this.firstRefreshFinished();
                            Log.e(LOG_TAG, "Error downloading resources and campaigns", e);
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
                    Log.e(LOG_TAG, "Could not update resources and campaigns, invalid parameters", e);
                }
            }
        });
    }

    protected SwrveMessage _getMessageForEvent(String event) {
        return getMessageForEvent(event, SwrveOrientation.Both);
    }

    @SuppressLint("UseSparseArrays")
    protected SwrveMessage _getMessageForEvent(String event, SwrveOrientation orientation) {
        SwrveMessage result = null;
        SwrveCampaign campaign = null;

        Date now = getNow();
        Map<Integer, String> campaignReasons = null;
        Map<Integer, Integer> campaignMessages = null;

        if (campaigns != null) {
            if (campaigns.size() == 0) {
                noMessagesWereShown(event, "No campaigns available");
                return null;
            }

            // Ignore delay after launch throttle limit for auto show messages
            if (!event.equalsIgnoreCase(SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER) && isTooSoonToShowMessageAfterLaunch(now)) {
                noMessagesWereShown(event, "{App throttle limit} Too soon after launch. Wait until " + timestampFormat.format(showMessagesAfterLaunch));
                return null;
            }

            if (isTooSoonToShowMessageAfterDelay(now)) {
                noMessagesWereShown(event, "{App throttle limit} Too soon after last message. Wait until " + timestampFormat.format(showMessagesAfterDelay));
                return null;
            }

            if (hasShowTooManyMessagesAlready()) {
                noMessagesWereShown(event, "{App Throttle limit} Too many messages shown");
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
                Iterator<SwrveCampaign> itCampaign = campaigns.iterator();
                while (itCampaign.hasNext()) {
                    SwrveCampaign nextCampaign = itCampaign.next();
                    SwrveMessage nextMessage = nextCampaign.getMessageForEvent(event, now, campaignReasons);
                    if (nextMessage != null) {
                        // Add to list of returned messages
                        availableMessages.add(nextMessage);
                        // Check if it is a candidate to be shown
                        if (nextMessage.getPriority() <= minPriority) {
                            minPriority = nextMessage.getPriority();
                            if (nextMessage.getPriority() < minPriority) {
                                candidateMessages.clear();
                            }
                            candidateMessages.add(nextMessage);
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
            Log.w(LOG_TAG, "Not showing message: no candidate messages for " + event);
        } else {
            // Notify message has been returned
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("id", String.valueOf(result.getId()));
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", "Swrve.Messages.message_returned");
            queueEvent("event", parameters, payload);
        }

        return result;
    }

    protected SwrveMessage _getMessageForId(int messageId) {
        SwrveMessage result = null;

        if (campaigns != null && campaigns.size() > 0) {
            synchronized (campaigns) {
                Iterator<SwrveCampaign> itCampaign = campaigns.iterator();
                while (itCampaign.hasNext() && result == null) {
                    SwrveCampaign campaign = itCampaign.next();
                    result = campaign.getMessageForId(messageId);
                }
            }
        }

        if (result == null) {
            Log.i(LOG_TAG, "Not showing messages: no candidate messages");
        }

        return result;
    }

    protected void _buttonWasPressedByUser(SwrveButton button) {
        if (button.getActionType() != SwrveActionType.Dismiss) {
            String clickEvent = "Swrve.Messages.Message-" + button.getMessage().getId() + ".click";
            Log.i(LOG_TAG, "Sending click event: " + clickEvent + "(" + button.getName() + ")");
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("name", button.getName());
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", clickEvent);
            queueEvent("event", parameters, payload);
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

    protected void _messageWasShownToUser(SwrveMessageFormat messageFormat) {
        if (messageFormat != null) {
            setMessageMinDelayThrottle();
            this.messagesLeftToShow = this.messagesLeftToShow - 1;

            // Update next for round robin
            SwrveMessage message = messageFormat.getMessage();
            SwrveCampaign campaign = message.getCampaign();
            if (campaign != null) {
                campaign.messageWasShownToUser(messageFormat);
            }

            String viewEvent = "Swrve.Messages.Message-" + message.getId() + ".impression";
            Log.i(LOG_TAG, "Sending view event: " + viewEvent);
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("format", messageFormat.getName());
            payload.put("orientation", messageFormat.getOrientation().name());
            payload.put("size", messageFormat.getSize().x + "x" + messageFormat.getSize().y);
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", viewEvent);
            queueEvent("event", parameters, payload);
            saveCampaignSettings();
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
            eventListener = new SwrveEventListener(this, messageListener);
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
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void sessionEnd() {
        try {
            _sessionEnd();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void event(String name) {
        try {
            _event(name);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void event(String name, Map<String, String> payload) {
        try {
            _event(name, payload);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void purchase(String item, String currency, int cost, int quantity) {
        try {
            _purchase(item, currency, cost, quantity);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void currencyGiven(String givenCurrency, double givenAmount) {
        try {
            _currencyGiven(givenCurrency, givenAmount);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void userUpdate(Map<String, String> attributes) {
        try {
            _userUpdate(attributes);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void iap(int quantity, String productId, double productPrice, String currency) {
        try {
            _iap(quantity, productId, productPrice, currency);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards) {
        try {
            _iap(quantity, productId, productPrice, currency, rewards);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public SwrveResourceManager getResourceManager() {
        try {
            return _getResourceManager();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void getUserResources(final ISwrveUserResourcesListener listener) {
        try {
            _getUserResources(listener);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void getUserResourcesDiff(final ISwrveUserResourcesDiffListener listener) {
        try {
            _getUserResourcesDiff(listener);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void sendQueuedEvents() {
        try {
            _sendQueuedEvents();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void flushToDisk() {
        try {
            _flushToDisk();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void onPause() {
        try {
            _onPause();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void onResume(Activity ctx) {
        try {
            _onResume(ctx);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void onLowMemory() {
        try {
            _onLowMemory();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void onDestroy(Activity ctx) {
        try {
            _onDestroy(ctx);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            _shutdown();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void setLanguage(Locale locale) {
        try {
            _setLanguage(locale);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public String getLanguage() {
        try {
            return _getLanguage();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
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
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public String getApiKey() {
        try {
            return _getApiKey();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public String getUserId() {
        try {
            return _getUserId();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public JSONObject getDeviceInfo() {
        try {
            return _getDeviceInfo();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void refreshCampaignsAndResources() {
        try {
            _refreshCampaignsAndResources();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public SwrveMessage getMessageForEvent(String event) {
        try {
            return _getMessageForEvent(event);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    public SwrveMessage getMessageForEvent(String event, SwrveOrientation orientation) {
        try {
            return _getMessageForEvent(event, orientation);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public SwrveMessage getMessageForId(int messageId) {
        try {
            return _getMessageForId(messageId);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void buttonWasPressedByUser(SwrveButton button) {
        try {
            _buttonWasPressedByUser(button);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void messageWasShownToUser(SwrveMessageFormat messageFormat) {
        try {
            _messageWasShownToUser(messageFormat);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public String getAppStoreURLForApp(int appId) {
        try {
            return _getAppStoreURLForApp(appId);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public File getCacheDir() {
        try {
            return _getCacheDir();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setMessageListener(ISwrveMessageListener messageListener) {
        try {
            _setMessageListener(messageListener);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void setResourcesListener(ISwrveResourcesListener resourcesListener) {
        try {
            _setResourcesListener(resourcesListener);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public Date getInitialisedTime() {
        try {
            return _getInitialisedTime();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public ISwrveInstallButtonListener getInstallButtonListener() {
        try {
            return _getInstallButtonListener();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setInstallButtonListener(ISwrveInstallButtonListener installButtonListener) {
        try {
            _setInstallButtonListener(installButtonListener);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public ISwrveCustomButtonListener getCustomButtonListener() {
        try {
            return _getCustomButtonListener();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setCustomButtonListener(ISwrveCustomButtonListener customButtonListener) {
        try {
            _setCustomButtonListener(customButtonListener);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public ISwrveDialogListener getDialogListener() {
        try {
            return _getDialogListener();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setDialogListener(ISwrveDialogListener dialogListener) {
        try {
            _setDialogListener(dialogListener);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public Context getContext() {
        try {
            return _getContext();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public C getConfig() {
        try {
            return _getConfig();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }
}
