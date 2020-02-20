package com.swrve.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.SwrveConversationListener;
import com.swrve.sdk.device.AndroidTelephonyManagerWrapper;
import com.swrve.sdk.device.ITelephonyManager;
import com.swrve.sdk.localstorage.InMemoryLocalStorage;
import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveConversationCampaign;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveDismissButtonListener;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageListener;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.qa.SwrveQAUser;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.RESTClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.swrve.sdk.ISwrveCommon.CACHE_CAMPAIGNS;
import static com.swrve.sdk.ISwrveCommon.CACHE_CAMPAIGNS_STATE;
import static com.swrve.sdk.ISwrveCommon.CACHE_ETAG;
import static com.swrve.sdk.ISwrveCommon.CACHE_QA;
import static com.swrve.sdk.ISwrveCommon.CACHE_QA_RESET_DEVICE;
import static com.swrve.sdk.ISwrveCommon.CACHE_RESOURCES;
import static com.swrve.sdk.SwrveTrackingState.EVENT_SENDING_PAUSED;
import static com.swrve.sdk.SwrveTrackingState.ON;

/**
 * Internal base class implementation of the Swrve SDK.
 */
abstract class SwrveImp<T, C extends SwrveConfigBase> implements ISwrveCampaignManager, Application.ActivityLifecycleCallbacks {
    protected static final String PLATFORM = "Android ";
    protected static String version = "6.3.1";
    protected static final int CAMPAIGN_ENDPOINT_VERSION = 6;
    protected static final String CAMPAIGN_RESPONSE_VERSION = "2";
    protected static final String CAMPAIGNS_AND_RESOURCES_ACTION = "/api/1/user_resources_and_campaigns";
    protected static final String USER_RESOURCES_DIFF_ACTION = "/api/1/user_resources_diff";
    protected static final String BATCH_EVENTS_ACTION = "/1/batch";
    protected static final String IDENTITY_ACTION = "/identify";
    protected static final String SDK_PREFS_NAME = "swrve_prefs";
    protected static final String EMPTY_JSON_ARRAY = "[]";
    protected static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    protected static final String REFERRER = "referrer";
    protected static final String SWRVE_REFERRER_ID = "swrve.referrer_id";
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
    protected SwrveMessageListener messageListener;
    protected SwrveConversationListener conversationListener;
    protected SwrveInstallButtonListener installButtonListener;
    protected SwrveCustomButtonListener customButtonListener;
    protected SwrveDismissButtonListener inAppDismissButtonListener;
    protected SwrveResourcesListener resourcesListener;
    protected ExecutorService autoShowExecutor;
    protected AtomicInteger bindCounter;
    protected long newSessionInterval;
    protected long lastSessionTick;
    protected boolean destroyed;
    protected SwrveMultiLayerLocalStorage multiLayerLocalStorage;
    protected IRESTClient restClient;
    protected IRESTClient qaRestClient;
    protected ExecutorService storageExecutor;
    protected ExecutorService restClientExecutor;
    protected ScheduledThreadPoolExecutor campaignsAndResourcesExecutor;
    protected SwrveResourceManager resourceManager;
    protected List<SwrveBaseCampaign> campaigns;
    protected SwrveCampaignDisplayer campaignDisplayer;
    protected Map<Integer, SwrveCampaignState> campaignsState;
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
    protected boolean mustCleanInstance;
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
    protected SwrveQAUser qaUser;
    protected SwrveDeeplinkManager swrveDeeplinkManager;
    protected SwrveCampaignInfluence campaignInfluence = new SwrveCampaignInfluence();
    protected String notificationSwrveCampaignId;
    protected SwrveTrackingState trackingState = ON;
    protected boolean identifiedOnAnotherDevice;
    protected SwrveSessionListener sessionListener;
    protected List<EventQueueItem> pausedEvents = Collections.synchronizedList(new ArrayList<EventQueueItem>());

