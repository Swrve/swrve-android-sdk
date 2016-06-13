package com.swrve.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.swrve.sdk.messaging.ISwrveCustomButtonListener;
import com.swrve.sdk.messaging.ISwrveDialogListener;
import com.swrve.sdk.messaging.ISwrveInstallButtonListener;
import com.swrve.sdk.messaging.ISwrveMessageListener;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveOrientation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class SwrveSDKBase {

    protected static ISwrveBase instance;

    /**
     * Typically this function is called in your main activity's onCreate function.
     *
     * @param activity your activity
     */
    public static void onCreate(final Activity activity) {
        checkInstanceCreated();
        instance.onCreate(activity);
    }

    /**
     * Add a Swrve.session.start event to the event queue. This event should typically be added in
     * your main activity's onStart method.
     */
    public static void sessionStart() {
        checkInstanceCreated();
        instance.sessionStart();
    }

    /**
     * Add a Swrve.session.end event to the event queue. This event should typically be added in
     * your main activity's onStop method.
     */
    public static void sessionEnd() {
        checkInstanceCreated();
        instance.sessionEnd();
    }

    /**
     * Add a generic named event to the event queue.
     *
     * @param name the name of the event in question. The character '.' is used as grouping syntax.
     */
    public static void event(String name) {
        checkInstanceCreated();
        instance.event(name);
    }

    /**
     * Add a generic named event to the event queue.
     *
     * @param name the name of the event in question. The character '.' is used as grouping syntax.
     * @param payload a dictionary of key-value pairs to be supplied with this named event. Typically
     *                this would be information about the event rather than about the user. Compare
     *                with the userUpdate function for properties of the user.
     */
    public static void event(String name, Map<String, String> payload) {
        checkInstanceCreated();
        instance.event(name, payload);
    }

    /**
     * Add a Swrve.user_purchase event to the event queue. This event should be added on virtual
     * goods purchase in your app.
     *
     * @param item     unique name of the purchased item
     * @param currency currency in Swrve to be used for this purchase. This currency must be
     *                 declared in the Swrve dashboard before calling this function. If this
     *                 currency is not declared this event will be rejected.
     * @param cost     cost of the item in units of 'currency'
     * @param quantity number of the item purchased
     */
    public static void purchase(String item, String currency, int cost, int quantity) {
        checkInstanceCreated();
        instance.purchase(item, currency, cost, quantity);
    }

    /**
     * Add a Swrve.currency_given event to the event queue. This event should be
     * added on award of virtual currency in the app.
     *
     * @param givenCurrency currency in Swrve to be used for this gift. This currency must
     *                      be declared in the Swrve dashboard before calling this
     *                      function. If this currency is not declared this event will be
     *                      rejected.
     * @param givenAmount   amount of currency given to the user.
     */
    public static void currencyGiven(String givenCurrency, double givenAmount) {
        checkInstanceCreated();
        instance.currencyGiven(givenCurrency, givenAmount);
    }

    /**
     * Add a Swrve.user event to the event queue. This event would typically be
     * added to the queue after session_start and at points where properties of
     * your users change - for example, levelUp.
     *
     * @param attributes key-value pairs of properties of the user. Typical values
     *                   would be level => number, referrer => channel, coin balance =>
     *                   number.
     */
    public static void userUpdate(Map<String, String> attributes) {
        checkInstanceCreated();
        instance.userUpdate(attributes);
    }

    /**
     * Add a Swrve.iap event to the event queue. This event should be added for
     * unvalidated real money transactions where a single item was purchased.
     * (i.e where no in-app currency or bundle was purchased)
     *
     * @param quantity     Quantity purchased. Must be greater than zero.
     * @param productId    Unique product identifier for the item bought. This should
     *                     match the Swrve resource name. Required, cannot be empty.
     * @param productPrice The price (in real money) of the product that was purchased.
     *                     Note: this is not the price of the total transaction, but the
     *                     per-product price. Must be greater than or equal to zero.
     * @param currency     real world currency used for this transaction. This must be an
     *                     ISO currency code. A typical value would be "USD". Required,
     *                     cannot be empty.
     */
    public static void iap(int quantity, String productId, double productPrice, String currency) {
        checkInstanceCreated();
        instance.iap(quantity, productId, productPrice, currency);
    }

    /**
     * Add a Swrve.iap event to the event queue. This event should be added for
     * unvalidated real money transactions where in-app currency was purchased
     * or where multiple items and/or currencies were purchased.
     * <p/>
     * To create the rewards object, create an instance of SwrveIAPRewards and
     * use addItem() and addCurrency() to add the individual rewards
     *
     * @param quantity     Quantity purchased. Must be greater than zero.
     * @param productId    Unique product identifier for the item bought. This should
     *                     match the Swrve resource name. Required, cannot be empty.
     * @param productPrice price of the product in real money. Note that this is the
     *                     price per product, not the total price of the transaction
     *                     (when quantity > 1) A typical value would be 0.99. Must be
     *                     greater than or equal to zero.
     * @param currency     real world currency used for this transaction. This must be an
     *                     ISO currency code. A typical value would be "USD". Required,
     *                     cannot be empty.
     * @param rewards      SwrveIAPRewards object containing any in-app currency and/or
     *                     additional items included in this purchase that need to be
     *                     recorded.
     */
    public static void iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards) {
        checkInstanceCreated();
        instance.iap(quantity, productId, productPrice, currency, rewards);
    }

    /**
     * Get the SwrveResourceManager, which can be queried for up-to-date resource attribute values
     */
    public static SwrveResourceManager getResourceManager() {
        checkInstanceCreated();
        return instance.getResourceManager();
    }

    /**
     * The resourcesListener onResourcesUpdated() method is invoked when user resources in the SwrveResourceManager
     * have been initially loaded and each time user resources are updated.
     */
    public static void setResourcesListener(ISwrveResourcesListener resourcesListener) {
        checkInstanceCreated();
        instance.setResourcesListener(resourcesListener);
    }

    /**
     * Request the list of resources for the user with full attribute data after
     * any applicable AB Tests have been applied. This request is executed on a
     * background thread, which will call methods on the user-provided listener
     * parameter.
     * <p/>
     * If no user id has been specified this function raises a
     * NoUserIdSwrveException exception to the listener object.
     *
     * @param listener
     */
    public static void getUserResources(final ISwrveUserResourcesListener listener) {
        checkInstanceCreated();
        instance.getUserResources(listener);
    }

    /**
     * Request all applicable AB-Tested resources for the user. This request is
     * executed on a background thread, which will call methods on the
     * user-provided listener parameter.
     * <p/>
     * If no user id has been specified this function raises a
     * NoUserIdSwrveException exception to the listener object.
     *
     * @param listener
     */
    public static void getUserResourcesDiff(final ISwrveUserResourcesDiffListener listener) {
        checkInstanceCreated();
        instance.getUserResourcesDiff(listener);
    }

    /**
     * Send events to Swrve servers.
     */
    public static void sendQueuedEvents() {
        checkInstanceCreated();
        instance.sendQueuedEvents();
    }

    /**
     * Flush events and others to the device's disk.
     */
    public static void flushToDisk() {
        checkInstanceCreated();
        instance.flushToDisk();
    }

    /**
     * Default SDK behavior for activity onPause(). Flush data to disk.
     * Notify the SDK that the binded activity may be finishing.
     */
    public static void onPause() {
        checkInstanceCreated();
        instance.onPause();
    }

    /**
     * Default SDK behavior for activity onResume(). Send events to Swrve.
     *
     * @param activity Activity that called this method
     */
    public static void onResume(Activity activity) {
        checkInstanceCreated();
        instance.onResume(activity);
    }

    /**
     * Notify that the app is low on memory.
     */
    public static void onLowMemory() {
        checkInstanceCreated();
        instance.onLowMemory();
    }

    /**
     * Notify the SDK that a new intent has been received.
     *
     * @param intent The intent received
     */
    public static void onNewIntent(Intent intent) {
        checkInstanceCreated();
        instance.onNewIntent(intent);
    }

    /**
     * Notify that the app has closed.
     *
     * @param activity Activity that called this method
     */
    public static void onDestroy(Activity activity) {
        checkInstanceCreated();
        instance.onDestroy(activity);
    }

    /**
     * Shutdown the SDK. This instance will be unusable after shutdown.
     * <p/>
     * Note: All the background jobs will try to stop when this happens.
     */
    public static void shutdown() {
        checkInstanceCreated();
        instance.shutdown();
    }

    /**
     * Set the current language
     */
    public static void setLanguage(Locale locale) {
        checkInstanceCreated();
        instance.setLanguage(locale);
    }

    /**
     * Get the current language
     */
    public static String getLanguage() {
        checkInstanceCreated();
        return instance.getLanguage();
    }

    /**
     * Set the current language
     *
     * @deprecated use {@link #setLanguage(Locale)} instead
     */
    @Deprecated
    public static void setLanguage(String language) {
        checkInstanceCreated();
        instance.setLanguage(language);
    }

    /**
     * Get the current api key
     */
    public static String getApiKey() {
        checkInstanceCreated();
        return instance.getApiKey();
    }

    /**
     * Get the current user id
     */
    public static String getUserId() {
        checkInstanceCreated();
        return instance.getUserId();
    }

    /**
     * Collect device information
     *
     * @throws JSONException
     */
    public static JSONObject getDeviceInfo() throws JSONException {
        checkInstanceCreated();
        return instance.getDeviceInfo();
    }

    /**
     * Update campaign and resources values
     * This function will be called automatically to keep campaigns and resources up-to-date.
     * You should only call this function manually if you have changed the value of
     * config.autoDownloadCampaignsAndResources to false.
     */
    public static void refreshCampaignsAndResources() {
        checkInstanceCreated();
        instance.refreshCampaignsAndResources();
    }

    /**
     * Returns a message for a given trigger event. There may be messages for the trigger but the
     * rules avoid a message from being displayed at some point.
     *
     * @param event trigger event
     * @return SwrveMessage supported message from a campaign set up for the
     * given trigger
     */
    @Deprecated
    public static SwrveMessage getMessageForEvent(String event) {
        checkInstanceCreated();
        return instance.getMessageForEvent(event);
    }

    /**
     * Returns a message for a given id. This function should be used for retrieving a known
     * message that is being displayed when the device's orientation changes.
     *
     * @param messageId id of the message
     * @return SwrveMessage message with the given id
     */
    @Deprecated
    public static SwrveMessage getMessageForId(int messageId) {
        checkInstanceCreated();
        return instance.getMessageForId(messageId);
    }

    /**
     * Process a message button event. This function should be called by your
     * implementation of the message renderer to inform Swrve of a button event.
     *
     * @param button button that was pressed.
     */
    public static void buttonWasPressedByUser(SwrveButton button) {
        checkInstanceCreated();
        instance.buttonWasPressedByUser(button);
    }

    /**
     * Inform that a message has been shown. This function should be called by your implementation
     * of the message renderer to update the campaign information and send the appropriate data to
     * Swrve.
     *
     * @param messageFormat message that was shown to the user for the first time in this session.
     */
    public static void messageWasShownToUser(SwrveMessageFormat messageFormat) {
        checkInstanceCreated();
        instance.messageWasShownToUser(messageFormat);
    }

    /**
     * Get app store link configured in the dashboard for a given app id.
     *
     * @param appId id of the app
     * @return String App store link for the app
     */
    public static String getAppStoreURLForApp(int appId) {
        checkInstanceCreated();
        return instance.getAppStoreURLForApp(appId);
    }

    /**
     * Get location of the chosen cache folder where the resources will be  downloaded.
     *
     * @return File path to the choosen cache folder
     */
    public static File getCacheDir() {
        checkInstanceCreated();
        return instance.getCacheDir();
    }

    /**
     * Set a message listener to process Talk messages.
     *
     * @param messageListener logic to listen for messages
     */
    public static void setMessageListener(ISwrveMessageListener messageListener) {
        checkInstanceCreated();
        instance.setMessageListener(messageListener);
    }

    /**
     * Get the time when the SDK was initialized.
     *
     * @return the time the SDK was initialized.
     */
    public static Date getInitialisedTime() {
        checkInstanceCreated();
        return instance.getInitialisedTime();
    }

    /**
     * Get the custom listener to process Talk message install button clicks
     *
     * @return buttonListener
     */
    public static ISwrveInstallButtonListener getInstallButtonListener() {
        checkInstanceCreated();
        return instance.getInstallButtonListener();
    }

    /**
     * Set the custom listener to process Talk message install button clicks
     *
     * @param installButtonListener
     */
    public static void setInstallButtonListener(ISwrveInstallButtonListener installButtonListener) {
        checkInstanceCreated();
        instance.setInstallButtonListener(installButtonListener);
    }

    /**
     * Get the custom listener to process Talk message custom button clicks
     *
     * @return the custom buttonListener
     */
    public static ISwrveCustomButtonListener getCustomButtonListener() {
        checkInstanceCreated();
        return instance.getCustomButtonListener();
    }

    /**
     * Set the custom listener to process Talk message custom button clicks
     *
     * @param customButtonListener
     */
    public static void setCustomButtonListener(ISwrveCustomButtonListener customButtonListener) {
        checkInstanceCreated();
        instance.setCustomButtonListener(customButtonListener);
    }

    /**
     * Get the custom dialog listener for Talk message dialogs
     *
     * @return buttonListener
     */
    public static ISwrveDialogListener getDialogListener() {
        checkInstanceCreated();
        return instance.getDialogListener();
    }

    /**
     * Set the custom dialog listener for Talk message dialogs
     *
     * @param dialogListener
     */
    public static void setDialogListener(ISwrveDialogListener dialogListener) {
        checkInstanceCreated();
        instance.setDialogListener(dialogListener);
    }

    /**
     * Get the context where the SDK is attached.
     *
     * @return activity or application context
     */
    public static Context getContext() {
        checkInstanceCreated();
        return instance.getContext();
    }

    /**
     * Get the list active MessageCenter campaigns targeted for this user.
     * It will exclude campaigns that have been deleted with the
     * removeMessageCenterCampaign method and those that do not support the current orientation.
     * <p>
     * To obtain all MessageCenter campaigns independent of their orientation support
     * use the getMessageCenterCampaigns(SwrveOrientation.Both) method.
     *
     * @return list of active MessageCenter campaigns.
     */
    public static List<SwrveBaseCampaign> getMessageCenterCampaigns() {
        checkInstanceCreated();
        return instance.getMessageCenterCampaigns();
    }

    /**
     * Get the list active MessageCenter campaigns targeted for this user.
     * It will exclude campaigns that have been deleted with the
     * removeMessageCenterCampaign method and those that do not support the given orientation.
     *
     * @return list of active MessageCenter campaigns.
     */
    public static List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation) {
        checkInstanceCreated();
        return instance.getMessageCenterCampaigns(orientation);
    }

    /**
     * Display the given campaign without the need to trigger an event and skipping
     * the configured rules.
     *
     * @param campaign
     * @return true if the campaign was displayed.
     */
    public static boolean showMessageCenterCampaign(SwrveBaseCampaign campaign) {
        checkInstanceCreated();
        return instance.showMessageCenterCampaign(campaign);
    }

    /**
     * Remove this campaign. It won't be returned anymore by the 'getMessageCenterCampaigns' methods.
     *
     * @param campaign
     */
    public static void removeMessageCenterCampaign(SwrveBaseCampaign campaign) {
        checkInstanceCreated();
        instance.removeMessageCenterCampaign(campaign);
    }

    protected static void checkInstanceCreated() throws RuntimeException {
        if (instance == null) {
            SwrveLogger.e(SwrveImp.LOG_TAG, "Please call SwrveSDK.createInstance first in your Application class.");
            throw new RuntimeException("Please call SwrveSDK.createInstance first in your Application class.");
        }
    }

    public static ISwrveBase getInstance() {
        return instance;
    }

}
