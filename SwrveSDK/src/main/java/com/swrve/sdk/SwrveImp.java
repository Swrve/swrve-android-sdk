package com.swrve.sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import com.swrve.sdk.SwrveLogger;

import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.conversations.ISwrveConversationListener;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.device.AndroidTelephonyManagerWrapper;
import com.swrve.sdk.device.ITelephonyManager;
import com.swrve.sdk.localstorage.ILocalStorage;
import com.swrve.sdk.localstorage.MemoryCachedLocalStorage;
import com.swrve.sdk.localstorage.MemoryLocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.locationcampaigns.model.LocationCampaign;
import com.swrve.sdk.messaging.ISwrveCustomButtonListener;
import com.swrve.sdk.messaging.ISwrveDialogListener;
import com.swrve.sdk.messaging.ISwrveInstallButtonListener;
import com.swrve.sdk.messaging.ISwrveMessageListener;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveCampaign;
import com.swrve.sdk.messaging.SwrveConversationCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.view.SwrveDialog;
import com.swrve.sdk.messaging.view.SwrveMessageView;
import com.swrve.sdk.messaging.view.SwrveMessageViewFactory;
import com.swrve.sdk.qa.SwrveQAUser;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTClient;
import com.swrve.sdk.rest.RESTResponse;
import com.swrve.sdk.rest.SwrveFilterInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Internal base class implementation of the Swrve SDK.
 */
abstract class SwrveImp<T, C extends SwrveConfigBase> {
    protected static final String PLATFORM = "Android ";
    protected static String version = "4.1";
    protected static final String CAMPAIGN_CATEGORY = "CMCC2"; // Saved securely
    protected static final String LOCATION_CAMPAIGN_CATEGORY = "LocationCampaign";
    protected static final String CAMPAIGN_SETTINGS_CATEGORY = "SwrveCampaignSettings";
    protected static final String APP_VERSION_CATEGORY = "AppVersion";
    protected static final int CAMPAIGN_ENDPOINT_VERSION = 5;
    protected static final String TEMPLATE_VERSION = "1";
    protected static final int CONVERSATION_VERSION = 2;
    protected static final int LOCATION_VERSION = 1;
    protected static final String CAMPAIGNS_AND_RESOURCES_ACTION = "/api/1/user_resources_and_campaigns";
    protected static final String USER_RESOURCES_DIFF_ACTION = "/api/1/user_resources_diff";
    protected static final String BATCH_EVENTS_ACTION = "/1/batch";
    protected static final String RESOURCES_CACHE_CATEGORY = "srcngt2"; // Saved securely
    protected static final String RESOURCES_DIFF_CACHE_CATEGORY = "rsdfngt2"; // Saved securely
    protected static final String SDK_PREFS_NAME = "swrve_prefs";
    protected static final String EMPTY_JSON_ARRAY = "[]";
    protected static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    protected static final String SWRVE_DEVICE_NAME = "swrve.device_name";
    protected static final String SWRVE_OS = "swrve.os";
    protected static final String SWRVE_OS_VERSION = "swrve.os_version";
    protected static final String SWRVE_DEVICE_WIDTH = "swrve.device_width";
    protected static final String SWRVE_DEVICE_HEIGHT = "swrve.device_height";
    protected static final String SWRVE_DEVICE_DPI = "swrve.device_dpi";
    protected static final String SWRVE_CONVERSATION_VERSION = "swrve.conversation_version";
    protected static final String SWRVE_LOCATION_VERSION = "swrve.location_version";
    protected static final String SWRVE_ANDROID_DEVICE_XDPI = "swrve.android_device_xdpi";
    protected static final String SWRVE_ANDROID_DEVICE_YDPI = "swrve.android_device_ydpi";
    protected static final String SWRVE_LANGUAGE = "swrve.language";
    protected static final String SWRVE_UTC_OFFSET_SECONDS = "swrve.utc_offset_seconds";
    protected static final String SWRVE_TIMEZONE_NAME = "swrve.timezone_name";
    protected static final String SWRVE_SDK_VERSION = "swrve.sdk_version";
    protected static final String SWRVE_APP_STORE = "swrve.app_store";
    protected static final String SWRVE_INSTALL_DATE = "swrve.install_date";
    protected static final String SWRVE_SIM_OPERATOR_NAME = "swrve.sim_operator.name";
    protected static final String SWRVE_SIM_OPERATOR_ISO_COUNTRY = "swrve.sim_operator.iso_country_code";
    protected static final String SWRVE_SIM_OPERATOR_CODE = "swrve.sim_operator.code";
    protected static final String SWRVE_DEVICE_REGION = "swrve.device_region";
    protected static final String SWRVE_ANDROID_ID = "swrve.android_id";
    protected static final String SWRVE_REFERRER_ID = "swrve.referrer_id";

    protected static final String DEVICE_NAME = "device_name";
    protected static final String APP_VERSION = "app_version";
    protected static final String OS_VERSION = "os_version";
    protected static final String CONVERSATION_VERSION_KEY = "conversation_version";
    protected static final String VERSION_KEY = "version";
    protected static final String JOINED_KEY = "joined";
    protected static final String USER_KEY = "user";
    protected static final String API_KEY = "api_key";
    protected static final String APP_STORE_KEY = "app_store";
    protected static final String LANGUAGE_KEY = "language";
    protected static final String ANDROID_DEVICE_YDPI = "android_device_ydpi";
    protected static final String ANDROID_DEVICE_XDPI = "android_device_xdpi";
    protected static final String DEVICE_DPI = "device_dpi";
    protected static final String DEVICE_HEIGHT = "device_height";
    protected static final String DEVICE_WIDTH = "device_width";
    protected static final String ORIENTATION = "orientation";

