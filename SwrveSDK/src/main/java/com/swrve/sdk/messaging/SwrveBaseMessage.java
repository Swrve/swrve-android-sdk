package com.swrve.sdk.messaging;

public interface SwrveBaseMessage {

    int getId();

    int getCampaignId();

    int getPriority();

    String getName();

    SwrveBaseCampaign getCampaign();

    boolean supportsOrientation(SwrveOrientation orientation);

    boolean isControl();
}
