package com.swrve.sdk.messaging;

import com.swrve.sdk.SwrveLogger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used internally to save the state of campaigns
 */
public class SwrveCampaignState {
    protected static final String LOG_TAG = "SwrveMessagingSDK";

    // Number of impressions of this campaign. Used to disable the campaign if
    // it reaches total impressions
    protected int impressions;

    // Inbox status of the campaign
    protected SwrveCampaignStatus status;

    // Next message to be shown if round robin campaign
    protected int next;

    public SwrveCampaignState() {
        impressions = 0;
        status = SwrveCampaignStatus.Unseen;
        next = 0;
    }

    public SwrveCampaignState(JSONObject state) {
        try {
            if (state.has("next")) {
                this.next = state.getInt("next");
            }

            if (state.has("impressions")) {
                this.impressions = state.getInt("impressions");
            }

            if (state.has("status")) {
                this.status = SwrveCampaignStatus.valueOf(state.getString("status"));
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error while trying to load campaign settings", e);
        }
    }

    /**
     * Serialize the campaign state.
     *
     * @return JSONObject State for the campaign.
     * @throws org.json.JSONException
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject state = new JSONObject();
        state.put("next", next);
        state.put("impressions", impressions);
        state.put("status", status.toString());
        return state;
    }
}
