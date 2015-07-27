package com.swrve.sdk;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Reperesents the rewards given to a user after a purchase.
 */
public class SwrveIAPRewards {
    protected static final String LOG_TAG = "SwrveSDK";

    /**
     * Stores the content (reward currency + reward items) of the IAP.
     */
    protected Map<String, Map<String, Object>> rewards;

    public SwrveIAPRewards() {
        this.rewards = new HashMap<String, Map<String, Object>>();
    }

    /**
     * Create a currency reward.
     * @param currencyName name of the currency as specified on the dashboard.
     * @param amount amount to be given.
     */
    public SwrveIAPRewards(String currencyName, long amount) {
        this.rewards = new HashMap<String, Map<String, Object>>();
        this.addCurrency(currencyName, amount);
    }

    protected void _addItem(String resourceName, long quantity) {
        this.addObject(resourceName, quantity, "item");
    }

    protected void _addCurrency(String currencyName, long amount) {
        this.addObject(currencyName, amount, "currency");
    }

    protected JSONObject _getRewardsJSON() throws JSONException {
        JSONObject json = new JSONObject();

        for (Map.Entry<String, Map<String, Object>> entry : this.rewards.entrySet()) {
            json.put(entry.getKey(), new JSONObject(entry.getValue()));
        }

        return json;
    }

    protected void addObject(String name, long quantity, String type) {
        if (checkParameters(name, quantity, type)) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("amount", quantity);
            item.put("type", type);
            this.rewards.put(name, item);
        }
    }

    protected boolean checkParameters(String name, long quantity, String type) throws IllegalArgumentException {
        if (SwrveHelper.isNullOrEmpty(type)) {
            Log.e(LOG_TAG, "SwrveIAPRewards illegal argument: type cannot be empty");
            return false;
        }
        if (SwrveHelper.isNullOrEmpty(name)) {
            Log.e(LOG_TAG, "SwrveIAPRewards illegal argument: reward name cannot be empty");
            return false;
        }
        if (quantity <= 0) {
            Log.e(LOG_TAG, "SwrveIAPRewards illegal argument: reward amount must be greater than zero");
            return false;
        }

        return true;
    }

    /**
     * Add a resource reward.
     * @param resourceName name of the resource as specified on the dashboard.
     * @param quantity quantity to be given.
     */
    public void addItem(String resourceName, long quantity) {
        try {
            _addItem(resourceName, quantity);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    /**
     * Add currency reward.
     * @param currencyName name of the currency as specified on the dashboard.
     * @param amount amount to be given.
     */
    public void addCurrency(String currencyName, long amount) {
        try {
            _addCurrency(currencyName, amount);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
    }

    /**
     * Convert the reward to JSON for its usage by the SDK.
     * @return JSON reward.
     */
    public JSONObject getRewardsJSON() {
        try {
            return _getRewardsJSON();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception thrown in Swrve SDK", e);
        }
        return null;
    }
}