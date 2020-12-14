package com.swrve.sdk.messaging;

public interface SwrveBaseMessage {

    int getId();

    int getPriority();

    SwrveBaseCampaign getCampaign();

    boolean supportsOrientation(SwrveOrientation orientation);
}
