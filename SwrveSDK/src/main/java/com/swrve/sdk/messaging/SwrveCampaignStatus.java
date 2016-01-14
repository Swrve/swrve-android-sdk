package com.swrve.sdk.messaging;

/**
 * Used for device orientation and specifying orientation filters.
 */
public enum SwrveCampaignStatus {
    Unseen, Seen, Deleted;

    /**
     * Convert from String to SwrveCampaignStatus.
     *
     * @param status String campaign status.
     * @return SwrveCampaignStatus
     */
    public static SwrveCampaignStatus parse(String status) {
        if (status.equalsIgnoreCase("seen")) {
            return SwrveCampaignStatus.Seen;
        } else if (status.equalsIgnoreCase("deleted")) {
            return SwrveCampaignStatus.Deleted;
        }

        return SwrveCampaignStatus.Unseen;
    }
}
