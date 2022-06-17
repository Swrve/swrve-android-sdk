package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.CACHE_CAMPAIGNS;
import static com.swrve.sdk.ISwrveCommon.CACHE_CAMPAIGNS_STATE;
import static com.swrve.sdk.ISwrveCommon.CACHE_ETAG;
import static com.swrve.sdk.ISwrveCommon.CACHE_QA;
import static com.swrve.sdk.ISwrveCommon.CACHE_REALTIME_USER_PROPERTIES;
import static com.swrve.sdk.ISwrveCommon.CACHE_RESOURCES;
import static com.swrve.sdk.SwrveTrackingState.EVENT_SENDING_PAUSED;
import static com.swrve.sdk.SwrveTrackingState.STARTED;
import static com.swrve.sdk.SwrveTrackingState.STOPPED;
import static com.swrve.sdk.SwrveTrackingState.UNKNOWN;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.device.AndroidTelephonyManagerWrapper;
import com.swrve.sdk.device.ITelephonyManager;
import com.swrve.sdk.localstorage.InMemoryLocalStorage;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveConversationCampaign;
import com.swrve.sdk.messaging.SwrveEmbeddedCampaign;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedMessageListener;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessagePage;
import com.swrve.sdk.messaging.SwrveMessagePersonalizationProvider;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.RESTClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Internal base class implementation of the Swrve SDK.
 */
abstract class SwrveImp<T, C extends SwrveConfigBase> implements ISwrveCampaignManager, Application.ActivityLifecycleCallbacks {
    protected static final String PLATFORM = "Android ";
    protected static String version = "10.0.0";
    protected static final int CAMPAIGN_ENDPOINT_VERSION = 9;
    protected static final int EMBEDDED_CAMPAIGN_VERSION = 1;
    protected static final int IN_APP_CAMPAIGN_VERSION = 7;
    protected static final String CAMPAIGN_RESPONSE_VERSION = "2";
    protected static final String USER_CONTENT_ACTION = "/api/1/user_content";
    protected static final String USER_RESOURCES_DIFF_ACTION = "/api/1/user_resources_diff";
    protected static final String BATCH_EVENTS_ACTION = "/1/batch";
    protected static final String IDENTITY_ACTION = "/identify";
    protected static final String EMPTY_JSON_ARRAY = "[]";
    protected static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    protected static final String REFERRER = "referrer";
    protected static final int SWRVE_DEFAULT_CAMPAIGN_RESOURCES_FLUSH_FREQUENCY = 60000;
    protected static final int SWRVE_DEFAULT_CAMPAIGN_RESOURCES_FLUSH_REFRESH_DELAY = 5000;
    protected static final String SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER = "Swrve.Messages.showAtSessionStart";
    protected static final List<String> SUPPORTED_REQUIREMENTS = Arrays.asList("android");
    protected static int DEFAULT_DELAY_FIRST_MESSAGE = 150;
    protected static long DEFAULT_MAX_SHOWS = 99999;
    protected static int DEFAULT_MIN_DELAY = 55;
    protected WeakReference<Application> application;
    protected WeakReference<Context> context;
    protected WeakReference<Activity> activityContext;
    protected String appVersion;
    protected int appId;
    protected String apiKey;
    protected SwrveProfileManager profileManager;
    protected String language;
    protected C config;
    protected ISwrveEventListener eventListener;
    protected SwrveEmbeddedMessageListener embeddedMessageListener;
    protected SwrveMessagePersonalizationProvider personalizationProvider;
    protected SwrveResourcesListener resourcesListener;
    protected ExecutorService autoShowExecutor;
    protected long newSessionInterval;
    protected long lastSessionTick;
    protected SwrveMultiLayerLocalStorage multiLayerLocalStorage;
    protected IRESTClient restClient;
    protected ExecutorService lifecycleExecutor;
    protected ExecutorService storageExecutor;
    protected ExecutorService restClientExecutor;
    protected ScheduledThreadPoolExecutor campaignsAndResourcesExecutor;
    protected SwrveResourceManager resourceManager;
    protected List<SwrveBaseCampaign> campaigns;
    protected SwrveCampaignDisplayer campaignDisplayer;
    protected Map<Integer, SwrveCampaignState> campaignsState;
    protected Map<String, String> realTimeUserProperties;
    protected SwrveAssetsManager swrveAssetsManager;
    protected SparseArray<String> appStoreURLs;
    protected boolean autoShowMessagesEnabled;
    protected Integer campaignsAndResourcesFlushFrequency;
    protected Integer campaignsAndResourcesFlushRefreshDelay;
    protected String campaignsAndResourcesLastETag;
    protected Date campaignsAndResourcesLastRefreshed;
    protected boolean campaignsAndResourcesInitialized = false;
    protected boolean eventsWereSent = false;
    protected boolean initialised = false;
    protected boolean started = false;
    protected Date initialisedTime;
    protected int deviceWidth;
    protected int deviceHeight;
    protected float deviceDpi;
    protected float androidDeviceXdpi;
    protected float androidDeviceYdpi;
    protected String simOperatorName;
    protected String simOperatorIsoCountryCode;
    protected String simOperatorCode;
    protected String androidId;
    protected SwrveDeeplinkManager swrveDeeplinkManager;
    protected SwrveCampaignInfluence campaignInfluence = new SwrveCampaignInfluence();
    protected String notificationSwrveCampaignId;
    protected boolean identifiedOnAnotherDevice;
    protected SwrveSessionListener sessionListener;
    protected List<EventQueueItem> pausedEvents = Collections.synchronizedList(new ArrayList<EventQueueItem>());
    protected Map<String, String> lastEventPayloadUsed;
    protected String foregroundActivity = "";

