package com.swrve.sdk.messaging;

import com.swrve.sdk.ISwrveCampaignManager;
import com.swrve.sdk.QaCampaignInfo;
import com.swrve.sdk.SwrveCampaignDisplayer;
import com.swrve.sdk.SwrveLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class SwrveEmbeddedCampaign extends SwrveBaseCampaign {

    protected SwrveEmbeddedMessage message;

    public SwrveEmbeddedCampaign(ISwrveCampaignManager campaignManager, SwrveCampaignDisplayer campaignDisplayer, JSONObject campaignData) throws JSONException {
        super(campaignManager, campaignDisplayer, campaignData);

        if (campaignData.has("embedded_message")) {
            this.message = createMessage(this, campaignData.getJSONObject("embedded_message"));
        }
    }

    @Override
    public boolean supportsOrientation(SwrveOrientation orientation) {
        // there is no orientation defined as part of embedded so this is purely up to the developer whether or not to display it
        return true;
    }

    @Override
    public boolean areAssetsReady(Set<String> assetsOnDisk, Map<String, String> properties) {
        return (this.message.data != null);
    }

    @Override
    public QaCampaignInfo.CAMPAIGN_TYPE getCampaignType() {
        return QaCampaignInfo.CAMPAIGN_TYPE.EMBEDDED;
    }

    protected SwrveEmbeddedMessage createMessage(SwrveEmbeddedCampaign swrveCampaign, JSONObject messageData) throws JSONException {
        return new SwrveEmbeddedMessage(swrveCampaign, messageData);
    }

    public SwrveEmbeddedMessage getMessage() {
        return this.message;
    }

    /**
     * @param event             trigger event
     * @param payload           payload to compare conditions against
     * @param now               device time
     * @param qaCampaignInfoMap will contain the reason the campaign wasn't triggered
     * @return SwrveEmbeddedMessage message setup to the given trigger or null
     * otherwise.
     */
    public SwrveEmbeddedMessage getMessageForEvent(String event, Map<String, String> payload, Date now, Map<Integer, QaCampaignInfo> qaCampaignInfoMap) {
        boolean canShowCampaign = campaignDisplayer.shouldShowCampaign(this, event, payload, now, qaCampaignInfoMap, 1);
        if (canShowCampaign) {
            SwrveLogger.i("%s matches a trigger in %s", event, id);
            return this.getMessage();
        }
        return null;
    }

}
