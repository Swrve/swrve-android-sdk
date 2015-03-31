package com.swrve.sdk.config;

import android.graphics.Color;

import com.swrve.sdk.SwrveAppStore;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.messaging.SwrveOrientation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Configuration for the Swrve SDK.
 */
public abstract class SwrveConfigBase {
    /**
     * Custom unique user id.
     */
    private String userId;

    /**
     * Enable in-app messages.
     */
    private boolean talkEnabled = true;

    /**
     * Maximum size of the internal SQLite database.
     */
    private long maxSqliteDbSize = 1 * 1024 * 1024;

    /**
     * Maximum number of events to send per flush.
     */
    private int maxEventsPerFlush = 50;

    /**
     * Maximum number of concurrent downloads.
     */
    private int maxConcurrentDownloads = 10;

    /**
     * Name of SQLite database to use for storage.
     */
    private String dbName = "swrve.db";

    /**
     * Events end-point.
     */
    private URL eventsUrl = null;
    private URL defaultEventsUrl = null;
    private boolean useHttpsForEventsUrl = true;

    /**
     * Content end-point.
     */
    private URL contentUrl = null;
    private URL defaultContentUrl = null;
    private boolean useHttpsForContentUrl = false;

    /**
     * Session timeout time.
     */
    private long newSessionInterval = 30000;

    /**
     * App version.
     */
    private String appVersion;

    /**
     * App Store where the app will be distributed.
     */
    private String appStore = SwrveAppStore.Google;

    /**
     * App language. If null it defaults to the value returned by Locale.getDefault().
     */
    private String language;

    /**
     * Orientation supported by the application.
     */
    private SwrveOrientation orientation = SwrveOrientation.Both;

    /**
     * Automatically download campaigns and resources.
     */
    private boolean autoDownloadCampaignsAndResources = true;

    /**
     * Sample size to use when loading in-app message images. Has to be a power
     * of two.
     */
    private int minSampleSize = 1;

    /**
     * Cache folder used to save the in-app message images.
     */
    private File cacheDir;

    /**
     * Automatically send events onResume.
     */
    private boolean sendQueuedEventsOnResume = true;

    /**
     * Maximum delay for in-app messages to appear after initialization.
     */
    private long autoShowMessagesMaxDelay = 5000;

    /**
     * Will load the campaign and resources cache on the UI thread.
     */
    private boolean loadCachedCampaignsAndResourcesOnUIThread = true;

    /**
     * Default in-app background color used if none is specified in the template.
     */
    private int defaultBackgroundColor = Color.TRANSPARENT;

    /**
     * Create an instance of the SDK advance preferences.
     */
    public SwrveConfigBase() {
    }

    /**
     * @return Whether campaigns and resources will automatically be downloaded.
     */
    public boolean isAutoDownloadCampaingsAndResources() {
        return this.autoDownloadCampaignsAndResources;
    }

    /**
     * Download resources and in-app campaigns automatically.
     *
     * @param autoDownload Automatically download campaigns and resources.
     */
    public SwrveConfigBase setAutoDownloadCampaignsAndResources(boolean autoDownload) {
        this.autoDownloadCampaignsAndResources = autoDownload;
        return this;
    }

    /**
     * @return Orientation supported by the application.
     */
    public SwrveOrientation getOrientation() {
        return orientation;
    }

    /**
     * @param orientation Orientation supported by the application.
     */
    public SwrveConfigBase setOrientation(SwrveOrientation orientation) {
        this.orientation = orientation;
        return this;
    }

    /**
     * Set the locale of the app. If empty or null then
     * the default locale is used.
     *
     * @param locale Locale of the app.
     */
    public SwrveConfigBase setLanguage(Locale locale) {
        this.language = SwrveHelper.toLanguageTag(locale);
        return this;
    }

    /**
     * @return Language of the app.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Set the language of the app. If empty or null then
     * the default locale is used.
     *
     * @param language Language of the app.
     * @deprecated Use {@link #setLanguage(Locale)} instead.
     */
    @Deprecated
    public SwrveConfigBase setLanguage(String language) {
        this.language = language;
        return this;
    }

    /**
     * @return Maximum byte size of the internal SQLite database.
     */
    public long getMaxSqliteDbSize() {
        return maxSqliteDbSize;
    }

