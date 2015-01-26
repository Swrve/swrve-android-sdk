package com.swrve.sdk;


import android.app.Activity;
import android.content.Context;
import android.util.Log;

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
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public abstract class SwrveSDKBase {

    // TODO move all javadoc comments from ISwrveBase to here

    protected static ISwrveBase instance;

    public static void onCreate(final Activity activity) {
        checkInstanceCreated();
        instance.onCreate(activity);
    }

    public static void sessionStart() {
        checkInstanceCreated();
        instance.sessionStart();
    }

    public static void sessionEnd() {
        checkInstanceCreated();
        instance.sessionEnd();
    }

    public static void event(String name) {
        checkInstanceCreated();
        instance.event(name);
    }

    public static void event(String name, Map<String, String> payload) {
        checkInstanceCreated();
        instance.event(name, payload);
    }

    public static void purchase(String item, String currency, int cost, int quantity) {
        checkInstanceCreated();
        instance.purchase(item, currency, cost, quantity);
    }

    public static void currencyGiven(String givenCurrency, double givenAmount) {
        checkInstanceCreated();
        instance.currencyGiven(givenCurrency, givenAmount);
    }

    public static void userUpdate(Map<String, String> attributes) {
        checkInstanceCreated();
        instance.userUpdate(attributes);
    }

    public static void iap(int quantity, String productId, double productPrice, String currency) {
        checkInstanceCreated();
        instance.iap(quantity, productId, productPrice, currency);
    }

    public static void iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards) {
        checkInstanceCreated();
        instance.iap(quantity, productId, productPrice, currency, rewards);
    }

    public static SwrveResourceManager getResourceManager() {
        checkInstanceCreated();
        return instance.getResourceManager();
    }

    public static void setResourcesListener(ISwrveResourcesListener resourcesListener) {
        checkInstanceCreated();
        instance.setResourcesListener(resourcesListener);
    }

    public static void getUserResources(final ISwrveUserResourcesListener listener) {
        checkInstanceCreated();
        instance.getUserResources(listener);
    }

    public static void getUserResourcesDiff(final ISwrveUserResourcesDiffListener listener) {
        checkInstanceCreated();
        instance.getUserResourcesDiff(listener);
    }

    public static void sendQueuedEvents() {
        checkInstanceCreated();
        instance.sendQueuedEvents();
    }

    public static void flushToDisk() {
        checkInstanceCreated();
        instance.flushToDisk();
    }

    public static void onPause() {
        checkInstanceCreated();
        instance.onPause();
    }

    public static void onResume() {
        checkInstanceCreated();
        instance.onResume();
    }

    public static void onLowMemory() {
        checkInstanceCreated();
        instance.onLowMemory();
    }

    public static void onDestroy() {
        checkInstanceCreated();
        instance.onDestroy();
    }

    public static void shutdown() {
        checkInstanceCreated();
        instance.shutdown();
    }

    public static void setLanguage(Locale locale) {
        checkInstanceCreated();
        instance.setLanguage(locale);
    }

    public static String getLanguage() {
        checkInstanceCreated();
        return instance.getLanguage();
    }

    public static void setLanguage(String language) {
        checkInstanceCreated();
        instance.setLanguage(language);
    }

    public static void getApiKey() {
        checkInstanceCreated();
        instance.getApiKey();
    }

    public static void getUserId() {
        checkInstanceCreated();
        instance.getUserId();
    }

    public static JSONObject getDeviceInfo() throws JSONException {
        checkInstanceCreated();
        return instance.getDeviceInfo();
    }

    public static void refreshCampaignsAndResources() {
        checkInstanceCreated();
        instance.refreshCampaignsAndResources();
    }

    public static SwrveMessage getMessageForEvent(String event) {
        checkInstanceCreated();
        return instance.getMessageForEvent(event);
    }

    public static SwrveMessage getMessageForId(int messageId) {
        checkInstanceCreated();
        return instance.getMessageForId(messageId);
    }

    public static void buttonWasPressedByUser(SwrveButton button) {
        checkInstanceCreated();
        instance.buttonWasPressedByUser(button);
    }

    public static void messageWasShownToUser(SwrveMessageFormat messageFormat) {
        checkInstanceCreated();
        instance.messageWasShownToUser(messageFormat);
    }

    public static String getAppStoreURLForApp(int appId) {
        checkInstanceCreated();
        return instance.getAppStoreURLForApp(appId);
    }

    public static File getCacheDir() {
        checkInstanceCreated();
        return instance.getCacheDir();
    }

    public static void setMessageListener(ISwrveMessageListener messageListener) {
        checkInstanceCreated();
        instance.setMessageListener(messageListener);
    }

    public static Date getInitialisedTime() {
        checkInstanceCreated();
        return instance.getInitialisedTime();
    }

    public static ISwrveInstallButtonListener getInstallButtonListener() {
        checkInstanceCreated();
        return instance.getInstallButtonListener();
    }

    public static void setInstallButtonListener(ISwrveInstallButtonListener installButtonListener) {
        checkInstanceCreated();
        instance.setInstallButtonListener(installButtonListener);
    }

    public static ISwrveCustomButtonListener getCustomButtonListener() {
        checkInstanceCreated();
        return instance.getCustomButtonListener();
    }

    public static void setCustomButtonListener(ISwrveCustomButtonListener customButtonListener) {
        checkInstanceCreated();
        instance.setCustomButtonListener(customButtonListener);
    }

    public static ISwrveDialogListener getDialogListener() {
        checkInstanceCreated();
        return instance.getDialogListener();
    }

    public static void setDialogListener(ISwrveDialogListener dialogListener) {
        checkInstanceCreated();
        instance.setDialogListener(dialogListener);
    }

    public static Context getContext() {
        checkInstanceCreated();
        return instance.getContext();
    }

    protected static void checkInstanceCreated() throws RuntimeException {
        if (instance == null) {
            Log.e(SwrveImp.LOG_TAG, "Please call SwrveSDK.createInstance first in your Application class.");
            throw new RuntimeException("Please call SwrveSDK.createInstance first in your Application class.");
        }
    }

    public static boolean sdkAvailable() {
        // Returns true if current SDK is higher or equal than 2.3.3 (API 10)
        return (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1);
    }
}