    protected SwrveImp(Application application, int appId, String apiKey, C config) {
        SwrveLogger.setLoggingEnabled(config.isLoggingEnabled());
        if (appId <= 0 || SwrveHelper.isNullOrEmpty(apiKey)) {
            SwrveHelper.logAndThrowException("Please setup a correct appId and apiKey");
        }

        this.appId = appId;
        this.apiKey = apiKey;
        this.config = config;

        Context applicationContext = application.getApplicationContext();
        this.context = new WeakReference<>(applicationContext);
        this.application = new WeakReference<>(application);
        this.restClient = new RESTClient(config.getHttpTimeout(), config.getSSlSocketFactoryConfig());
        this.swrveAssetsManager = new SwrveAssetsManagerImp(applicationContext);
        this.newSessionInterval = config.getNewSessionInterval();
        this.multiLayerLocalStorage = new SwrveMultiLayerLocalStorage(new InMemoryLocalStorage());
        this.autoShowExecutor = Executors.newSingleThreadExecutor();
        this.storageExecutor = Executors.newSingleThreadExecutor();
        this.restClientExecutor = Executors.newSingleThreadExecutor();
        this.lifecycleExecutor = Executors.newSingleThreadExecutor();

        initProfileManager(applicationContext);
        initAppVersion(applicationContext, config);
        initDefaultUrls(config);
        initLanguage(config);

        registerActivityLifecycleCallbacks(); // always register to ensure first activity is triggered
    }

    private void initProfileManager(Context context) {
        profileManager = new SwrveProfileManager(context, appId, apiKey, config, restClient);
        lifecycleExecutorExecute(() -> {
            profileManager.initUserId();
            profileManager.initTrackingState();
            if (profileManager.getTrackingState() == STOPPED) {
                SwrveLogger.i("SwrveSDK is currently in stopped state and will not start until an api is called.");
            } else if (shouldAutostart()) {
                profileManager.persistUser();
                if (profileManager.getTrackingState() == UNKNOWN) {
                    profileManager.setTrackingState(STARTED);
                }
                started = true;
            }
        });
    }

    private boolean shouldAutostart() {
        boolean shouldAutostart = false;
        if (config.getInitMode() == SwrveInitMode.AUTO && config.isAutoStartLastUser()) {
            shouldAutostart = true;
        } else if (config.getInitMode() == SwrveInitMode.MANAGED && config.isAutoStartLastUser()) {
            String savedUserIdFromPrefs = profileManager.getSavedUserIdFromPrefs();
            if (savedUserIdFromPrefs != null) {
                shouldAutostart = true;
            }
        }
        return shouldAutostart;
    }

