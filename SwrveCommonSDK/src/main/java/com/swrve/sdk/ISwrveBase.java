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
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * SwrveSDK interface. You can obtain an instance of this class using the SwrveFactory or
 * SwrveInstance that creates a singleton Swrve object.
 */
public interface ISwrveBase<T, C extends SwrveConfigBase> {

    /**
     * Create or bind to a Swrve object. Typically this function is called in your main
     * activity's onCreate function.
     *
     * @param activity your activity
     * @param appId   your app id in the Swrve dashboard
     * @param apiKey  your app api_key in the Swrve dashboard
     * @throws IllegalArgumentException
     */
    T onCreate(final Activity activity, final int appId, final String apiKey) throws IllegalArgumentException;

    /**
     * Create or bind to a Swrve object. Typically this function is called in your main
     * activity's onCreate function.
     *
     * @param activity your activity
     * @param appId   your app id in the Swrve dashboard
     * @param apiKey  your app api_key in the Swrve dashboard
     * @param config  your SwrveConfig options
     * @throws IllegalArgumentException
     */
    T onCreate(final Activity activity, final int appId, final String apiKey, final C config) throws IllegalArgumentException;

    /**
     * Add a Swrve.session.start event to the event queue. This event should
     * typically be added in your main activity's onStart method.
     */
    void sessionStart();

    /**
     * Add a Swrve.session.end event to the event queue. This event should
     * typically be added in your main activity's onStop method.
     */
    void sessionEnd();

    /**
     * Add a generic named event to the event queue.
     *
     * @param name the name of the event in question. The character '.' is used
     *             as grouping syntax.
     */
    void event(String name);

    /**
     * Add a generic named event to the event queue.
     *
     * @param name    the name of the event in question. The character '.' is used
     *                as grouping syntax.
     * @param payload a dictionary of key-value pairs to be supplied with this named
     *                event. Typically this would be information about the event
     *                rather than about the user. Compare with the userUpdate
     *                function for properties of the user.
     */
    void event(String name, Map<String, String> payload);

    /**
     * Add a Swrve.user_purchase event to the event queue. This event should be
     * added on virtual goods purchase in your app.
     *
     * @param item     unique name of the purchased item
     * @param currency currency in Swrve to be used for this purchase. This currency
     *                 must be declared in the Swrve dashboard before calling this
     *                 function. If this currency is not declared this event will be
     *                 rejected.
     * @param cost     cost of the item in units of 'currency'
     * @param quantity number of the item purchased
     */
    void purchase(String item, String currency, int cost, int quantity);

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
    void currencyGiven(String givenCurrency, double givenAmount);

    /**
     * Add a Swrve.user event to the event queue. This event would typically be
     * added to the queue after session_start and at points where properties of
     * your users change - for example, levelUp.
     *
     * @param attributes key-value pairs of properties of the user. Typical values
     *                   would be level => number, referrer => channel, coin balance =>
     *                   number.
     */
    void userUpdate(Map<String, String> attributes);

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
    void iap(int quantity, String productId, double productPrice, String currency);

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
    void iap(int quantity, String productId, double productPrice, String currency, SwrveIAPRewards rewards);

    /**
     * Get the SwrveResourceManager, which can be queried for up-to-date resource attribute values
     */
    SwrveResourceManager getResourceManager();

    /**
     * The resourcesListener onResourcesUpdated() method is invoked when user resources in the SwrveResourceManager
     * have been initially loaded and each time user resources are updated.
     */
    void setResourcesListener(ISwrveResourcesListener resourcesListener);

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
    void getUserResources(final ISwrveUserResourcesListener listener);

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
    void getUserResourcesDiff(final ISwrveUserResourcesDiffListener listener);

    /**
     * Send events to Swrve servers.
     */
    void sendQueuedEvents();

    /**
     * Flush events and others to the device's disk.
     */
    void flushToDisk();

    /**
     * Default SDK behavior for activity onPause(). Flush data to disk.
     * Notify the SDK that the binded activity may be finishing.
     */
    void onPause();

    /**
     * Default SDK behavior for activity onResume(). Send events to Swrve.
     *
     * @param ctx Activity that called this method
     */
    void onResume(Activity ctx);

    /**
     * Notify that the app is low on memory.
     */
    void onLowMemory();

