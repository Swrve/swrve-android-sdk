package com.swrve.sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

import com.swrve.sdk.SwrveCampaignDisplayer.Result;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.conversations.SwrveConversationListener;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.exceptions.NoUserIdSwrveException;
import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveMessageListener;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveConversationCampaign;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.ui.SwrveInAppMessageActivity;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.swrve.sdk.SwrveCampaignDisplayer.DisplayResult.CAMPAIGN_WRONG_ORIENTATION;
import static com.swrve.sdk.SwrveCampaignDisplayer.DisplayResult.ELIGIBLE_BUT_OTHER_CHOSEN;

/**
 * Main base class implementation of the Swrve SDK.
 */
public abstract class SwrveBase<T, C extends SwrveConfigBase> extends SwrveImp<T, C> implements ISwrveBase<T, C>, ISwrveCommon, ISwrveConversationSDK {

    protected SwrveBase(Application application, int appId, String apiKey, C config) {
        super(application, appId, apiKey, config);
        SwrveCommon.setSwrveCommon(this);
    }

    protected static String _getVersion() {
        return version;
    }

    public static String getVersion() {
        try {
            return _getVersion();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    protected T onCreate(final Activity activity) throws IllegalArgumentException {
        if (destroyed) {
            destroyed = initialised = false;
            bindCounter.set(0);
        }
        if (!initialised) {
            // First time it is initialized
            return init(activity);
        }
        bindToContext(activity);
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
            final String userId = profileManager.getUserId();
            initialisedTime = getNow();
            initialised = true;
            lastSessionTick = getSessionTime();

            appStoreURLs = new SparseArray<>();

            initCacheFolder(activity);

            // Open access to local storage
            openLocalStorageConnection();

            // Obtain extra device info
            beforeSendDeviceInfo(resolvedContext);

            if (this.resourceManager == null) {
                this.resourceManager = new SwrveResourceManager();
            }

            if (preloadRandC) {
                // Initialize resources from cache
                initResources(userId);
            }

            queueSessionStart();
            generateNewSessionInterval();
            initInstallTimeAndCheckFirstSession();

            // Android referrer information
            SharedPreferences settings = activity.getSharedPreferences(SDK_PREFS_NAME, 0);
            String referrer = settings.getString(SWRVE_REFERRER_ID, null);
            if (!SwrveHelper.isNullOrEmpty(referrer)) {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(SWRVE_REFERRER_ID, referrer);
                SwrveLogger.i("Received install referrer, so sending userUpdate:%s", attributes);
                userUpdate(attributes);
                settings.edit().remove(SWRVE_REFERRER_ID).apply();
            }

            // Get device info
            getDeviceInfo(resolvedContext);
            queueDeviceInfoNow(userId, profileManager.getSessionToken(), true);

            // Messaging initialization

            if (SwrveHelper.isNullOrEmpty(language)) {
                SwrveHelper.logAndThrowException("Language needed to use Talk");
            } else if (SwrveHelper.isNullOrEmpty(config.getAppStore())) {
                SwrveHelper.logAndThrowException("App store needed to use Talk");
            }

            if (preloadRandC) {
                initCampaigns(userId); // Initialize campaigns from cache
            }

            // Add default message listener
            if (messageListener == null) {
                setDefaultMessageListener();
            }


            // Add custom conversation listener
            if (conversationListener == null) {
                setConversationListener(new SwrveConversationListener() {
                        public void onMessage(final SwrveConversation conversation) {
                            // Start a Conversation activity to display the campaign
                            if (SwrveBase.this.context != null) {
                                ConversationActivity.showConversation(SwrveBase.this.context.get(), conversation, config.getOrientation());
                                conversation.getCampaign().messageWasShownToUser(); // Report that the conversation was shown to the user
                            }
                        }
                    });
            }


            if (preloadRandC && config.isABTestDetailsEnabled()) {
                initABTestDetails(userId);
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
                        initResources(userId);

                        initCampaigns(userId);

                        if (config.isABTestDetailsEnabled()) {
                            initABTestDetails(userId);
                        }
                    }
                });
            }

            sendCrashlyticsMetadata();