    private void initAppVersion(Context context, C config) {
        this.appVersion = config.getAppVersion();
        if (SwrveHelper.isNullOrEmpty(this.appVersion)) {
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                this.appVersion = pInfo.versionName;
            } catch (Exception ex) {
                SwrveLogger.e("Couldn't get app version from PackageManager. Please provide the app version manually through the config object.", ex);
            }
        }
    }

    private void initDefaultUrls(C config) {
        try {
            config.generateUrls(appId); // Generate default urls for the given app id
        } catch (MalformedURLException ex) {
            SwrveLogger.e("Couldn't generate urls for appId:" + appId, ex);
        }
    }

    private void initLanguage(C config) {
        if (SwrveHelper.isNullOrEmpty(config.getLanguage())) {
            this.language = SwrveHelper.toLanguageTag(Locale.getDefault());
        } else {
            this.language = config.getLanguage();
        }
    }

    protected void registerActivityLifecycleCallbacks() {
        Application application = this.application.get();
        if (application != null) {
            application.registerActivityLifecycleCallbacks(this);
            SwrveLogger.i("SwrveSDK registered ActivityLifecycleCallbacks.");
        }
    }

    protected void unregisterActivityLifecycleCallbacks() {
        Application application = this.application.get();
        if (application != null) {
            application.unregisterActivityLifecycleCallbacks(this);
            SwrveLogger.i("SwrveSDK unregistered ActivityLifecycleCallbacks.");
        }
    }

    protected void bindToActivity(Activity activity) {
        context = new WeakReference<>(activity.getApplicationContext());
        activityContext = new WeakReference<>(activity);
    }

    // Internal function to add a Swrve.iap event to the event queue.
    protected void _iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards, String receipt, String receiptSignature, String paymentProvider) {
        if (_iap_check_parameters(quantity, productId, productPrice, currency, paymentProvider)) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("local_currency", currency);
            parameters.put("cost", productPrice);
            parameters.put("product_id", productId);
            parameters.put("quantity", quantity);
            parameters.put("app_store", paymentProvider);
            parameters.put("rewards", rewards.getRewardsJSON());

            if (!SwrveHelper.isNullOrEmpty(receipt)) {
                parameters.put("receipt", receipt);
            }

            if (!SwrveHelper.isNullOrEmpty(receiptSignature)) {
                parameters.put("receipt_signature", receiptSignature);
            }

            queueEvent("iap", parameters, null);

            if (config.isAutoDownloadCampaignsAndResources()) {
                startCampaignsAndResourcesTimer(false);
            }
        }
    }

    protected boolean _iap_check_parameters(int quantity, String productId, double productPrice, String currency, String paymentProvider) throws IllegalArgumentException {
        // Strings cannot be null or empty
        if (SwrveHelper.isNullOrEmpty(productId)) {
            SwrveLogger.e("IAP event illegal argument: productId cannot be empty");
            return false;
        }
        if (SwrveHelper.isNullOrEmpty(currency)) {
            SwrveLogger.e("IAP event illegal argument: currency cannot be empty");
            return false;
        }
        if (SwrveHelper.isNullOrEmpty(paymentProvider)) {
            SwrveLogger.e("IAP event illegal argument: paymentProvider cannot be empty");
            return false;
        }
        if (quantity <= 0) {
            SwrveLogger.e("IAP event illegal argument: quantity must be greater than zero");
            return false;
        }
        if (productPrice < 0) {
            SwrveLogger.e("IAP event illegal argument: productPrice must be greater than or equal to zero");
            return false;
        }
        return true;
    }

    protected String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        } else {
            return manufacturer + " " + model;
        }
    }

    protected void processUserResourcesDiffData(String resourcesAsJSON, final SwrveUserResourcesDiffListener listener) {
        // Parse raw response
        JSONArray jsonResourcesDiff;
        try {
            jsonResourcesDiff = new JSONArray(resourcesAsJSON);
            // Convert to map
            Map<String, Map<String, String>> mapOldResources = new HashMap<>();
            Map<String, Map<String, String>> mapNewResources = new HashMap<>();
            for (int i = 0, j = jsonResourcesDiff.length(); i < j; i++) {
                Map<String, String> mapOldResourceValues = new HashMap<>();
                Map<String, String> mapNewResourceValues = new HashMap<>();
                JSONObject resourceJSON = jsonResourcesDiff.getJSONObject(i);
                String uid = resourceJSON.getString("uid");
                JSONObject resourceDiffsJSON = resourceJSON.getJSONObject("diff");
                @SuppressWarnings("unchecked")
                Iterator<String> it = resourceDiffsJSON.keys();
                while (it.hasNext()) {
                    String key = it.next();
                    mapOldResourceValues.put(key, resourceDiffsJSON.getJSONObject(key).getString("old"));
                    mapNewResourceValues.put(key, resourceDiffsJSON.getJSONObject(key).getString("new"));
                }
                mapOldResources.put(uid, mapOldResourceValues);
                mapNewResources.put(uid, mapNewResourceValues);
            }
            // Execute callback (NOTE: Executed in same thread!)
            listener.onUserResourcesDiffSuccess(mapOldResources, mapNewResources, resourcesAsJSON);
        } catch (Exception exp) {
            // Launch exception
            listener.onUserResourcesDiffError(exp);
        }
    }

    protected void queueEvent(String eventType, Map<String, Object> parameters, Map<String, String> payload) {
        String userId = profileManager.getUserId();
        queueEvent(userId, eventType, parameters, payload, true);
    }

    protected boolean queueEvent(String userId, String eventType, Map<String, Object> parameters, Map<String, String> payload, boolean triggerEventListener) {
        if (profileManager.getTrackingState() == EVENT_SENDING_PAUSED) {
            SwrveLogger.d("SwrveSDK event sending paused so attempt to queue events has failed. Will auto retry when event sending resumes.");
            pausedEvents.add(new EventQueueItem(userId, eventType, parameters, payload, triggerEventListener));
            return false;
        }
        try {
            storageExecutorExecute(new QueueEventRunnable(multiLayerLocalStorage, userId, eventType, parameters, payload));
            if (triggerEventListener && eventListener != null) {
                eventListener.onEvent(EventHelper.getEventName(eventType, parameters), payload);
            }
        } catch (Exception exp) {
            SwrveLogger.e("Unable to queue event", exp);
        }
        return true;
    }

    protected void deviceUpdate(String userId, JSONObject attributes) {
        if (attributes != null && attributes.length() != 0) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("attributes", attributes);
            queueEvent(userId, "device_update", parameters, null, true);
        }
    }

    protected boolean restClientExecutorExecute(Runnable runnable) {
        try {
            if (restClientExecutor.isShutdown()) {
                SwrveLogger.i("Trying to handle a rest execution while shutdown");
            } else {
                restClientExecutor.execute(SwrveRunnables.withoutExceptions(runnable));
                return true;
            }
        } catch (Exception e) {
            SwrveLogger.e("Error while scheduling a rest execution", e);
        }
        return false;
    }

    protected boolean storageExecutorExecute(Runnable runnable) {
        try {
            if (storageExecutor.isShutdown()) {
                SwrveLogger.i("Trying to handle a storage execution while shutdown");
            } else {
                storageExecutor.execute(SwrveRunnables.withoutExceptions(runnable));
                return true;
            }
        } catch (Exception e) {
            SwrveLogger.e("Error while scheduling a storage execution", e);
        }
        return false;
    }

    protected boolean lifecycleExecutorExecute(Runnable runnable) {
        try {
            if (lifecycleExecutor.isShutdown()) {
                SwrveLogger.i("Trying to handle a lifecycle execution while shutdown");
            } else {
                lifecycleExecutor.execute(SwrveRunnables.withoutExceptions(runnable));
                return true;
            }
        } catch (Exception e) {
            SwrveLogger.e("Error while scheduling a lifecycle execution", e);
        }
        return false;
    }

    protected long getSessionTime() {
        return getNow().getTime();
    }

    protected void generateNewSessionInterval() {
        lastSessionTick = getSessionTime() + newSessionInterval;
    }

    protected void buildDeviceInfo(Context context) {
        try {
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            int width = SwrveHelper.getDisplayWidth(context);
            int height = SwrveHelper.getDisplayHeight(context);
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

            // Set device info
            this.deviceWidth = width;
            this.deviceHeight = height;
            this.deviceDpi = metrics.densityDpi;
            this.androidDeviceXdpi = xdpi;
            this.androidDeviceYdpi = ydpi;

            // Carrier details
            ITelephonyManager tmanager = getTelephonyManager(context);
            this.simOperatorName = tmanager.getSimOperatorName();
            this.simOperatorIsoCountryCode = tmanager.getSimCountryIso();
            this.simOperatorCode = tmanager.getSimOperator();

            // Android ID
            if (config.isAndroidIdLoggingEnabled()) {
                this.androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        } catch (Exception exp) {
            SwrveLogger.e("Get device screen info failed", exp);
        }
    }

    protected ITelephonyManager getTelephonyManager(Context context) {
        return new AndroidTelephonyManagerWrapper(context);
    }

    protected boolean checkPermissionGranted(Context context, String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    protected void requestPermissions(Activity activity, String permissions[]) {
        ActivityCompat.requestPermissions(activity, permissions, 0);
    }

    @Override
    public Date getNow() {
        return new Date();
    }

    protected void saveCampaignsInCache(final JSONObject campaignContent) {
        final String userId = profileManager.getUserId(); // user can change so retrieve now as a final String for thread safeness
        storageExecutorExecute(() -> multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_CAMPAIGNS, campaignContent.toString(), getUniqueKey(userId)));
    }

    protected void saveResourcesInCache(final JSONArray resourcesContent) {
        final String userId = profileManager.getUserId(); // user can change so retrieve now as a final String for thread safeness
        storageExecutorExecute(() -> multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_RESOURCES, resourcesContent.toString(), getUniqueKey(userId)));
    }

    protected void saveRealTimeUserPropertiesInCache(final JSONObject userPropertiesContent) {
        final String userId = profileManager.getUserId();
        storageExecutorExecute(() -> multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_REALTIME_USER_PROPERTIES, userPropertiesContent.toString(), getUniqueKey(userId)));
    }

    protected void updateQaUser(final String qaUserJson) {
        final String userId = profileManager.getUserId(); // user can change so retrieve now as a final String for thread safeness
        storageExecutorExecute(() -> {
            multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_QA, qaUserJson, getUniqueKey(userId));
            QaUser.update();
        });
    }

    protected void autoShowMessages() {
        // Don't do anything if we've already shown a message or if its too long after session start
        if (!autoShowMessagesEnabled) {
            return;
        }

        // Only execute if at least 1 call to the /user_content api endpoint has been completed
        // And ensure all assets have been downloaded
        if (!campaignsAndResourcesInitialized || campaigns == null) {
            return;
        }

        Map<Integer, QaCampaignInfo> dummyQaCampaignInfoMap = new HashMap<>(); // no qalog triggered from this.
        Map<String, String> dummyEmptyPayload = new HashMap<>();
        for (final SwrveBaseCampaign campaign : campaigns) {
            final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
            boolean canTrigger = campaignDisplayer.canTrigger(campaign, SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER, dummyEmptyPayload, dummyQaCampaignInfoMap);
            if (canTrigger) {
                synchronized (this) {
                    if (autoShowMessagesEnabled && activityContext != null) {
                        Activity activity = activityContext.get();
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                autoShowConversation(swrve);
                                autoShowMessage(swrve);
                            });
                        }
                    }
                }
                break;
            }
        }
    }

    protected void autoShowMessage(SwrveBase<T, C> swrve) {
        try {
            if (autoShowMessagesEnabled) {
                SwrveBaseMessage message = swrve.getBaseMessageForEvent(SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER);
                if (message != null && message.supportsOrientation(getDeviceOrientation())) {
                    if (message instanceof SwrveMessage) {
                        displaySwrveMessage((SwrveMessage) message, null);
                    } else if (embeddedMessageListener != null && message instanceof SwrveEmbeddedMessage) {
                        Map<String, String> personalizationProperties = retrievePersonalizationProperties(null, null);
                        embeddedMessageListener.onMessage(swrve.getContext(), (SwrveEmbeddedMessage) message, personalizationProperties);
                    }
                    autoShowMessagesEnabled = false;
                }
            }
        } catch (Exception exp) {
            SwrveLogger.e("Could not launch campaign automatically.", exp);
        }
    }

    protected void displaySwrveMessage(final SwrveMessage message, Map<String, String> properties) {
        if (context == null || getContext() == null) {
            return;
        }

        Map<String, String> personalizationProperties = retrievePersonalizationProperties(lastEventPayloadUsed, properties);
        if (message.supportsOrientation(getDeviceOrientation())) {
            if (SwrveMessageTextTemplatingChecks.checkTextTemplating(message, personalizationProperties)) {
                Intent intent = new Intent(getContext(), SwrveInAppMessageActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, message.getId());

                if (personalizationProperties != null) {
                    // Cannot pass a Map to intent, converting to HashMap
                    HashMap<String, String> personalization = new HashMap<>(personalizationProperties);
                    intent.putExtra(SwrveInAppMessageActivity.SWRVE_PERSONALISATION_KEY, personalization);
                }

                getContext().startActivity(intent);
            }
        } else {
            SwrveLogger.i("Can't display the in-app message as it doesn't support the current orientation");
        }
    }

    protected void autoShowConversation(SwrveBase<T, C> swrve) {
        try {
            if (autoShowMessagesEnabled) {
                SwrveConversation conversation = swrve.getConversationForEvent(SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER, new HashMap<String, String>());
                if (conversation != null) {
                    ConversationActivity.showConversation(getContext(), conversation, config.getOrientation());
                    conversation.getCampaign().messageWasShownToUser();
                    autoShowMessagesEnabled = false;
                    QaUser.campaignTriggeredMessageNoDisplay(SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER, null);
                }
            }
        } catch (Exception exp) {
            SwrveLogger.e("Could not launch conversation automatically.", exp);
        }
    }

    /*
     * Ensure that after SwrveConfig.autoShowMessagesMaxDelay milliseconds autoshow is disabled
     */
    protected void disableAutoShowAfterDelay() {
        final ScheduledExecutorService timedService = Executors.newSingleThreadScheduledExecutor();
        timedService.schedule(() -> {
            try {
                autoShowMessagesEnabled = false;
            } finally {
                timedService.shutdownNow();
            }
        }, config.getInAppMessageConfig().getAutoShowMessagesMaxDelay(), TimeUnit.MILLISECONDS);
    }

    @SuppressLint("UseSparseArrays")
    protected void loadCampaignsFromJSON(String userId, JSONObject json, Map<Integer, SwrveCampaignState> states, boolean loadPreviousCampaignState) {
        if (json == null) {
            SwrveLogger.i("NULL JSON for campaigns, aborting load.");
            return;
        }

        if (json.length() == 0) {
            SwrveLogger.i("Campaign JSON empty, no campaigns downloaded");
            campaigns.clear();
            return;
        }

        SwrveLogger.i("Campaign JSON data: %s", json);

        try {
            // Check if schema has a version number
            if (!json.has("version")) {
                return;
            }

            // Version check
            String version = json.getString("version");
            if (!version.equals(CAMPAIGN_RESPONSE_VERSION)) {
                SwrveLogger.i("Campaign JSON (%s) has the wrong version for this sdk (%s). No campaigns loaded.", version, CAMPAIGN_RESPONSE_VERSION);
                return;
            }

            updateCdnPaths(json);

            // App Data
            JSONObject gamesData = json.getJSONObject("game_data");
            if (gamesData != null) {
                @SuppressWarnings("unchecked")
                Iterator<String> gamesDataIt = gamesData.keys();
                while (gamesDataIt.hasNext()) {
                    String appId = gamesDataIt.next();
                    JSONObject gameData = gamesData.getJSONObject(appId);
                    if (gameData.has("app_store_url")) {
                        String url = gameData.getString("app_store_url");
                        this.appStoreURLs.put(Integer.parseInt(appId), url);
                        if (SwrveHelper.isNullOrEmpty(url)) {
                            SwrveLogger.e("App store link %s is empty!", appId);
                        } else {
                            SwrveLogger.i("App store Link %s: %s", appId, url);
                        }
                    }
                }
            }

            JSONObject rules = json.getJSONObject("rules");
            int delay = (rules.has("delay_first_message")) ? rules.getInt("delay_first_message") : DEFAULT_DELAY_FIRST_MESSAGE;
            long maxShows = (rules.has("max_messages_per_session")) ? rules.getLong("max_messages_per_session") : DEFAULT_MAX_SHOWS;
            int minDelay = (rules.has("min_delay_between_messages")) ? rules.getInt("min_delay_between_messages") : DEFAULT_MIN_DELAY;

            Date now = getNow();
            Date showMessagesAfterLaunch = SwrveHelper.addTimeInterval(initialisedTime, delay, Calendar.SECOND);
            campaignDisplayer.setShowMessagesAfterLaunch(showMessagesAfterLaunch);
            campaignDisplayer.setMinDelayBetweenMessage(minDelay);
            campaignDisplayer.setMessagesLeftToShow(maxShows);

            SwrveLogger.i("App rules OK: Delay Seconds: %s Max shows: %s", delay, maxShows);
            SwrveLogger.i("Time is %s show messages after %s", now.toString(), showMessagesAfterLaunch.toString());

            JSONArray jsonCampaigns = json.getJSONArray("campaigns");
            List<Integer> newCampaignIds = new ArrayList<>();

            // Save the state of previous campaigns
            saveCampaignsState(userId);

            // Remove any campaigns that aren't in the new list
            // We do this before updating campaigns and adding new campaigns to ensure
            // there isn't a gap where no campaigns are available while reloading
            for (int i = 0, j = jsonCampaigns.length(); i < j; i++) {
                JSONObject campaignData = jsonCampaigns.getJSONObject(i);
                newCampaignIds.add(campaignData.getInt("id"));
            }
            for (int i = campaigns.size() - 1; i >= 0; i--) {
                SwrveBaseCampaign campaign = campaigns.get(i);
                if (!newCampaignIds.contains(campaign.getId())) {
                    campaigns.remove(i);
                }
            }
            
            Map<String, String> personalizationProperties = retrievePersonalizationProperties(null, null);

            List<QaCampaignInfo> qaCampaignInfoList = new ArrayList<>();
            List<SwrveBaseCampaign> newCampaigns = new ArrayList<>();
            Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
            for (int i = 0, j = jsonCampaigns.length(); i < j; i++) {
                JSONObject campaignData = jsonCampaigns.getJSONObject(i);
                // Load campaign and get assets to be loaded
                Set<SwrveAssetsQueueItem> campaignAssetQueue = new HashSet<>();

                // Check filters (permission requests, platform)
                boolean passesAllFilters = true;
                String lastCheckedFilter = null;
                if (campaignData.has("filters")) {
                    JSONArray filters = campaignData.getJSONArray("filters");
                    for (int ri = 0; ri < filters.length() && passesAllFilters; ri++) {
                        lastCheckedFilter = filters.getString(ri);
                        passesAllFilters = supportsDeviceFilter(lastCheckedFilter);
                    }
                }

                if (passesAllFilters) {
                    SwrveBaseCampaign campaign = null;
                    if (campaignData.has("conversation")) {
                        int conversationVersionDownloaded = campaignData.optInt("conversation_version", 1);
                        if (conversationVersionDownloaded <= ISwrveConversationSDK.CONVERSATION_VERSION) {
                            campaign = loadConversationCampaignFromJSON(campaignData, assetsQueue);
                        } else {
                            SwrveLogger.i("Conversation version %s cannot be loaded with this SDK version", conversationVersionDownloaded);
                        }
                    } else if (campaignData.has("message")) {
                        campaign = loadCampaignFromJSON(campaignData, campaignAssetQueue, personalizationProperties);
                        boolean filterCampaignWithCapabilityRequest =  filterCampaignCapabilityRequest((SwrveInAppCampaign)campaign);
                        if (filterCampaignWithCapabilityRequest) {
                            campaign = null;
                            SwrveLogger.i("Campaign with capability request is currently not supported");
                        }

                    } else if (campaignData.has("embedded_message")) {
                        campaign = loadEmbeddedCampaignFromJSON(campaignData);
                    }

                    if (campaign != null) {
                        assetsQueue.addAll(campaignAssetQueue);

                        // Check if we need to reset the device for QA, otherwise load campaign state
                        if (loadPreviousCampaignState) {
                            SwrveCampaignState state = states.get(campaign.getId());
                            if (state != null) {
                                campaign.setSaveableState(state);
                            }
                        }

                        newCampaigns.add(campaign);
                        campaignsState.put(campaign.getId(), campaign.getSaveableState());
                        SwrveLogger.i("Got campaign with id %s", campaign.getId());

                        if (QaUser.isLoggingEnabled()) {
                            if (campaign instanceof SwrveConversationCampaign) {
                                int variantId = ((SwrveConversationCampaign) campaign).getConversation().getId();
                                qaCampaignInfoList.add(new QaCampaignInfo(campaign.getId(), variantId, campaign.getCampaignType(), false, ""));
                            } else if (campaign instanceof SwrveInAppCampaign) {
                                int variantId = ((SwrveInAppCampaign) campaign).getVariantId();
                                qaCampaignInfoList.add(new QaCampaignInfo(campaign.getId(), variantId, campaign.getCampaignType(), false, ""));
                            } else if (campaign instanceof SwrveEmbeddedCampaign) {
                                int variantId = ((SwrveEmbeddedCampaign) campaign).getMessage().getId();
                                qaCampaignInfoList.add((new QaCampaignInfo(campaign.getId(), variantId, campaign.getCampaignType(), false, "")));
                            }
                        }
                    }
                } else {
                    SwrveLogger.i("Not all requirements were satisfied for this campaign: %s", lastCheckedFilter);
                }
            }

            QaUser.campaignsDownloaded(qaCampaignInfoList);

            // Launch load assets, then add to active campaigns
            // Note that campaign is also added to campaigns list in this function
            downloadAssets(assetsQueue);

            // Update current list of campaigns with new ones
            this.campaigns = new ArrayList<>(newCampaigns);
        } catch (JSONException exp) {
            SwrveLogger.e("Error parsing campaign JSON", exp);
        }
    }

    private void updateCdnPaths(JSONObject json) throws JSONException {
        if (json.has("cdn_root")) {
            String cdnRoot = json.getString("cdn_root");
            swrveAssetsManager.setCdnImages(cdnRoot);
            SwrveLogger.i("CDN URL %s", cdnRoot);
        } else if (json.has("cdn_paths")) {
            JSONObject cdnPaths = json.getJSONObject("cdn_paths");
            String cdnImages = cdnPaths.getString("message_images");
            String cdnFonts = cdnPaths.getString("message_fonts");
            swrveAssetsManager.setCdnImages(cdnImages);
            swrveAssetsManager.setCdnFonts(cdnFonts);
            SwrveLogger.i("CDN URL images:%s fonts:%s", cdnImages, cdnFonts);
        }
    }

    private boolean supportsDeviceFilter(String requirement) {
        return SUPPORTED_REQUIREMENTS.contains(requirement.toLowerCase(Locale.ENGLISH));
    }

    protected void downloadAssets(final Set<SwrveAssetsQueueItem> assetsQueue) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            final SwrveAssetsCompleteCallback callback = () -> autoShowMessages();
            executorService.execute(SwrveRunnables.withoutExceptions(() -> swrveAssetsManager.downloadAssets(assetsQueue, callback)));
        } finally {
            executorService.shutdown();
        }
    }

    protected SwrveInAppCampaign loadCampaignFromJSON(JSONObject campaignData, Set<SwrveAssetsQueueItem> assetsQueue, Map<String, String> properties) throws JSONException {
        return new SwrveInAppCampaign(this, campaignDisplayer, campaignData, assetsQueue, properties);
    }

    protected SwrveEmbeddedCampaign loadEmbeddedCampaignFromJSON(JSONObject campaignData) throws JSONException {
        return new SwrveEmbeddedCampaign(this, campaignDisplayer, campaignData);
    }

    protected SwrveConversationCampaign loadConversationCampaignFromJSON(JSONObject campaignData, Set<SwrveAssetsQueueItem> assetsQueue) throws JSONException {
        return new SwrveConversationCampaign(this, campaignDisplayer, campaignData, assetsQueue);
    }

    protected void saveCampaignsState(final String userId) {
        try {
            // Save campaigns state
            JSONObject campaignStateJson = new JSONObject();
            if (campaignsState != null) {
                for (int campaignId : campaignsState.keySet()) {
                    SwrveCampaignState campaignState = campaignsState.get(campaignId);
                    campaignStateJson.put(String.valueOf(campaignId), campaignState.toJSON());
                }
            }

            final String serializedCampaignsState = campaignStateJson.toString();
            storageExecutorExecute(() -> {
                multiLayerLocalStorage.setCacheEntry(userId, CACHE_CAMPAIGNS_STATE, serializedCampaignsState);
                SwrveLogger.i("Saved and flushed campaign state in cache");
            });
        } catch (JSONException exp) {
            SwrveLogger.e("Error saving campaigns settings", exp);
        }
    }

    protected Context getContext() {
        Context appCtx = context.get();
        if (appCtx == null) {
            return getActivityContext();
        }
        return appCtx;
    }

    protected SwrveOrientation getDeviceOrientation() {
        Context ctx = context.get();
        if (ctx != null) {
            return SwrveOrientation.parse(ctx.getResources().getConfiguration().orientation);
        }
        return SwrveOrientation.Both;
    }

    @Nullable
    protected Activity getActivityContext() {
        Activity activity = null;
        if (activityContext != null && activityContext.get() != null) {
            activity = activityContext.get();
        }
        return activity;
    }

    // Get device info and send it to Swrve
    protected void queueDeviceUpdateNow(final String userId, final String sessionToken, final boolean sendNow) {
        final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
        storageExecutorExecute(() -> {
            try {
                deviceUpdate(userId, swrve._getDeviceInfo());
            } catch (Exception e) {
                SwrveLogger.e("Exception queuing device update.", e);
            }
            // Send event after it has been queued, trigger from the storage executor
            // to wait for all events. Will be executed on the rest thread.
            if (sendNow) {
                storageExecutorExecute(() -> {
                    SwrveLogger.i("Sending device info for userId:%s", userId);
                    swrve._sendQueuedEvents(userId, sessionToken, true);
                });
            }
        });
    }

    // Create a unique key for this user
    public String getUniqueKey(String userId) {
        return userId + this.apiKey;
    }

    // Clear the etag to force a refresh of content.
    private void invalidateETag(final String userId) {
        SwrveLogger.v("SwrveSDK: clearing stored etag to force a content refresh.");
        multiLayerLocalStorage.setCacheEntry(userId, CACHE_ETAG, "");
    }

    // Cached content is corrupt or has been tampered with. Clear the etag to force a refresh of content and send an event.
    protected void invalidSignatureError(final String userId, String content) {
        SwrveLogger.e("SwrveSDK: Signature for %s invalid; could not retrieve data from cache. Forcing a refresh.", content);
        invalidateETag(userId);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.signature_invalid");
        queueEvent(userId, "event", parameters, null, false);
    }

    protected void initResources(String userId) {
        String cachedResources = null;

        try {
            cachedResources = multiLayerLocalStorage.getSecureCacheEntryForUser(userId, CACHE_RESOURCES, getUniqueKey(userId));
        } catch (SecurityException e) {
            invalidSignatureError(userId, CACHE_RESOURCES);
        }

        if (cachedResources != null) {
            try {
                JSONArray resourceJson = new JSONArray(cachedResources);
                this.resourceManager.setResourcesFromJSON(resourceJson);
            } catch (Exception e) {
                SwrveLogger.e("Could not parse cached json content for resources", e);
            }
        } else {
            invalidateETag(userId);
        }
    }

    protected void initRealTimeUserProperties(String userId) {
        realTimeUserProperties = new HashMap<>();

        try {
            String realTimeUserPropertiesFromCache = multiLayerLocalStorage.getSecureCacheEntryForUser(userId, CACHE_REALTIME_USER_PROPERTIES, getUniqueKey(userId));
            if (!SwrveHelper.isNullOrEmpty(realTimeUserPropertiesFromCache)) {
                JSONObject realTimeUserPropertiesJSON = new JSONObject(realTimeUserPropertiesFromCache);
                Iterator<String> userPropertyIterator = realTimeUserPropertiesJSON.keys();
                while (userPropertyIterator.hasNext()) {
                    String userPropertyKey = userPropertyIterator.next();
                    try {
                        String userPropertyValue = realTimeUserPropertiesJSON.getString(userPropertyKey);
                        realTimeUserProperties.put(userPropertyKey, userPropertyValue);
                    } catch (Exception exp) {
                        SwrveLogger.e("Could not load realtime user property for key: " + userPropertyKey, exp);
                    }
                }
            }
            SwrveLogger.i("Loaded realtime user properties from cache.");
        } catch (SecurityException e) {
            invalidSignatureError(userId, CACHE_REALTIME_USER_PROPERTIES);
        } catch (Exception e) {
            SwrveLogger.e("Could not load real time user properties", e);
        }
    }


    protected void initABTestDetails(String userId) {
        if (!config.isABTestDetailsEnabled()) {
            return;
        }

        try {
            String campaignsFromCache = multiLayerLocalStorage.getSecureCacheEntryForUser(userId, CACHE_CAMPAIGNS, getUniqueKey(userId));
            if (!SwrveHelper.isNullOrEmpty(campaignsFromCache)) {
                JSONObject campaignsJson = new JSONObject(campaignsFromCache);
                if (campaignsJson != null) {
                    JSONObject abTestInformationJson = campaignsJson.optJSONObject("ab_test_details");
                    if (abTestInformationJson != null) {
                        resourceManager.setABTestDetailsFromJSON(abTestInformationJson);
                    }
                }
            }
        } catch (SecurityException e) {
            invalidSignatureError(userId, "ab_test_details");
        } catch (Exception e) {
            SwrveLogger.e("Could not load ab test information", e);
        }
    }

    // Initialize campaigns with cache content
    protected void initCampaigns(String userId) {
        campaigns = new ArrayList<>();
        campaignDisplayer = new SwrveCampaignDisplayer();
        campaignsState = new HashMap<>();
        loadCampaignsFromCache(userId);
    }
    
    // load or refresh campaigns with cache content
    protected void loadCampaignsFromCache(String userId) {
        try {
            String campaignsFromCache = multiLayerLocalStorage.getSecureCacheEntryForUser(userId, CACHE_CAMPAIGNS, getUniqueKey(userId));
            if (!SwrveHelper.isNullOrEmpty(campaignsFromCache)) {
                JSONObject campaignsJson = new JSONObject(campaignsFromCache);
                // Load campaigns state
                loadCampaignsStateFromCache();
                // Update campaigns with the loaded JSON content
                boolean loadPreviousCampaignState = !QaUser.isResetDevice();
                loadCampaignsFromJSON(userId, campaignsJson, campaignsState, loadPreviousCampaignState);
                SwrveLogger.i("Loaded campaigns from cache.");
            } else {
                invalidateETag(userId);
            }
        } catch (SecurityException e) {
            invalidSignatureError(userId, CACHE_CAMPAIGNS);
        } catch (Exception e) {
            SwrveLogger.e("Could not load campaigns", e);
            invalidateETag(userId);
        }
    }


    private void loadCampaignsStateFromCache() {
        try {
            String campaignsStateFromCache = multiLayerLocalStorage.getCacheEntry(profileManager.getUserId(), CACHE_CAMPAIGNS_STATE);
            if (!SwrveHelper.isNullOrEmpty(campaignsStateFromCache)) {
                JSONObject campaignsStateJson = new JSONObject(campaignsStateFromCache);
                Iterator<String> campaignIdIterator = campaignsStateJson.keys();
                while (campaignIdIterator.hasNext()) {
                    String campaignIdStr = campaignIdIterator.next();
                    try {
                        int campaignId = Integer.parseInt(campaignIdStr);
                        SwrveCampaignState campaignState = new SwrveCampaignState(campaignsStateJson.getJSONObject(campaignIdStr));
                        campaignsState.put(campaignId, campaignState);
                    } catch (Exception exp) {
                        SwrveLogger.e("Could not load state for campaign " + campaignIdStr, exp);
                    }
                }
            }
        } catch (JSONException e) {
            SwrveLogger.e("Could not load state of campaigns, bad JSON", e);
        }
    }

    /*
     * Call resource listener if one is set up, and ensure it is called on the UI thread if possible
     */
    protected void invokeResourceListener() {
        if (resourcesListener != null) {
            Activity activity = getActivityContext();
            if (activity != null) {
                activity.runOnUiThread(() -> resourcesListener.onResourcesUpdated());
            } else {
                // If we do not have access to the activity context run on current thread
                resourcesListener.onResourcesUpdated();
            }
        }
    }

    /*
     * Check if any events need sending, then after flush delay reload campaigns and resources
     * This function should be called periodically.
     */
    protected void checkForCampaignAndResourcesUpdates() {
        if (initialisedTime == null) {
            SwrveLogger.w("Not executing checkForCampaignAndResourcesUpdates because initialisedTime is null indicating the sdk is not initialised.");
            return;
        }
        // If there are any events to be sent, or if any events were sent since last refresh
        // send events queued, wait campaignsAndResourcesFlushRefreshDelay for events to reach servers and refresh

        final String userId = profileManager.getUserId();
        final String sessionToken = profileManager.getSessionToken();
        boolean hasQueuedEvents = multiLayerLocalStorage.hasQueuedEvents(userId);
        if (hasQueuedEvents || eventsWereSent) {
            SwrveLogger.d("SwrveSDK events recently queued or sent, so sending and executing a delayed refresh of campaigns");
            final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
            if (hasQueuedEvents) {
                swrve._sendQueuedEvents(userId, sessionToken, false);
            }

            eventsWereSent = false;

            final ScheduledExecutorService timedService = Executors.newSingleThreadScheduledExecutor();
            timedService.schedule(() -> {
                try {
                    swrve.refreshCampaignsAndResources();
                } finally {
                    timedService.shutdownNow();
                }
            }, campaignsAndResourcesFlushRefreshDelay.longValue(), TimeUnit.MILLISECONDS);
        }
    }

    /*
     * Set up timer for checking for campaign and resources updates. Called when session begins and after IAP.
     */
    protected void startCampaignsAndResourcesTimer(boolean sessionStart) {
        if (!config.isAutoDownloadCampaignsAndResources() || !initialised) {
            return;
        }

        if (campaignsAndResourcesExecutor != null) {
            SwrveLogger.d("SwrveSDK not creating a new timer for refreshing campaigns because there is already an existing one.");
            return;
        }

        // For session start, execute immediately.
        if (sessionStart) {
            SwrveLogger.d("SwrveSDK sessionstart is true so executing an immediate refresh of campaigns before starting a delayed timer for refreshing campaigns.");
            final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
            swrve.refreshCampaignsAndResources();
            eventsWereSent = true; // this will force a refresh when the delayed campaignsAndResourcesExecutor executes.
        }

        // Start repeating timer to begin checking if campaigns/resources needs updating. It starts straight away.

        SwrveLogger.d("SwrveSDK starting repeating delayed timer for refreshing campaigns.");
        final ScheduledThreadPoolExecutor localExecutor = new ScheduledThreadPoolExecutor(1);
        localExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        localExecutor.scheduleWithFixedDelay(() -> checkForCampaignAndResourcesUpdates(), 0L, campaignsAndResourcesFlushFrequency.longValue(), TimeUnit.MILLISECONDS);
        campaignsAndResourcesExecutor = localExecutor;
    }

    protected void shutdownCampaignsAndResourcesTimer() {
        if (campaignsAndResourcesExecutor != null) {
            SwrveLogger.d("SwrveSDK shutting down campaigns refresh timer.");
            try {
                campaignsAndResourcesExecutor.shutdown();
            } catch (Exception e) {
                SwrveLogger.e("Exception occurred shutting down campaignsAndResourcesExecutor", e);
            }
            campaignsAndResourcesExecutor = null;
        }
    }

    @Override
    public Set<String> getAssetsOnDisk() {
        return swrveAssetsManager == null ? new HashSet<>() : swrveAssetsManager.getAssetsOnDisk();
    }

    protected Boolean filterCampaignCapabilityRequest(SwrveInAppCampaign campaign){
        SwrveMessage message = campaign.getMessage();
        if (message != null) {
            for (final SwrveMessageFormat format : message.getFormats()) {
                for (Map.Entry<Long, SwrveMessagePage> entry : format.getPages().entrySet()) {
                    SwrveMessagePage page = entry.getValue();
                    for (final SwrveButton button : page.getButtons()) {
                        if (SwrveActionType.RequestCapabilty.equals(button.getActionType())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected Map<String, String> retrievePersonalizationProperties(Map<String, String> eventPayload, Map<String, String> properties) {
        Map<String, String> processedRealTimeUserProperties = processRealTimeUserProperties(realTimeUserProperties);

        if (personalizationProvider != null && SwrveHelper.isNullOrEmpty (properties)) {
            properties = personalizationProvider.personalize(eventPayload);
            properties = SwrveHelper.combineTwoStringMaps(processedRealTimeUserProperties, properties);
        } else if (!SwrveHelper.isNullOrEmpty(properties)) {
            // if there's properties then combine with RTUPs and ignore provider, this is from MC
            properties = SwrveHelper.combineTwoStringMaps(processedRealTimeUserProperties, properties);
        } else {
            // if there's no callback or properties then just use RTUP
            properties = processedRealTimeUserProperties;
        }
        return properties;
    }

    private static Map<String, String> processRealTimeUserProperties(Map<String, String> rtups) {
        if (SwrveHelper.isNullOrEmpty(rtups)) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        for (String key : rtups.keySet()) {
            result.put("user." + key, rtups.get(key));
        }

        return result;
    }
}