    protected static final String REFERRER = "referrer";
    protected static final int SWRVE_DEFAULT_CAMPAIGN_RESOURCES_FLUSH_FREQUENCY = 60000;
    protected static final int SWRVE_DEFAULT_CAMPAIGN_RESOURCES_FLUSH_REFRESH_DELAY = 5000;
    protected static final String SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER = "Swrve.Messages.showAtSessionStart";
    protected static final String LOG_TAG = "SwrveSDK";
    protected static final List<String> SUPPORTED_REQUIREMENTS = Arrays.asList("android");
    protected static int DEFAULT_DELAY_FIRST_MESSAGE = 150;
    protected static long DEFAULT_MAX_SHOWS = 99999;
    protected static int DEFAULT_MIN_DELAY = 55;
    protected static long MESSAGE_REAPPEAR_TIMEOUT = 1500;
    protected static SwrveMessage messageDisplayed;
    protected static long lastMessageDestroyed;
    private static String INSTALL_TIME_CATEGORY = "SwrveSDK.installTime";
    protected final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss ZZZZ", Locale.US);
    protected final SimpleDateFormat installTimeFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
    protected WeakReference<Context> context;
    protected WeakReference<Activity> activityContext;
    protected WeakReference<SwrveDialog> currentDialog;
    protected String appVersion;
    protected int appId;
    protected String apiKey;
    protected String userId;
    protected String sessionToken;
    protected String language;
    protected C config;
    protected ISwrveEventListener eventListener;
    protected ISwrveMessageListener messageListener;
    protected ISwrveConversationListener conversationListener;
    protected ISwrveInstallButtonListener installButtonListener;
    protected ISwrveCustomButtonListener customButtonListener;
    protected ISwrveDialogListener dialogListener;
    protected ISwrveResourcesListener resourcesListener;
    protected ExecutorService autoShowExecutor;
    protected String userInstallTime;
    protected String lastProcessedMessage;
    protected AtomicInteger bindCounter;
    protected AtomicLong installTime;
    protected CountDownLatch installTimeLatch;
    protected long newSessionInterval;
    protected long lastSessionTick;
    protected boolean destroyed;
    protected MemoryCachedLocalStorage cachedLocalStorage;
    protected IRESTClient restClient;
    protected ExecutorService storageExecutor;
    protected ExecutorService restClientExecutor;
    protected ScheduledThreadPoolExecutor campaignsAndResourcesExecutor;
    protected SwrveResourceManager resourceManager;
    protected Map<String, LocationCampaign> locationCampaigns;
    protected List<SwrveBaseCampaign> campaigns;
    protected Set<String> assetsOnDisk;
    protected boolean assetsCurrentlyDownloading;
    protected SparseArray<String> appStoreURLs;
    protected boolean autoShowMessagesEnabled;
    protected Integer campaignsAndResourcesFlushFrequency;
    protected Integer campaignsAndResourcesFlushRefreshDelay;
    protected String campaignsAndResourcesLastETag;
    protected Date campaignsAndResourcesLastRefreshed;
    protected boolean campaignsAndResourcesInitialized = false;
    protected boolean eventsWereSent = false;
    protected String cdnRoot = "http://content-cdn.swrve.com/messaging/message_image/";
    protected boolean initialised = false;
    protected boolean mustCleanInstance;
    protected Date initialisedTime;
    protected Date showMessagesAfterLaunch;
    protected Date showMessagesAfterDelay;
    protected long messagesLeftToShow;
    protected int minDelayBetweenMessage;
    protected File cacheDir;
    protected int deviceWidth;
    protected int deviceHeight;
    protected float deviceDpi;
    protected float androidDeviceXdpi;
    protected float androidDeviceYdpi;
    protected String simOperatorName;
    protected String simOperatorIsoCountryCode;
    protected String simOperatorCode;
    protected String androidId;

    protected int previousOrientation;
    protected SwrveQAUser qaUser;

    protected SwrveImp() {
        installTime = new AtomicLong();
        installTimeLatch = new CountDownLatch(1);
        destroyed = false;
        autoShowExecutor = Executors.newSingleThreadExecutor();
        bindCounter = new AtomicInteger();
        autoShowMessagesEnabled = true;
        assetsOnDisk = new HashSet<String>();
        assetsCurrentlyDownloading = false;
    }

    protected void showPreviousMessage() {
        if (config.isTalkEnabled()) {
            // Re-launch message that was displayed before
            if (messageDisplayed != null && messageListener != null) {
                long currentTime = getNow().getTime();
                if (currentTime < (lastMessageDestroyed + MESSAGE_REAPPEAR_TIMEOUT)) {
                    messageListener.onMessage(messageDisplayed, false);
                }
                messageDisplayed = null;
            }
        }
    }

    protected void queueSessionStart() {
        queueEvent("session_start", null, null);
    }

    protected Context bindToContext(Activity activity) {
        bindCounter.incrementAndGet();
        this.context = new WeakReference<Context>(activity.getApplicationContext());
        this.activityContext = new WeakReference<Activity>(activity);
        return this.context.get();
    }

    protected String getUniqueUserId(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SDK_PREFS_NAME, 0);
        String newUserId = settings.getString("userId", null);
        if (SwrveHelper.isNullOrEmpty(newUserId)) {
            // Create a random UUID
            newUserId = UUID.randomUUID().toString();
        }

