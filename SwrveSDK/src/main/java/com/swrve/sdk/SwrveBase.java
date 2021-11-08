package com.swrve.sdk;

import static com.swrve.sdk.QaCampaignInfo.CAMPAIGN_TYPE.CONVERSATION;
import static com.swrve.sdk.SwrveTrackingState.EVENT_SENDING_PAUSED;
import static com.swrve.sdk.SwrveTrackingState.STARTED;
import static com.swrve.sdk.SwrveTrackingState.STOPPED;

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
import android.util.SparseArray;

import androidx.core.app.NotificationManagerCompat;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.SwrveConversationListener;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.exceptions.NoUserIdSwrveException;
import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;
import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveClipboardButtonListener;
import com.swrve.sdk.messaging.SwrveConversationCampaign;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveDismissButtonListener;
import com.swrve.sdk.messaging.SwrveEmbeddedCampaign;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
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

    protected void onCreate(final Activity activity) throws IllegalArgumentException {
        if (!initialised) {
            init(activity); // First time it is initialized
        }
    }

    protected synchronized void init(final Activity activity) throws IllegalArgumentException {

        // if tracking was stopped, then this is a restart so reset the initialised flag.
        if (profileManager.getTrackingState() == STOPPED) {
            initialised = false;
        }
        profileManager.setTrackingState(STARTED);
        started = true;
        if (initialised) {
            return;
        }
        initialised = true;

        try {
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
            beforeSendDeviceInfo(application.get());

            if (this.resourceManager == null) {
                this.resourceManager = new SwrveResourceManager();
            }

            initResources(userId); // Initialize resources from cache

            sessionStart(); // this should be sent immediately and then refresh campaigns executes
            generateNewSessionInterval();

            initUserJoinedTimeAndFirstSession();

            // Android referrer information
            SharedPreferences settings = activity.getSharedPreferences(SDK_PREFS_NAME, 0);
            String referrer = settings.getString(SDK_PREFS_REFERRER_ID, null);
            if (!SwrveHelper.isNullOrEmpty(referrer)) {
                Map<String, String> attributes = new HashMap<>();
                attributes.put(SWRVE_REFERRER_ID, referrer);
                SwrveLogger.i("Received install referrer, so sending userUpdate:%s", attributes);
                userUpdate(attributes);
                settings.edit().remove(SDK_PREFS_REFERRER_ID).apply();
            }

            // Get device info
            buildDeviceInfo(application.get());
            queueDeviceUpdateNow(userId, profileManager.getSessionToken(), true);

            // Messaging initialization

            if (SwrveHelper.isNullOrEmpty(language)) {
                SwrveHelper.logAndThrowException("Language needed to use in-app messages");
            } else if (SwrveHelper.isNullOrEmpty(config.getAppStore())) {
                SwrveHelper.logAndThrowException("App store needed to use in-app messages");
            }

            initRealTimeUserProperties(userId); // Initialize realtime user properties

            // we need to initialize these at the time AFTER real time user properties
            if (config.getInAppMessageConfig() != null) {
                if (config.getInAppMessageConfig().getPersonalizationProvider() != null) {
                    personalizationProvider = config.getInAppMessageConfig().getPersonalizationProvider();
                }
            }

            initCampaigns(userId); // Initialize campaigns from cache

            // Add default message listener
            if (messageListener == null) {
                setDefaultMessageListener();
            }

            if (config.getEmbeddedMessageConfig() != null) {
                if (config.getEmbeddedMessageConfig().getEmbeddedMessageListener() != null) {
                    embeddedMessageListener = config.getEmbeddedMessageConfig().getEmbeddedMessageListener();
                }
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

            initABTestDetails(userId);

            // Retrieve values for resource/campaigns flush frequencies and ETag
            campaignsAndResourcesFlushFrequency = settings.getInt(SDK_PREFS_KEY_FLUSH_FREQ, SWRVE_DEFAULT_CAMPAIGN_RESOURCES_FLUSH_FREQUENCY);
            campaignsAndResourcesFlushRefreshDelay = getFlushRefreshDelay();
            campaignsAndResourcesLastETag = multiLayerLocalStorage.getCacheEntry(userId, CACHE_ETAG);

            startCampaignsAndResourcesTimer(true);

            SwrveLogger.i("Init finished");
        } catch (Exception exp) {
            SwrveLogger.e("Swrve init failed", exp);
        }
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

                    Map<String, String> personalizationProperties = retrievePersonalizationProperties(lastEventPayloadUsed, properties);

                    if (message.supportsOrientation(getDeviceOrientation())) {
                        if (SwrveMessageTextTemplatingChecks.checkTextTemplating(message, personalizationProperties)) {
                            Intent intent = new Intent(ctx, SwrveInAppMessageActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, message.getId());
                            
                            if (personalizationProperties != null) {
                                // Cannot pass a Map to intent, converting to HashMap
                                HashMap<String, String> personalization = new HashMap<>(personalizationProperties);
                                intent.putExtra(SwrveInAppMessageActivity.SWRVE_PERSONALISATION_KEY, personalization);
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

    protected abstract String getPlatformOS(Context context);

    protected void _sessionStart() {
        long sessionTime = getSessionTime();
        restClientExecutorExecute(() -> sendSessionStart(sessionTime));

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

    protected void _sendQueuedEvents(final String userId, final String sessionToken, final boolean updateSentFlag) {
        if (profileManager.getTrackingState() == EVENT_SENDING_PAUSED) {
            SwrveLogger.d("SwrveSDK tracking state:EVENT_SENDING_PAUSED so cannot send events now.");
            return;
        }
        if (SwrveHelper.isNotNullOrEmpty(userId) && SwrveHelper.isNotNullOrEmpty(sessionToken)) {
            restClientExecutorExecute(() -> {
                boolean hasQueuedEvents = multiLayerLocalStorage.hasQueuedEvents(userId);
                if (hasQueuedEvents) {
                    if (updateSentFlag) {
                        eventsWereSent = true;
                    }

                    String deviceId = getDeviceId();
                    SwrveEventsManager swrveEventsManager = getSwrveEventsManager(userId, deviceId, sessionToken);
                    swrveEventsManager.sendStoredEvents(multiLayerLocalStorage);
                } else {
                    SwrveLogger.d("SwrveSDK no event to send");
                }
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

        SwrveLogger.i("onPause");
        flushToDisk();
        // Session management
        generateNewSessionInterval();
        // Save campaign state if needed
        saveCampaignsState(profileManager.getUserId());
    }

    protected void _onStop(Activity activity) {
        if (foregroundActivity.equals(activity.getClass().getCanonicalName())) {
            foregroundActivity = "";
            shutdownCampaignsAndResourcesTimer();
        }
    }

    protected void _onResume(Activity activity) {
        SwrveLogger.i("onResume");
        foregroundActivity = activity.getClass().getCanonicalName();

        long currentTime = getSessionTime();
        boolean sessionStart = currentTime > lastSessionTick;
        if (sessionStart) {
            sessionStart();
        } else {
            if (config.isSendQueuedEventsOnResume()) {
                sendQueuedEvents();
            }
        }
        generateNewSessionInterval();

        startCampaignsAndResourcesTimer(sessionStart);
        disableAutoShowAfterDelay();

        // Detect if user is influenced by a push notification
        campaignInfluence.processInfluenceData(getContext(), this);

        loadCampaignFromNotification();
    }

    protected void loadCampaignFromNotification() {
        if (notificationSwrveCampaignId != null) {
            Bundle b = new Bundle();
            b.putString(SwrveNotificationConstants.SWRVE_CAMPAIGN_KEY, notificationSwrveCampaignId);
            initSwrveDeepLinkManager();
            this.swrveDeeplinkManager.handleDeeplink(b);
            notificationSwrveCampaignId = null;
        }
    }

    protected void _shutdown() {

        SwrveLogger.i("Shutting down the SDK");

        // Forget the initialised time
        initialisedTime = null;

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

        if (lifecycleExecutor != null) {
            try {
                lifecycleExecutor.shutdown();
                lifecycleExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                SwrveLogger.e("Exception occurred shutting down lifecycleExecutorQueue", e);
            }
        }
        unregisterActivityLifecycleCallbacks();
    }

    protected void _stopTracking() {

        started = false;
        profileManager.setTrackingState(STOPPED);
        queueDeviceUpdateNow(profileManager.getUserId(), profileManager.getSessionToken(), true);

        shutdownCampaignsAndResourcesTimer();

        clearAllAuthenticatedNotifications();

        Activity currentActivity = getActivityContext();
        if (currentActivity != null && (currentActivity instanceof SwrveInAppMessageActivity || currentActivity instanceof ConversationActivity)) {
            currentActivity.finish();
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
                deviceInfo.put(SWRVE_DEVICE_TYPE, SwrveHelper.getPlatformDeviceType(contextRef));
            } catch (Exception exp) {
                SwrveLogger.e("Get device screen info failed", exp);
            }
            //OS
            deviceInfo.put(SWRVE_OS, SwrveBase.this.getPlatformOS(contextRef));
            deviceInfo.put(SWRVE_LANGUAGE, SwrveBase.this.language);
            String deviceRegion = Locale.getDefault().getCountry();
            deviceInfo.put(SWRVE_DEVICE_REGION, deviceRegion);
            deviceInfo.put(SWRVE_SDK_VERSION, PLATFORM + version);
            deviceInfo.put(SWRVE_APP_STORE, config.getAppStore());
            deviceInfo.put(SWRVE_SDK_FLAVOUR, Swrve.FLAVOUR);
            SwrveInitMode mode = config.getInitMode();
            String initModeDeviceInfo = mode.toString().toLowerCase(Locale.ENGLISH);
            if (config.isAutoStartLastUser()) {
                initModeDeviceInfo += "_auto";
            }
            deviceInfo.put(SWRVE_INIT_MODE, initModeDeviceInfo);
            deviceInfo.put(SWRVE_TRACKING_STATE, profileManager.getTrackingState());
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

            if (notificationsEnabled) {
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
                                    } catch (JSONException e) {
                                        SwrveLogger.e("SwrveSDK unable to decode user_content JSON : \"%s\".", response.responseBody);
                                        throw e;
                                    }

                                    boolean loadPreviousCampaignState = true;
                                    if (responseJson.toString().equals("{}")) { // if response is {} then etag hasn't changed.
                                        SwrveLogger.d("SwrveSDK etag has not changed");
                                    } else if (responseJson.has("qa")) {
                                        SwrveLogger.i("SwrveSDK You are a QA user!");
                                        JSONObject jsonQa = responseJson.getJSONObject("qa");
                                        boolean wasPreviouslyResetDevice = QaUser.isResetDevice();
                                        boolean resetDevice = jsonQa.optBoolean("reset_device_state", false);
                                        if (!wasPreviouslyResetDevice && resetDevice) {
                                            loadPreviousCampaignState = false;
                                        }
                                        updateQaUser(jsonQa.toString());
                                    } else {
                                        updateQaUser("");
                                    }

                                    if (responseJson.has("flush_frequency")) {
                                        Integer flushFrequency = responseJson.getInt("flush_frequency");
                                        campaignsAndResourcesFlushFrequency = flushFrequency;
                                        settingsEditor.putInt(SDK_PREFS_KEY_FLUSH_FREQ, campaignsAndResourcesFlushFrequency);
                                    }

                                    if (responseJson.has("flush_refresh_delay")) {
                                        Integer flushDelay = responseJson.getInt("flush_refresh_delay");
                                        campaignsAndResourcesFlushRefreshDelay = flushDelay;
                                        settingsEditor.putInt(SDK_PREFS_KEY_FLUSH_DELAY, campaignsAndResourcesFlushRefreshDelay);
                                    }

                                    if (responseJson.has("campaigns")) {
                                        JSONObject campaignJson = responseJson.getJSONObject("campaigns");
                                        saveCampaignsInCache(campaignJson);
                                        loadCampaignsFromJSON(userId, campaignJson, campaignsState, loadPreviousCampaignState);
                                        autoShowMessages();

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
                                    }

                                    if (responseJson.has("real_time_user_properties")) {
                                        JSONObject realTimeUserPropertiesJson = responseJson.getJSONObject("real_time_user_properties");
                                        realTimeUserProperties = SwrveHelper.JSONToMap(realTimeUserPropertiesJson);
                                        saveRealTimeUserPropertiesInCache(realTimeUserPropertiesJson);
                                    }

                                    if (responseJson.has("user_resources") || responseJson.has("real_time_user_properties")) {
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
        SwrveConversationCampaign campaign = null;

        Date now = getNow();
        Map<Integer, Integer> availableCampaignsIgnoredList = new HashMap<>();
        Map<Integer, QaCampaignInfo> qaCampaignInfoMap = new HashMap<>();

        if (campaigns != null) {
            if (!campaignDisplayer.checkAppCampaignRules(campaigns.size(), "conversation", event, payload, now)) {
                return null;
            }
            synchronized (campaigns) {
                List<SwrveConversation> availableConversations = new ArrayList<>();
                // Select messages with higher priority
                int minPriority = Integer.MAX_VALUE;
                List<SwrveConversation> candidateConversations = new ArrayList<>();
                for (SwrveBaseCampaign nextCampaign : campaigns) {
                    if (nextCampaign instanceof SwrveConversationCampaign) {
                        SwrveConversation nextConversation = ((SwrveConversationCampaign) nextCampaign).getConversationForEvent(event, payload, now, qaCampaignInfoMap);
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
                    campaign = candidateConversations.get(0).getCampaign();
                }
                if (QaUser.isLoggingEnabled() && campaign != null && result != null) {
                    // A message was chosen, set the reason for the others
                    for (SwrveConversation otherMessage : availableConversations) {
                        if (otherMessage != result) {
                            int otherCampaignId = otherMessage.getCampaign().getId();
                            if (!availableCampaignsIgnoredList.containsKey(otherCampaignId)) {
                                availableCampaignsIgnoredList.put(otherCampaignId, otherMessage.getId());
                                String resultText = "Campaign " + campaign.getId() + " was selected for display ahead of this campaign";
                                int variantId = otherMessage.getCampaign().getConversation().getId();
                                qaCampaignInfoMap.put(otherCampaignId, new QaCampaignInfo(otherCampaignId, variantId, CONVERSATION, false, resultText));
                            }
                        }
                    }
                }
            }
        }

        QaUser.campaignTriggeredConversation(event, payload, result != null, qaCampaignInfoMap);

        if (result == null) {
            SwrveLogger.w("Not showing message: no candidate messages for %s", event);
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
                        result = ((SwrveInAppCampaign) campaign).getMessageForId(messageId);
                    }
                }
            }
        }

        if (result == null) {
            SwrveLogger.i("Not showing messages: no candidate messages");
        }

        return result;
    }

    protected SwrveEmbeddedMessage _getEmbeddedMessageForId(int messageId) {
        SwrveEmbeddedMessage result = null;

        if (campaigns != null && campaigns.size() > 0) {
            synchronized (campaigns) {
                Iterator<SwrveBaseCampaign> itCampaign = campaigns.iterator();
                while (itCampaign.hasNext() && result == null) {
                    SwrveBaseCampaign campaign = itCampaign.next();
                    if (campaign instanceof SwrveEmbeddedCampaign) {
                        SwrveEmbeddedMessage candidateMessage = ((SwrveEmbeddedCampaign) campaign).getMessage();
                        if (messageId == candidateMessage.getId()) {
                            result = candidateMessage;
                        }
                    }
                }
            }
        }

        if (result == null) {
            SwrveLogger.i("Not returning embedded message: no candidate embedded messages");
        }

        return result;
    }

    @SuppressLint("UseSparseArrays")
    protected SwrveBaseMessage _getBaseMessageForEvent(String event, Map<String, String> payload, SwrveOrientation orientation) {
        SwrveBaseMessage result = null;
        SwrveBaseCampaign campaign = null;

        Date now = getNow();
        Map<Integer, Integer> availableCampaignsIgnoredList = new HashMap<>();
        Map<Integer, QaCampaignInfo> qaCampaignInfoMap = new HashMap<>();

        Map<String, String> properties = retrievePersonalizationProperties(lastEventPayloadUsed, null);

        if (campaigns != null) {
            if (!campaignDisplayer.checkAppCampaignRules(campaigns.size(), "message", event, payload, now)) {
                return null;
            }
            synchronized (campaigns) {
                List<SwrveBaseMessage> availableMessages = new ArrayList<>();
                // Select messages with higher priority
                int minPriority = Integer.MAX_VALUE;
                List<SwrveBaseMessage> candidateMessages = new ArrayList<>();
                for (SwrveBaseCampaign nextCampaign : campaigns) {
                    SwrveBaseMessage nextMessage = null;
                    if (nextCampaign instanceof SwrveInAppCampaign) {
                        nextMessage = ((SwrveInAppCampaign) nextCampaign).getMessageForEvent(event, payload, now, qaCampaignInfoMap, properties);
                    } else if (nextCampaign instanceof SwrveEmbeddedCampaign) {
                        nextMessage = ((SwrveEmbeddedCampaign) nextCampaign).getMessageForEvent(event, payload, now, qaCampaignInfoMap);
                    }

                    if (nextMessage != null) {
                        availableMessages.add((nextMessage));
                    }
                }

                // rank all of them by priority
                for (SwrveBaseMessage nextMessage : availableMessages) {
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

                // Select randomly from the highest messages
                Collections.shuffle(candidateMessages);
                Iterator<SwrveBaseMessage> itCandidateMessage = candidateMessages.iterator();
                while (campaign == null && itCandidateMessage.hasNext()) {
                    SwrveBaseMessage candidateMessage = itCandidateMessage.next();
                    // Check that the message supports the current orientation
                    if (candidateMessage.supportsOrientation(orientation)) {
                        result = candidateMessage;
                        campaign = candidateMessage.getCampaign();
                    } else {
                        if (QaUser.isLoggingEnabled()) {
                            int campaignId = candidateMessage.getCampaign().getId();
                            availableCampaignsIgnoredList.put(campaignId, candidateMessage.getId());
                            String resultText = "Message didn't support the given orientation: " + orientation;
                            int variantId = candidateMessage.getId();
                            qaCampaignInfoMap.put(campaignId, new QaCampaignInfo(campaignId, variantId, candidateMessage.getCampaign().getCampaignType(), false, resultText));
                        }
                    }
                }

                if (QaUser.isLoggingEnabled() && campaign != null && result != null) {
                    // A message was chosen, set the reason for the others
                    for (SwrveBaseMessage otherMessage : availableMessages) {
                        if (otherMessage != result) {
                            int otherCampaignId = otherMessage.getCampaign().getId();
                            if (!availableCampaignsIgnoredList.containsKey(otherCampaignId)) {
                                availableCampaignsIgnoredList.put(otherCampaignId, otherMessage.getId());
                                String resultText = "Campaign " + campaign.getId() + " was selected for display ahead of this campaign";
                                int variantId = otherMessage.getId();
                                qaCampaignInfoMap.put(otherCampaignId, new QaCampaignInfo(otherCampaignId, variantId, otherMessage.getCampaign().getCampaignType(), false, resultText));
                            }

                        }
                    }
                }
            }
        }

        QaUser.campaignTriggeredMessage(event, payload, result != null, qaCampaignInfoMap);

        if (result == null) {
            SwrveLogger.w("Not showing message: no candidate messages for %s", event);
        }

        return result;
    }

    protected void _buttonWasPressedByUser(SwrveButton button) {
        if (button.getActionType() != SwrveActionType.Dismiss) {
            String clickEvent = "Swrve.Messages.Message-" + button.getMessage().getId() + ".click";
            SwrveLogger.i("Sending click event: %s(%s)", clickEvent, button.getName());
            Map<String, String> payload = new HashMap<>();
            payload.put("name", button.getName());
            payload.put("embedded", "false");

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

            Map<String, String> payload = new HashMap<>();
            payload.put("embedded", "false");
            queueMessageImpressionEvent(message, payload);
        }
    }

    protected void _embeddedMessageWasShownToUser(SwrveEmbeddedMessage message) {
        if (message != null) {
            campaignDisplayer.setMessageMinDelayThrottle(getNow());
            campaignDisplayer.decrementMessagesLeftToShow();

            // Update next for round robin
            SwrveEmbeddedCampaign campaign = message.getCampaign();
            if (campaign != null) {
                campaign.messageWasShownToUser();
            }

            Map<String, String> payload = new HashMap<>();
            payload.put("embedded", "true");
            queueMessageImpressionEvent(message, payload);
        }
    }

    protected void queueMessageImpressionEvent(SwrveBaseMessage message, Map<String, String> payload) {
        String viewEvent = "Swrve.Messages.Message-" + message.getId() + ".impression";
        SwrveLogger.i("Sending view event: %s" + viewEvent);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", viewEvent);
        queueEvent(profileManager.getUserId(), "event", parameters, payload, false);
        saveCampaignsState(profileManager.getUserId());
    }

    protected void _embeddedMessageButtonWasPressed(SwrveEmbeddedMessage message, String buttonName) {
        if (message != null) {
            String clickEvent = "Swrve.Messages.Message-" + message.getId() + ".click";
            SwrveLogger.i("Sending click event: %s(%s)", clickEvent, buttonName);
            Map<String, String> payload = new HashMap<>();
            payload.put("name", buttonName);
            payload.put("embedded", "true");
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", clickEvent);
            queueEvent(profileManager.getUserId(), "event", parameters, payload, false);
        }
    }

    protected String _getPersonalizedEmbeddedMessageData(SwrveEmbeddedMessage message, Map<String, String> personalizationProperties) {
        if (message != null) {
            try {
                if(message.getType() == SwrveEmbeddedMessage.EMBEDDED_CAMPAIGN_TYPE.JSON) {
                    return SwrveTextTemplating.applytoJSON(message.getData(), personalizationProperties);
                } else {
                    return SwrveTextTemplating.apply(message.getData(), personalizationProperties);
                }

            } catch(SwrveSDKTextTemplatingException exception) {
                SwrveEmbeddedCampaign campaign = message.getCampaign();
                QaUser.embeddedPersonalizationFailed(campaign.getId(), message.getId(), message.getData(), "Failed to resolve personalization");
                SwrveLogger.e("Campaign id:%s Could not resolve, error with personalization", exception, campaign.getId());
            }
        }
        return null;
    }

    protected String _getPersonalizedText(String text, Map<String, String> personalizationProperties) {
        if (text != null) {
            try {
                return SwrveTextTemplating.apply(text, personalizationProperties);
            } catch(SwrveSDKTextTemplatingException exception) {
                SwrveLogger.e("Could not resolve, error with personalization", exception);
            }
        }
        return null;
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
            eventListener = new SwrveEventListener(this, messageListener, conversationListener, embeddedMessageListener);
        } else {
            eventListener = null;
        }
    }

    protected void _setConversationListener(SwrveConversationListener listener) {
        this.conversationListener = listener;
        if (conversationListener != null) {
            eventListener = new SwrveEventListener(this, messageListener, conversationListener, embeddedMessageListener);
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
            if (isValidEventName(name)) {
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
            if (isValidEventName(name)) {
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

        for (String restricted : restrictedNamesStartWith) {
            if (name == null || name.startsWith(restricted)) {
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
            _sendQueuedEvents(userId, sessionToken, true);
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

    protected void onStop(Activity activity) {
        try {
            _onStop(activity);
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

    @Override
    public void shutdown() {
        try {
            _shutdown();
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void stopTracking() {
        try {
            _stopTracking();
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
    public boolean isTrackingStateStopped() {
        try {
            return profileManager.getTrackingState() == STOPPED;
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return true;
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

    public SwrveMessage getMessageForId(int messageId) {
        try {
            return _getMessageForId(messageId);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    public SwrveEmbeddedMessage getEmbeddedMessageForId(int messageId) {
        try {
            return _getEmbeddedMessageForId(messageId);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    protected SwrveBaseMessage getBaseMessageForEvent(String event) {
        try {
            return getBaseMessageForEvent(event, new HashMap<String, String>(), SwrveOrientation.Both);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    protected SwrveBaseMessage getBaseMessageForEvent(String event, Map<String, String> payload) {
        try {
            return _getBaseMessageForEvent(event, payload, SwrveOrientation.Both);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    protected SwrveBaseMessage getBaseMessageForEvent(String event, Map<String, String> payload, SwrveOrientation orientation) {
        try {
            return _getBaseMessageForEvent(event, payload, orientation);
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
    public void embeddedMessageWasShownToUser(SwrveEmbeddedMessage message) {
        if (!isSdkReady()) return;

        try {
            _embeddedMessageWasShownToUser(message);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public void embeddedMessageButtonWasPressed(SwrveEmbeddedMessage message, String buttonName) {
        if (!isSdkReady()) return;

        try {
            _embeddedMessageButtonWasPressed(message, buttonName);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
    }

    @Override
    public String getPersonalizedEmbeddedMessageData(SwrveEmbeddedMessage message, Map<String, String> personalizationProperties) {
        if (!isSdkReady()) return null;

        try {
            return _getPersonalizedEmbeddedMessageData(message, personalizationProperties);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
    }

    @Override
    public String getPersonalizedText(String text, Map<String, String> personalizationProperties) {
        if (!isSdkReady()) return null;

        try {
            return _getPersonalizedText(text, personalizationProperties);
        } catch (Exception e) {
            SwrveLogger.e("Exception thrown in Swrve SDK", e);
        }
        return null;
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

        Map<String, String> personalizedProperties = retrievePersonalizationProperties(null, properties);

        if (campaigns != null) {
            synchronized (campaigns) {
                for (int i = 0; i < campaigns.size(); i++) {
                    SwrveBaseCampaign campaign = campaigns.get(i);
                    if (campaign.isMessageCenter()
                            && campaign.getStatus() != SwrveCampaignState.Status.Deleted
                            && campaign.isActive(getNow())
                            && campaign.supportsOrientation(orientation)
                            && campaign.areAssetsReady(getAssetsOnDisk(), personalizedProperties)) {
                        if (campaign instanceof SwrveInAppCampaign) {
                            // Check personalization for matching key/value pairs
                            SwrveMessage message = ((SwrveInAppCampaign) campaign).getMessage();
                            boolean messagesCanResolve = true;
                            if (message.supportsOrientation(orientation)) {
                                messagesCanResolve = SwrveMessageTextTemplatingChecks.checkTextTemplating(message, personalizedProperties);
                            }
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
            Map<String, String> personalizedProperties = retrievePersonalizationProperties(null, properties);
            SwrveInAppCampaign iamCampaign = (SwrveInAppCampaign) campaign;
            if (iamCampaign != null && iamCampaign.getMessage() != null && messageListener != null) {
                // Display message in the in-app campaign
                messageListener.onMessage(iamCampaign.getMessage(), personalizedProperties);
                return true;
            } else {
                SwrveLogger.e("No in-app message or message listener.");
            }
        } else if (campaign instanceof SwrveConversationCampaign) {
            SwrveConversationCampaign conversationCampaign = (SwrveConversationCampaign) campaign;
            if (conversationCampaign != null && conversationCampaign.getConversation() != null && conversationListener != null) {
                conversationListener.onMessage(conversationCampaign.getConversation());
                return true;
            } else {
                SwrveLogger.e("No conversation campaign or conversation listener.");
            }
        } else if (campaign instanceof SwrveEmbeddedCampaign) {
            SwrveEmbeddedCampaign embeddedCampaign = (SwrveEmbeddedCampaign) campaign;
            if (embeddedCampaign != null && embeddedMessageListener != null) {
                Map<String, String> personalizedProperties = retrievePersonalizationProperties(null, properties);
                embeddedMessageListener.onMessage(getContext(), embeddedCampaign.getMessage(), personalizedProperties);
                return true;
            } else {
                SwrveLogger.e("No embedded message or embedded message listener.");
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
        params.put("embedded_campaign_version", String.valueOf(EMBEDDED_CAMPAIGN_VERSION));
        params.put("in_app_version", String.valueOf(IN_APP_CAMPAIGN_VERSION));

        // Device info
        params.put("device_width", String.valueOf(deviceWidth));
        params.put("device_height", String.valueOf(deviceHeight));
        params.put("device_dpi", String.valueOf(deviceDpi));
        params.put("android_device_xdpi", String.valueOf(androidDeviceXdpi));
        params.put("android_device_ydpi", String.valueOf(androidDeviceYdpi));
        params.put("orientation", config.getOrientation().toString().toLowerCase(Locale.US));
        params.put("device_name", getDeviceName());
        params.put("os_version", Build.VERSION.RELEASE);

        Context contextRef = context.get();
        if (contextRef != null) {
            params.put("os", this.getPlatformOS(contextRef));
            params.put("device_type", SwrveHelper.getPlatformDeviceType(contextRef));
        }

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
        return getEventsServer() + BATCH_EVENTS_ACTION;
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
        QaUser.wrappedEvents(new ArrayList<>(events)); // use copy of events
        getSwrveBackgroundEventSender(context).send(userId, events);
    }

    protected SwrveBackgroundEventSender getSwrveBackgroundEventSender(Context context) {
        return new SwrveBackgroundEventSender(this, context);
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
        if (isEngageActivity(activity)) {
            return;
        }
        bindToActivity(activity);
        lifecycleExecutorExecute(() -> {
            if (isStarted()) {
                onCreate(activity);
            }
        });
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // empty
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (isEngageActivity(activity)) {
            return;
        }
        bindToActivity(activity);
        lifecycleExecutorExecute(() -> {
            if (isStarted()) {
                onResume(activity);
            }
        });
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (isEngageActivity(activity)) {
            return;
        }
        lifecycleExecutorExecute(() -> {
            if (isStarted()) {
                onPause();
            }
        });
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (isEngageActivity(activity)) {
            return;
        }
        lifecycleExecutorExecute(() -> {
            if (isStarted()) {
                onStop(activity);
            }
        });
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // empty
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // empty
    }

    /*
     * eo Application.ActivityLifecycleCallbacks
     */

    private boolean isEngageActivity(Activity activity) {
        boolean isEngageActivity = false;
        String activityName = activity.getClass().getCanonicalName();
        if (activityName != null && activityName.contains("SwrveNotificationEngageActivity")) {
            isEngageActivity = true;
            SwrveLogger.v("SwrveNotificationEngageActivity has been launched so skip ActivityLifecycleCallbacks method and use next Activity that is launched");
        }
        return isEngageActivity;
    }

    @Override
    public void setNotificationSwrveCampaignId(String swrveCampaignId) {
        if (!isSdkReady()) return;
        notificationSwrveCampaignId = swrveCampaignId;
    }

    protected void switchUser(String newUserId) {

        // don't do anything if the current user id is the same as the new one
        if (isStarted()) {
            if (SwrveHelper.isNullOrEmpty(newUserId) || newUserId.equals(getUserId())) {
                enableEventSending();
                queuePausedEvents();
                return;
            }
        }

        clearAllAuthenticatedNotifications();

        profileManager.setUserId(newUserId);
        profileManager.updateSessionToken();

        if (getActivityContext() != null) {
            initialised = false;
            QaUser.update();
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
        if (isStarted()) {
            sendQueuedEvents();
        }
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
        identifiedOnAnotherDevice = false; // reset the flag
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
        profileManager.setTrackingState(STARTED);
        startCampaignsAndResourcesTimer(false);
        sendQueuedEvents();
    }

    protected void pauseEventSending() {
        profileManager.setTrackingState(EVENT_SENDING_PAUSED);
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
        return settings.getInt(SDK_PREFS_KEY_FLUSH_DELAY, SWRVE_DEFAULT_CAMPAIGN_RESOURCES_FLUSH_REFRESH_DELAY);
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
        if (!isStarted()) {
            this.profileManager.persistUser();
            clearAllAuthenticatedNotifications();
            init(activity);
        }
    }

    @Override
    public void start(Activity activity, String userId) {
        if (config.getInitMode() == SwrveInitMode.AUTO) {
            throw new RuntimeException("Cannot call start method when running on SwrveInitMode.AUTO mode");
        }

        if (!isStarted()) {
            profileManager.setUserId(userId);
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

    protected boolean isSdkReady() {
        boolean isSdkReady = true;
        if (profileManager.getTrackingState() == STOPPED) {
            SwrveLogger.w("Warning: SwrveSDK is stopped and needs to be started before calling this api.");
            isSdkReady = false;
        } else if (!isStarted()) {
            SwrveLogger.w("Warning: SwrveSDK needs to be started before calling this api.");
            isSdkReady = false;
        }
        return isSdkReady;
    }

    protected void sendSessionStart(long time) {
        int seqNum = getNextSequenceNumber();
        List<String> sessionStartEvent = EventHelper.createSessionStartEvent(time, seqNum);
        try {
            String userId = profileManager.getUserId();
            String deviceId = SwrveLocalStorageUtil.getDeviceId(multiLayerLocalStorage);
            String sessionToken = profileManager.getSessionToken();
            SwrveEventsManager swrveEventsManager = getSwrveEventsManager(userId, deviceId, sessionToken);
            swrveEventsManager.storeAndSendEvents(sessionStartEvent, multiLayerLocalStorage.getPrimaryStorage());
        } catch (Exception e) {
            SwrveLogger.e("Exception sending session start event", e);
        }

        if (eventListener != null) {
            eventListener.onEvent(EventHelper.getEventName("session_start", null), null);
        }
        QaUser.wrappedEvents(sessionStartEvent);
    }

    protected SwrveEventsManager getSwrveEventsManager(String userId, String deviceId, String sessionToken) {
        return new SwrveEventsManagerImp(context.get(), config, restClient, userId, appVersion, sessionToken, deviceId);
    }

    @Override
    public void saveEvent(String event) {
        try {
            openLocalStorageConnection();
            multiLayerLocalStorage.addEvent(getUserId(), event); // Not calling QaUser.wrappedEvents on purpose. Use queueEvent if that is desired.
            multiLayerLocalStorage.flush();
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK: Exception saving event to storage.", e);
        }
    }
}
