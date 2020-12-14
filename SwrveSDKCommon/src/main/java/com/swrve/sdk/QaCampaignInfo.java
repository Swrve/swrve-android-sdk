package com.swrve.sdk;

public class QaCampaignInfo {

    public enum CAMPAIGN_TYPE {
        IAM {
            public String toString() {
                return "iam";
            }
        },
        CONVERSATION {
            public String toString() {
                return "conversation";
            }
        },
        EMBEDDED {
            public String toString() { return "embedded"; }
        }
    }

    final long id;
    final long variantId;
    final CAMPAIGN_TYPE type;
    final boolean displayed;
    final String reason;

    public QaCampaignInfo(long id, long variantId, CAMPAIGN_TYPE type, boolean displayed, String reason) {
        this.id = id;
        this.variantId = variantId;
        this.type = type;
        this.displayed = displayed;
        this.reason = reason;
    }
}
