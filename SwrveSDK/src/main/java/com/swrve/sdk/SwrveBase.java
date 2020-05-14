package com.swrve.sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.NotificationManagerCompat;
import android.util.SparseArray;

import com.swrve.sdk.SwrveCampaignDisplayer.Result;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.SwrveConversationListener;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.exceptions.NoUserIdSwrveException;
import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveClipboardButtonListener;
import com.swrve.sdk.messaging.SwrveConversationCampaign;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveDismissButtonListener;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessageListener;
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
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.swrve.sdk.SwrveCampaignDisplayer.DisplayResult.CAMPAIGN_WRONG_ORIENTATION;
import static com.swrve.sdk.SwrveCampaignDisplayer.DisplayResult.ELIGIBLE_BUT_OTHER_CHOSEN;
import static com.swrve.sdk.SwrveTrackingState.EVENT_SENDING_PAUSED;
import static com.swrve.sdk.SwrveTrackingState.ON;

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

    protected synchronized T init(final Activity activity) throws IllegalArgumentException {

        if (initialised) {
            return (T) this;
        }
        initialised = true;

        try {
            trackingState = ON;
            final Context resolvedContext = bindToContext(activity);
            final boolean preloadRandC = config.isLoadCachedCampaignsAndResourcesOnUIThread();

            final String userId = profileManager.getUserId();
            initialisedTime = getNow();

            lastSessionTick = getSessionTime();

            autoShowMessagesEnabled = true;
            disableAutoShowAfterDelay();

            appStoreURLs = new SparseArray<>();

            initCacheDir(activity);

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
            if (sessionListener != null) {
                sessionListener.sessionStarted();
            }
            generateNewSessionInterval();

            initUserJoinedTimeAndFirstSession();

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
            queueDeviceUpdateNow(userId, profileManager.getSessionToken(), true);

            // Messaging initialization

            if (SwrveHelper.isNullOrEmpty(language)) {
                SwrveHelper.logAndThrowException("Language needed to use in-app messages");
            } else if (SwrveHelper.isNullOrEmpty(config.getAppStore())) {
                SwrveHelper.logAndThrowException("App store needed to use in-app messages");
            }

            if (preloadRandC) {
                initCampaigns(userId); // Initialize campaigns from cache
                initRealTimeUserProperties(userId); // Initialize realtime user properties
            }

            // Add default message listener
            if (messageListener == null) {
                setDefaultMessageListener();
            }
            if (config.getInAppMessageConfig() != null) {
                personalisationProvider = config.getInAppMessageConfig().getPersonalisationProvider();
            }

            // Add custom conversation listener
            if (conversationListener == null) {
                setConversationListener(conversation -> {
                    // Start a Conversation activity to display the campaign
                    if (SwrveBase.this.context != null) {
                        ConversationActivity.showConversation(SwrveBase.this.context.get(), conversation, config.getOrientation());
                        conversation.getCampaign().messageWasShownToUser(); // Report that the conversation was shown to the user
                    }
                });
            }

            if (preloadRandC && config.isABTestDetailsEnabled()) {
                initABTestDetails(userId);
            }

            // Retrieve values for resource/campaigns flush frequencies and ETag
            campaignsAndResourcesFlushFrequency = settings.getInt("swrve_cr_flush_frequency", SWRVE_DEFAULT_CAMPAIGN_RESOURCES_FLUSH_FREQUENCY);
            campaignsAndResourcesFlushRefreshDelay = getFlushRefreshDelay();
            campaignsAndResourcesLastETag = multiLayerLocalStorage.getCacheEntry(userId, CACHE_ETAG);

            startCampaignsAndResourcesTimer(true);

            if (!preloadRandC) {
                // Load campaigns and resources in a background thread
                storageExecutorExecute(() -> {
                    initResources(userId);

                    initCampaigns(userId);

                    initRealTimeUserProperties(userId);

                    if (config.isABTestDetailsEnabled()) {
                        initABTestDetails(userId);
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

    private void initCacheDir(Activity activity) {
        File cacheDir = getCacheDir(activity);
        swrveAssetsManager.setStorageDir(cacheDir);
        SwrveLogger.d("Using cache directory at %s", cacheDir.getPath());
    }

    @Override
    public File getCacheDir(Context context) {
        File cacheDir = config.getCacheDir();

        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        } else {
            if (!checkPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                if (context instanceof Activity) {
                    requestPermissions((Activity) context, permissions);
                }
                cacheDir = context.getCacheDir(); // fall back to internal cache until permission granted.
            }

            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        }
        return cacheDir;
    }

    private void initUserJoinedTimeAndFirstSession() {
        String userInitTimeRaw = multiLayerLocalStorage.getCacheEntry(profileManager.getUserId(), CACHE_USER_JOINED_TIME);
        if (SwrveHelper.isNullOrEmpty(userInitTimeRaw)) {
            long userJoinedTime = initialisedTime.getTime();
            multiLayerLocalStorage.setCacheEntry(profileManager.getUserId(), CACHE_USER_JOINED_TIME, String.valueOf(userJoinedTime)); // Save to memory and secondary storage
            // Only send first_session event if user hasn't already identified on a different device.
            if (identifiedOnAnotherDevice == false) {
                _event(EVENT_FIRST_SESSION); // First time we see this user
            }
        }
    }

    private void setDefaultMessageListener() {
        setMessageListener(new SwrveMessageListener() {

            public void onMessage(final SwrveMessage message) {
                onMessage(message, null);
            }

            public void onMessage(final SwrveMessage message, Map<String, String> properties) {
                // Start a Conversation activity to display the campaign
                if (SwrveBase.this.context != null) {
                    final Context ctx = SwrveBase.this.context.get();
                    if (ctx == null) {
                        SwrveLogger.e("Can't display a in-app message without a context");
                        return;
                    }

                    if (personalisationProvider != null && (properties == null || properties.size() == 0)) {
                        properties = personalisationProvider.personalize(lastEventPayloadUsed);
                    }

                    if (message.supportsOrientation(getDeviceOrientation())) {
                        if (SwrveMessageTextTemplatingChecks.checkTemplating(message, properties)) {
                            Intent intent = new Intent(ctx, SwrveInAppMessageActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, message.getId());

                            if (properties != null) {
                                // Cannot pass a Map to intent, converting to HashMap
                                HashMap<String, String> personalisation = new HashMap<>(properties);
                                intent.putExtra(SwrveInAppMessageActivity.SWRVE_PERSONALISATION_KEY, personalisation);
                            }

                            ctx.startActivity(intent);
                        }
                    } else {
                        SwrveLogger.i("Can't display the in-app message as it doesn't support the current orientation");
                    }
                }
            }
        });
    }

    protected void openLocalStorageConnection() {
        try {
            if (multiLayerLocalStorage != null && multiLayerLocalStorage.getSecondaryStorage() != null && multiLayerLocalStorage.getSecondaryStorage() instanceof SQLiteLocalStorage) {
                // empty
            } else {
                SQLiteLocalStorage sqLiteLocalStorage = new SQLiteLocalStorage(context.get(), config.getDbName(), config.getMaxSqliteDbSize());
                multiLayerLocalStorage.setSecondaryStorage(sqLiteLocalStorage);
            }
        } catch (Exception ex) {
            SwrveLogger.e("Swrve error opening database.", ex);
        }
    }

    protected abstract void beforeSendDeviceInfo(Context context);

    protected abstract void extraDeviceInfo(JSONObject deviceInfo) throws JSONException;

    protected void _sessionStart() {
        queueSessionStart();
        final String userId = profileManager.getUserId();
        final String sessionToken = profileManager.getSessionToken();
        // Send event after it has been queued
        storageExecutorExecute(() -> _sendQueuedEvents(userId, sessionToken));

        if (sessionListener != null) {
            sessionListener.sessionStarted();
        }
    }

    protected void _sessionEnd() {
        queueEvent("session_end", null, null);
    }

    protected void _event(String name) {
        _event(name, null);
    }

    protected void _event(String name, Map<String, String> payload) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", name);
        try {
            if (payload != null && payload.size() > 0) {
                new JSONObject(payload); // validate payload: if payload is invalid exception is thrown and caught by calling method.
            }
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: JSONException when encoding payload event. Not queueing.", ex);
            return;
        }
        queueEvent("event", parameters, payload);
    }

    protected void _purchase(String item, String currency, int cost, int quantity) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("item", item);
        parameters.put("currency", currency);
        parameters.put("cost", Integer.toString(cost));
        parameters.put("quantity", Integer.toString(quantity));
        queueEvent("purchase", parameters, null);
    }

    protected void _currencyGiven(String givenCurrency, double givenAmount) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("given_currency", givenCurrency);
        parameters.put("given_amount", Double.toString(givenAmount));
        queueEvent("currency_given", parameters, null);
    }

    protected void _userUpdate(Map<String, String> attributes) {
        Map<String, Object> parameters = new HashMap<>();
        try {
            parameters.put("attributes", new JSONObject(attributes));
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: JSONException when encoding user attributes. Not queueing.", ex);
            return;
        }
        queueEvent("user", parameters, null);
    }

    protected void _userUpdate(String name, Date date) {
        Map<String, Object> parameters = new HashMap<>();
        try {
            Map<String, String> dateAttributes = new HashMap<>();
            String formattedDate = getStringFromDate(date);
            dateAttributes.put(name, formattedDate);
            parameters.put("attributes", new JSONObject(dateAttributes));
        } catch (Exception ex) {
            SwrveLogger.e("SwrveSDK: JSONException when encoding user date attributes. Not queueing.", ex);
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
        restClientExecutorExecute(() -> {
            if (profileManager.getUserId() != null) {
                Map<String, String> params = new HashMap<>();
                params.put("api_key", apiKey);
                params.put("user", profileManager.getUserId());
                params.put("app_version", appVersion);
                long userJoinedTime = Long.parseLong(multiLayerLocalStorage.getCacheEntry(userId, CACHE_USER_JOINED_TIME));
                params.put("joined", String.valueOf(userJoinedTime));
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
        });
    }

    protected void _getRealTimeUserProperties(final SwrveRealTimeUserPropertiesListener listener) {
        String cachedRealTimeUserProps = null;

        // Read cached real time user properties
        try {
            cachedRealTimeUserProps = multiLayerLocalStorage.getSecureCacheEntryForUser(profileManager.getUserId(), CACHE_REALTIME_USER_PROPERTIES, getUniqueKey(getUserId()));
        } catch (SecurityException e) {
            listener.onRealTimeUserPropertiesError(e);
        }

        if (!SwrveHelper.isNullOrEmpty(cachedRealTimeUserProps)) {
            try {
                JSONObject realTimeUserPropertiesJSON = new JSONObject(cachedRealTimeUserProps);
                Iterator<String> userPropertyIterator = realTimeUserPropertiesJSON.keys();

                Map<String, String> mapProperties = new HashMap<>();
                while (userPropertyIterator.hasNext()) {
                    String userPropertyKey = userPropertyIterator.next();
                    try {
                        String userPropertyValue = realTimeUserPropertiesJSON.getString(userPropertyKey);
                        mapProperties.put(userPropertyKey, userPropertyValue);
                    } catch (Exception exp) {
                        SwrveLogger.e("Could not load realtime user property for key: " + userPropertyKey, exp);
                        listener.onRealTimeUserPropertiesError(exp);
                    }
                }

                // Execute callback (NOTE: Executed in same thread!)
                listener.onRealTimeUserPropertiesSuccess(mapProperties, cachedRealTimeUserProps);
            } catch (Exception exp) {
                // Launch exception
                listener.onRealTimeUserPropertiesError(exp);
            }
        }
    }

    protected void _sendQueuedEvents(final String userId, final String sessionToken) {
        if (trackingState == EVENT_SENDING_PAUSED) {
            SwrveLogger.d("SwrveSDK tracking state:%s so cannot send events now.", trackingState);
            return;
        }
        if (SwrveHelper.isNotNullOrEmpty(userId) && SwrveHelper.isNotNullOrEmpty(sessionToken)) {
            restClientExecutorExecute(() -> {
                String deviceId = getDeviceId();
                SwrveEventsManager swrveEventsManager = new SwrveEventsManagerImp(context.get(), config, restClient, userId, appVersion, sessionToken, deviceId);
                swrveEventsManager.sendStoredEvents(multiLayerLocalStorage);
                eventsWereSent = true;
            });
        }
    }

    protected void _flushToDisk() {
        storageExecutorExecute(() -> {
            try {
                SwrveLogger.i("Flushing to disk");
                if (multiLayerLocalStorage != null) {
                    multiLayerLocalStorage.flush();
                }
            } catch (Exception e) {
                SwrveLogger.e("Flush to disk failed", e);
            }
        });
    }

    protected void _onPause() {
        shutdownCampaignsAndResourcesTimer();
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

        // Detect if user is influenced by a push notification
        campaignInfluence.processInfluenceData(getContext(), this);

        loadCampaignFromNotification(this.notificationSwrveCampaignId);
    }

    protected void loadCampaignFromNotification(String swrveCampaignId) {
        if (swrveCampaignId != null) {
            if (canProcessPushToInapp()) {
                Bundle b = new Bundle();
                b.putString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY, swrveCampaignId);
                initSwrveDeepLinkManager();
                this.swrveDeeplinkManager.handleDeeplink(b);
            }
            notificationSwrveCampaignId = null;
        }
    }

    protected void _onDestroy() {
        unbindAndShutdown();
    }

    protected void _shutdown() {
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
            if (restClientExecutor != null) {
                try {
                    restClientExecutor.shutdown();
                    restClientExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    SwrveLogger.e("Exception occurred shutting down restClientExecutor", e);
                }
            }
            if (storageExecutor != null) {
                try {
                    storageExecutor.shutdown();
                    storageExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    SwrveLogger.e("Exception occurred shutting down storageExecutor", e);
                }
            }
            shutdownCampaignsAndResourcesTimer();
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

    protected String _getJoined() {
        openLocalStorageConnection(); // getJoined can be called from GeoSDK so ensure storage connection is open.
        return multiLayerLocalStorage.getCacheEntry(profileManager.getUserId(), CACHE_USER_JOINED_TIME);
    }

    protected JSONObject _getDeviceInfo() throws JSONException {
        JSONObject deviceInfo = new JSONObject();

        deviceInfo.put(SWRVE_DEVICE_NAME, getDeviceName());
        deviceInfo.put(SWRVE_OS_VERSION, Build.VERSION.RELEASE);

        Context contextRef = context.get();

        if (contextRef != null) {
            try {
                deviceInfo.put(SWRVE_DEVICE_WIDTH, deviceWidth);
                deviceInfo.put(SWRVE_DEVICE_HEIGHT, deviceHeight);
                deviceInfo.put(SWRVE_DEVICE_DPI, deviceDpi);
                deviceInfo.put(SWRVE_ANDROID_DEVICE_XDPI, androidDeviceXdpi);
                deviceInfo.put(SWRVE_ANDROID_DEVICE_YDPI, androidDeviceYdpi);
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
            //OS
            deviceInfo.put(SWRVE_OS, SwrveHelper.getPlatformOS(contextRef));
            deviceInfo.put(SWRVE_LANGUAGE, SwrveBase.this.language);
            String deviceRegion = Locale.getDefault().getCountry();
            deviceInfo.put(SWRVE_DEVICE_REGION, deviceRegion);
            deviceInfo.put(SWRVE_SDK_VERSION, PLATFORM + version);
            deviceInfo.put(SWRVE_APP_STORE, config.getAppStore());
            deviceInfo.put(SWRVE_SDK_FLAVOUR, Swrve.FLAVOUR_NAME);
            SwrveInitMode mode = config.getInitMode();
            if (mode == SwrveInitMode.MANAGED && config.isManagedModeAutoStartLastUser()) {
                deviceInfo.put(SWRVE_INIT_MODE, "managed_auto");
            } else {
                deviceInfo.put(SWRVE_INIT_MODE, mode.toString().toLowerCase(Locale.ENGLISH));
            }
            Calendar cal = new GregorianCalendar();
            deviceInfo.put(SWRVE_TIMEZONE_NAME, cal.getTimeZone().getID());
            deviceInfo.put(SWRVE_UTC_OFFSET_SECONDS, cal.getTimeZone().getOffset(System.currentTimeMillis()) / 1000);

            String appInstallTime = SwrveHelper.getAppInstallTime(context.get());
            deviceInfo.put(SWRVE_INSTALL_DATE, appInstallTime);

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(contextRef);
            boolean notificationsEnabled = notificationManagerCompat.areNotificationsEnabled();
            deviceInfo.put(SWRVE_NOTIFICATIONS_ENABLED, notificationsEnabled);
            int notificationsImportance = notificationManagerCompat.getImportance();
            deviceInfo.put(SWRVE_NOTIFICATIONS_IMPORTANCE, notificationsImportance);

            if(notificationsEnabled) {
                // Rich Push Capabilities
                deviceInfo.put(SWRVE_NOTIFICATIONS_BUTTONS, true);
                deviceInfo.put(SWRVE_NOTIFICATIONS_ATTACHMENT, true);
                // Authenticated Push
                deviceInfo.put(SWRVE_CAN_RECEIVE_AUTH_PUSH, true);
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

        final String userId = getUserId(); // user can change so retrieve now as a final String for thread safeness
        restClientExecutorExecute(new Runnable() {
            @Override
            public void run() {
                Map<String, String> params = getContentRequestParams(userId);
                if (config.isABTestDetailsEnabled()) {
                    params.put("ab_test_details", "1");
                }
                // If we have a last ETag value, send that along with the request
                if (!SwrveHelper.isNullOrEmpty(campaignsAndResourcesLastETag)) {
                    params.put("etag", campaignsAndResourcesLastETag);
                }

                try {
                    restClient.get(config.getContentUrl() + USER_CONTENT_ACTION, params, new IRESTResponseListener() {
                        @Override
                        public void onResponse(RESTResponse response) {
                            // Response received from server
                            if (response.responseCode == HttpURLConnection.HTTP_OK) {
                                SharedPreferences settings = context.get().getSharedPreferences(SDK_PREFS_NAME, 0);
                                SharedPreferences.Editor settingsEditor = settings.edit();

                                String etagHeader = response.getHeaderValue("ETag");
                                if (!SwrveHelper.isNullOrEmpty(etagHeader)) {
                                    campaignsAndResourcesLastETag = etagHeader;
                                    multiLayerLocalStorage.setCacheEntry(userId, CACHE_ETAG, etagHeader);
                                }

                                try {
                                    JSONObject responseJson;
                                    try {
                                        responseJson = new JSONObject(response.responseBody);
                                    } catch(JSONException e) {
                                        SwrveLogger.e("SwrveSDK unable to decode user_content JSON : \"%s\".", response.responseBody);
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

                                    if (responseJson.has("real_time_user_properties")) {
                                        JSONObject realTimeUserPropertiesJson = responseJson.getJSONObject("real_time_user_properties");
                                        saveRealTimeUserPropertiesInCache(realTimeUserPropertiesJson);
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
                for (SwrveBaseCampaign nextCampaign : campaigns) {
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
                    for (SwrveConversation otherMessage : availableConversations) {
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
                for (SwrveBaseCampaign nextCampaign : campaigns) {
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
                    for (SwrveMessage otherMessage : availableMessages) {
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

    protected C _getConfig() {
        return config;
    }

    /**
     * Public functions all wrapped in try/catch to avoid exceptions leaving the SDK and affecting the app itself.
     */
    @Override
    public void sessionStart() {
        if (!isSdkReady()) return;

        try {
            _sessionStart();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void sessionEnd() {
        if (!isSdkReady()) return;

        try {
            _sessionEnd();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void event(String name) {
        if (!isSdkReady()) return;

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
        if (!isSdkReady()) return;

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
        if (!isSdkReady()) return;

        try {
            _purchase(item, currency, cost, quantity);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void currencyGiven(String givenCurrency, double givenAmount) {
        if (!isSdkReady()) return;

        try {
            _currencyGiven(givenCurrency, givenAmount);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void userUpdate(Map<String, String> attributes) {
        if (!isSdkReady()) return;

        try {
            _userUpdate(attributes);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void userUpdate(String name, Date date) {
        if (!isSdkReady()) return;

        try {
            _userUpdate(name, date);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void iap(int quantity, String productId, double productPrice, String currency) {
        if (!isSdkReady()) return;

        try {
            _iap(quantity, productId, productPrice, currency);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards) {
        if (!isSdkReady()) return;

        try {
            _iap(quantity, productId, productPrice, currency, rewards);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public SwrveResourceManager getResourceManager() {
        if (!isSdkReady()) {
            return new SwrveResourceManager();
        }

        try {
            return _getResourceManager();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void getUserResources(final SwrveUserResourcesListener listener) {
        if (!isSdkReady()) return;

        try {
            _getUserResources(listener);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void getUserResourcesDiff(final SwrveUserResourcesDiffListener listener) {
        if (!isSdkReady()) return;

        try {
            _getUserResourcesDiff(listener);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void getRealTimeUserProperties(final SwrveRealTimeUserPropertiesListener listener) {
        if (!isSdkReady()) return;

        try {
            _getRealTimeUserProperties(listener);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void sendQueuedEvents() {
        if (!isSdkReady()) return;

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
        if (!isSdkReady()) return;

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
            _onDestroy();
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
    public String getJoined() {
        try {
            return _getJoined();
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
        if (!isSdkReady()) return new JSONObject();

        try {
            return _getDeviceInfo();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public void refreshCampaignsAndResources() {
        if (!isSdkReady()) return;

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


    public SwrveMessage getAdMesage() {
        SwrveMessage result = null;

        if (this.swrveDeeplinkManager != null) {
            result = this.swrveDeeplinkManager.getSwrveMessage();
        }

        if (result == null) {
            SwrveLogger.i("Not showing messages: no candidate messages");
        }

        return result;
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
        if (!isSdkReady()) return;

        try {
            _buttonWasPressedByUser(button);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void messageWasShownToUser(SwrveMessageFormat messageFormat) {
        if (!isSdkReady()) return;

        try {
            _messageWasShownToUser(messageFormat);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public String getAppStoreURLForApp(int appId) {
        if (!isSdkReady()) return null;

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
        if (!isSdkReady()) return new Date();

        try {
            return _getInitialisedTime();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    public SwrveInstallButtonListener getInstallButtonListener() {
        SwrveInAppMessageConfig inAppConfig = config.getInAppMessageConfig();
        if (inAppConfig != null) {
            return inAppConfig.getInstallButtonListener();
        }
        return null;
    }

    public SwrveCustomButtonListener getCustomButtonListener() {
        SwrveInAppMessageConfig inAppConfig = config.getInAppMessageConfig();
        if (inAppConfig != null) {
            return inAppConfig.getCustomButtonListener();
        }
        return null;
    }

    public SwrveClipboardButtonListener getClipboardButtonListener() {
        SwrveInAppMessageConfig inAppConfig = config.getInAppMessageConfig();
        if (inAppConfig != null) {
            return inAppConfig.getClipboardButtonListener();
        }
        return null;
    }

    public SwrveDismissButtonListener getDismissButtonListener() {
        SwrveInAppMessageConfig inAppConfig = config.getInAppMessageConfig();
        if (inAppConfig != null) {
            return inAppConfig.getDismissButtonListener();
        }
        return null;
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
    public List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation, Map<String, String> properties) {
        List<SwrveBaseCampaign> result = new ArrayList<>();
        if (!isSdkReady()) return result;

        if (campaigns != null) {
            synchronized (campaigns) {
                for (int i = 0; i < campaigns.size(); i++) {
                    SwrveBaseCampaign campaign = campaigns.get(i);
                    if (campaign.isMessageCenter()
                            && campaign.getStatus() != SwrveCampaignState.Status.Deleted
                            && campaign.isActive(getNow())
                            && campaign.supportsOrientation(orientation)
                            && campaign.areAssetsReady(getAssetsOnDisk())) {
                        if (properties != null && (campaign instanceof SwrveInAppCampaign)) {
                            // Check personalisation for the
                            List<SwrveMessage> messages = ((SwrveInAppCampaign)campaign).getMessages();
                            boolean messagesCanResolve = true;
                            for (int mi = 0; mi < messages.size() && messagesCanResolve; mi++) {
                                SwrveMessage message = messages.get(mi);
                                if (message.supportsOrientation(orientation)) {
                                    messagesCanResolve = SwrveMessageTextTemplatingChecks.checkTemplating(message, properties);
                                }
                            }

                            // All messages that support the required orientation can be resolved
                            if (messagesCanResolve) {
                                result.add(campaign);
                            }
                        } else {
                            result.add(campaign);
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public List<SwrveBaseCampaign> getMessageCenterCampaigns() {
        return getMessageCenterCampaigns(getDeviceOrientation(), null);
    }

    @Override
    public List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation) {
        return getMessageCenterCampaigns(orientation, null);

    }

    @Override
    public List<SwrveBaseCampaign> getMessageCenterCampaigns(Map<String, String> properties) {
        return getMessageCenterCampaigns(getDeviceOrientation(), properties);
    }

    @Override
    public boolean showMessageCenterCampaign(SwrveBaseCampaign campaign) {
        return showMessageCenterCampaign(campaign, null);
    }

    @Override
    public boolean showMessageCenterCampaign(SwrveBaseCampaign campaign, Map<String, String> properties) {
        if (!isSdkReady()) return false;

        if (campaign instanceof SwrveInAppCampaign) {
            SwrveInAppCampaign iamCampaign = (SwrveInAppCampaign)campaign;
            if (iamCampaign != null && iamCampaign.getMessages().size() > 0 && messageListener != null) {
                // Display first message in the in-app campaign
                messageListener.onMessage(iamCampaign.getMessages().get(0), properties);
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
        if (!isSdkReady() || campaign == null) return;

        campaign.setStatus(SwrveCampaignState.Status.Deleted);
        saveCampaignsState(getUserId());
    }

    @Override
    public void markMessageCenterCampaignAsSeen(SwrveBaseCampaign campaign) {
        if (!isSdkReady() || campaign == null) return;

        campaign.setStatus(SwrveCampaignState.Status.Seen);
        saveCampaignsState(getUserId());
    }

    protected Map<String, String> getContentRequestParams(String userId) {

        String userJoinedTime = multiLayerLocalStorage.getCacheEntry(userId, CACHE_USER_JOINED_TIME);


        Map<String, String> params = new HashMap<>();

        // General params
        params.put("api_key", apiKey);
        params.put("user", this.profileManager.getUserId());
        params.put("app_version", appVersion);
        params.put("joined", userJoinedTime);

        // In-app only params
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

        return params;
    }

    protected void initSwrveDeepLinkManager() {
        if (this.swrveDeeplinkManager == null) {
            final String userId = profileManager.getUserId();
            Map<String, String> params = getContentRequestParams(userId);
            this.swrveDeeplinkManager = new SwrveDeeplinkManager(params, (SwrveConfig) this.getConfig(), this.getContext(), this.swrveAssetsManager, this.restClient);
        }
    }

    @Override
    public void handleDeferredDeeplink(Bundle bundle) {
        if (!isSdkReady()) return;
        if (!SwrveDeeplinkManager.isSwrveDeeplink(bundle)) return;

        initSwrveDeepLinkManager();
        this.swrveDeeplinkManager.handleDeferredDeeplink(bundle);
    }

    @Override
    public void handleDeeplink(Bundle bundle) {
        if (!isSdkReady()) return;
        if (!SwrveDeeplinkManager.isSwrveDeeplink(bundle)) return;

        initSwrveDeepLinkManager();
        this.swrveDeeplinkManager.handleDeeplink(bundle);
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
    public String getContentURL() {
        return config.getContentUrl().toString();
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
            SwrveEventSenderJobIntentService.enqueueWork(context, userId, events);
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
    public String getDeviceId() {
        openLocalStorageConnection();
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
    public synchronized int getNextSequenceNumber() {
        openLocalStorageConnection();

        String id = multiLayerLocalStorage.getCacheEntry(profileManager.getUserId(), CACHE_SEQNUM);
        int seqnum = 1;
        if (!SwrveHelper.isNullOrEmpty(id)) {
            seqnum = Integer.parseInt(id) + 1;
        }
        multiLayerLocalStorage.setCacheEntry(profileManager.getUserId(), CACHE_SEQNUM, Integer.toString(seqnum));
        return seqnum;
    }

    @Override
    public NotificationChannel getDefaultNotificationChannel() {
        NotificationChannel channel = null;
        if (config != null && config.getNotificationConfig() != null) {
            channel = config.getNotificationConfig().getDefaultNotificationChannel();
        }
        return channel;
    }

    @Override
    public SwrveNotificationConfig getNotificationConfig() {
        return config.getNotificationConfig();
    }

    @Override
    public SwrvePushNotificationListener getNotificationListener() {
        return config.getNotificationListener();
    }

    @Override
    public SwrveSilentPushListener getSilentPushListener() {
        return config.getSilentPushListener();
    }

    /*
     * eo ISwrveCommon
     */

    /*
     * Implementation of ISwrveConversationSDK methods
     */

    @Override
    public void queueConversationEvent(String eventParamName, String eventPayloadName, String page, int conversationId, Map<String, String> payload) {
        if (!isSdkReady()) return;

        if (payload == null) {
            payload = new HashMap<>();
        }
        payload.put("event", eventPayloadName);
        payload.put("conversation", Integer.toString(conversationId));
        payload.put("page", page);

        SwrveLogger.d("Sending view conversation event: %s", eventParamName);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", eventParamName);
        String userId = profileManager.getUserId();
        queueEvent(userId, "event", parameters, payload, false);
    }

    /*
     * eo ISwrveConversationSDK
     */

    /*
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

    /*
     * eo Application.ActivityLifecycleCallbacks
     */

    @Override
    public void setNotificationSwrveCampaignId(String swrveCampaignId) {
        notificationSwrveCampaignId = swrveCampaignId;
    }

    protected void switchUser(String newUserId) {

        // don't do anything if the current user id is the same as the new one
        if (started) {
            if (SwrveHelper.isNullOrEmpty(newUserId) || newUserId.equals(getUserId())) {
                enableEventSending();
                queuePausedEvents();
                return;
            }
        }

        clearAllAuthenticatedNotifications();

        profileManager.updateUserId(newUserId);
        profileManager.updateSessionToken();

        if (getActivityContext() != null) {
            initialised = false;
            qaUser = null;
            init(getActivityContext());
            queuePausedEvents();
        } else {
            SwrveLogger.d("Switching user before activity loaded, unable to call init");
        }
    }

    @Override
    public void identify(final String externalUserId, final SwrveIdentityResponse identityResponse) {
        if (config.getInitMode() == SwrveInitMode.MANAGED) {
            throw new RuntimeException("Cannot call Identify when running on SwrveInitMode.MANAGED mode");
        }

        try {
            _identify(externalUserId, identityResponse);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    protected void _identify(final String externalUserId, final SwrveIdentityResponse identityResponse) {
        if (SwrveHelper.isNullOrEmpty(externalUserId)) {
            SwrveLogger.d("External user id cannot be null or empty");
            if (identityResponse != null) {
                identityResponse.onError(-1, "External user id cannot be null or empty");
            }
            return;
        }

        // if identify is called from Application class, swrve may not be init'ed yet, we need to open db.
        openLocalStorageConnection();

        // always flush events and pause event sending
        sendQueuedEvents();
        pauseEventSending();

        SwrveUser cachedSwrveUser = multiLayerLocalStorage.getUserByExternalUserId(externalUserId);

        if (identifyCachedUser(cachedSwrveUser, identityResponse)) {
            SwrveLogger.d("Identity API call skipped, user loaded from cache. Event sending reenabled");
            return;
        }

        identifyUnknownUser(externalUserId, identityResponse, cachedSwrveUser);
    }

    private boolean identifyCachedUser(SwrveUser cachedSwrveUser, SwrveIdentityResponse identityResponse) {
        boolean isVerified = false;
        if (cachedSwrveUser != null && cachedSwrveUser.isVerified()) {
            switchUser(cachedSwrveUser.getSwrveUserId());
            if (identityResponse != null) {
                identityResponse.onSuccess("Loaded from cache", cachedSwrveUser.getSwrveUserId());
            }
            isVerified = true;
        }
        return isVerified;
    }

    private void identifyUnknownUser(final String externalUserId, final SwrveIdentityResponse identityResponse, SwrveUser cachedSwrveUser) {

        final String unidentifiedUserId = getUnidentifiedUserId(externalUserId, cachedSwrveUser);
        identifiedOnAnotherDevice =  false; // reset the flag
        profileManager.identify(externalUserId, unidentifiedUserId, getDeviceId(), new SwrveIdentityResponse() {
            @Override
            public void onSuccess(String status, String swrveId) {
                // Update User in DB
                SwrveUser verifiedUser = new SwrveUser(swrveId, externalUserId, true);
                multiLayerLocalStorage.saveUser(verifiedUser);

                if (!unidentifiedUserId.equalsIgnoreCase(swrveId)) {
                    identifiedOnAnotherDevice = true;
                }
                switchUser(swrveId);

                if (identityResponse != null) {
                    identityResponse.onSuccess(status, swrveId);
                }
            }

            @Override
            public void onError(int responseCode, String errorMessage) {

                if (responseCode == 403) {
                    multiLayerLocalStorage.deleteUser(getUserId());
                }
                switchUser(unidentifiedUserId);

                if (identityResponse != null) {
                    identityResponse.onError(responseCode, errorMessage);
                }
            }
        });
    }

    private String getUnidentifiedUserId(String externalUserId, SwrveUser cachedSwrveUser) {
        String userId;
        if (cachedSwrveUser == null) {
            // if the current swrve user id hasn't already been used, we can use it
            SwrveUser existingUser = multiLayerLocalStorage.getUserBySwrveUserId(getUserId());
            userId = (existingUser == null) ? getUserId() : UUID.randomUUID().toString();

            // save unverified user
            SwrveUser unVerifiedUser = new SwrveUser(userId, externalUserId, false);
            multiLayerLocalStorage.saveUser(unVerifiedUser);
        } else {
            userId = cachedSwrveUser.getSwrveUserId(); // a previous identify call didn't complete so user has been cached and is unverified
        }
        return userId;
    }

    protected void queuePausedEvents() {

        synchronized (pausedEvents) {
            for (EventQueueItem item : pausedEvents) {
                queueEvent(item.userId, item.eventType, item.parameters, item.payload, item.triggerEventListener);
            }
            if (pausedEvents.size() > 0) {
                sendQueuedEvents();
            }
            pausedEvents.clear();
        }
    }

    protected void enableEventSending() {
        trackingState = ON;
        startCampaignsAndResourcesTimer(false);
        sendQueuedEvents();
    }

    protected void pauseEventSending() {
        trackingState = EVENT_SENDING_PAUSED;
        shutdownCampaignsAndResourcesTimer();
    }

    @Override
    public void saveNotificationAuthenticated(int notificationId) {
        multiLayerLocalStorage.saveNotificationAuthenticated(notificationId);
    }

    protected void clearAllAuthenticatedNotifications() {
        // Authenticated notifications are persisted to db because NotificationManager.getActiveNotifications is only available
        // in API 23 and current minVersion is below that
        final NotificationManager notificationManager = (NotificationManager) context.get().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Integer> currentNotifications = multiLayerLocalStorage.getNotificationsAuthenticated();
        for (Integer notificationId : currentNotifications) {
            notificationManager.cancel(notificationId);
        }
        multiLayerLocalStorage.deleteNotificationsAuthenticated();
    }

    @Override
    public String getExternalUserId() {
        if (!isSdkReady()) return null;

        SwrveUser existingUser = multiLayerLocalStorage.getUserBySwrveUserId(getUserId());
        return (existingUser == null) ? "" : existingUser.getExternalUserId();
    }

    @Override
    public void setCustomPayloadForConversationInput(Map payload) {
        if (!isSdkReady()) return;

        SwrveConversationEventHelper.setCustomPayload(payload);
    }

    @Override
    public int getFlushRefreshDelay() {
        SharedPreferences settings = context.get().getSharedPreferences(SDK_PREFS_NAME, 0);
        return settings.getInt("swrve_cr_flush_delay", SWRVE_DEFAULT_CAMPAIGN_RESOURCES_FLUSH_REFRESH_DELAY);
    }

    @Override
    public void setSessionListener(SwrveSessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }

    @Override
    public void fetchNotificationCampaigns(Set<Long> campaignIds) {
        try {
            initSwrveDeepLinkManager();
            this.swrveDeeplinkManager.fetchOfflineCampaigns(campaignIds);
        } catch (Exception e) {
            SwrveLogger.e("Exception fetching notifications campaigns", e);
        }
    }


    @Override
    public void start(Activity activity) {
        if (config.getInitMode() == SwrveInitMode.AUTO) {
            throw new RuntimeException("Cannot call start method when running on SwrveInitMode.AUTO mode");
        }

        if (!started) {
            started = true;
            this.profileManager.persistUser();
            registerActivityLifecycleCallbacks();
            clearAllAuthenticatedNotifications();
            init(activity);
        }
    }

    @Override
    public void start(Activity activity, String userId) {
        if (config.getInitMode() == SwrveInitMode.AUTO) {
            throw new RuntimeException("Cannot call start method when running on SwrveInitMode.AUTO mode");
        }

        if (!started) {
            started = true;
            registerActivityLifecycleCallbacks();
            profileManager.updateUserId(userId);
            profileManager.updateSessionToken();
            clearAllAuthenticatedNotifications();
            init(activity);
        } else {
            switchUser(userId);
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    // Push-to-in app won't work on MANAGED mode with no auto session start as we can't guarantee a user is present.
    private boolean canProcessPushToInapp() {
        return (config.getInitMode() == SwrveInitMode.AUTO || (config.getInitMode() == SwrveInitMode.MANAGED && config.isManagedModeAutoStartLastUser()));
    }

    private boolean isSdkReady() {
       if (config.getInitMode() == SwrveInitMode.MANAGED && !started) {
           SwrveLogger.w("Warning: SwrveSDK needs to be started in SwrveInitMode.MANAGED mode before calling this api.");
           return false;
       }
       return true;
    }
}
