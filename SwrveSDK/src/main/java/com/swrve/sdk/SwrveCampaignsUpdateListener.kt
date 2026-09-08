package com.swrve.sdk

/**
 * Implement this interface to be notified when campaigns change.
 */
fun interface SwrveCampaignsUpdateListener {

    /**
     * Invoked once after the initial content load attempt, whether or not it succeeded or anything changed, and
     * again whenever content the SDK has fetched may have changed the campaigns. Invocations are independent and
     * arrive in no guaranteed order.
     *
     * Campaigns are a snapshot, so re-read them on every invocation rather than holding the previous result. The
     * SDK does not compare against what you last read, so let your own comparison decide whether to redraw. It
     * covers every campaign surface, including Message Center and embedded.
     *
     * Register where the SDK is created: the initial notification is sent once and never replayed, so a listener
     * installed later may miss it.
     *
     * SDK-driven changes only. markMessageCenterCampaignAsSeen and removeMessageCenterCampaign change what the
     * getters return without invoking this, so re-read after your own calls.
     *
     * One of the invocations follows the SDK's attempt to download campaign assets, which is when new campaigns
     * normally become readable — a campaign is not listable until its assets are on disk, and individual downloads
     * can fail, so this is not a promise that everything is present.
     *
     * Note: the thread this is invoked on is not guaranteed. It is the main UI thread when an activity context is available, and the calling thread otherwise.
     */
    fun onCampaignsUpdated()
}
