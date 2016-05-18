package com.swrve.sdk.messaging;

import com.swrve.sdk.SwrveLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Used internally to save the state of campaigns
 */
public class SwrveCampaignState {
    protected static final String LOG_TAG = "SwrveMessagingSDK";

    /**
     * The status of the campaign
     */
    public enum Status {
        Unseen, Seen, Deleted;

        /**
         * Convert from String to SwrveCampaignStatus.
         *
         * @param status String campaign status.
         * @return SwrveCampaignStatus
         */
        public static Status parse(String status) {
            if (status.equalsIgnoreCase("seen")) {
                return Status.Seen;
            } else if (status.equalsIgnoreCase("deleted")) {
                return Status.Deleted;
            }

            return Status.Unseen;
        }
    }

    // Number of impressions of this campaign. Used to disable the campaign if it reaches total impressions
    protected int impressions;

    // MessageCenter status of the campaign
    protected Status status;

    // Next message to be shown if round robin campaign
    protected int next;

    // Time we can show the next message
    // Will be based on time previous message was shown + minDelayBetweenMessage
    public Date showMessagesAfterDelay;

    public SwrveCampaignState() {
        impressions = 0;
        status = Status.Unseen;
        next = 0;
    }

    public SwrveCampaignState(JSONObject state) {
        this();

        try {
            if (state.has("next")) {
                this.next = state.getInt("next");
            }

            if (state.has("impressions")) {
                this.impressions = state.getInt("impressions");
            }

            if (state.has("status")) {
                this.status = Status.parse(state.getString("status"));
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error while trying to load campaign settings", e);
        }
    }

    public int getImpressions() {
        return impressions;
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
