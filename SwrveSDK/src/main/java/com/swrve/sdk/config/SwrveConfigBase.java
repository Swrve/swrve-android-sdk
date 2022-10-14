package com.swrve.sdk.config;

import com.swrve.sdk.SwrveAppStore;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveInitMode;
import com.swrve.sdk.SwrveNotificationConfig;
import com.swrve.sdk.SwrvePushNotificationListener;
import com.swrve.sdk.SwrveSSLSocketFactoryConfig;
import com.swrve.sdk.SwrveSilentPushListener;
import com.swrve.sdk.messaging.SwrveOrientation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Configuration for the Swrve SDK.
 */
public abstract class SwrveConfigBase {

    private long maxSqliteDbSize = 1 * 1024 * 1024; // Maximum size of the internal SQLite database.
    private int maxEventsPerFlush = 50; // Maximum number of events to send per flush.
    private String dbName = "swrve.db";
    private SwrveStack selectedStack = SwrveStack.US;
    private URL eventsUrl = null;
    private URL defaultEventsUrl = null;
    private URL contentUrl = null;
    private URL defaultContentUrl = null;
    private URL identityUrl = null;
    private URL defaultIdentityUrl = null;
    private long newSessionInterval = 30000; // Session timeout time
    private String appVersion;
    private String appStore = SwrveAppStore.Google; // App Store where the app will be distributed.
    private String language; // App language. If null it defaults to the value returned by Locale.getDefault().
    private SwrveOrientation orientation = SwrveOrientation.Both;
    private boolean autoDownloadCampaignsAndResources = true;
    private int minSampleSize = 1; // Sample size to use when loading in-app message images. Has to be a power of two.
    private File cacheDir;
    private boolean sendQueuedEventsOnResume = true;
    private int httpTimeout = 60000; // HTTP timeout used when contacting the Swrve APIs, in milliseconds.
    private boolean androidIdLoggingEnabled; // Automatically log Android ID as "swrve.android_id".
    private boolean abTestDetailsEnabled; // Obtain information about the AB Tests a user is part of.
    private List<String> modelBlackList;
    private boolean loggingEnabled = true;
    private SwrveNotificationConfig notificationConfig; // null as default, but attempts to populate from manifest if not instantiated.
    private SwrvePushNotificationListener notificationListener;
    private SwrveSilentPushListener silentPushListener;
    private SwrveInAppMessageConfig inAppMessageConfig = new SwrveInAppMessageConfig.Builder().build(); // All default values set in the init
    private SwrveEmbeddedMessageConfig embeddedMessageConfig = null;
    private SwrveInitMode initMode = SwrveInitMode.AUTO;
    private boolean autoStartLastUser = true;
    private SwrveSSLSocketFactoryConfig sslSocketFactoryConfig = null;

    public SwrveSSLSocketFactoryConfig getSSlSocketFactoryConfig() {
        return sslSocketFactoryConfig;
    }

    public void setSSlSocketFactoryConfig(SwrveSSLSocketFactoryConfig sslSocketFactoryConfig) {
        this.sslSocketFactoryConfig = sslSocketFactoryConfig;
    }

    /**
     * Create an instance of the SDK advance preferences.
     */
    public SwrveConfigBase() {
        modelBlackList = new ArrayList<>();
        modelBlackList.add("Calypso AppCrawler");
    }

    /**
     * @return Whether campaigns and resources will automatically be downloaded.
     */
    public boolean isAutoDownloadCampaignsAndResources() {
        return this.autoDownloadCampaignsAndResources;
    }

    /**
     * Download resources and in-app campaigns automatically.
     *
     * @param autoDownload Automatically download campaigns and resources.
     * @return the config object
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
     * Set the orientation to limit in-app messages.
     *
     * @param orientation Orientation supported by the application.
     * @return the config object
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
     * @return the config object
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
     * Get the selected Stack
     * @return The selected stack
     */
    public SwrveStack getSelectedStack() {
        return selectedStack;
    }

