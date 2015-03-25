package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.ISwrveCustomButtonListener;
import com.swrve.sdk.messaging.ISwrveDialogListener;
import com.swrve.sdk.messaging.ISwrveInstallButtonListener;
import com.swrve.sdk.messaging.ISwrveMessageListener;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Empty implementation of the Swrve SDK. Will be returned when the SDK is used from an unsupported runtime version (< 2.3.3).
 */
public class SwrveBaseEmpty<T, C extends SwrveConfigBase> implements ISwrveBase<T, C> {

    private WeakReference<Context> context;
    private C config;
    private ISwrveCustomButtonListener customButtonListener;
    private ISwrveInstallButtonListener installButtonListener;
    private ISwrveDialogListener dialogListener;
    private String language = "en-US";
    private String apiKey;
    private String userId;
    private File cacheDir;

    @SuppressWarnings("unchecked")
    @Override
    public T onCreate(Activity activity, int appId, String apiKey, C config) throws IllegalArgumentException {
        this.context = new WeakReference<Context>(activity);
        this.apiKey = apiKey;
        this.config = config;
        this.language = config.getLanguage();
        this.userId = config.getUserId();
        cacheDir = config.getCacheDir();
        if (cacheDir == null) {
            cacheDir = activity.getCacheDir();
        }

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T onCreate(Activity activity, int appId, String apiKey) throws IllegalArgumentException {
        return onCreate(activity, appId, apiKey, (C) new SwrveConfigBaseImp());
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
    public void setResourcesListener(ISwrveResourcesListener resourcesListener) {
    }

    @Override
    public void getUserResources(ISwrveUserResourcesListener listener) {
        if (listener != null) {
            listener.onUserResourcesSuccess(new HashMap<String, Map<String, String>>(), null);
        }
    }

    @Override
    public void getUserResourcesDiff(ISwrveUserResourcesDiffListener listener) {
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
    public void onPause() {
    }

    @Override
    public void onResume(Activity ctx) {
    }

    @Override
    public void onDestroy(Activity ctx) {
    }

    @Override
    public void onLowMemory() {
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
    @Deprecated
    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String getApiKey() {
        return this.apiKey;
    }

    @Override
    public String getUserId() {
        if (SwrveHelper.isNullOrEmpty(userId)) {
            return "unsupported_version";
        }
        return userId;
    }

    @Override
    public JSONObject getDeviceInfo() throws JSONException {
        return new JSONObject();
    }

    @Override
    public void refreshCampaignsAndResources() {
    }

    @Override
    public SwrveMessage getMessageForEvent(String event) {
        return null;
    }

    @Override
    public SwrveMessage getMessageForId(int messageId) {
        return null;
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
    public void setMessageListener(ISwrveMessageListener messageListener) {
    }

    @Override
    public Date getInitialisedTime() {
        return new Date();
    }

    @Override
    public Context getContext() {
        return context.get();
    }

    @Override
    public C getConfig() {
        return config;
    }

    @Override
    public ISwrveCustomButtonListener getCustomButtonListener() {
        return this.customButtonListener;
    }

    @Override
    public void setCustomButtonListener(ISwrveCustomButtonListener customButtonListener) {
        this.customButtonListener = customButtonListener;
    }

    @Override
    public ISwrveInstallButtonListener getInstallButtonListener() {
        return installButtonListener;
    }

    @Override
    public void setInstallButtonListener(ISwrveInstallButtonListener installButtonListener) {
        this.installButtonListener = installButtonListener;
    }

    @Override
    public ISwrveDialogListener getDialogListener() {
        return dialogListener;
    }

    @Override
    public void setDialogListener(ISwrveDialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }

    @Override
    public void shutdown() {
    }

    private class SwrveConfigBaseImp extends SwrveConfigBase {
    }
}
