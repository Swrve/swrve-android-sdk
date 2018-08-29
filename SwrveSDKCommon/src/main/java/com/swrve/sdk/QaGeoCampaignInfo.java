package com.swrve.sdk;

class QaGeoCampaignInfo {
    QaGeoCampaignInfo(long variantId, boolean displayed, String reason) {
        this.variantId = variantId;
        this.displayed = displayed;
        this.reason = reason;
    }

    long variantId;
    boolean displayed;
    String reason;
}
