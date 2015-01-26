package com.swrve.sdk;

import android.content.Intent;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.gcm.ISwrvePushNotificationListener;

/**
 * Swrve Google SDK interface.
 */
public interface ISwrve extends ISwrveBase<ISwrve, SwrveConfig> {

    /**
     * Set the push notification listener. It is only called if the activity's intent is processed.
     *
     * @param pushNotificationListener
     */
    void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener);

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
     * @param currency      real world currency used for this transaction. This must be an
     *                      ISO currency code. A typical value would be "USD".
     *                      Required, cannot be empty.
     * @param dataSignature The purchase data received from Google Play.
     *                      Required, cannot be empty.
     * @param dataSignature The signature for the purchase data
     *                      Required, cannot be empty.
     */
    void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature);

    /**
     * Add a Swrve.iap event to the event queue. This event should be added
     * for unvalidated real money transactions in the Google Play Store, where in-app currency was purchased
     * or where multiple items and/or currencies were purchased.
     * <p/>
     * To create the rewards object, create an instance of SwrveIAPRewards and
     * use addItem() and addCurrency() to add the individual rewards
     *
     * @param productId     Unique product identifier for the item bought. This should
     *                      match the Swrve resource name.
     *                      Required, cannot be empty.
     * @param productPrice  price of the product in real money. Note that this is the price
     *                      per product, not the total price of the transaction (when quantity > 1)
     *                      A typical value would be 0.99.
     *                      Must be greater or equal to zero.
     * @param currency      real world currency used for this transaction. This must be an
     *                      ISO currency code. A typical value would be "USD".
     *                      Required, cannot be empty.
     * @param rewards       SwrveIAPRewards object containing any in-app currency and/or additional
     *                      items included in this purchase that need to be recorded.
     * @param dataSignature The purchase data received from Google Play.
     *                      Required, cannot be empty.
     * @param dataSignature The signature for the purchase data
     *                      Required, cannot be empty.
     */
    public void iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String purchaseData, String dataSignature);

    /**
     * Process the push notification received from GCM
     * that opened the app. This should be called on
     * the Activity's onCreate that was opened from a
     * push notification.
     *
     * @param intent The intent that opened the activity
     */
    public void processIntent(Intent intent);
}
