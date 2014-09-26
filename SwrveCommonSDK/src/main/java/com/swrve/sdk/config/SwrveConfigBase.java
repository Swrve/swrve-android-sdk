package com.swrve.sdk.config;

import com.swrve.sdk.SwrveAppStore;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.messaging.SwrveOrientation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;

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
    private boolean useHttpsForEventsUrl = false;

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
     * Target app store.
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
     * Create an instance of the SDK advance preferences.
     */
    public SwrveConfigBase() {
    }

    /**
     * @return the autoDownloadCampaignsAndResources
     */
    public boolean isAutoDownloadCampaingsAndResources() {
        return this.autoDownloadCampaignsAndResources;
    }

    /**
     * @param autoDownload enable auto downloading of campaigns and resources
     */
    public SwrveConfigBase setAutoDownloadCampaignsAndResources(boolean autoDownload) {
        this.autoDownloadCampaignsAndResources = autoDownload;
        return this;
    }

    /**
     * @return the orientation
     */
    public SwrveOrientation getOrientation() {
        return orientation;
    }

    /**
     * @param orientation the orientation to set
     */
    public SwrveConfigBase setOrientation(SwrveOrientation orientation) {
        this.orientation = orientation;
        return this;
    }

    /**
     * @param locale the locale to set
     */
    public SwrveConfigBase setLanguage(Locale locale) {
        this.language = SwrveHelper.toLanguageTag(locale);
        return this;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language the language to set
     * @deprecated Use {@link #setLanguage(Locale)} instead.
     */
    @Deprecated
    public SwrveConfigBase setLanguage(String language) {
        this.language = language;
        return this;
    }

    /**
     * @return the maxSqliteDbSize
     */
    public long getMaxSqliteDbSize() {
        return maxSqliteDbSize;
    }

    /**
     * @param maxSqliteDbSize the maxSqliteDbSize to set
     */
    public SwrveConfigBase setMaxSqliteDbSize(long maxSqliteDbSize) {
        this.maxSqliteDbSize = maxSqliteDbSize;
        return this;
    }

    /**
     * @return the maxEventsPerFlush
     */
    public int getMaxEventsPerFlush() {
        return maxEventsPerFlush;
    }

    /**
     * @param maxEventsPerFlush the maxEventsPerFlush to set
     */
    public SwrveConfigBase setMaxEventsPerFlush(int maxEventsPerFlush) {
        this.maxEventsPerFlush = maxEventsPerFlush;
        return this;
    }

    /**
     * @return the maxConcurrentDownloads
     */
    public int getMaxConcurrentDownloads() {
        return maxConcurrentDownloads;
    }

    /**
     * @param maxConcurrentDownloads the maxConcurrentDownloads to set
     */
    public SwrveConfigBase setMaxConcurrentDownloads(int maxConcurrentDownloads) {
        this.maxConcurrentDownloads = maxConcurrentDownloads;
        return this;
    }

    /**
     * @return the db_name
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * @param dbName the dbName to set
     */
    public SwrveConfigBase setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    /**
     * @return the eventsUrl
     */
    public URL getEventsUrl() {
        if (eventsUrl == null)
            return defaultEventsUrl;
        return eventsUrl;
    }

    /**
     * @param eventsUrl the eventsUrl to set
     */
    public SwrveConfigBase setEventsUrl(URL eventsUrl) {
        this.eventsUrl = eventsUrl;
        return this;
    }

    /**
     * @return the useHttpsForEventsUrl
     */
    public boolean getUseHttpsForEventsUrl() {
        return useHttpsForEventsUrl;
    }

    /**
     * @param useHttpsForEventsUrl the useHttpsForEventsUrl to set
     */
    public SwrveConfigBase setUseHttpsForEventsUrl(boolean useHttpsForEventsUrl) {
        this.useHttpsForEventsUrl = useHttpsForEventsUrl;
        return this;
    }

    /**
     * @return the contentUrl
     */
    public URL getContentUrl() {
        if (contentUrl == null)
            return defaultContentUrl;
        return contentUrl;
    }

    /**
     * @param contentUrl the contentUrl to set
     */
    public SwrveConfigBase setContentUrl(URL contentUrl) {
        this.contentUrl = contentUrl;
        return this;
    }

    /**
     * @return the useHttpsForContentUrl
     */
    public boolean getUseHttpsForContentUrl() {
        return useHttpsForContentUrl;
    }

    /**
     * @param useHttpsForContentUrl the useHttpsForContentUrl to set
     */
    public SwrveConfigBase setUseHttpsForContentUrl(boolean useHttpsForContentUrl) {
        this.useHttpsForContentUrl = useHttpsForContentUrl;
        return this;
    }

    /**
     * @return the appVersion
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * @param appVersion the appVersion to set
     */
    public SwrveConfigBase setAppVersion(String appVersion) {
        this.appVersion = appVersion;
        return this;
    }

    /**
     * @return the newSessionInterval
     */
    public long getNewSessionInterval() {
        return newSessionInterval;
    }

    /**
     * @param newSessionInterval the newSessionInterval to set
     */
    public SwrveConfigBase setNewSessionInterval(long newSessionInterval) {
        this.newSessionInterval = newSessionInterval;
        return this;
    }

    /**
     * @return the appStore
     */
    public String getAppStore() {
        return appStore;
    }

    /**
     * @param appStore the appStore to set
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
        defaultEventsUrl = new URL((useHttpsForEventsUrl? "https" : "http") + "://" + appId + ".api.swrve.com");
        defaultContentUrl = new URL((useHttpsForContentUrl? "https" : "http") + "://" + appId + ".content.swrve.com");
    }

    /**
     * @return the user id
     */
    public String getUserId() {
        return this.userId;
    }

    /**
     * @param userId the user id to set
     */
    public SwrveConfigBase setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * @return if talk is enabled
     */
    public boolean isTalkEnabled() {
        return this.talkEnabled;
    }

    /**
     * @param enabled activate/deactivate talk
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
     * @return if the sdk will send events onResume
     */
    public boolean isSendQueuedEventsOnResume() {
        return sendQueuedEventsOnResume;
    }

    /**
     * Automatically send events onResume
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
}
