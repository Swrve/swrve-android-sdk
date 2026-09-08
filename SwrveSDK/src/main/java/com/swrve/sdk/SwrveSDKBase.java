package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_SENT_TIME;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
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
     * The release version of this SDK.
     *
     * @return SDK version as a string, for example "12.2.1".
     */
    public static String getSdkVersion() {
        checkInstanceCreated();
        return SwrveBase.getVersion();
    }

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
     * Request the list of real time user properties which have been retrieved along with
     * Campaign and Resources for this user.
     * <p>
     *
     * @param listener The custom listener
     */
    public static void getRealTimeUserProperties(final SwrveRealTimeUserPropertiesListener listener) {
        checkInstanceCreated();
        instance.getRealTimeUserProperties(listener);
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
     * Stop the SDK from tracking. The sdk will remain stopped until a start api is called.
     */
    public static void stopTracking() {
        checkInstanceCreated();
        instance.stopTracking();
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
     * Get the device id
     *
     * @return device id
     */
    public static String getDeviceId() {
        checkInstanceCreated();
        return instance.getDeviceId();
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
     * config.autoDownloadCampaignsAndResources to false. Use refreshContent() instead.
     */
    public static void refreshCampaignsAndResources() {
        checkInstanceCreated();
        instance.refreshContent(null);
    }

    /**
     * Refresh content from server, which includes campaigns, resources, push inbox, real time user properties.
     * Content is refreshed automatically but you can use this API to refresh manually or if
     * config.autoDownloadCampaignsAndResources to false.
     * This is an asynchronous operation and the listener will be called when the operation is complete.
     * Check the returned result object for success or failure. Set the listener to null if you
     * don't want to be notified when the operation is complete.
     *
     * @param listener  the listener to trigger after this operation has completed
     */
    public static void refreshContent(SwrveRefreshContentListener listener) {
        checkInstanceCreated();
        instance.refreshContent(listener);
    }

    /**
     * Process an embedded message engagement event. This function should be called by your
     * implementation to inform Swrve of a button event.
     *
     * @param message embedded message that has been processed
     * @param buttonName button that was pressed.
     */
    public static void embeddedMessageButtonWasPressed(SwrveEmbeddedMessage message, String buttonName) {
        checkInstanceCreated();
        instance.embeddedMessageButtonWasPressed(message, buttonName);
    }

    /**
     * Inform that an embedded message has been served and processed. This function should be called
     * by your implementation to update the campaign information and send the appropriate data to
     * Swrve.
     *
     * @param message embedded message that has been processed
     */
    public static void embeddedMessageWasShownToUser(SwrveEmbeddedMessage message) {
        checkInstanceCreated();
        instance.embeddedMessageWasShownToUser(message);
    }

    /**
     * Send impression event for an embedded control campaign
     *
     * @param message embedded message that has been processed
     */
    public static void embeddedControlMessageImpressionEvent(SwrveEmbeddedMessage message) {
        checkInstanceCreated();
        instance.embeddedControlMessageImpressionEvent(message);
    }

    /**
     * Get the personalized data string from a SwrveEmbeddedMessage campaign with a map of custom
     * personalization properties.
     *
     * @param message Embedded message campaign to personalize
     * @param personalizationProperties Custom properties which are used for personalization.
     * @return The data string with personalization properties applied. Null is returned if personalization fails with the custom properties passed in.
     */
    public static String getPersonalizedEmbeddedMessageData(SwrveEmbeddedMessage message, Map<String, String> personalizationProperties) {
        checkInstanceCreated();
        return instance.getPersonalizedEmbeddedMessageData(message, personalizationProperties);
    }

    /**
     * Get the personalized data string from a piece of text with a map of custom personalization
     * properties.
     *
     * @param text String value which will be personalized
     * @param personalizationProperties Custom properties which are used for personalization.
     * @return The data string with personalization properties applied. Null is returned if personalization fails with the custom properties passed in.
     */
    public static String getPersonalizedText(String text, Map<String, String> personalizationProperties) {
        checkInstanceCreated();
        return instance.getPersonalizedText(text, personalizationProperties);
    }

    /**
     * Get location of the chosen cache folder where the resources will be  downloaded.
     *
     * @return File path to the chosen cache folder
     */
    public static File getCacheDir() {
        checkInstanceCreated();
        return instance.getCacheDir();
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
     * Gets active In-app and Embedded Message Center campaigns for the current orientation, excluding
     * deleted campaigns. Use {@link #getMessageCenterCampaigns(SwrveOrientation)} with {@link SwrveOrientation#Both}
     * for all orientations.
     *
     * @return List of active {@link SwrveBaseCampaign} for the current orientation (empty if none).
     */
    public static List<SwrveBaseCampaign> getMessageCenterCampaigns() {
        checkInstanceCreated();
        return instance.getMessageCenterCampaigns();
    }

    /**
     * Gets active In-app and Embedded Message Center campaigns for the specified orientation,
     * excluding deleted campaigns. Use {@link #getMessageCenterCampaigns(SwrveOrientation)} with
     * {@link SwrveOrientation#Both} for all orientations.
     *
     * @param orientation The required {@link SwrveOrientation}.
     * @return List of active {@link SwrveBaseCampaign} for the {@code orientation} (empty if none).
     */
    public static List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation) {
        checkInstanceCreated();
        return instance.getMessageCenterCampaigns(orientation);
    }

    /**
     * Gets active In-app and Embedded Message Center campaigns for the current orientation, with personalization
     * properties, excluding deleted campaigns. Use {@link #getMessageCenterCampaigns(SwrveOrientation, Map)} with
     * {@link SwrveOrientation#Both} for all orientations.
     *
     * @param properties Additional properties for personalization.
     * @return List of active {@link SwrveBaseCampaign} for the current orientation (empty if none).
     */
    public static List<SwrveBaseCampaign> getMessageCenterCampaigns(Map<String, String> properties) {
        checkInstanceCreated();
        return instance.getMessageCenterCampaigns(properties);
    }

    /**
     * Gets active In-app and Embedded Message Center campaigns for the specified orientation, with personalization
     * properties, excluding deleted campaigns.
     *
     * @param orientation The required {@link SwrveOrientation}.
     * @param properties  Additional properties for personalization.
     * @return List of active {@link SwrveBaseCampaign} for the {@code orientation} (empty if none).
     */
    public static List<SwrveBaseCampaign> getMessageCenterCampaigns(SwrveOrientation orientation, Map<String, String> properties) {
        checkInstanceCreated();
        return instance.getMessageCenterCampaigns(orientation, properties);
    }

    /**
     * Gets active In-app Message Center campaigns for the specified orientation, with personalization properties,
     * excluding deleted campaigns.
     *
     * @param orientation The required {@link SwrveOrientation}.
     * @param properties  Additional properties for personalization.
     * @return List of active {@link SwrveInAppCampaign} (empty if none).
     */
    public static List<SwrveInAppCampaign> getInAppMessageCenterCampaigns(SwrveOrientation orientation, Map<String, String> properties) {
        checkInstanceCreated();
        return instance.getInAppMessageCenterCampaigns(orientation, properties);
    }

    /**
     * Gets active Embedded Message Center campaigns, excluding deleted ones. The embedded message data is personalized.
     *
     * @return List of active {@link SwrveEmbeddedMessage} (empty if none).
     */
    public static List<SwrveEmbeddedMessage> getEmbeddedMessageCenterCampaigns() {
        checkInstanceCreated();
        return instance.getEmbeddedMessageCenterCampaigns(null);
    }

    /**
     * Gets active Embedded Message Center campaigns, excluding deleted ones. The embedded message data is personalized.
     *
     * @param properties  Additional properties for personalization.
     * @return List of active {@link SwrveEmbeddedMessage} (empty if none).
     */
    public static List<SwrveEmbeddedMessage> getEmbeddedMessageCenterCampaigns(Map<String, String> properties) {
        checkInstanceCreated();
        return instance.getEmbeddedMessageCenterCampaigns(properties);
    }
    
    /**
     * Get the active MessageCenter campaign targeted for this user. It will exclude campaigns that have been deleted with the
     * removeMessageCenterCampaign method and those that do not support the current orientation.
     * @param campaignId the id of the campaign to get
     * @param properties additional properties which can be used for personalization.
     * @return The active MessageCenter campaign is returned if campaign id is valid. Returns null if the campaign id is invalid or campaign is not active.
     */
    public static SwrveBaseCampaign getMessageCenterCampaign(int campaignId, Map<String, String> properties) {
        checkInstanceCreated();
        return instance.getMessageCenterCampaign(campaignId, properties);
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
     * Display the given campaign without the need to trigger an event and skipping
     * the configured rules.
     *
     * @param campaign The campaign
     * @param properties additional properties which can be used for IAM personalization.
     * @return true if the campaign was displayed.
     */
    public static boolean showMessageCenterCampaign(SwrveBaseCampaign campaign, Map<String, String> properties) {
        checkInstanceCreated();
        return instance.showMessageCenterCampaign(campaign, properties);
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

    /**
     * Remove this campaign. It won't be returned anymore by the 'getMessageCenterCampaigns' methods.
     *
     * @param campaignId the id of the campaign to remove
     */
    public static void removeMessageCenterCampaign(int campaignId) {
        checkInstanceCreated();
        instance.removeMessageCenterCampaign(campaignId);
    }

    /**
     * Mark this campaign as seen. This is done automatically by Swrve but you can call this if you are rendering the messages on your own.
     *
     * @param campaign the campaign
     */
    public static void markMessageCenterCampaignAsSeen(SwrveBaseCampaign campaign) {
        checkInstanceCreated();
        instance.markMessageCenterCampaignAsSeen(campaign);
    }

    /**
     * Mark this campaign as seen. This is done automatically by Swrve but you can call this if you are rendering the messages on your own.
     *
     * @param campaignId the id of the campaign to mark as seen
     */
    public static void markMessageCenterCampaignAsSeen(int campaignId) {
        checkInstanceCreated();
        instance.markMessageCenterCampaignAsSeen(campaignId);
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
     * @return current external user id, or an empty string if there is none
     */
    public static String getExternalUserId() {
        checkInstanceCreated();
        return instance.getExternalUserId();
    }

    public static ISwrveBase getInstance() {
        return instance;
    }

    /**
     * Start the sdk if stopped or in SwrveInitMode.MANAGED mode.
     * Tracking will begin using the last user or an auto generated userId if the first time the sdk is started.
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
     * Check if the SDK is started and currently tracking.
     *
     * @return true if sdk is not waiting to be started and hasn't been stopped using the stopTracking api.
     */
    public static boolean isStarted() {
        checkInstanceCreated();
        return instance.isStarted();
    }

    /**
     * Gets a list of eligible push inbox messages.
     *
     * @return List of SwrvePushInboxMessage
     */
    public static List<SwrvePushInboxMessage> getPushInboxMessages() {
        checkInstanceCreated();
        return instance.getPushInboxMessages();
    }

    /**
     * Mark the Push Inbox Message as read. This is an asynchronous operation and the listener will be called when the
     * operation is complete. Check the returned result object for success or failure.
     *
     * @param messageId the messageId of the SwrvePushInboxMessage to update as read
     * @param listener  the listener to trigger after this operation has completed
     */
    public static void readPushInboxMessage(long messageId, SwrvePushInboxListener listener) {
        checkInstanceCreated();
        instance.readPushInboxMessage(messageId, listener);
    }

    /**
     * Mark the Push Inbox Message as read and send engagement event. This is an asynchronous operation and the
     * listener will be called when the operation is complete. Check the returned result object for success or failure.
     *
     * @param messageId the messageId of the SwrvePushInboxMessage to update as read and engaged
     * @param listener  the listener to trigger after this operation has completed
     */
    public static void engagePushInboxMessage(long messageId, SwrvePushInboxListener listener) {
        checkInstanceCreated();
        instance.engagePushInboxMessage(messageId, listener);
    }

    /**
     * Delete the Push Inbox Message. This is an asynchronous operation and the listener will be called when the
     * operation is complete. Check the returned result object for success or failure.
     *
     * @param messageId the messageId of the SwrvePushInboxMessage to delete
     * @param listener  the listener to trigger after this operation has completed
     */
    public static void deletePushInboxMessage(long messageId, SwrvePushInboxListener listener) {
        checkInstanceCreated();
        instance.deletePushInboxMessage(messageId, listener);
    }

    /**
     * The pushInboxUpdateListener onMessagesUpdated() method is invoked when Push Inbox messages
     * have been initially loaded and each time messages are updated/changed.
     * <p>
     * Held with a weak reference, so keep a strong reference to the listener for as long as you want
     * updates — an anonymous one with no other reference may be garbage collected, after which no
     * callbacks arrive. Pass null to stop updates.
     *
     * @param pushInboxUpdateListener Called when the push inbox messages are initially loaded and each time messages are updated/changed.
     */
    public static void setPushInboxUpdateListener(SwrvePushInboxUpdateListener pushInboxUpdateListener) {
        checkInstanceCreated();
        instance.setPushInboxUpdateListener(pushInboxUpdateListener);
    }

    /**
     * The SwrveCampaignsUpdateListener onCampaignsUpdated() method is invoked once after the initial content load attempt, whether or not it succeeded or anything changed, and again whenever
     * content the SDK has fetched <i>may</i> have changed the campaigns. Invocations are independent and arrive in no guaranteed order. Campaigns are a snapshot, so re-read them on every invocation rather than holding the previous result. The SDK does not compare against what you last read, so let your own
     * comparison decide whether to redraw. It covers every campaign surface, including Message Center and embedded.
     * <p>
     * Register where the SDK is created: the initial notification is sent once and never replayed, so a listener installed later may miss it.
     * <p>
     * SDK-driven changes only. {@link #markMessageCenterCampaignAsSeen(int)} and {@link #removeMessageCenterCampaign(int)} change what the getters return without invoking this, so re-read
     * after your own calls.
     * <p>
     * One of the invocations follows the SDK's attempt to download campaign assets, which is when new campaigns normally become readable — a campaign is not listable until its assets are on
     * disk, and individual downloads can fail, so it is not a promise that everything is present.
     * <p>
     * Held with a weak reference, so keep a strong reference to the listener for as long as you want updates — an anonymous one with no other reference may be garbage collected, after which no
     * callbacks arrive. Pass null to stop updates.
     *
     * @param campaignsUpdateListener Called after the initial content load attempt and whenever fetched content may have changed the campaigns.
     */
    public static void setCampaignsUpdateListener(SwrveCampaignsUpdateListener campaignsUpdateListener) {
        checkInstanceCreated();
        instance.setCampaignsUpdateListener(campaignsUpdateListener);
    }

    /**
     * Send a device update to Swrve. This will update the device information on the Swrve backend.
     */
    public static void sendDeviceUpdate() {
        checkInstanceCreated();
        instance.sendDeviceUpdate();
    }

    /**
     * Returns whether the provided data payload represents a Swrve push.
     *
     * @param data key/value map received from the push provider. For firebase this is RemoteMessage.getData().
     * @return true if one of the Swrve tracking keys is present; false otherwise.
     */
    public static boolean isSwrvePush(@NonNull Map<String, String> data) {
        checkInstanceCreated();
        return SwrveHelper.isSwrvePush(data);
    }

    /**
     * This method should be used when multiple push providers are integrated and Swrve's default
     * push implementation is not being used. See samples directory in the public repository on how to use it.
     *
     * @param context   Application or Service {@link android.content.Context} used to enqueue background work.
     * @param data      Provider payload key/value map. For firebase this is RemoteMessage.getData().
     * @param messageId Provider message identifier. For firebase this is RemoteMessage.getMessageId().
     * @param sentTime  Provider message timestamp in milliseconds. For firebase this is RemoteMessage.getSentTime().
     * @return true if Swrve handled the payload and scheduled processing; false otherwise.
     */
    public static boolean handleSwrvePush(Context context, Map<String, String> data, String messageId, long sentTime) {
        checkInstanceCreated();
        return SwrvePushServiceDefault.handle(context, data, messageId, sentTime);
    }
}