    protected SwrveImp(Application application, int appId, String apiKey, C config) {
        SwrveLogger.setLoggingEnabled(config.isLoggingEnabled());
        if (appId <= 0 || SwrveHelper.isNullOrEmpty(apiKey)) {
            SwrveHelper.logAndThrowException("Please setup a correct appId and apiKey");
        }

        Context applicationContext = application.getApplicationContext();

        this.appId = appId;
        this.apiKey = apiKey;
        this.config = config;

        this.destroyed = false;
        this.autoShowExecutor = Executors.newSingleThreadExecutor();
        this.storageExecutor = Executors.newSingleThreadExecutor();
        this.restClientExecutor = Executors.newSingleThreadExecutor();
        this.restClient = createRESTClient();
        this.bindCounter = new AtomicInteger();
        this.swrveAssetsManager = new SwrveAssetsManagerImp(application.getApplicationContext());
        this.newSessionInterval = config.getNewSessionInterval();
        this.multiLayerLocalStorage = new SwrveMultiLayerLocalStorage(new InMemoryLocalStorage());
        this.profileManager = new SwrveProfileManager(application.getApplicationContext(), appId, apiKey, config, restClient);
        this.context = new WeakReference<>(application.getApplicationContext());
        this.application = new WeakReference<>(application);

        initAppVersion(applicationContext, config);
        initDefaultUrls(config);
        initLanguage(config);

        if (shouldAutostart(applicationContext)) {
            this.profileManager.persistUser();
            registerActivityLifecycleCallbacks();
            started = true;
        }
    }

