package com.swrve.sdk;

import java.util.Map;

/**
 * Used internally to intercept events raised from the SDK.
 */
public interface ISwrveEventListener {

    void onEvent(String eventName, Map<String, String> payload);
}
