package com.swrve.sdk.messaging;

import androidx.annotation.Nullable;

import java.util.Map;

public interface SwrveMessagePersonalisationProvider {

    /**
     * Invoked when a campaign is getting ready to show and might need personalisation sources.
     *
     * @param eventPayload payload of the event that triggered the campaign, if any
     * @return Personalisation for in-app message
     */
    Map<String, String> personalize(@Nullable Map<String, String> eventPayload);
}