    private boolean shouldAutostart(Context context) {
        return config.getInitMode() == SwrveInitMode.AUTO
                || (config.getInitMode() == SwrveInitMode.MANAGED && config.isManagedModeAutoStartLastUser() && SwrveProfileManager.getSavedUserIdFromPrefs(context) != null);
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
        }catch (MalformedURLException ex) {
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
            SwrveLogger.i("registered ActivityLifecycleCallbacks.");
        }
    }

    protected void queueSessionStart() {
        queueEvent("session_start", null, null);
    }

    protected Context bindToContext(Activity activity) {
        bindCounter.incrementAndGet();
        this.context = new WeakReference<>(activity.getApplicationContext());
        this.activityContext = new WeakReference<>(activity);
        return this.context.get();
    }

    protected void sendCrashlyticsMetadata() {
        try {
            Class c = Class.forName("com.crashlytics.android.Crashlytics");
            if (c != null) {
                Method m = c.getMethod("setString", new Class[]{String.class, String.class});
                if (m != null) {
                    m.invoke(null, "Swrve_version", version);
                }
            }
        } catch (Exception exp) {
            SwrveLogger.i("Could not set Crashlytics metadata");
        }
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

    protected IRESTClient createRESTClient() {
        return new RESTClient(config.getHttpTimeout());
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
        if (trackingState == EVENT_SENDING_PAUSED) {
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

    protected long getSessionTime() {
        return getNow().getTime();
    }

    protected void generateNewSessionInterval() {
        lastSessionTick = getSessionTime() + newSessionInterval;
    }

    protected void getDeviceInfo(Context context) {
        try {
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_CAMPAIGNS, campaignContent.toString(), getUniqueKey(userId));
            }
        });
    }

    protected void saveResourcesInCache(final JSONArray resourcesContent) {
        final String userId = profileManager.getUserId(); // user can change so retrieve now as a final String for thread safeness
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_RESOURCES, resourcesContent.toString(), getUniqueKey(userId));
            }
        });
    }

    protected void saveIsQaUserInCache(final boolean isQaUser) {
        final String userId = profileManager.getUserId(); // user can change so retrieve now as a final String for thread safeness
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_QA, String.valueOf(isQaUser), getUniqueKey(userId));
            }
        });
    }

    protected void saveQaUserResetDeviceInCache(JSONObject jsonQa) {
        final boolean resetDevice = jsonQa.optBoolean("reset_device_state", false);
        final String userId = profileManager.getUserId(); // user can change so retrieve now as a final String for thread safeness
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                multiLayerLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CACHE_QA_RESET_DEVICE, String.valueOf(resetDevice), getUniqueKey(userId));
            }
        });
    }

    protected void autoShowMessages() {
        // Don't do anything if we've already shown a message or if its too long after session start
        if (!autoShowMessagesEnabled) {
            return;
        }

        // Only execute if at least 1 call to the /user_resources_and_campaigns api endpoint has been completed
        // And ensure all assets have been downloaded
        if (!campaignsAndResourcesInitialized || campaigns == null) {
            return;
        }

        for (final SwrveBaseCampaign campaign : campaigns) {
            final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
            Map<String, String> emptyPayload = new HashMap<>();
            boolean canTrigger = campaignDisplayer.canTrigger(campaign, SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER, emptyPayload, null);
            if (canTrigger) {
                synchronized (this) {
                    if (autoShowMessagesEnabled && activityContext != null) {
                        Activity activity = activityContext.get();
                        if (activity != null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    autoShowConversation(swrve);
                                    autoShowInAppMessage(swrve);
                                }
                            });
                        }
                    }
                }
                break;
            }
        }
    }

    protected void autoShowInAppMessage(SwrveBase<T, C> swrve) {
        try {
            if (messageListener != null && autoShowMessagesEnabled) {
                SwrveMessage message = swrve.getMessageForEvent(SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER);
                if (message != null && message.supportsOrientation(getDeviceOrientation())) {
                    messageListener.onMessage(message);
                    autoShowMessagesEnabled = false;
                }
            }
        } catch (Exception exp) {
            SwrveLogger.e("Could not launch campaign automatically.", exp);
        }
    }

    protected void autoShowConversation(SwrveBase<T, C> swrve) {
        try {
            if (conversationListener != null && autoShowMessagesEnabled) {
                SwrveConversation conversation = swrve.getConversationForEvent(SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER, new HashMap<String, String>());
                if (conversation != null) {
                    conversationListener.onMessage(conversation);
                    autoShowMessagesEnabled = false;
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
        timedService.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    autoShowMessagesEnabled = false;
                } finally {
                    timedService.shutdownNow();
                }
            }
        }, config.getAutoShowMessagesMaxDelay(), TimeUnit.MILLISECONDS);
    }

    @SuppressLint("UseSparseArrays")
    protected void loadCampaignsFromJSON(String userId, JSONObject json, Map<Integer, SwrveCampaignState> states) {
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
                SwrveLogger.i("Campaign JSON (%s) has the wrong version for this sdk (%s). No campaigns loaded.", version, CAMPAIGN_RESPONSE_VERSION );
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

            Map<Integer, String> campaignsDownloaded = null;

            // QA
            boolean wasPreviouslyQAUser = (qaUser != null);
            if (json.has("qa")) {
                JSONObject jsonQa = json.getJSONObject("qa");
                campaignsDownloaded = new HashMap<>();
                SwrveLogger.i("You are a QA user!");
                if (qaRestClient == null) {
                    qaRestClient = new RESTClient(config.getHttpTimeout());
                }
                qaUser = new SwrveQAUser(appId, apiKey, profileManager.getUserId(), qaRestClient, jsonQa);
                qaUser.bindToServices();

                if (jsonQa.has("campaigns")) {
                    JSONArray jsonQaCampaigns = jsonQa.getJSONArray("campaigns");
                    for (int i = 0; i < jsonQaCampaigns.length(); i++) {
                        JSONObject jsonQaCampaign = jsonQaCampaigns.getJSONObject(i);
                        int campaignId = jsonQaCampaign.getInt("id");
                        String campaignReason = jsonQaCampaign.getString("reason");

                        SwrveLogger.i("Campaign %s not downloaded because: %s", campaignId, campaignReason);

                        // Add campaign for QA purposes
                        campaignsDownloaded.put(campaignId, campaignReason);
                    }
                }
                saveIsQaUserInCache(true);
                saveQaUserResetDeviceInCache(jsonQa);
                QaUser.update();
            } else if (qaUser != null) {
                qaUser.unbindToServices();
                qaUser = null;
                saveIsQaUserInCache(false);
            }

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

            boolean mustLoadPreviousState = (wasPreviouslyQAUser || qaUser == null || !qaUser.isResetDevice());
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
                    } else {
                        campaign = loadCampaignFromJSON(campaignData, campaignAssetQueue);
                    }

                    if (campaign != null) {
                        assetsQueue.addAll(campaignAssetQueue);

                        // Check if we need to reset the device for QA, otherwise load campaign state
                        if (mustLoadPreviousState) {
                            SwrveCampaignState state = states.get(campaign.getId());
                            if (state != null) {
                                campaign.setSaveableState(state);
                            }
                        }

                        newCampaigns.add(campaign);
                        campaignsState.put(campaign.getId(), campaign.getSaveableState());
                        SwrveLogger.i("Got campaign with id %s", campaign.getId());

                        if (qaUser != null) {
                            // Add campaign for QA purposes
                            campaignsDownloaded.put(campaign.getId(), null);
                        }
                    }
                } else {
                    SwrveLogger.i("Not all requirements were satisfied for this campaign: %s", lastCheckedFilter);
                }
            }

            // QA logging
            if (qaUser != null) {
                qaUser.talkSession(campaignsDownloaded);
            }

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
        if(json.has("cdn_root")) {
            String cdnRoot = json.getString("cdn_root");
            swrveAssetsManager.setCdnImages(cdnRoot);
            SwrveLogger.i("CDN URL %s", cdnRoot);
        } else if(json.has("cdn_paths")) {
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
            final SwrveAssetsCompleteCallback callback = new SwrveAssetsCompleteCallback() {
                @Override
                public void complete() {
                    autoShowMessages();
                }

            };
            executorService.execute(SwrveRunnables.withoutExceptions(new Runnable() {
                @Override
                public void run() {
                    swrveAssetsManager.downloadAssets(assetsQueue, callback);
                }
            }));
        } finally {
            executorService.shutdown();
        }
    }

    protected SwrveInAppCampaign loadCampaignFromJSON(JSONObject campaignData, Set<SwrveAssetsQueueItem> assetsQueue) throws JSONException {
        return new SwrveInAppCampaign(this, campaignDisplayer, campaignData, assetsQueue);
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
            storageExecutorExecute(new Runnable() {
                @Override
                public void run() {
                    multiLayerLocalStorage.setCacheEntry(userId, CACHE_CAMPAIGNS_STATE, serializedCampaignsState);
                    SwrveLogger.i("Saved and flushed campaign state in cache");
                }
            });
        } catch (JSONException exp) {
            SwrveLogger.e("Error saving campaigns settings", exp);
        }
    }

    protected Context getContext() {
        Context appCtx = context.get();
        if(appCtx == null) {
            return getActivityContext();
        }
        return appCtx;
    }

    protected void unbindAndShutdown() {
        // Reduce the references to the SDK
        int counter = bindCounter.decrementAndGet();

        // Check if there are no more references to this object
        if (counter == 0) {
            this.activityContext = null;  // Remove the binding to the current activity, if any
            if (mustCleanInstance) {
                ((SwrveBase<?, ?>) this).shutdown();
            }
        }
    }

    protected SwrveOrientation getDeviceOrientation() {
        Context ctx = context.get();
        if (ctx != null) {
            return SwrveOrientation.parse(ctx.getResources().getConfiguration().orientation);
        }
        return SwrveOrientation.Both;
    }

    protected Activity getActivityContext() {
        if (activityContext != null) {
            Activity ctx = activityContext.get();
            if (ctx != null) {
                return ctx;
            }
        }

        return null;
    }

    protected boolean isActivityFinishing(Activity activity) {
        return activity.isFinishing();
    }

    // Get device info and send it to Swrve
    protected void queueDeviceUpdateNow(final String userId, final String sessionToken, final boolean sendNow) {
        final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                deviceUpdate(userId, swrve.getDeviceInfo());
                // Send event after it has been queued, trigger from the storage executor
                // to wait for all events. Will be executed on the rest thread.
                if (sendNow) {
                    storageExecutorExecute(new Runnable() {
                        @Override
                        public void run() {
                            SwrveLogger.i("Sending device info for userId:%s", userId);
                            swrve._sendQueuedEvents(userId, sessionToken);
                        }
                    });
                }
            }
        });
    }

    // Create a unique key for this user
    public String getUniqueKey(String userId) {
        return userId + this.apiKey;
    }

    // Invalidates the currently stored ETag
    // Should be called when a refresh of campaigns and resources needs to be forced (eg. when cached data cannot be read)
    protected void invalidateETag(final String userId) {
        multiLayerLocalStorage.setCacheEntry(userId, CACHE_ETAG, "");
    }

    // Initialize Resource Manager with cache content
    protected void initResources(String userId) {
        String cachedResources = null;

        try {
            cachedResources = multiLayerLocalStorage.getSecureCacheEntryForUser(userId, CACHE_RESOURCES, getUniqueKey(userId));
        } catch (SecurityException e) {
            invalidateETag(userId);
            SwrveLogger.i("Signature for %s invalid; could not retrieve data from cache", CACHE_RESOURCES);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "Swrve.signature_invalid");
            queueEvent(userId, "event", parameters, null, false);
        }

        if (cachedResources != null) {
            try {
                JSONArray resourceJson = new JSONArray(cachedResources);
                this.resourceManager.setResourcesFromJSON(resourceJson);
            } catch (JSONException e) {
                SwrveLogger.e("Could not parse cached json content for resources", e);
            }
        } else {
            invalidateETag(userId);
        }
    }

    protected void initABTestDetails(String userId) {
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
        } catch (JSONException e) {
            SwrveLogger.e("Invalid json in cache, cannot load ab test information", e);
        } catch (SecurityException e) {
            SwrveLogger.e("Signature validation failed when trying to load ab test information from cache.", e);
        }
    }

    // Initialize campaigns with cache content
    protected void initCampaigns(String userId) {
        campaigns = new ArrayList<>();
        campaignDisplayer = new SwrveCampaignDisplayer(qaUser);
        campaignsState = new HashMap<>();

        try {
            String campaignsFromCache = multiLayerLocalStorage.getSecureCacheEntryForUser(userId, CACHE_CAMPAIGNS, getUniqueKey(userId));
            if (!SwrveHelper.isNullOrEmpty(campaignsFromCache)) {
                JSONObject campaignsJson = new JSONObject(campaignsFromCache);
                // Load campaigns state
                loadCampaignsStateFromCache();
                // Update campaigns with the loaded JSON content
                loadCampaignsFromJSON(userId, campaignsJson, campaignsState);
                SwrveLogger.i("Loaded campaigns from cache.");
            } else {
                invalidateETag(userId);
            }
        } catch (JSONException e) {
            invalidateETag(userId);
            SwrveLogger.e("Invalid json in cache, cannot load campaigns", e);
        } catch (SecurityException e) {
            invalidateETag(userId);
            SwrveLogger.e("Signature validation failed when trying to load campaigns from cache.", e);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("name", "Swrve.signature_invalid");
            queueEvent(userId, "event", parameters, null, false);
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
        } catch(JSONException e) {
            SwrveLogger.e("Could not load state of campaigns, bad JSON", e);
        }
    }

    /*
     * Call resource listener if one is set up, and ensure it is called on the UI thread if possible
     */
    protected void invokeResourceListener() {
        if (resourcesListener != null) {
            Activity ctx = getActivityContext();
            if (ctx != null) {
                ctx.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resourcesListener.onResourcesUpdated();
                    }

                });
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
        if(initialisedTime == null) {
            SwrveLogger.w("Not executing checkForCampaignAndResourcesUpdates because initialisedTime is null indicating the sdk is not initialised.");
            return;
        }
        // If there are any events to be sent, or if any events were sent since last refresh
        // send events queued, wait campaignsAndResourcesFlushRefreshDelay for events to reach servers and refresh
        final LinkedHashMap<LocalStorage, LinkedHashMap<Long, String>> combinedEvents = multiLayerLocalStorage.getCombinedFirstNEvents(config.getMaxEventsPerFlush(), profileManager.getUserId());
        if (!combinedEvents.isEmpty() || eventsWereSent) {
            final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
            swrve.sendQueuedEvents();
            eventsWereSent = false;
            final ScheduledExecutorService timedService = Executors.newSingleThreadScheduledExecutor();
            timedService.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        swrve.refreshCampaignsAndResources();
                    } finally {
                        timedService.shutdownNow();
                    }
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

        final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
        // If there is an existing executor, shut it down. This will finish any tasks in progress, but not execute any tasks currently scheduled or accept new tasks
        shutdownCampaignsAndResourcesTimer();

        // For session start, execute immediately.
        if (sessionStart) {
            swrve.refreshCampaignsAndResources();
        }

        // Start repeating timer to begin checking if campaigns/resources needs updating. It starts straight away.
        eventsWereSent = true;

        final ScheduledThreadPoolExecutor localExecutor = new ScheduledThreadPoolExecutor(1);
        localExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        localExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                checkForCampaignAndResourcesUpdates();
            }
        }, 0L, campaignsAndResourcesFlushFrequency.longValue(), TimeUnit.MILLISECONDS);
        campaignsAndResourcesExecutor = localExecutor;
    }

    protected void shutdownCampaignsAndResourcesTimer() {
        if (campaignsAndResourcesExecutor != null) {
            try {
                campaignsAndResourcesExecutor.shutdown();
            } catch (Exception e) {
                SwrveLogger.e("Exception occurred shutting down campaignsAndResourcesExecutor", e);
            }
        }
    }

    @Override
    public Set<String> getAssetsOnDisk() {
        return swrveAssetsManager == null ? new HashSet<String>() : swrveAssetsManager.getAssetsOnDisk();
    }

}