            SwrveLogger.i("Init finished");
        } catch (Exception exp) {
            SwrveLogger.e("Swrve init failed", exp);
        }
        return (T) this;
    }

    private void initCacheFolder(Activity activity) {
        File cacheDir = config.getCacheDir();

        if (cacheDir == null) {
            cacheDir = activity.getCacheDir();
        } else {
            if (!checkPermissionGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(activity, permissions);
                cacheDir = activity.getCacheDir(); // fall back to internal cache until permission granted.
            }

            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        }
        swrveAssetsManager.setStorageDir(cacheDir);
        SwrveLogger.d("Using cache directory at %s", cacheDir.getPath());
    }

    private void initInstallTimeAndCheckFirstSession() {
        String installTimeRaw = multiLayerLocalStorage.getCacheEntry(profileManager.getUserId(), CACHE_INSTALL_TIME);
        if (SwrveHelper.isNullOrEmpty(installTimeRaw)) {
            installTime = initialisedTime.getTime();
            multiLayerLocalStorage.setCacheEntry(profileManager.getUserId(), CACHE_INSTALL_TIME, String.valueOf(installTime)); // Save to memory and secondary storage
            _event("Swrve.first_session"); // First time we see this user
            userInstallTime = installTimeFormat.format(initialisedTime);
        } else {
            installTime = Long.parseLong(installTimeRaw);
        }
    }

    private void setDefaultMessageListener() {
        setMessageListener(new SwrveMessageListener() {
            public void onMessage(final SwrveMessage message) {
                // Start a Conversation activity to display the campaign
                if (SwrveBase.this.context != null) {
                    final Context ctx = SwrveBase.this.context.get();
                    if (ctx == null) {
                        SwrveLogger.e("Can't display a in-app message without a context");
                        return;
                    }

                    if (message.supportsOrientation(getDeviceOrientation())) {
                        Intent intent = new Intent(ctx, SwrveInAppMessageActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, message.getId());
                        ctx.startActivity(intent);
                    } else {
                        SwrveLogger.i("Can't display the in-app message as it doesn't support the current orientation");
                    }
                }
            }
        });
    }

    protected void openLocalStorageConnection() {
        try {
            SQLiteLocalStorage sqLiteLocalStorage = new SQLiteLocalStorage(context.get(), config.getDbName(), config.getMaxSqliteDbSize());;
            multiLayerLocalStorage.setSecondaryStorage(sqLiteLocalStorage);
        } catch (Exception exp) {
            SwrveLogger.e("Error opening database", exp);
        }
    }

    protected abstract void beforeSendDeviceInfo(Context context);

    protected abstract void extraDeviceInfo(JSONObject deviceInfo) throws JSONException;

    protected void _sessionStart() {
        queueSessionStart();
        final String userId = profileManager.getUserId();
        final String sessionToken = profileManager.getSessionToken();
        // Send event after it has been queued
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                _sendQueuedEvents(userId, sessionToken);
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
            SwrveLogger.e("JSONException when encoding user attributes", ex);
        }

        queueEvent("user", parameters, null);
    }

    protected void _userUpdate (String name, Date date) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        Map<String, String> resultAttributes = new HashMap<String, String>();

        String resultantDate = getStringFromDate(date);
        resultAttributes.put(name, resultantDate);

        try {
            parameters.put("attributes", new JSONObject(resultAttributes));
        }catch (NullPointerException ex) {
            SwrveLogger.e("JSONException when encoding user attributes", ex);
            return;
        }

        queueEvent("user", parameters, null);
    }

    private String getStringFromDate(Date date) {

        TimeZone timezone = TimeZone.getTimeZone("UTC");
        DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateformat.setTimeZone(timezone);

        return dateformat.format(date);
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

    protected void _getUserResources(final SwrveUserResourcesListener listener) {
        String cachedResources = null;

        // Read cached resources
        try {
            cachedResources = multiLayerLocalStorage.getSecureCacheEntryForUser(profileManager.getUserId(), CACHE_RESOURCES, getUniqueKey(getUserId()));
        } catch (SecurityException e) {

            SwrveLogger.i("Signature for %s invalid; could not retrieve data from cache", CACHE_RESOURCES);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "Swrve.signature_invalid");
            queueEvent(profileManager.getUserId(), "event", parameters, null, false);
            listener.onUserResourcesError(e);
        }

        if (!SwrveHelper.isNullOrEmpty(cachedResources)) {
            try {
                // Parse raw response
                JSONArray jsonResources = new JSONArray(cachedResources);
                // Convert to map
                Map<String, Map<String, String>> mapResources = new HashMap<>();
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

    protected void _getUserResourcesDiff(final SwrveUserResourcesDiffListener listener) {
        final SwrveBase<?, ?> swrveReference = this;
        final String userId = getUserId();
        restClientExecutorExecute(new Runnable() {
            @Override
            public void run() {
                if (profileManager.getUserId() != null) {
                    Map<String, String> params = new HashMap<>();
                    params.put("api_key", apiKey);
                    params.put("user", profileManager.getUserId());
                    params.put("app_version", appVersion);
                    params.put("joined", String.valueOf(installTime));
                    try {
                        SwrveLogger.i("Contacting AB Test server %s", config.getContentUrl());
                        restClient.get(config.getContentUrl() + USER_RESOURCES_DIFF_ACTION, params, new RESTCacheResponseListener(swrveReference, multiLayerLocalStorage, userId, CACHE_RESOURCES_DIFF, EMPTY_JSON_ARRAY) {
                            @Override
                            public void onResponseCached(int responseCode, String responseBody) {
                                SwrveLogger.i("Got AB Test response code %s", responseCode);
                                if (!SwrveHelper.isNullOrEmpty(responseBody)) {
                                    // Process data and launch listener
                                    processUserResourcesDiffData(responseBody, listener);
                                }
                            }

                            @Override
                            public void onException(Exception exp) {
                                // Launch exception
                                SwrveLogger.e("AB Test exception", exp);
                                listener.onUserResourcesDiffError(exp);
                            }
                        });
                    } catch (Exception exp) {
                        // Launch exception
                        SwrveLogger.e("AB Test exception", exp);
                        listener.onUserResourcesDiffError(exp);
                    }
                } else {
                    // No user specified...
                    SwrveLogger.e("Error: No user specified");
                    listener.onUserResourcesDiffError(new NoUserIdSwrveException());
                }
            }
        });
    }

    protected void _sendQueuedEvents(final String userId, final String sessionToken) {
        if (SwrveHelper.isNotNullOrEmpty(userId) && SwrveHelper.isNotNullOrEmpty(sessionToken)) {
            restClientExecutorExecute(new Runnable() {
                @Override
                public void run() {
                    short deviceId = getDeviceId();
                    SwrveEventsManager swrveEventsManager = new SwrveEventsManagerImp(config, restClient, userId, appVersion, sessionToken, deviceId);
                    swrveEventsManager.sendStoredEvents(multiLayerLocalStorage);
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
                    SwrveLogger.i("Flushing to disk");
                    if (multiLayerLocalStorage != null) {
                        multiLayerLocalStorage.flush();
                    }
                } catch (Exception e) {
                    SwrveLogger.e("Flush to disk failed", e);
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

        SwrveLogger.i("onPause");
        flushToDisk();
        // Session management
        generateNewSessionInterval();
        // Save campaign state if needed
        saveCampaignsState(profileManager.getUserId());
    }

    protected void _onResume(Activity activity) {
        SwrveLogger.i("onResume");
        if (activity != null) {
            bindToContext(activity);
        }
        startCampaignsAndResourcesTimer(true);
        disableAutoShowAfterDelay();

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
    }

    protected void _onDestroy(Activity ctx) {
        unbindAndShutdown();
    }

    protected void _shutdown() throws InterruptedException {
        if (!destroyed) {
            SwrveLogger.i("Shutting down the SDK");
            destroyed = true;

            // Forget the initialised time
            initialisedTime = null;

            // Remove the binding to the current activity, if any
            this.activityContext = null;

            // Remove QA user from push notification listener
            if (qaUser != null) {
                try {
                    qaUser.unbindToServices();
                }
                catch (Exception e) {
                    SwrveLogger.e("Exception occurred unbinding services from qaUser", e);
                }
                qaUser = null;
            }

            // Do not accept any more jobs but try to finish sending data
            if(restClientExecutor != null) {
                try {
                    restClientExecutor.shutdown();
                    restClientExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    SwrveLogger.e("Exception occurred shutting down restClientExecutor", e);
                }
            }
            if(storageExecutor != null) {
                try {
                    storageExecutor.shutdown();
                    storageExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    SwrveLogger.e("Exception occurred shutting down storageExecutor", e);
                }
            }
            if(campaignsAndResourcesExecutor != null) {
                try {
                    campaignsAndResourcesExecutor.shutdown();
                } catch (Exception e) {
                    SwrveLogger.e("Exception occurred shutting down campaignsAndResourcesExecutor", e);
                }
            }
        }
    }

    protected void _setLanguage(Locale locale) {
        this.language = SwrveHelper.toLanguageTag(locale);
    }

    protected String _getLanguage() {
        return language;
    }

    protected String _getApiKey() {
        return this.apiKey;
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
                deviceInfo.put(SWRVE_CONVERSATION_VERSION, ISwrveConversationSDK.CONVERSATION_VERSION);
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
                SwrveLogger.e("Get device screen info failed", exp);
            }
            deviceInfo.put(SWRVE_LANGUAGE, SwrveBase.this.language);
            String deviceRegion = Locale.getDefault().getCountry();
            deviceInfo.put(SWRVE_DEVICE_REGION, deviceRegion);
            deviceInfo.put(SWRVE_SDK_VERSION, PLATFORM + version);
            deviceInfo.put(SWRVE_APP_STORE, config.getAppStore());
            deviceInfo.put(SWRVE_SDK_FLAVOUR, Swrve.FLAVOUR_NAME);
            Calendar cal = new GregorianCalendar();
            deviceInfo.put(SWRVE_TIMEZONE_NAME, cal.getTimeZone().getID());
            deviceInfo.put(SWRVE_UTC_OFFSET_SECONDS, cal.getTimeZone().getOffset(System.currentTimeMillis()) / 1000);

            if (!SwrveHelper.isNullOrEmpty(userInstallTime)) {
                deviceInfo.put(SWRVE_INSTALL_DATE, userInstallTime);
            }

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(contextRef);
            boolean notificationsEnabled = notificationManagerCompat.areNotificationsEnabled();
            deviceInfo.put(SWRVE_NOTIFICATIONS_ENABLED, notificationsEnabled);
            int notificationsImportance = notificationManagerCompat.getImportance();
            deviceInfo.put(SWRVE_NOTIFICATIONS_IMPORTANCE, notificationsImportance);

            if(notificationsEnabled) {
                // Rich Push Capabilities
                deviceInfo.put(SWRVE_NOTIFICATIONS_BUTTONS, true);
                deviceInfo.put(SWRVE_NOTIFICATIONS_ATTACHMENT, true);
            }
        }

        extraDeviceInfo(deviceInfo);
        return deviceInfo;
    }

    protected void _refreshCampaignsAndResources() {
        // When campaigns need to be downloaded manually, enforce max. flush frequency
        if (!config.isAutoDownloadCampaignsAndResources()) {
            Date now = getNow();
            if (campaignsAndResourcesLastRefreshed != null) {
                Date nextAllowedTime = new Date(campaignsAndResourcesLastRefreshed.getTime() + campaignsAndResourcesFlushFrequency);
                if (now.compareTo(nextAllowedTime) < 0) {
                    SwrveLogger.i("Request to retrieve campaign and user resource data was rate-limited");
                    return;
                }
            }
            campaignsAndResourcesLastRefreshed = now;
        }

        final String userId = getUserId(); // user can logout or change so retrieve now as a final String for thread safeness
        restClientExecutorExecute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = new HashMap<>();

                // General params
                params.put("api_key", apiKey);
                params.put("user", userId);
                params.put("app_version", appVersion);
                params.put("joined", String.valueOf(installTime));


                // Talk only params
                params.put("version", String.valueOf(CAMPAIGN_ENDPOINT_VERSION));
                params.put("conversation_version", String.valueOf(ISwrveConversationSDK.CONVERSATION_VERSION));
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


                if (locationSegmentVersion > 0) {
                    params.put("location_version", String.valueOf(locationSegmentVersion));
                }

                if (config.isABTestDetailsEnabled()) {
                    params.put("ab_test_details", "1");
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
                                    JSONObject responseJson;
                                    try {
                                        responseJson = new JSONObject(response.responseBody);
                                    } catch(JSONException e) {
                                        SwrveLogger.e("SwrveSDK unable to decode user_resources_and_campaigns JSON : \"%s\".", response.responseBody);
                                        throw e;
                                    }

                                    if (responseJson.has("flush_frequency")) {
                                        Integer flushFrequency = responseJson.getInt("flush_frequency");
                                        campaignsAndResourcesFlushFrequency = flushFrequency;
                                        settingsEditor.putInt("swrve_cr_flush_frequency", campaignsAndResourcesFlushFrequency);
                                    }

                                    if (responseJson.has("flush_refresh_delay")) {
                                        Integer flushDelay = responseJson.getInt("flush_refresh_delay");
                                        campaignsAndResourcesFlushRefreshDelay = flushDelay;
                                        settingsEditor.putInt("swrve_cr_flush_delay", campaignsAndResourcesFlushRefreshDelay);
                                    }

                                    if (responseJson.has("campaigns")) {
                                        JSONObject campaignJson = responseJson.getJSONObject("campaigns");
                                        saveCampaignsInCache(campaignJson);
                                        loadCampaignsFromJSON(userId, campaignJson, campaignsState);
                                        autoShowMessages();

                                        // Notify campaigns have been downloaded
                                        Map<String, String> payload = new HashMap<>();
                                        StringBuilder campaignIds = new StringBuilder();
                                        for (int i = 0; i < campaigns.size(); i++) {
                                            if (i != 0) {
                                                campaignIds.append(',');
                                            }
                                            campaignIds.append(campaigns.get(i).getId());
                                        }
                                        payload.put("ids", campaignIds.toString());
                                        payload.put("count", String.valueOf(campaigns.size()));
                                        Map<String, Object> parameters = new HashMap<>();
                                        parameters.put("name", "Swrve.Messages.campaigns_downloaded");
                                        queueEvent(userId, "event", parameters, payload, false);


                                        if (resourceManager != null && campaignJson.has("ab_test_details")) {
                                            JSONObject abTestDetailsJson = campaignJson.optJSONObject("ab_test_details");
                                            if (abTestDetailsJson != null) {
                                                resourceManager.setABTestDetailsFromJSON(abTestDetailsJson);
                                            }
                                        }
                                    }

                                    // If json contains location campaigns then save it to sqlite cache to be used by the location sdk
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
                                    SwrveLogger.e("Could not parse JSON for campaigns and resources", e);
                                }

                                settingsEditor.apply();
                            }

                            this.firstRefreshFinished();
                        }

                        @Override
                        public void onException(Exception e) {
                            this.firstRefreshFinished();
                            SwrveLogger.e("Error downloading resources and campaigns", e);
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
                    SwrveLogger.e("Could not update resources and campaigns, invalid parameters", e);
                }
            }
        });
    }

    @SuppressLint("UseSparseArrays")
    protected SwrveConversation _getConversationForEvent(String event, Map<String, String> payload) {
        SwrveConversation result = null;
        SwrveInAppCampaign campaign = null;

        Date now = getNow();
        Map<Integer, Result> campaignDisplayResults = null;
        Map<Integer, Integer> campaignMessages = null;

        if (campaigns != null) {
            if (!campaignDisplayer.checkAppCampaignRules(campaigns.size(), "conversation", event, now)) {
                return null;
            }
            if (qaUser != null) {
                campaignDisplayResults = new HashMap<>();
                campaignMessages = new HashMap<>();
            }
            synchronized (campaigns) {
                List<SwrveConversation> availableConversations = new ArrayList<>();
                // Select messages with higher priority
                int minPriority = Integer.MAX_VALUE;
                List<SwrveConversation> candidateConversations = new ArrayList<>();
                Iterator<SwrveBaseCampaign> itCampaign = campaigns.iterator();
                while (itCampaign.hasNext()) {
                    SwrveBaseCampaign nextCampaign = itCampaign.next();
                    if (nextCampaign instanceof SwrveConversationCampaign) {
                        SwrveConversation nextConversation = ((SwrveConversationCampaign)nextCampaign).getConversationForEvent(event, payload, now, campaignDisplayResults);
                        if (nextConversation != null) {
                            // Add to list of returned messages
                            availableConversations.add(nextConversation);
                            // Check if it is a candidate to be shown
                            if (nextConversation.getPriority() <= minPriority) {
                                if (nextConversation.getPriority() < minPriority) {
                                    // If it is lower than any of the previous ones
                                    // remove those from being candidates
                                    candidateConversations.clear();
                                }
                                minPriority = nextConversation.getPriority();
                                candidateConversations.add(nextConversation);
                            }
                        }
                    }
                }
                if (candidateConversations.size() > 0) {
                    // Select randomly
                    Collections.shuffle(candidateConversations);
                    result = candidateConversations.get(0);
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
                                String resultText = "Campaign " + campaign.getId() + " was selected for display ahead of this campaign";
                                campaignDisplayResults.put(otherCampaignId, campaignDisplayer.buildResult(ELIGIBLE_BUT_OTHER_CHOSEN, resultText));
                            }
                        }
                    }
                }
            }
        }

        // If QA enabled, send message selection information
        if (qaUser != null) {
            qaUser.trigger(event, result, campaignDisplayResults, campaignMessages);
        }

        if (result == null) {
            SwrveLogger.w("Not showing message: no candidate messages for %s", event);
        } else {
            // Notify message has been returned
            Map<String, String> eventPayload = new HashMap<>();
            eventPayload.put("id", String.valueOf(result.getId()));
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "Swrve.Conversations.conversation_returned");
            queueEvent(profileManager.getUserId(), "event", parameters, eventPayload, false);
        }

        return result;
    }

    @SuppressLint("UseSparseArrays")
    protected SwrveMessage _getMessageForEvent(String event, Map<String, String> payload, SwrveOrientation orientation) {
        SwrveMessage result = null;
        SwrveInAppCampaign campaign = null;

        Date now = getNow();
        Map<Integer, Result> campaignDisplayResults = null;
        Map<Integer, Integer> campaignMessages = null;

        if (campaigns != null) {
            if (!campaignDisplayer.checkAppCampaignRules(campaigns.size(), "message", event, now)) {
                return null;
            }
            if (qaUser != null) {
                campaignDisplayResults = new HashMap<>();
                campaignMessages = new HashMap<>();
            }
            synchronized (campaigns) {
                List<SwrveMessage> availableMessages = new ArrayList<>();
                // Select messages with higher priority
                int minPriority = Integer.MAX_VALUE;
                List<SwrveMessage> candidateMessages = new ArrayList<>();
                Iterator<SwrveBaseCampaign> itCampaign = campaigns.iterator();
                while (itCampaign.hasNext()) {
                    SwrveBaseCampaign nextCampaign = itCampaign.next();
                    if (nextCampaign instanceof SwrveInAppCampaign) {
                        SwrveMessage nextMessage = ((SwrveInAppCampaign)nextCampaign).getMessageForEvent(event, payload, now, campaignDisplayResults);
                        if (nextMessage != null) {
                            // Add to list of returned messages
                            availableMessages.add(nextMessage);
                            // Check if it is a candidate to be shown
                            if (nextMessage.getPriority() <= minPriority) {
                                if (nextMessage.getPriority() < minPriority) {
                                    // If it is lower than any of the previous ones remove those from being candidates
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
                            String resultText = "Message didn't support the given orientation: " + orientation;
                            campaignDisplayResults.put(campaignId, campaignDisplayer.buildResult(CAMPAIGN_WRONG_ORIENTATION, resultText));
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
                                String resultText = "Campaign " + campaign.getId() + " was selected for display ahead of this campaign";
                                campaignDisplayResults.put(otherCampaignId, campaignDisplayer.buildResult(ELIGIBLE_BUT_OTHER_CHOSEN, resultText));
                            }
                        }
                    }
                }
            }
        }

        // If QA enabled, send message selection information
        if (qaUser != null) {
            qaUser.trigger(event, result, campaignDisplayResults, campaignMessages);
        }

        if (result == null) {
            SwrveLogger.w("Not showing message: no candidate messages for %s", event);
        } else {
            // Notify message has been returned
            Map<String, String> eventPayload = new HashMap<>();
            eventPayload.put("id", String.valueOf(result.getId()));
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "Swrve.Messages.message_returned");
            queueEvent(profileManager.getUserId(), "event", parameters, eventPayload, false);
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
                    if (campaign instanceof SwrveInAppCampaign) {
                        result = ((SwrveInAppCampaign)campaign).getMessageForId(messageId);
                    }
                }
            }
        }

        if (result == null) {
            SwrveLogger.i("Not showing messages: no candidate messages");
        }

        return result;
    }

    protected void _buttonWasPressedByUser(SwrveButton button) {
        if (button.getActionType() != SwrveActionType.Dismiss) {
            String clickEvent = "Swrve.Messages.Message-" + button.getMessage().getId() + ".click";
            SwrveLogger.i("Sending click event: %s(%s)", clickEvent, button.getName());
            Map<String, String> payload = new HashMap<>();
            payload.put("name", button.getName());
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", clickEvent);
            queueEvent(profileManager.getUserId(), "event", parameters, payload, false);
        }
    }

    protected void _messageWasShownToUser(SwrveMessageFormat messageFormat) {
        if (messageFormat != null) {
            campaignDisplayer.setMessageMinDelayThrottle(getNow());
            campaignDisplayer.decrementMessagesLeftToShow();

            // Update next for round robin
            SwrveMessage message = messageFormat.getMessage();
            SwrveInAppCampaign campaign = message.getCampaign();
            if (campaign != null) {
                campaign.messageWasShownToUser();
            }

            String viewEvent = "Swrve.Messages.Message-" + message.getId() + ".impression";
            SwrveLogger.i("Sending view event: %s" + viewEvent);
            Map<String, String> payload = new HashMap<>();
            payload.put("format", messageFormat.getName());
            payload.put("orientation", messageFormat.getOrientation().name());
            payload.put("size", messageFormat.getSize().x + "x" + messageFormat.getSize().y);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", viewEvent);
            queueEvent(profileManager.getUserId(), "event", parameters, payload, false);
            saveCampaignsState(profileManager.getUserId());
        }
    }

    protected String _getAppStoreURLForApp(int appId) {
        return appStoreURLs.get(appId);
    }

    protected File _getCacheDir() {
        return swrveAssetsManager.getStorageDir();
    }

    protected void _setMessageListener(SwrveMessageListener messageListener) {
        this.messageListener = messageListener;
        if (messageListener != null) {
            eventListener = new SwrveEventListener(this, messageListener, conversationListener);
        } else {
            eventListener = null;
        }
    }

    protected void _setConversationListener(SwrveConversationListener listener) {
        this.conversationListener = listener;
        if (conversationListener != null) {
            eventListener = new SwrveEventListener(this, messageListener, conversationListener);
        } else {
            eventListener = null;
        }
    }


    protected void _setResourcesListener(SwrveResourcesListener resourcesListener) {
        this.resourcesListener = resourcesListener;
    }

    protected Date _getInitialisedTime() {
        return initialisedTime;
    }

    protected void _setInstallButtonListener(SwrveInstallButtonListener installButtonListener) {
        this.installButtonListener = installButtonListener;
    }

    protected SwrveInstallButtonListener _getInstallButtonListener() {
        return installButtonListener;
    }

    protected void _setCustomButtonListener(SwrveCustomButtonListener customButtonListener) {
        this.customButtonListener = customButtonListener;
    }

    protected SwrveCustomButtonListener _getCustomButtonListener() {
        return customButtonListener;
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
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void sessionEnd() {
        try {
            _sessionEnd();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void event(String name) {
        try {
            if(isValidEventName(name)) {
                _event(name);
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void event(String name, Map<String, String> payload) {
        try {
            if(isValidEventName(name)) {
                _event(name, payload);
            }
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    private boolean isValidEventName(String name) {
        List<String> restrictedNamesStartWith = new ArrayList<String>() {{
            add("Swrve.");
            add("swrve.");
        }};

        for(String restricted: restrictedNamesStartWith) {
            if(name==null || name.startsWith(restricted)) {
                SwrveLogger.e("Event names cannot begin with %s* This event will not be sent. Eventname:%s", restricted, name);
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
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void currencyGiven(String givenCurrency, double givenAmount) {
        try {
            _currencyGiven(givenCurrency, givenAmount);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void userUpdate(Map<String, String> attributes) {
        try {
            _userUpdate(attributes);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void userUpdate(String name, Date date) {
        try {
            _userUpdate(name, date);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void iap(int quantity, String productId, double productPrice, String currency) {
        try {
            _iap(quantity, productId, productPrice, currency);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards) {
        try {
            _iap(quantity, productId, productPrice, currency, rewards);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public SwrveResourceManager getResourceManager() {
        try {
            return _getResourceManager();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void getUserResources(final SwrveUserResourcesListener listener) {
        try {
            _getUserResources(listener);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void getUserResourcesDiff(final SwrveUserResourcesDiffListener listener) {
        try {
            _getUserResourcesDiff(listener);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void sendQueuedEvents() {
        try {
            final String userId = profileManager.getUserId();
            final String sessionToken = profileManager.getSessionToken();
            _sendQueuedEvents(userId, sessionToken);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void flushToDisk() {
        try {
            _flushToDisk();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    protected void onPause() {
        try {
            _onPause();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    protected void onResume(Activity ctx) {
        try {
            _onResume(ctx);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    protected void onDestroy(Activity ctx) {
        try {
            _onDestroy(ctx);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void shutdown() {
        try {
            _shutdown();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void setLanguage(Locale locale) {
        try {
            _setLanguage(locale);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public String getLanguage() {
        try {
            return _getLanguage();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public String getApiKey() {
        try {
            return _getApiKey();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public String getUserId() {
        try {
            return this.profileManager.getUserId();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public JSONObject getDeviceInfo() {
        try {
            return _getDeviceInfo();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void refreshCampaignsAndResources() {
        try {
            _refreshCampaignsAndResources();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    protected SwrveMessage getMessageForEvent(String event) {
        try {
            return getMessageForEvent(event, new HashMap<String, String>(), SwrveOrientation.Both);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    protected SwrveMessage getMessageForEvent(String event, Map<String, String> payload, SwrveOrientation orientation) {
        try {
            return _getMessageForEvent(event, payload, orientation);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    public SwrveMessage getMessageForId(int messageId) {
        try {
            return _getMessageForId(messageId);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    protected SwrveConversation getConversationForEvent(String event, Map<String, String> payload) {
        try {
            return _getConversationForEvent(event, payload);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void buttonWasPressedByUser(SwrveButton button) {
        try {
            _buttonWasPressedByUser(button);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void messageWasShownToUser(SwrveMessageFormat messageFormat) {
        try {
            _messageWasShownToUser(messageFormat);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public String getAppStoreURLForApp(int appId) {
        try {
            return _getAppStoreURLForApp(appId);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public File getCacheDir() {
        try {
            return _getCacheDir();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setMessageListener(SwrveMessageListener messageListener) {
        try {
            _setMessageListener(messageListener);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    public void setConversationListener(SwrveConversationListener listener) {
        try {
            _setConversationListener(listener);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void setResourcesListener(SwrveResourcesListener resourcesListener) {
        try {
            _setResourcesListener(resourcesListener);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public Date getInitialisedTime() {
        try {
            return _getInitialisedTime();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public SwrveInstallButtonListener getInstallButtonListener() {
        try {
            return _getInstallButtonListener();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setInstallButtonListener(SwrveInstallButtonListener installButtonListener) {
        try {
            _setInstallButtonListener(installButtonListener);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public SwrveCustomButtonListener getCustomButtonListener() {
        try {
            return _getCustomButtonListener();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void setCustomButtonListener(SwrveCustomButtonListener customButtonListener) {
        try {
            _setCustomButtonListener(customButtonListener);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public C getConfig() {
        try {
            return _getConfig();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
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
                        && campaign.areAssetsReady(getAssetsOnDisk())) {
                    result.add(campaign);
                }
            }
        }
        return result;
    }

    @Override
    public boolean showMessageCenterCampaign(SwrveBaseCampaign campaign) {
        if (campaign instanceof SwrveInAppCampaign) {
            SwrveInAppCampaign iamCampaign = (SwrveInAppCampaign)campaign;
            if (iamCampaign != null && iamCampaign.getMessages().size() > 0 && messageListener != null) {
                // Display first message in the in-app campaign
                messageListener.onMessage(iamCampaign.getMessages().get(0));
                return true;
            } else {
                SwrveLogger.e("No in-app message or message listener.");
            }
        } else if (campaign instanceof SwrveConversationCampaign) {
            SwrveConversationCampaign conversationCampaign = (SwrveConversationCampaign)campaign;
            if (conversationCampaign != null && conversationCampaign.getConversation() != null && conversationListener != null) {
                conversationListener.onMessage(conversationCampaign.getConversation());
                return true;
            } else {
                SwrveLogger.e("No conversation campaign or conversation listener.");
            }
        }
        return false;
    }

    @Override
    public void removeMessageCenterCampaign(SwrveBaseCampaign campaign) {
        campaign.setStatus(SwrveCampaignState.Status.Deleted);
        saveCampaignsState(getUserId());
    }

    /***
     * Implementation of ISwrveCommon methods
     */

    @Override
    public int getAppId() {
        return appId;
    }

    @Override
    public String getAppVersion() {
        return appVersion;
    }

    @Override
    public String getBatchURL() {
        return getEventsServer() +  BATCH_EVENTS_ACTION;
    }

    @Override
    public void setLocationSegmentVersion(int locationSegmentVersion) {
        this.locationSegmentVersion = locationSegmentVersion;
    }

    @Override
    public String getSwrveSDKVersion() {
        return version;
    }

    @Override
    public String getCachedData(String userId, String key) {
        String data = null;
        try {
            LocalStorage localStorage = new SQLiteLocalStorage(context.get(), config.getDbName(), config.getMaxSqliteDbSize());
            data = localStorage.getSecureCacheEntryForUser(userId, key, getUniqueKey(userId));
        } catch (Exception e) {
            SwrveLogger.e("Error getting cached data. userId:" + userId + " key:" + key, e);
        }
        return data;
    }

    @Override
    public void sendEventsInBackground(Context context, String userId, ArrayList<String> events) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Avoid using the deprecated wakeful receiver
            SwrveEventSenderJobService.scheduleJob(context, userId, events);
        } else {
            Intent intent = new Intent(context, SwrveWakefulReceiver.class);
            intent.putExtra(SwrveBackgroundEventSender.EXTRA_USER_ID, userId);
            intent.putStringArrayListExtra(SwrveBackgroundEventSender.EXTRA_EVENTS, events);
            context.sendBroadcast(intent);
        }
    }

    @Override
    public String getSessionKey() {
        return profileManager.getSessionToken();
    }

    @Override
    public short getDeviceId() {
        return SwrveLocalStorageUtil.getDeviceId(multiLayerLocalStorage);
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

    @Override
    public synchronized int getNextSequenceNumber() {
        return SwrveLocalStorageUtil.getNextSequenceNumber(context.get(), config, multiLayerLocalStorage, profileManager.getUserId());
    }

    @Override
    public NotificationChannel getDefaultNotificationChannel() {
        return config.getDefaultNotificationChannel();
    }

    /***
     * eo ISwrveCommon
     */

    /***
     * Implementation of ISwrveConversationSDK methods
     */

    @Override
    public void queueConversationEvent(String eventParamName, String eventPayloadName, String page, int conversationId, Map<String, String> payload) {
        if (payload == null) {
            payload = new HashMap<>();
        }
        payload.put("event", eventPayloadName);
        payload.put("conversation", Integer.toString(conversationId));
        payload.put("page", page);

        SwrveLogger.d("Sending view conversation event: %s", eventParamName);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("name", eventParamName);
        String userId = profileManager.getUserId();
        queueEvent(userId, "event", parameters, payload, false);
    }

    /***
     * eo ISwrveConversationSDK
     */

    /***
     * Implementation of Application.ActivityLifecycleCallbacks methods
     */

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        onCreate(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // empty
    }

    @Override
    public void onActivityResumed(Activity activity) {
        onResume(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        onPause();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // empty
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // empty
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        onDestroy(activity);
    }

    /***
     * eo Application.ActivityLifecycleCallbacks
     */
}