    /**
     * Set the maximum byte size of the internal SQLite database.
     *
     * @param maxSqliteDbSize Maximum size in bytes.
     */
    public SwrveConfigBase setMaxSqliteDbSize(long maxSqliteDbSize) {
        this.maxSqliteDbSize = maxSqliteDbSize;
        return this;
    }

    /**
     * @return Maximum number of events per batch.
     */
    public int getMaxEventsPerFlush() {
        return maxEventsPerFlush;
    }

    /**
     * Set the maximum number of events per batch to the event server.
     *
     * @param maxEventsPerFlush Maximum number of events per batch.
     */
    public SwrveConfigBase setMaxEventsPerFlush(int maxEventsPerFlush) {
        this.maxEventsPerFlush = maxEventsPerFlush;
        return this;
    }

    /**
     * @return Maximum number of concurrent asset downloads.
     */
    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }

    /**
     * Set the maximum number of concurrent asset downloads.
     *
     * @param maxConcurrentDownloads Maximum number of concurrent asset downloads.
     */
    public SwrveConfigBase setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
        return this;
    }

    /**
     * @return Name of the internal SQLite database.
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * Override the name of the internal SQLite database.
     *
     * @param dbName Name of the internal SQLite database.
     */
    public SwrveConfigBase setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    /**
     * @return Location of the event server.
     */
    public URL getEventsUrl() {
        if (eventsUrl == null)
            return defaultEventsUrl;
        return eventsUrl;
    }

    /**
     * Set to override the default location of the server to which Swrve will send analytics events.
     * If your company has a special API end-point enabled, then you should specify it here.
     * You should only need to change this value if you are working with Swrve support on a specific support issue.
     *
     * @param eventsUrl Custom location of the event server.
     */
    public SwrveConfigBase setEventsUrl(URL eventsUrl) {
        this.eventsUrl = eventsUrl;
        return this;
    }

    /**
     * @return Whether to use HTTPS for events.
     */
    public boolean getUseHttpsForEventsUrl() {
        return useHttpsForEventsUrl;
    }

    /**
     * Enable HTTPS for event requests.
     *
     * @param useHttpsForEventsUrl Whether to use HTTPS for api.swrve.com.
     */
    public SwrveConfigBase setUseHttpsForEventsUrl(boolean useHttpsForEventsUrl) {
        this.useHttpsForEventsUrl = useHttpsForEventsUrl;
        return this;
    }

    /**
     * @return Location of the content server.
     */
    public URL getContentUrl() {
        if (contentUrl == null)
            return defaultContentUrl;
        return contentUrl;
    }

    /**
     * Set to override the default location of the server used to obtain resources and in-app campaigns.
     * If your company has a special API end-point enabled, then you should specify it here.
     * You should only need to change this value if you are working with Swrve support on a specific support issue.
     *
     * @param contentUrl Custom location of the content server.
     */
    public SwrveConfigBase setContentUrl(URL contentUrl) {
        this.contentUrl = contentUrl;
        return this;
    }

    /**
     * @return Whether to use HTTPS for resources and in-app campagins.
     */
    public boolean getUseHttpsForContentUrl() {
        return useHttpsForContentUrl;
    }

    /**
     * Enable HTTPS for resources and in-app campaign requests.
     *
     * @param useHttpsForContentUrl Whether to use HTTPS for content.swrve.com.
     */
    public SwrveConfigBase setUseHttpsForContentUrl(boolean useHttpsForContentUrl) {
        this.useHttpsForContentUrl = useHttpsForContentUrl;
        return this;
    }

    /**
     * @return App version.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Set the app version. If empty or null then PackageInfo.versionName
     * will be used.
     *
     * @param appVersion Version of the app.
     */
    public SwrveConfigBase setAppVersion(String appVersion) {
        this.appVersion = appVersion;
        return this;
    }

    /**
     * @return Session timeout time in milliseconds.
     */
    public long getNewSessionInterval() {
        return newSessionInterval;
    }

    /**
     * Set the session timeout time.
     * User activity after this time will be considered a new session.
     *
     * @param newSessionInterval session timeout in milliseconds.
     */
    public SwrveConfigBase setNewSessionInterval(long newSessionInterval) {
        this.newSessionInterval = newSessionInterval;
        return this;
    }

    /**
     * @return the App Store where the app will be distributed.
     */
    public String getAppStore() {
        return appStore;
    }

    /**
     * Set the App Store where the app will be distributed.
     *
     * @param appStore App Store where the app will be distributed.
     */
    public SwrveConfigBase setAppStore(String appStore) {
        this.appStore = appStore;
        return this;
    }

    /**
     * Generate default endpoints with the given app id. Used internally.
     *
     * @throws MalformedURLException
     */
    public void generateUrls(int appId) throws MalformedURLException {
        defaultEventsUrl = new URL(getSchema(useHttpsForEventsUrl) + "://" + appId + ".api.swrve.com");
        defaultContentUrl = new URL(getSchema(useHttpsForContentUrl) + "://" + appId + ".content.swrve.com");
    }

    private static String getSchema(boolean https) {
        return https? "https" : "http";
    }

    /**
     * @return Custom unique user id.
     */
    public String getUserId() {
        return this.userId;
    }

    /**
     * Set a custom unique user id.
     *
     * @param userId Custom unique user id.
     */
    public SwrveConfigBase setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * @return Whether in-app messages are enabled.
     */
    public boolean isTalkEnabled() {
        return this.talkEnabled;
    }

    /**
     * @param enabled Enabled in-app messages.
     */
    public SwrveConfigBase setTalkEnabled(boolean enabled) {
        this.talkEnabled = enabled;
        return this;
    }

    /**
     * The sample size used when loading in-app message images.
     *
     * @return sample size
     */
    public int getMinSampleSize() {
        return minSampleSize;
    }

    /**
     * Minimum sample size to use when loading in-app message images. Has to be a power
     * of two.
     *
     * @param sampleSize sample size
     */
    public void setMinSampleSize(int sampleSize) {
        this.minSampleSize = sampleSize;
    }

    /**
     * Cache folder used to save the in-app message images.
     *
     * @return cache folder
     */
    public File getCacheDir() {
        return cacheDir;
    }

    /**
     * Cache folder used to save the in-app message images.
     * The default is Context.getCacheDir().
     *
     * @param cacheDir cache folder
     */
    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * @return Whether the SDK will send events onResume.
     */
    public boolean isSendQueuedEventsOnResume() {
        return sendQueuedEventsOnResume;
    }

    /**
     * Automatically send events onResume.
     *
     * @param sendQueuedEventsOnResume
     */
    public void setSendQueuedEventsOnResume(boolean sendQueuedEventsOnResume) {
        this.sendQueuedEventsOnResume = sendQueuedEventsOnResume;
    }

    /**
     * Maximum delay for in-app messages to appear after initialization.
     *
     * @return maximum delay in milliseconds
     */
    public long getAutoShowMessagesMaxDelay() {
        return autoShowMessagesMaxDelay;
    }

    /**
     * Maximum delay for in-app messages to appear after initialization.
     *
     * @param autoShowMessagesMaxDelay
     */
    public void setAutoShowMessagesMaxDelay(long autoShowMessagesMaxDelay) {
        this.autoShowMessagesMaxDelay = autoShowMessagesMaxDelay;
    }

    /**
     * Load campaigns and resources cache on the UI thread. Allows to get user resources and
     * campaigns on the early start.
     *
     * @return Whether the SDK will load this data on the UI thread.
     */
    public boolean isLoadCachedCampaignsAndResourcesOnUIThread() {
        return loadCachedCampaignsAndResourcesOnUIThread;
    }

    /**
     * Load campaigns and resources cache on the UI thread. Allows to get user resources and
     * campaigns on the early start.
     *
     * @param loadOnUIThread
     */
    public void setLoadCachedCampaignsAndResourcesOnUIThread(boolean loadOnUIThread) {
        this.loadCachedCampaignsAndResourcesOnUIThread = loadOnUIThread;
    }


    /**
     * The default in-app background color, if none is specified in the template.
     *
     * @return The default in-app background color.
     */
    public int getDefaultBackgroundColor() {
        return defaultBackgroundColor;
    }

    /**
     * Set the default in-app background color.
     * @param defaultBackgroundColor
     */
    public void setDefaultBackgroundColor(int defaultBackgroundColor) {
        this.defaultBackgroundColor = defaultBackgroundColor;
    }
}
