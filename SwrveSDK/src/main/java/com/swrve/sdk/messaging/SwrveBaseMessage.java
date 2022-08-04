package com.swrve.sdk.messaging;

public interface SwrveBaseMessage {

    int getId();

    int getPriority();

    String getName();

    SwrveBaseCampaign getCampaign();

    boolean supportsOrientation(SwrveOrientation orientation);
}