        return newUserId;
    }

    protected void saveUniqueUserId(Context context, String userId) {
        SharedPreferences settings = context.getSharedPreferences(SDK_PREFS_NAME, 0);
        // Save new user id
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("userId", userId);
        editor.commit();
    }

    protected void checkUserId(String userId) {
        if (userId != null && userId.matches("^.*\\..*@\\w+$")) {
            SwrveLogger.w(LOG_TAG, "Please double-check your user id. It seems to be Object.toString(): " + userId);
        }
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
            SwrveLogger.i(LOG_TAG, "Could not set Crashlytics metadata");
        }
    }

    /**
     * Internal function to add a Swrve.iap event to the event queue.
     */
    protected void _iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards, String receipt, String receiptSignature, String paymentProvider) {
        if (_iap_check_parameters(quantity, productId, productPrice, currency, paymentProvider)) {
            Map<String, Object> parameters = new HashMap<String, Object>();
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

            if (config.isAutoDownloadCampaingsAndResources()) {
                startCampaignsAndResourcesTimer(false);
            }
        }
    }

    protected boolean _iap_check_parameters(int quantity, String productId, double productPrice, String currency, String paymentProvider) throws IllegalArgumentException {
        // Strings cannot be null or empty
        if (SwrveHelper.isNullOrEmpty(productId)) {
            SwrveLogger.e(LOG_TAG, "IAP event illegal argument: productId cannot be empty");
            return false;
        }
        if (SwrveHelper.isNullOrEmpty(currency)) {
            SwrveLogger.e(LOG_TAG, "IAP event illegal argument: currency cannot be empty");
            return false;
        }
        if (SwrveHelper.isNullOrEmpty(paymentProvider)) {
            SwrveLogger.e(LOG_TAG, "IAP event illegal argument: paymentProvider cannot be empty");
            return false;
        }
        if (quantity <= 0) {
            SwrveLogger.e(LOG_TAG, "IAP event illegal argument: quantity must be greater than zero");
            return false;
        }
        if (productPrice < 0) {
            SwrveLogger.e(LOG_TAG, "IAP event illegal argument: productPrice must be greater than or equal to zero");
            return false;
        }
        return true;
    }

    protected void openLocalStorageConnection() {
        try {
            ILocalStorage newlocalStorage = createLocalStorage();
            cachedLocalStorage.setSecondaryStorage(newlocalStorage);
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, "Error opening database", exp);
        }
    }

    protected ILocalStorage createLocalStorage() {
        return new SQLiteLocalStorage(context.get(), config.getDbName(), config.getMaxSqliteDbSize());
    }

    protected IRESTClient createRESTClient() {
        return new RESTClient(config.getHttpTimeout());
    }

    protected MemoryCachedLocalStorage createCachedLocalStorage() {
        return new MemoryCachedLocalStorage(new MemoryLocalStorage(), null);
    }

    protected ExecutorService createStorageExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    protected ExecutorService createRESTClientExecutor() {
        return Executors.newSingleThreadExecutor();
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

    protected void processUserResourcesDiffData(String resourcesAsJSON, final ISwrveUserResourcesDiffListener listener) {
        // Parse raw response
        JSONArray jsonResourcesDiff;
        try {
            jsonResourcesDiff = new JSONArray(resourcesAsJSON);
            // Convert to map
            Map<String, Map<String, String>> mapOldResources = new HashMap<String, Map<String, String>>();
            Map<String, Map<String, String>> mapNewResources = new HashMap<String, Map<String, String>>();
            for (int i = 0, j = jsonResourcesDiff.length(); i < j; i++) {
                Map<String, String> mapOldResourceValues = new HashMap<String, String>();
                Map<String, String> mapNewResourceValues = new HashMap<String, String>();
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

    protected long getInstallTime() {
        long installTime = (new Date()).getTime();
        try {
            // Try to get install time from storage
            String installTimeRaw = getSavedInstallTime();
            if (!SwrveHelper.isNullOrEmpty(installTimeRaw)) {
                installTime = Long.parseLong(installTimeRaw);
            } else {
                // Save to memory and secondary storage
                cachedLocalStorage.setAndFlushSharedEntry(INSTALL_TIME_CATEGORY, String.valueOf(installTime));
            }
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, "Could not get or save install time", exp);
        }

        return installTime;
    }

    protected String getSavedInstallTime() {
        return cachedLocalStorage.getSharedCacheEntry(INSTALL_TIME_CATEGORY);
    }

    protected long getOrWaitForInstallTime() {
        try {
            installTimeLatch.await();
            return this.installTime.get();
        } catch (Exception e) {
            return (new Date()).getTime(); // Assume install was now.
        }
    }

    protected void queueEvent(String eventType, Map<String, Object> parameters, Map<String, String> payload) {
        queueEvent(eventType, parameters, payload, true);
    }

    protected void queueEvent(String eventType, Map<String, Object> parameters, Map<String, String> payload, boolean triggerEventListener) {
        try {
            storageExecutorExecute(new QueueEventRunnable(eventType, parameters, payload));
            if (triggerEventListener && eventListener != null) {
                eventListener.onEvent(EventHelper.getEventName(eventType, parameters));
            }
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, "Unable to queue event", exp);
        }
    }

    protected void userUpdate(JSONObject attributes) {
        if (attributes != null && attributes.length() != 0) {
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("attributes", attributes);
            queueEvent("user", parameters, null);
        }
    }

    protected void postBatchRequest(C config, String postData, final IPostBatchRequestListener listener) {
        restClient.post(config.getEventsUrl() + BATCH_EVENTS_ACTION, postData, new IRESTResponseListener() {
            @Override
            public void onResponse(RESTResponse response) {
                boolean deleteEvents = true;
                if (SwrveHelper.userErrorResponseCode(response.responseCode)) {
                    SwrveLogger.e(LOG_TAG, "Error sending events to Swrve: " + response.responseBody);
                } else if (SwrveHelper.successResponseCode(response.responseCode)) {
                    SwrveLogger.i(LOG_TAG, "Events sent to Swrve");
                } else if (SwrveHelper.serverErrorResponseCode(response.responseCode)) {
                    deleteEvents = false;
                    SwrveLogger.e(LOG_TAG, "Error sending events to Swrve: " + response.responseBody);
                }

                // Resend if we got a server error (5XX)
                listener.onResponse(deleteEvents);
            }

            @Override
            public void onException(Exception exp) {
            }
        });
    }

    protected boolean restClientExecutorExecute(Runnable runnable) {
        try {
            if (restClientExecutor.isShutdown()) {
                SwrveLogger.i(LOG_TAG, "Trying to schedule a rest execution while shutdown");
            } else {
                restClientExecutor.execute(SwrveRunnables.withoutExceptions(runnable));
                return true;
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error while scheduling a rest execution", e);
        }
        return false;
    }

    protected boolean storageExecutorExecute(Runnable runnable) {
        try {
            if (storageExecutor.isShutdown()) {
                SwrveLogger.i(LOG_TAG, "Trying to schedule a storage execution while shutdown");
            } else {
                storageExecutor.execute(SwrveRunnables.withoutExceptions(runnable));
                return true;
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error while scheduling a storage execution", e);
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
            if (config.isAndroidIdLogging()) {
                this.androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, "Get device screen info failed", exp);
        }
    }

    protected ITelephonyManager getTelephonyManager(Context context) {
        return new AndroidTelephonyManagerWrapper(context);
    }

    @RequiresPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE)
    protected void findCacheFolder(Activity activity) {
        cacheDir = config.getCacheDir();

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
        SwrveLogger.d(LOG_TAG, "Using cache directory at " + cacheDir.getPath());
    }

    protected boolean checkPermissionGranted(Context context, String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    protected void requestPermissions(Activity activity, String permissions[]) {
        ActivityCompat.requestPermissions(activity, permissions, 0);
    }

    public Date getNow() {
        return new Date();
    }

    protected boolean hasShowTooManyMessagesAlready() {
        return (messagesLeftToShow <= 0);
    }

    protected boolean isTooSoonToShowMessageAfterLaunch(Date now) {
        return now.before(showMessagesAfterLaunch);
    }

    protected boolean isTooSoonToShowMessageAfterDelay(Date now) {
        if (showMessagesAfterDelay == null) {
            return false;
        }
        return now.before(showMessagesAfterDelay);
    }

    protected void saveCampaignsInCache(final JSONObject campaignContent) {
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                cachedLocalStorage.setAndFlushSecureSharedEntryForUser(userId, CAMPAIGN_CATEGORY, campaignContent.toString(), getUniqueKey());
            }
        });
    }

    protected void saveLocationCampaignsInCache(final JSONObject locationCampaignContent) {
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                cachedLocalStorage.setAndFlushSecureSharedEntryForUser(userId, LOCATION_CAMPAIGN_CATEGORY, locationCampaignContent.toString(), getUniqueKey());
            }
        });
    }

    protected void saveResourcesInCache(final JSONArray resourcesContent) {
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                cachedLocalStorage.setAndFlushSecureSharedEntryForUser(userId, RESOURCES_CACHE_CATEGORY, resourcesContent.toString(), getUniqueKey());
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
            if (campaign.hasElementForEvent(SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER)) {
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
                    messageListener.onMessage(message, true);
                    autoShowMessagesEnabled = false;
                }
            }
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, "Could not launch campaign automatically.", exp);
        }
    }

    protected void autoShowConversation(SwrveBase<T, C> swrve) {
        try {
            if (conversationListener != null && autoShowMessagesEnabled) {
                SwrveConversation conversation = swrve.getConversationForEvent(SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER);
                if (conversation != null) {
                    conversationListener.onMessage(conversation);
                    autoShowMessagesEnabled = false;
                }
            }
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, "Could not launch conversation automatically.", exp);
        }
    }

    /**
     * Ensure that after SwrveConfig.autoShowMessagesMaxDelay milliseconds autoshow is disabled
     */
    protected void disableAutoShowAfterDelay() {
        ScheduledExecutorService timedService = Executors.newSingleThreadScheduledExecutor();
        timedService.schedule(new Runnable() {
            @Override
            public void run() {
                autoShowMessagesEnabled = false;
            }
        }, config.getAutoShowMessagesMaxDelay(), TimeUnit.MILLISECONDS);
    }

    @SuppressLint("UseSparseArrays")
    protected void loadCampaignsFromJSON(JSONObject json, JSONObject settings) {
        if (json == null) {
            SwrveLogger.i(LOG_TAG, "NULL JSON for campaigns, aborting load.");
            return;
        }

        if (json.length() == 0) {
            SwrveLogger.i(LOG_TAG, "Campaign JSON empty, no campaigns downloaded");
            campaigns.clear();
            return;
        }

        SwrveLogger.i(LOG_TAG, "Campaign JSON data: " + json);

        try {
            // Check if schema has a version number
            if (!json.has("version")) {
                return;
            }

            // Version check
            String version = json.getString("version");
            if (!version.equals(TEMPLATE_VERSION)) {
                SwrveLogger.i(LOG_TAG, "Campaign JSON has the wrong version. No campaigns loaded.");
                return;
            }

            // CDN
            this.cdnRoot = json.getString("cdn_root");
            SwrveLogger.i(LOG_TAG, "CDN URL " + this.cdnRoot);

            // App Data
            JSONObject gamesData = json.getJSONObject("game_data");
            if (gamesData != null) {
                @SuppressWarnings("unchecked")
                Iterator<String> gamesDataIt = gamesData.keys();
                while (gamesDataIt.hasNext()) {
                    String appId = (String) gamesDataIt.next();
                    JSONObject gameData = gamesData.getJSONObject(appId);
                    if (gameData.has("app_store_url")) {
                        String url = gameData.getString("app_store_url");
                        this.appStoreURLs.put(Integer.parseInt(appId), url);
                        if (SwrveHelper.isNullOrEmpty(url)) {
                            SwrveLogger.e(LOG_TAG, "App store link " + appId + " is empty!");
                        } else {
                            SwrveLogger.i(LOG_TAG, "App store Link " + appId + ": " + url);
                        }
                    }
                }
            }

            JSONObject rules = json.getJSONObject("rules");
            int delay = (rules.has("delay_first_message")) ? rules.getInt("delay_first_message") : DEFAULT_DELAY_FIRST_MESSAGE;
            long maxShows = (rules.has("max_messages_per_session")) ? rules.getLong("max_messages_per_session") : DEFAULT_MAX_SHOWS;
            int minDelay = (rules.has("min_delay_between_messages")) ? rules.getInt("min_delay_between_messages") : DEFAULT_MIN_DELAY;

            Date now = getNow();
            this.showMessagesAfterLaunch = SwrveHelper.addTimeInterval(initialisedTime, delay, Calendar.SECOND);
            this.minDelayBetweenMessage = minDelay;
            this.messagesLeftToShow = maxShows;

            SwrveLogger.i(LOG_TAG, "App rules OK: Delay Seconds: " + delay + " Max shows: " + maxShows);
            SwrveLogger.i(LOG_TAG, "Time is " + now.toString() + " show messages after " + this.showMessagesAfterLaunch.toString());

            Map<Integer, String> campaignsDownloaded = null;

            // QA
            if (json.has("qa")) {
                JSONObject jsonQa = json.getJSONObject("qa");
                campaignsDownloaded = new HashMap<Integer, String>();
                SwrveLogger.i(LOG_TAG, "You are a QA user!");
                // Load QA user settings
                qaUser = new SwrveQAUser((SwrveBase<T, C>) this, jsonQa);
                qaUser.bindToServices();

                if (jsonQa.has("campaigns")) {
                    JSONArray jsonQaCampaigns = jsonQa.getJSONArray("campaigns");
                    for (int i = 0; i < jsonQaCampaigns.length(); i++) {
                        JSONObject jsonQaCampaign = jsonQaCampaigns.getJSONObject(i);
                        int campaignId = jsonQaCampaign.getInt("id");
                        String campaignReason = jsonQaCampaign.getString("reason");

                        SwrveLogger.i(LOG_TAG, "Campaign " + campaignId + " not downloaded because: " + campaignReason);

                        // Add campaign for QA purposes
                        campaignsDownloaded.put(campaignId, campaignReason);
                    }
                }
            } else if (qaUser != null) {
                qaUser.unbindToServices();
                qaUser = null;
            }

            JSONArray jsonCampaigns = json.getJSONArray("campaigns");
            List<Integer> newCampaignIds = new ArrayList<Integer>();

            // Remove any campaigns that aren't in the new list
            // We do this before updating campaigns and adding new campaigns to ensure
            // there isn't a gap where no campaigns are available while reloading
            for (int i = 0, j = jsonCampaigns.length(); i < j; i++) {
                JSONObject campaignData = jsonCampaigns.getJSONObject(i);
                newCampaignIds.add(campaignData.getInt("id"));
            }

            // Also serialize the campaign settings
            JSONObject jsonSettings = (settings == null) ? new JSONObject() : settings;
            boolean saveCampaignSettings = (qaUser == null || !qaUser.isResetDevice());
            for (int i = campaigns.size() - 1; i >= 0; i--) {
                SwrveBaseCampaign campaign = campaigns.get(i);
                if (!newCampaignIds.contains(campaign.getId())) {
                    campaigns.remove(i);
                } else if (saveCampaignSettings) {
                    jsonSettings.put(Integer.toString(campaign.getId()), campaign.createSettings());
                }
            }

            List<SwrveBaseCampaign> newCampaigns = new ArrayList<SwrveBaseCampaign>();
            Set<String> assetsQueue = new HashSet<String>();
            for (int i = 0, j = jsonCampaigns.length(); i < j; i++) {
                JSONObject campaignData = jsonCampaigns.getJSONObject(i);
                // Load campaign and get assets to be loaded
                Set<String> campaignAssetsQueue = new HashSet<String>();

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
                        int conversationVersion = campaignData.optInt("conversation_version", 1);
                        if (conversationVersion <= CONVERSATION_VERSION) {
                            campaign = loadConversationCampaignFromJSON(campaignData, campaignAssetsQueue);
                        } else {
                            SwrveLogger.i(LOG_TAG, "Conversation version " + conversationVersion + " cannot be loaded with this SDK version");
                        }
                    } else {
                        campaign = loadCampaignFromJSON(campaignData, campaignAssetsQueue);
                    }

                    if (campaign != null) {
                        assetsQueue.addAll(campaignAssetsQueue);

                        // Check if we need to reset the device for QA, otherwise load campaign settings into downloaded campaign
                        if (qaUser == null || !qaUser.isResetDevice()) {
                            String campaignIdStr = Integer.toString(campaign.getId());
                            if (jsonSettings.has(campaignIdStr)) {
                                JSONObject campaignSettings = jsonSettings.getJSONObject(campaignIdStr);
                                if (campaignSettings != null) {
                                    campaign.loadSettings(campaignSettings);
                                }
                            }
                        }

                        newCampaigns.add(campaign);
                        SwrveLogger.i(LOG_TAG, "Got campaign with id " + campaign.getId());

                        if (qaUser != null) {
                            // Add campaign for QA purposes
                            campaignsDownloaded.put(campaign.getId(), null);
                        }
                    }
                } else {
                    SwrveLogger.i(LOG_TAG, "Not all requirements were satisfied for this campaign: " + lastCheckedFilter);
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
            campaigns = new ArrayList<SwrveBaseCampaign>(newCampaigns);
        } catch (JSONException exp) {
            SwrveLogger.e(LOG_TAG, "Error parsing campaign JSON", exp);
        }
    }

    protected void loadLocationCampaignsFromJSON(JSONObject locationCampaignJSON) {
        if (locationCampaignJSON == null) {
            SwrveLogger.i(LOG_TAG, "NULL JSON for location campaigns, aborting load.");
            return;
        }

        if (locationCampaignJSON.length() == 0) {
            SwrveLogger.i(LOG_TAG, "Location campaign JSON empty, no location campaigns downloaded");
            locationCampaigns.clear();
            return;
        }

        if(locationCampaignJSON.has("campaigns") == false) {
            SwrveLogger.i(LOG_TAG, "No Location campaigns.");
            locationCampaigns.clear();
            return;
        }

        SwrveLogger.i(LOG_TAG, "Location campaign JSON data: " + locationCampaignJSON);

        try {
            String campaignsJsonString = locationCampaignJSON.getString("campaigns");
            Map<String, LocationCampaign> newLocationCampaigns = LocationCampaign.fromJSON(campaignsJsonString);

            // Update current list of campaigns with new ones
            locationCampaigns = new HashMap<String, LocationCampaign>(newLocationCampaigns);
        } catch (JSONException ex) {
            SwrveLogger.e(LOG_TAG, "Error parsing location campaign JSON", ex);
        }
    }

    private boolean supportsDeviceFilter(String requirement) {
        return SUPPORTED_REQUIREMENTS.contains(requirement.toLowerCase());
    }

    protected void downloadAssets(final Set<String> assetsQueue) {
        assetsCurrentlyDownloading = true;
        ExecutorService resourceDownloadExecutor = Executors.newSingleThreadExecutor();
        resourceDownloadExecutor.execute(SwrveRunnables.withoutExceptions(new Runnable() {
            @Override
            public void run() {
                Set<String> assetsToDownload = filterExistingFiles(assetsQueue);
                for (String asset : assetsToDownload) {
                    boolean success = downloadAssetSynchronously(asset);
                    if (success) {
                        synchronized (assetsOnDisk) {
                            assetsOnDisk.add(asset);
                        }
                    }
                }
                assetsCurrentlyDownloading = false;
                autoShowMessages();
            }
        }));
    }

    protected SwrveCampaign loadCampaignFromJSON(JSONObject campaignData, Set<String> assetsQueue) throws JSONException {
        return new SwrveCampaign((SwrveBase<?, ?>) this, campaignData, assetsQueue);
    }

    protected SwrveConversationCampaign loadConversationCampaignFromJSON(JSONObject campaignData, Set<String> assetsQueue) throws JSONException {
        return new SwrveConversationCampaign((SwrveBase<?, ?>) this, campaignData, assetsQueue);
    }

    @RequiresPermission (Manifest.permission.WRITE_EXTERNAL_STORAGE)
    protected boolean downloadAssetSynchronously(final String assetPath) {
        String url = cdnRoot + assetPath;
        InputStream inputStream = null;
        try {
            URLConnection openConnection = new URL(url).openConnection();
            inputStream = new SwrveFilterInputStream(openConnection.getInputStream());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                stream.write(buffer, 0, bytesRead);
            }
            byte[] fileContents = stream.toByteArray();
            String sha1File = SwrveHelper.sha1(stream.toByteArray());
            if (assetPath.equals(sha1File)) {
                if (cacheDir.canWrite()) {
                    FileOutputStream fileStream = new FileOutputStream(new File(cacheDir, assetPath));
                    fileStream.write(fileContents); // Save to file
                    fileStream.close();
                    return true;
                } else {
                    boolean permission = checkPermissionGranted(context.get(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    SwrveLogger.w(LOG_TAG, "Could not download assets because do not have write access to cacheDir:" + cacheDir + " WRITE_EXTERNAL_STORAGE permission granted:" + permission);
                }
            }

        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error downloading campaigns", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    SwrveLogger.e(LOG_TAG, "Error closing assets stream.", e);
                }
            }
        }

        return false;
    }

    protected Set<String> filterExistingFiles(Set<String> assetsQueue) {
        Iterator<String> itDownloadQueue = assetsQueue.iterator();
        while (itDownloadQueue.hasNext()) {
            String assetPath = itDownloadQueue.next();
            File file = new File(cacheDir, assetPath);
            if (file.exists()) {
                itDownloadQueue.remove();
                synchronized (assetsOnDisk) {
                    assetsOnDisk.add(assetPath);
                }
            }
        }
        return assetsQueue;
    }

    protected void saveCampaignSettings() {
        try {
            // Save campaign state
            JSONObject jsonSettings = new JSONObject();
            Iterator<SwrveBaseCampaign> itCampaign = campaigns.iterator();
            while (itCampaign.hasNext()) {
                SwrveBaseCampaign campaign = itCampaign.next();
                jsonSettings.put(Integer.toString(campaign.getId()), campaign.createSettings());
            }

            final String serializedSettings = jsonSettings.toString();
            // Write to cache
            storageExecutorExecute(new Runnable() {
                @Override
                public void run() {
                    MemoryCachedLocalStorage cachedStorage = cachedLocalStorage;
                    cachedStorage.setCacheEntryForUser(userId, CAMPAIGN_SETTINGS_CATEGORY, serializedSettings);
                    if (cachedStorage.getSecondaryStorage() != null) {
                        cachedStorage.getSecondaryStorage().setCacheEntryForUser(userId, CAMPAIGN_SETTINGS_CATEGORY, serializedSettings);
                    }
                    SwrveLogger.i(LOG_TAG, "Saved campaigns in cache");
                }
            });
        } catch (JSONException exp) {
            SwrveLogger.e(LOG_TAG, "Error saving campaigns settings", exp);
        }
    }

    protected void noMessagesWereShown(String event, String reason) {
        SwrveLogger.i(LOG_TAG, "Not showing message for " + event + ": " + reason);
        if (qaUser != null) {
            qaUser.triggerFailure(event, reason);
        }
    }

    protected void unbindAndShutdown(Activity callerActivity) {
        // Reduce the references to the SDK
        int counter = bindCounter.decrementAndGet();

        removeCurrentDialog(callerActivity);

        // Check if there are no more references to this object
        if (counter == 0) {
            this.activityContext = null;  // Remove the binding to the current activity, if any
            if (mustCleanInstance) {
                ((SwrveBase<?, ?>) this).shutdown();
            }
        }
    }

    protected void removeCurrentDialog(Activity callerActivity) {
        if (currentDialog != null) {
            final SwrveDialog dialog = currentDialog.get();
            if (dialog != null && dialog.isShowing()) {
                Activity dialogActivity = dialog.getOwnerActivity();
                if (callerActivity == null || callerActivity ==  dialogActivity) {
                    messageDisplayed = dialog.getMessage();
                    lastMessageDestroyed = (new Date()).getTime();
                    Activity activity = dialogActivity;
                    if (activity == null) {
                        activity = getActivityContext();
                    }

                    if (activity != null) {
                        // Call from activity UI thread
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                            }
                        });
                    } else {
                        // Call from this thread
                        dialog.dismiss();
                    }
                    currentDialog = null;
                }
            } else {
                currentDialog = null;
            }
        }
    }

    protected void saveCurrentOrientation(Context ctx) {
        try {
            if (ctx != null) {
                final Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                previousOrientation = display.getRotation();
            }
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, "Could not obtain device orientation", exp);
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

    /**
     * Get device info and send it to Swrve
     */
    protected void queueDeviceInfoNow(final boolean sendNow) {
        final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
        storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                userUpdate(swrve.getDeviceInfo());
                // Send event after it has been queued, trigger from the storage executor
                // to wait for all events. Will be executed on the rest thread.
                if (sendNow) {
                    storageExecutorExecute(new Runnable() {
                        @Override
                        public void run() {
                            SwrveLogger.i(LOG_TAG, "Sending device info");
                            swrve.sendQueuedEvents();
                        }
                    });
                }
            }
        });
    }

    /**
     * Create a unique key for this user
     */
    public String getUniqueKey() {
        return this.userId + this.apiKey;
    }

    /**
     * Invalidates the currently stored ETag
     * Should be called when a refresh of campaigns and resources needs to be forced (eg. when cached data cannot be read)
     */
    protected void invalidateETag() {
        campaignsAndResourcesLastETag = null;
        SharedPreferences settings = context.get().getSharedPreferences(SDK_PREFS_NAME, 0);
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.remove("campaigns_and_resources_etag");
        settingsEditor.commit();
    }

    /**
     * Initialize Resource Manager with cache content
     */
    protected void initResources() {
        String cachedResources = null;

        // Read cached resources
        try {
            cachedResources = cachedLocalStorage.getSecureCacheEntryForUser(userId, RESOURCES_CACHE_CATEGORY, getUniqueKey());
        } catch (SecurityException e) {
            invalidateETag();
            SwrveLogger.i(LOG_TAG, "Signature for " + RESOURCES_CACHE_CATEGORY + " invalid; could not retrieve data from cache");
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", "Swrve.signature_invalid");
            queueEvent("event", parameters, null, false);
        }

        if (cachedResources != null) {
            try {
                JSONArray resourceJson = new JSONArray(cachedResources);
                this.resourceManager.setResourcesFromJSON(resourceJson);
            } catch (JSONException e) {
                SwrveLogger.e(LOG_TAG, "Could not parse cached json content for resources", e);
            }
        } else {
            invalidateETag();
        }
    }

    /**
     * Initialize campaigns with cache content
     */
    protected void initCampaigns() {
        campaigns = new ArrayList<SwrveBaseCampaign>();

        try {
            String campaignsFromCache = cachedLocalStorage.getSecureCacheEntryForUser(userId, CAMPAIGN_CATEGORY, getUniqueKey());
            if (!SwrveHelper.isNullOrEmpty(campaignsFromCache)) {
                JSONObject campaignsJson = new JSONObject(campaignsFromCache);
                // Load campaign settings
                String campaignSettingsFromCache = cachedLocalStorage.getCacheEntryForUser(userId, CAMPAIGN_SETTINGS_CATEGORY);
                JSONObject campaignSettingsJson = null;
                if (!SwrveHelper.isNullOrEmpty(campaignSettingsFromCache)) {
                    campaignSettingsJson = new JSONObject(campaignSettingsFromCache);
                }
                updateCampaigns(campaignsJson, campaignSettingsJson);
                SwrveLogger.i(LOG_TAG, "Loaded campaigns from cache.");
            } else {
                invalidateETag();
            }
        } catch (JSONException e) {
            invalidateETag();
            SwrveLogger.e(LOG_TAG, "Invalid json in cache, cannot load campaigns", e);
        } catch (SecurityException e) {
            invalidateETag();
            SwrveLogger.e(LOG_TAG, "Signature validation failed when trying to load campaigns from cache.", e);
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", "Swrve.signature_invalid");
            queueEvent("event", parameters, null, false);
        }
    }

    /**
     * Initialize location campaigns with cache content
     */
    protected void initLocationCampaigns() {
        locationCampaigns = new HashMap<String, LocationCampaign>();

        try {
            String locationCampaignsFromCache = cachedLocalStorage.getSecureCacheEntryForUser(userId, LOCATION_CAMPAIGN_CATEGORY, getUniqueKey());
            if (!SwrveHelper.isNullOrEmpty(locationCampaignsFromCache)) {
                JSONObject locationCampaignsJson = new JSONObject(locationCampaignsFromCache);
                loadLocationCampaignsFromJSON(locationCampaignsJson);
                SwrveLogger.i(LOG_TAG, "Loaded location campaigns from cache.");
            } else {
                invalidateETag();
            }
        } catch (JSONException e) {
            invalidateETag();
            SwrveLogger.e(LOG_TAG, "Invalid json in cache, cannot load location campaigns", e);
        } catch (SecurityException e) {
            invalidateETag();
            SwrveLogger.e(LOG_TAG, "Signature validation failed when trying to load location campaigns from cache.", e);
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("name", "Swrve.signature_invalid");
            queueEvent("event", parameters, null, false);
        }
    }

    // TODO: Change this for GA.
    public void initSDKForLocationCampaigns(Context context) {

        if (SwrveHelper.isNullOrEmpty(userId)) {
            userId = getUniqueUserId(context);
        }
        if (restClient == null) {
            restClient = createRESTClient();
        }
        if (cachedLocalStorage == null) {
            cachedLocalStorage = createCachedLocalStorage();
            openLocalStorageConnection();
        }
        if (storageExecutor == null) {
            storageExecutor = createStorageExecutor();
        }
        if (restClientExecutor == null) {
            restClientExecutor = createRESTClientExecutor();
        }

        if (sessionToken == null) {
            this.sessionToken = SwrveHelper.generateSessionToken(this.apiKey, this.appId, userId);
        }

        if (appVersion == null) {
            this.appVersion = config.getAppVersion();
            if (SwrveHelper.isNullOrEmpty(this.appVersion)) {
                try {
                    PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                    this.appVersion = pInfo.versionName;
                } catch (Exception exp) {
                    SwrveLogger.e(LOG_TAG, "Couldn't get app version from PackageManager. Please provide the app version manually through the config object.", exp);
                }
            }
        }

        if (locationCampaigns == null) {
            initLocationCampaigns();
        }
    }

    /**
     * Update campaigns with given JSON
     */
    protected void updateCampaigns(JSONObject campaignJSON, JSONObject campaignSettingsJSON) {
        loadCampaignsFromJSON(campaignJSON, campaignSettingsJSON);
    }

    /**
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

    /**
     * Check if any events need sending, then after flush delay reload campaigns and resources
     * This function should be called periodically.
     */
    protected void checkForCampaignAndResourcesUpdates() {
        // If there are any events to be sent, or if any events were sent since last refresh
        // send events queued, wait campaignsAndResourcesFlushRefreshDelay for events to reach servers and refresh
        final LinkedHashMap<ILocalStorage, LinkedHashMap<Long, String>> combinedEvents = cachedLocalStorage.getCombinedFirstNEvents(config.getMaxEventsPerFlush());
        if (!combinedEvents.isEmpty() || eventsWereSent) {
            final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
            swrve.sendQueuedEvents();
            eventsWereSent = false;
            ScheduledExecutorService timedService = Executors.newSingleThreadScheduledExecutor();
            timedService.schedule(new Runnable() {
                @Override
                public void run() {
                    swrve.refreshCampaignsAndResources();
                }
            }, campaignsAndResourcesFlushRefreshDelay.longValue(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Set up timer for checking for campaign and resources updates. Called when session begins and after IAP.
     */
    protected void startCampaignsAndResourcesTimer(boolean sessionStart) {
        if (!config.isAutoDownloadCampaingsAndResources()) {
            return;
        }

        final SwrveBase<T, C> swrve = (SwrveBase<T, C>) this;
        // If there is an existing executor, shut it down. This will finish any tasks in progress, but not execute any tasks currently scheduled or accept new tasks
        if (campaignsAndResourcesExecutor != null) {
            campaignsAndResourcesExecutor.shutdown();
        }

        // For session start, execute immediately.
        if (sessionStart) {
            swrve.refreshCampaignsAndResources();
        }

        // Start repeating timer to begin checking if campaigns/resources needs updating. It starts straight away.
        eventsWereSent = true;
        campaignsAndResourcesExecutor = new ScheduledThreadPoolExecutor(1);
        campaignsAndResourcesExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        campaignsAndResourcesExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                checkForCampaignAndResourcesUpdates();
            }
        }, 0l, campaignsAndResourcesFlushFrequency.longValue(), TimeUnit.MILLISECONDS);
    }

    public Set<String> getAssetsOnDisk() {
        synchronized (assetsOnDisk) {
            return this.assetsOnDisk;
        }
    }

    public String getAutoShowEventTrigger() {
        return SWRVE_AUTOSHOW_AT_SESSION_START_TRIGGER;
    }

    private class QueueEventRunnable implements Runnable {
        private String eventType;
        private Map<String, Object> parameters;
        private Map<String, String> payload;

        public QueueEventRunnable(String eventType, Map<String, Object> parameters, Map<String, String> payload) {
            this.eventType = eventType;
            this.parameters = parameters;
            this.payload = payload;
        }

        @Override
        public void run() {
            try {
                String eventString = EventHelper.eventAsJSON(eventType, parameters, payload, cachedLocalStorage);
                parameters = null;
                payload = null;
                cachedLocalStorage.addEvent(eventString);
                SwrveLogger.i(LOG_TAG, eventType + " event queued");
            } catch (JSONException je) {
                SwrveLogger.e(LOG_TAG, "Parameter or payload data not encodable as JSON", je);
            } catch (Exception se) {
                SwrveLogger.e(LOG_TAG, "Unable to insert into local storage", se);
            }
        }
    }

    protected class DisplayMessageRunnable implements Runnable {
        private SwrveBase<?, ?> sdk;
        private Activity activity;
        private SwrveMessage message;
        private boolean firstTime;

        public DisplayMessageRunnable(SwrveBase<?, ?> sdk, Activity activity, SwrveMessage message, boolean firstTime) {
            this.sdk = sdk;
            this.activity = activity;
            this.message = message;
            this.firstTime = firstTime;
        }

        @Override
        public void run() {
            try {
                SwrveLogger.d(LOG_TAG, "Called show dialog");
                SwrveDialog dialog = (currentDialog == null) ? null : currentDialog.get();
                if (dialog == null || !dialog.isShowing()) {
                    SwrveOrientation deviceOrientation = getDeviceOrientation();
                    SwrveLogger.d(LOG_TAG, "Trying to show dialog with orientation " + deviceOrientation);
                    SwrveMessageView swrveMessageView = SwrveMessageViewFactory.getInstance().buildLayout(activity, message, deviceOrientation, previousOrientation, installButtonListener, customButtonListener, firstTime, config.getMinSampleSize());
                    SwrveDialog newDialog = new SwrveDialog(activity, message, swrveMessageView, config.isHideToolbar());
                    newDialog.setOnDismissListener(new OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // Remove reference to the dialog on our side
                            if (currentDialog != null) {
                                SwrveDialog refDialog = currentDialog.get();
                                if (refDialog == dialog) {
                                    currentDialog = null;
                                }
                            }
                        }
                    });
                    saveCurrentOrientation(activity);

                    // Check if the customer wants to manage the dialog themselves
                    if (dialogListener != null) {
                        dialogListener.onDialog(newDialog);
                    } else {
                        // Save a reference to the dialog and display it now
                        sdk.currentDialog = new WeakReference<SwrveDialog>(newDialog);
                        newDialog.show();
                    }
                }

                activity = null;
                sdk = null;
                message = null;
            } catch (Exception e) {
                SwrveLogger.w(LOG_TAG, "Couldn't create a SwrveMessageView", e);
            }
        }
    }
}