    /**
     * Set the stack prefix for the events and content url
     * @param stack The chosen stack of the app
     */
    public void setSelectedStack(SwrveStack stack){
        selectedStack = stack;
    }

    /*
     * Get the stack prefix for the events and content url
     */
    private String getStackHostPrefix(){
        return (getSelectedStack() == SwrveStack.EU) ? "eu-" : "";
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
     * @return the config object
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
     * @return the config object
     */
    public SwrveConfigBase setMaxEventsPerFlush(int maxEventsPerFlush) {
        this.maxEventsPerFlush = maxEventsPerFlush;
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
     * @return the config object
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
     * @return the config object
     */
    public SwrveConfigBase setEventsUrl(URL eventsUrl) {
        this.eventsUrl = eventsUrl;
        return this;
    }

    /**
     * Set to override the default location of the server user to identify user id
     * If your company has a special API end-point enabled, then you should specify it here.
     * You should only need to change this value if you are working with Swrve support on a specific support issue.
     *
     * @param identityUrl Custom location of the identity server.
     * @return the config object
     */
    public SwrveConfigBase setIdentityUrl(URL identityUrl) {
        this.identityUrl = identityUrl;
        return this;
    }

    /**
     * @return Location of the identity server.
     */
    public URL getIdentityUrl() {
        if (identityUrl == null)
            return defaultIdentityUrl;
        return identityUrl;
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
     * @return the config object
     */
    public SwrveConfigBase setContentUrl(URL contentUrl) {
        this.contentUrl = contentUrl;
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
     * @return the config object
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
     * @return the config object
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
     * @return the config object
     */
    public SwrveConfigBase setAppStore(String appStore) {
        this.appStore = appStore;
        return this;
    }

    /**
     * Generate default endpoints with the given app id. Used internally.
     * @param appId application id
     *
     * @throws MalformedURLException If theres an error
     */
    public void generateUrls(int appId) throws MalformedURLException {
        // If the prefix is non empty, prepend it with a .
        String prefix = getStackHostPrefix();
        // Build the URL
        defaultEventsUrl = new URL("https://" + appId + "." + prefix + "api.swrve.com");
        defaultContentUrl = new URL("https://" + appId + "." + prefix + "content.swrve.com");
        defaultIdentityUrl = new URL("https://" + appId + "." + prefix + "identity.swrve.com");
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
     * Cache folder used to save the campaign images.
     * The default is Context.getCacheDir(). If permission to cacheDir is denied, then default is used.
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
     * @param sendQueuedEventsOnResume true to send queued event upon resume
     */
    public void setSendQueuedEventsOnResume(boolean sendQueuedEventsOnResume) {
        this.sendQueuedEventsOnResume = sendQueuedEventsOnResume;
    }

    /**
     * Set the HTTP timeout.
     *
     * @param httpTimeout the http timeout for rest calls
     */
    public void setHttpTimeout(int httpTimeout) {
        this.httpTimeout = httpTimeout;
    }

    /**
     * The HTTP timeout used in the Swrve API calls.
     *
     * @return The timeout used in the HTTP calls
     */
    public int getHttpTimeout() {
        return httpTimeout;
    }

    /**
     * @return if it will automatically log Android ID as "swrve.android_id".
     */
    public boolean isAndroidIdLoggingEnabled() {
        return androidIdLoggingEnabled;
    }

    /**
     * @param enabled to enable automatic logging of Android ID "swrve.android_id".
     */
    public void setAndroidIdLoggingEnabled(boolean enabled) {
        this.androidIdLoggingEnabled = enabled;
    }

    /**
     * @return if the SDK will request information about the AB Tests a user is part of.
     */
    public boolean isABTestDetailsEnabled() {
        return abTestDetailsEnabled;
    }

    /**
     * @param enabled to obtain information about the AB Tests a user is part of.
     */
    public void setABTestDetailsEnabled(boolean enabled) {
        this.abTestDetailsEnabled = enabled;
    }

    /**
     * @return list of Android models where the SDK won't run, for example the 'Calypso AppCrawler'.
     */
    public List<String> getModelBlackList() {
        return modelBlackList;
    }

    /**
     * @param modelBlackList list of Android models where the SDK won't run, for example 'Calypso AppCrawler'.
     */
    public void setModelBlackList(List<String> modelBlackList) {
        this.modelBlackList = modelBlackList;
    }

    /**
     * @return if the SDK will log information to the console.
     */
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * @param enabled to control whether the SDK will log information to the console.
     */
    public void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
    }

    /**
     * @return The custom SwrveNotificationConfig if one is set. If null, configuration from manifest will be attempted.
     */
    public SwrveNotificationConfig getNotificationConfig() {
        return notificationConfig;
    }

    /**
     * @param notificationConfig Set custom notification configurations
     */
    public void setNotificationConfig(SwrveNotificationConfig notificationConfig) {
        this.notificationConfig = notificationConfig;
    }

    /**
     * @return The custom notification listener.
     */
    public SwrvePushNotificationListener getNotificationListener() {
        return notificationListener;
    }

    /**
     * @param notificationListener Set custom notification listener to be executed when a notification is fired.
     */
    public void setNotificationListener(SwrvePushNotificationListener notificationListener) {
        this.notificationListener = notificationListener;
    }

    /**
     * @return The custom silent push listener.
     */
    public SwrveSilentPushListener getSilentPushListener() {
        return silentPushListener;
    }

    /**
     * @param silentPushListener Set custom silent push listener to be executed when a silent push is received.
     */
    public void setSilentPushListener(SwrveSilentPushListener silentPushListener) {
        this.silentPushListener = silentPushListener;
    }

    /**
     * Set the SwrveInitMode to MANAGED to delay starting the sdk until start api is called. Default mode is AUTO which automatically
     * starts when UI is shown.
     * @param initMode change to managed to control when the SDK starts and with what user.
     */
    public void setInitMode(SwrveInitMode initMode) {
        this.initMode = initMode;
    }

    /**
     * @return The init mode.
     */
    public SwrveInitMode getInitMode() {
        return initMode;
    }

    /**
     * This configuration can only be used in initMode MANAGED.
     *
     * @param autoStartLastUser If true, the sdk will delay starting until the start api is
     *                          called and the userId is set. Once set, it will autostart
     *                          when UI is shown. Set to false to force the sdk to always
     *                          delay tracking until a start api is called.
     */
    public void setAutoStartLastUser(boolean autoStartLastUser) {
        this.autoStartLastUser = autoStartLastUser;
    }

    /**
     * @return the current value of autoStartLastUser
     */
    public boolean isAutoStartLastUser() {
        return autoStartLastUser;
    }

    /**
     * The configuration file for InAppMessages.
     *
     * @return the inApp config object
     */
    public SwrveInAppMessageConfig getInAppMessageConfig() {
        return inAppMessageConfig;
    }

    /**
     * The configuration file for InAppMessages.
     *
     * @param inAppMessageConfig object with in app configuration
     */
    public void setInAppMessageConfig(SwrveInAppMessageConfig inAppMessageConfig) {
        this.inAppMessageConfig = inAppMessageConfig;
    }

    /**
     * The configuration file for EmbeddedMessages.
     *
     * @return the embeddedMessage config object
     */
    public SwrveEmbeddedMessageConfig getEmbeddedMessageConfig() {
        return embeddedMessageConfig;
    }

    /**
     * The configuration file for EmbeddedMessages.
     *
     * @param embeddedMessageConfig object with in app configuration
     */
    public void setEmbeddedMessageConfig(SwrveEmbeddedMessageConfig embeddedMessageConfig) {
        this.embeddedMessageConfig = embeddedMessageConfig;
    }
}
