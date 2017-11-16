package com.swrve.sdk;

import android.app.NotificationChannel;
import android.content.Context;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessageListener;
import com.swrve.sdk.messaging.SwrveOrientation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Empty implementation of the Swrve SDK. Will be returned when the SDK is used from an unsupported runtime version.
 */
public class SwrveBaseEmpty<T, C extends SwrveConfigBase> implements ISwrveBase<T, C>, ISwrveCommon {

    protected WeakReference<Context> context;
    protected String apiKey;

    private C config;
    private SwrveCustomButtonListener customButtonListener;
    private SwrveInstallButtonListener installButtonListener;
    private String language = "en-US";
    private String userId;
    private File cacheDir;

    protected SwrveBaseEmpty(Context context, String apiKey) {
        super();
        this.context = new WeakReference<>(context.getApplicationContext());
        this.apiKey = apiKey;
        this.config = (C) new SwrveConfigBaseImp();
        SwrveCommon.setSwrveCommon(this);
        this.language = config.getLanguage();
        this.userId = config.getUserId();
        cacheDir = config.getCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }
    }

    @Override
    public void sessionStart() {
    }

    @Override
    public void sessionEnd() {
    }

    @Override
    public void event(String name) {
    }

    @Override
    public void event(String name, Map<String, String> payload) {
    }

    @Override
    public void purchase(String item, String currency, int cost, int quantity) {
    }

    @Override
    public void currencyGiven(String givenCurrency, double givenAmount) {
    }

    @Override
    public void userUpdate(Map<String, String> attributes) {
    }

    @Override
    public void sendEventsInBackground(Context context, String userId, ArrayList<String> events) {
    }

    @Override
    public String getEventsServer() {
        return null;
    }

    @Override
    public int getHttpTimeout() {
        return 0;
    }

    @Override
    public int getMaxEventsPerFlush() {
        return 0;
    }

    @Override
    public void userUpdate(String name, Date date) {
    }

    @Override
    public void iap(int quantity, String productId, double productPrice, String currency) {
    }

    @Override
    public void iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards) {
    }

    @Override
    public SwrveResourceManager getResourceManager() {
        return new SwrveResourceManager();
    }

    @Override
    public void setResourcesListener(SwrveResourcesListener resourcesListener) {
    }

    @Override
    public void getUserResources(SwrveUserResourcesListener listener) {
        if (listener != null) {
            listener.onUserResourcesSuccess(new HashMap<String, Map<String, String>>(), null);
        }
    }

    @Override
    public void getUserResourcesDiff(SwrveUserResourcesDiffListener listener) {
        if (listener != null) {
            listener.onUserResourcesDiffSuccess(new HashMap<String, Map<String, String>>(), new HashMap<String, Map<String, String>>(), null);
        }
    }

    @Override
    public void sendQueuedEvents() {
    }

    @Override
    public void flushToDisk() {
    }

    @Override
    public void setLanguage(Locale locale) {
        this.language = SwrveHelper.toLanguageTag(locale);
    }

    @Override
    public String getLanguage() {
        return this.language;
    }

    @Override
    public String getApiKey() {
        return this.apiKey;
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public short getDeviceId() {
        return 0;
    }

    @Override
    public int getAppId() {
        return 0;
    }

    @Override
    public String getUserId() {
        if (SwrveHelper.isNullOrEmpty(userId)) {
            return "unsupported_version";
        }
        return userId;
    }

    @Override
    public String getAppVersion() {
        return null;
    }

    @Override
    public String getUniqueKey(String userId) {
        return null;
    }

    @Override
    public String getBatchURL() {
        return null;
    }

    @Override
    public String getCachedData(String userId, String key) {
        return null;
    }

    @Override
    public void setLocationSegmentVersion(int locationSegmentVersion) {

    }

    @Override
    public String getSwrveSDKVersion() {
        return SwrveBase.getVersion();
    }

    @Override
    public JSONObject getDeviceInfo() throws JSONException {
        return new JSONObject();
    }

    @Override
    public int getNextSequenceNumber() {
        return 0;
    }

    @Override
    public NotificationChannel getDefaultNotificationChannel() {
        return config.getDefaultNotificationChannel();
    }

    @Override
    public void refreshCampaignsAndResources() {
    }

    @Override
    public void buttonWasPressedByUser(SwrveButton button) {
    }

    @Override
    public void messageWasShownToUser(SwrveMessageFormat messageFormat) {
    }

    @Override
    public String getAppStoreURLForApp(int appId) {
        return null;
    }

    @Override
    public File getCacheDir() {
        return cacheDir;
    }

    @Override
    public void setMessageListener(SwrveMessageListener messageListener) {
    }

    @Override
    public Date getInitialisedTime() {
        return new Date();
    }

    @Override
    public C getConfig() {
        return config;
    }

    @Override
    public List<SwrveBaseCampaign> getMessageCenterCampaigns() {
        return new ArrayList<>();
    }

    @Override
    public List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation) {
        return new ArrayList<>();
    }

    @Override
    public boolean showMessageCenterCampaign(SwrveBaseCampaign campaign) {
        return false;
    }

    @Override
    public void removeMessageCenterCampaign(SwrveBaseCampaign campaign) {
    }

    @Override
    public SwrveCustomButtonListener getCustomButtonListener() {
        return this.customButtonListener;
    }

    @Override
    public void setCustomButtonListener(SwrveCustomButtonListener customButtonListener) {
        this.customButtonListener = customButtonListener;
    }

    @Override
    public SwrveInstallButtonListener getInstallButtonListener() {
        return installButtonListener;
    }

    @Override
    public void setInstallButtonListener(SwrveInstallButtonListener installButtonListener) {
        this.installButtonListener = installButtonListener;
    }

    @Override
    public void shutdown() {
    }

    private class SwrveConfigBaseImp extends SwrveConfigBase {
    }
}
