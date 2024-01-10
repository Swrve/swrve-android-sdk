package com.swrve.sdk;

import android.app.Application;
import android.content.Context;

import com.swrve.sdk.config.SwrveConfig;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;

public class SwrveSDK extends SwrveSDKBase {

    /**
     * Create a single Swrve SDK instance.
     * @param application your application context
     * @param appId   your app id in the Swrve dashboard
     * @param apiKey  your app api_key in the Swrve dashboard
     * @return singleton SDK instance.
     */
    public static synchronized ISwrve createInstance(final Application application, final int appId, final String apiKey) {
        return createInstance(application, appId, apiKey, new SwrveConfig());
    }

    /**
     * Create a single Swrve SDK instance.
     * @param application your application context
     * @param appId   your app id in the Swrve dashboard
     * @param apiKey  your app api_key in the Swrve dashboard
     * @param config  your SwrveConfig options
     * @return singleton SDK instance.
     */
    public static synchronized ISwrve createInstance(final Application application, final int appId, final String apiKey, final SwrveConfig config) {
        if (application == null) {
            SwrveHelper.logAndThrowException("Application is null");
        } else if (SwrveHelper.isNullOrEmpty(apiKey)) {
            SwrveHelper.logAndThrowException("Api key not specified");
        } else if (SwrveHelper.isInvalidAPIKey(apiKey)) {
            SwrveHelper.logAndThrowException("Api key should not start with secret-");
        }

        if (!SwrveHelper.sdkAvailable(config.getModelBlackList())) {
            instance =  new SwrveEmpty(application, apiKey);
        }
        if (instance == null) {
            instance = new Swrve(application, appId, apiKey, config);
        }
        return (ISwrve) instance;
    }

    /**
     * Returns the Swrve configuration that was used to initialize the SDK.
     *
     * @return configuration used to context the SDK
     */
    public static SwrveConfig getConfig() {
        checkInstanceCreated();
        return (SwrveConfig) instance.getConfig();
    }

    /**
     * Set the silent push listener.
     *
     * @param silentPushListener silent push listener
     */
    public static void setSilentPushListener(SwrveSilentPushListener silentPushListener) {
        checkInstanceCreated();
        SwrveConfig config = getConfig();
        if (config != null) {
            config.setSilentPushListener(silentPushListener);
        }
    }

    /**
     * Add a Swrve.iap event to the event queue. This event should be added
     * for unvalidated real money transactions in the Google Play Store, where a single item was purchased.
     * (i.e where no in-app currency or bundle was purchased)
     *
     * @param productId     Unique product identifier for the item bought. This should
     *                      match the Swrve resource name.
     *                      Required, cannot be empty.
     * @param productPrice  The price (in real money) of the product that was purchased.
     *                      Note: this is not the price of the total transaction, but the per-product price.
     *                      Must be greater than or equal to zero.
     * @param currency      real world currency used for this transaction. This must be an ISO
     *                      currency code. A typical value would be "USD". Required, cannot be empty.
     * @param purchaseData  Receipt information from Google play. Required, cannot be empty.
     * @param dataSignature The purchase data received from Google Play. Required, cannot be empty.
     */
    public static void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature) {
        checkInstanceCreated();
        ((ISwrve) instance).iapPlay(productId, productPrice, currency, purchaseData, dataSignature);
    }

    /**
     * Add a Swrve.iap event to the event queue. This event should be added for unvalidated real
     * money transactions in the Google Play Store, where in-app currency was purchased
     * or where multiple items and/or currencies were purchased.
     *
     * To create the rewards object, create an instance of SwrveIAPRewards and
     * use addItem() and addCurrency() to add the individual rewards
     *
     * @param productId     Unique product identifier for the item bought. This should match the
     *                      Swrve resource name. Required, cannot be empty.
     * @param productPrice  price of the product in real money. Note that this is the price
     *                      per product, not the total price of the transaction (when quantity is higher than 1)
     *                      A typical value would be 0.99. Must be greater or equal to zero.
     * @param currency      real world currency used for this transaction. This must be an
     *                      ISO currency code. A typical value would be "USD".
     *                      Required, cannot be empty.
     * @param rewards       SwrveIAPRewards object containing any in-app currency and/or additional
     *                      items included in this purchase that need to be recorded.
     * @param purchaseData  Receipt information from Google play. Required, cannot be empty.
     * @param dataSignature The purchase data received from Google Play. Required, cannot be empty.
     */
    public static void iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String purchaseData, String dataSignature) {
        checkInstanceCreated();
        ((ISwrve) instance).iapPlay(productId, productPrice, currency, rewards, purchaseData, dataSignature);
    }

    /**
     * Called to send the push engaged event to Swrve.
     *
     * @param context android context
     * @param pushId  The push id for engagement
     */
    public static void sendPushEngagedEvent(Context context, String pushId) {
        checkInstanceCreated();
        EventHelper.sendEngagedEvent(context, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, pushId, null);
    }

    /**
     * Update the Google Firebase registrationId.
     *
     * @param registrationId Updated registration Id
     */
    public static void setRegistrationId(String registrationId) {
        checkInstanceCreated();
        ((ISwrve) instance).setRegistrationId(registrationId);
    }
}
