package com.swrve.sdk.messaging;

import androidx.annotation.Nullable;

import java.util.Map;

public interface SwrveMessagePersonalizationProvider {

    /**
     * Invoked when a campaign is getting ready to show and might need personalization sources.
     *
     * @param eventPayload payload of the event that triggered the campaign, if any
     * @return Personalization for in-app message
     */
    Map<String, String> personalize(@Nullable Map<String, String> eventPayload);
}