    /**
     * Notify that the app has closed.
     *
     * @param ctx Activity that called this method
     */
    void onDestroy(Activity ctx);

    /**
     * Shutdown the SDK. This instance will be unusable after shutdown.
     * <p/>
     * Note: All the background jobs will try to stop when this happens.
     */
    void shutdown();

    /**
     * Set the current language
     */
    void setLanguage(Locale locale);

    /**
     * Get the current language
     */
    String getLanguage();

    /**
     * Set the current language
     *
     * @deprecated use {@link #setLanguage(Locale)} instead
     */
    @Deprecated
    void setLanguage(String language);

    /**
     * Get the current api key
     */
    String getApiKey();

    /**
     * Get the current user id
     */
    String getUserId();

    /**
     * Collect device information
     *
     * @throws JSONException
     */
    JSONObject getDeviceInfo() throws JSONException;

    /**
     * Update campaign and resources values
     * This function will be called automatically to keep campaigns and resources up-to-date.
     * You should only call this function manually if you have changed the value of
     * config.autoDownloadCampaignsAndResources to false.
     */
    void refreshCampaignsAndResources();

    /**
     * Returns a message for a given trigger event. There may be messages for
     * the trigger but the rules avoid a message from being displayed at some
     * point.
     *
     * @param event trigger event
     * @return SwrveMessage supported message from a campaign set up for the
     * given trigger
     */
    SwrveMessage getMessageForEvent(String event);

    /**
     * Returns a message for a given id. This function should be used for
     * retrieving a known message that is being displayed when the device's
     * orientation changes.
     *
     * @param messageId id of the message
     * @return SwrveMessage message with the given id
     */
    SwrveMessage getMessageForId(int messageId);

    /**
     * Process a message button event. This function should be called by your
     * implementation of the message renderer to inform Swrve of a button event.
     *
     * @param button button that was pressed.
     */
    void buttonWasPressedByUser(SwrveButton button);

    /**
     * Inform that a message has been shown. This function should be called by
     * your implementation of the message renderer to update the campaign
     * information and send the appropriate data to Swrve.
     *
     * @param messageFormat message that was shown to the user for the first time in this
     *                session.
     */
    void messageWasShownToUser(SwrveMessageFormat messageFormat);

    /**
     * Get app store link configured in the dashboard for a given app id.
     *
     * @param appId id of the app
     * @return String App store link for the app
     */
    String getAppStoreURLForApp(int appId);

    /**
     * Get location of the chosen cache folder where the resources will be
     * downloaded.
     *
     * @return File path to the chosen cache folder
     */
    File getCacheDir();

    /**
     * Set a message listener to process Talk messages.
     *
     * @param messageListener logic to listen for messages
     */
    void setMessageListener(ISwrveMessageListener messageListener);

    /**
     * Get the time when the SDK was initialized.
     *
     * @return the time the SDK was initialized
     */
    Date getInitialisedTime();

    /**
     * Get the custom listener to process Talk message install button clicks
     *
     * @return custom install button listener
     */
    ISwrveInstallButtonListener getInstallButtonListener();

    /**
     * Set the custom listener to process Talk message install button clicks
     *
     * @param installButtonListener
     */
    void setInstallButtonListener(ISwrveInstallButtonListener installButtonListener);

    /**
     * Get the custom listener to process Talk message custom button clicks
     *
     * @return custom button listener
     */
    ISwrveCustomButtonListener getCustomButtonListener();

    /**
     * Set the custom listener to process Talk message custom button clicks
     *
     * @param customButtonListener
     */
    void setCustomButtonListener(ISwrveCustomButtonListener customButtonListener);

    /**
     * Get the custom dialog listener for Talk message dialogs
     *
     * @return dialog listener
     */
    ISwrveDialogListener getDialogListener();

    /**
     * Set the custom dialog listener for Talk message dialogs
     *
     * @param dialogListener
     */
    void setDialogListener(ISwrveDialogListener dialogListener);

    /**
     * Get the context where the SDK is attached.
     *
     * @return activity or application context
     */
    Context getContext();

    /**
     * Returns the Swrve configuration that was used to initialize the SDK.
     *
     * @return configuration used to context the SDK
     */
    C getConfig();
}
