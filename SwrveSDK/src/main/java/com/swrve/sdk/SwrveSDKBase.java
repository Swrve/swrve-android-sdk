package com.swrve.sdk;

import android.app.Activity;
import android.os.Bundle;

import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveDismissButtonListener;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessageListener;
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
     * Identify users such that they can be tracked and targeted safely across multiple devices, platforms and channels.
     * Throws RunTimeException if called in SwrveInitMode.MANAGED mode.
     * <pre>
     * <code>
     * SwrveSDK.identify("12345", new SwrveIdentityResponse() {
     *      {@literal @}Override
     *      public void onSuccess(String status, String swrveId) {
     *
     *      }
     *
     *      {@literal @}Override
     *      public void onError(int responseCode, String errorMessage) {
     *          // please note in the event of an error the tracked userId will not reflect correctly on the backend until this
     *          // call completes successfully
     *      }
     * });
     * </code>
     * </pre>
     *
     * @param userId           ID that uniquely identifies your user. Personal identifiable information should not be used. An error may be returned if such information is submitted as the userID eg email, phone number etc.
     * @param identityResponse Interface with onSuccess onError callbacks
     */
    public static void identify(final String userId, final SwrveIdentityResponse identityResponse) {
        checkInstanceCreated();
        instance.identify(userId, identityResponse);
    }

    /**
     * Handle a deeplink when the app is already installed
     * i.e. using facebook API
     * <pre>
     * <code>
     * AppLinkData appLinkData = AppLinkData.createFromActivity(this);
        if (appLinkData != null) {
            SwrveSDK.handleDeeplink(appLinkData.getArgumentBundle());
        }
     * </code>
     * </pre>
     *
     * @param bundle the bundle to process
     */
    public static void handleDeeplink(Bundle bundle) {
        checkInstanceCreated();
        instance.handleDeeplink(bundle);
    }

    /**
     * Handle a deeplink when the app was installed first from the play store`
     * i.e. using facebook API
     * <pre>
     * <code>
     * AppLinkData.fetchDeferredAppLinkData(this, new AppLinkData.CompletionHandler(){
     *
     *     public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
     *          Bundle data = appLinkData.getArgumentBundle();
     *          SwrveSDK.handleDeferredDeeplink(data);
     *      }
     *      });
     * </code>
     * </pre>
     *
     * @param bundle the bundle to process
     */
    public static void handleDeferredDeeplink(Bundle bundle) {
        checkInstanceCreated();
        instance.handleDeferredDeeplink(bundle);
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
     * @param name    the name of the event in question. The character '.' is used as grouping syntax.
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
     * Add a group of custom user properties to the event queue. This event would typically be
     * added to the queue after session_start and at points where properties of
     * your users change - for example, levelUp.
     *
     * @param attributes key-value pairs of properties of the user. Typical values
     *                   would be level = number, referrer = channel, coin balance = number.
     */
    public static void userUpdate(Map<String, String> attributes) {
        checkInstanceCreated();
        instance.userUpdate(attributes);
    }

    /**
     * Add a single custom user property to the event queue specifying a date. This event
     * would typically be added to the queue after session_start and at points where
     * properties of your users change - for example, registration date or check-in date.
     *
     * @param name key for the custom user property
     * @param date date value for the custom user property
     */
    public static void userUpdate(String name, Date date) {
        checkInstanceCreated();
        instance.userUpdate(name, date);
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
     * <p>
     * To create the rewards object, create an instance of SwrveIAPRewards and
     * use addItem() and addCurrency() to add the individual rewards
     *
     * @param quantity     Quantity purchased. Must be greater than zero.
     * @param productId    Unique product identifier for the item bought. This should
     *                     match the Swrve resource name. Required, cannot be empty.
     * @param productPrice price of the product in real money. Note that this is the
     *                     price per product, not the total price of the transaction
     *                     (when quantity greater than 1) A typical value would be 0.99. Must be
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
     *
     * @return resource manager
     */
    public static SwrveResourceManager getResourceManager() {
        checkInstanceCreated();
        return instance.getResourceManager();
    }

    /**
     * The resourcesListener onResourcesUpdated() method is invoked when user resources in the SwrveResourceManager
     * have been initially loaded and each time user resources are updated.
     *
     * @param resourcesListener Resource listener
     */
    public static void setResourcesListener(SwrveResourcesListener resourcesListener) {
        checkInstanceCreated();
        instance.setResourcesListener(resourcesListener);
    }

    /**
     * Request the list of resources for the user with full attribute data after
     * any applicable AB Tests have been applied. This request is executed on a
     * background thread, which will call methods on the user-provided listener
     * parameter.
     * <p>
     * If no user id has been specified this function raises a
     * NoUserIdSwrveException exception to the listener object.
     *
     * @param listener The custom listener
     */
    public static void getUserResources(final SwrveUserResourcesListener listener) {
        checkInstanceCreated();
        instance.getUserResources(listener);
    }

    /**
     * Request all applicable AB-Tested resources for the user. This request is
     * executed on a background thread, which will call methods on the
     * user-provided listener parameter.
     * <p>
     * If no user id has been specified this function raises a
     * NoUserIdSwrveException exception to the listener object.
     *
     * @param listener The custom listener
     */
    public static void getUserResourcesDiff(final SwrveUserResourcesDiffListener listener) {
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
     * Shutdown the SDK. This instance will be unusable after shutdown.
     * <p>
     * Note: All the background jobs will try to stop when this happens.
     */
    public static void shutdown() {
        checkInstanceCreated();
        instance.shutdown();
    }

    /**
     * Set the current language
     *
     * @param locale Language locale to use.
     */
    public static void setLanguage(Locale locale) {
        checkInstanceCreated();
        instance.setLanguage(locale);
    }

    /**
     * Get the current language
     *
     * @return current language
     */
    public static String getLanguage() {
        checkInstanceCreated();
        return instance.getLanguage();
    }

    /**
     * Get the current api key
     *
     * @return current api key
     */
    public static String getApiKey() {
        checkInstanceCreated();
        return instance.getApiKey();
    }

    /**
     * Get the current user id
     *
     * @return current user id
     */
    public static String getUserId() {
        checkInstanceCreated();
        return instance.getUserId();
    }

    /**
     * Collect device information
     *
     * @return device information
     * @throws JSONException If there's an error
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
     * Set a message listener to process in-app messages.
     *
     * @param messageListener logic to listen for messages
     */
    public static void setMessageListener(SwrveMessageListener messageListener) {
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
     * Get the custom listener to process in-app message install button clicks
     *
     * @return buttonListener
     */
    public static SwrveInstallButtonListener getInstallButtonListener() {
        checkInstanceCreated();
        return instance.getInstallButtonListener();
    }

    /**
     * Set the custom listener to process in-app message install button clicks
     *
     * @param installButtonListener The custom listener
     */
    public static void setInstallButtonListener(SwrveInstallButtonListener installButtonListener) {
        instance.setInstallButtonListener(installButtonListener);
    }

    /**
     * Get the custom listener to process in-app message custom button clicks
     *
     * @return the custom buttonListener
     */
    public static SwrveCustomButtonListener getCustomButtonListener() {
        checkInstanceCreated();
        return instance.getCustomButtonListener();
    }

    /**
     * Set the custom listener to process in-app message custom button clicks
     *
     * @param customButtonListener The custom listener
     */
    public static void setCustomButtonListener(SwrveCustomButtonListener customButtonListener) {
        checkInstanceCreated();
        instance.setCustomButtonListener(customButtonListener);
    }

    /**
     * Get the in-app listener to get notified of in-app message dismiss button clicks
     *
     * @return the custom buttonListener
     */
    public static SwrveDismissButtonListener getInAppDismissButtonListener() {
        checkInstanceCreated();
        return instance.getDismissButtonListener();
    }

    /**
     * Set the in-app button listener to get notified of in-app message dismiss button clicks
     *
     * @param inAppDismissButtonListener The in-app dismiss button listener
     */
    public static void setCustomDismissButtonListener(SwrveDismissButtonListener inAppDismissButtonListener) {
        checkInstanceCreated();
        instance.setDismissButtonListener(inAppDismissButtonListener);
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
     * @param orientation Orientation whcih the messages have to support
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
     * @param campaign The campaign
     * @return true if the campaign was displayed.
     */
    public static boolean showMessageCenterCampaign(SwrveBaseCampaign campaign) {
        checkInstanceCreated();
        return instance.showMessageCenterCampaign(campaign);
    }

    /**
     * Remove this campaign. It won't be returned anymore by the 'getMessageCenterCampaigns' methods.
     *
     * @param campaign the campaign
     */
    public static void removeMessageCenterCampaign(SwrveBaseCampaign campaign) {
        checkInstanceCreated();
        instance.removeMessageCenterCampaign(campaign);
    }

    protected static void checkInstanceCreated() throws RuntimeException {
        if (instance == null) {
            SwrveLogger.e("Please call SwrveSDK.createInstance first in your Application class.");
            throw new RuntimeException("Please call SwrveSDK.createInstance first in your Application class.");
        }
    }

    /**
     * Get the current external user id.
     *
     * @return current external user id
     */
    public static String getExternalUserId() {
        checkInstanceCreated();
        return instance.getExternalUserId();
    }

    /**
     * Add a custom payload for user input events:
     * Selecting a star-rating,
     * Selecting a choice on a text questionnaire
     * Selecting play on a video
     * <p>
     * If key pair values added is greater than 5 or Keys added conflict with existing swrve internal keys then
     * the custom payload will be rejected and not added for the event. A debug log error will be generated.
     *
     * @param payload Map with custom key pair values.
     */
    public static void setCustomPayloadForConversationInput(Map payload) {
        checkInstanceCreated();
        instance.setCustomPayloadForConversationInput(payload);
    }

    public static ISwrveBase getInstance() {
        return instance;
    }

    /**
     * Start the sdk when in SwrveInitMode.MANAGED mode.
     * Tracking will begin using the last user or an auto generated userId if the first time the sdk is started.
     * Throws RunTimeException if called in SwrveInitMode.AUTO mode.
     *
     * @param activity Activity where the session was started.
     */
    public static void start(Activity activity) {
        checkInstanceCreated();
        instance.start(activity);
    }

    /**
     * Start the sdk when in SwrveInitMode.MANAGED mode.
     * Tracking will begin using the userId passed in.
     * Can be called multiple times to switch the current userId to something else. A new session is started if not
     * already started or if is already started with different userId.
     * The sdk will remain started until the createInstance is called again.
     * Throws RunTimeException if called in SwrveInitMode.AUTO mode.
     *
     * @param activity Activity where the session was started.
     * @param userId   User id to start sdk with.
     */
    public static void start(Activity activity, String userId) {
        checkInstanceCreated();
        instance.start(activity, userId);
    }

    /**
     * Check if the SDK has been started.
     *
     * @return true when in SwrveInitMode.AUTO mode. When in SwrveInitMode.MANAGED mode it will return true after one of the 'start' api's has been called.
     */
    public static boolean isStarted() {
        checkInstanceCreated();
        return instance.isStarted();
    }
}
