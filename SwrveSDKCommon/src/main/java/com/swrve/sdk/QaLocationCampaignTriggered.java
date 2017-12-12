package com.swrve.sdk;

class QaLocationCampaignTriggered {
    QaLocationCampaignTriggered(long id, long variantId, String plotId, boolean displayed, String reason) {
        this.id = id;
        this.variantId = variantId;
        this.plotId = plotId;
        this.displayed = displayed;
        this.reason = reason;
    }

    long id;
    long variantId;
    String plotId;
    boolean displayed;
    String reason;
}
