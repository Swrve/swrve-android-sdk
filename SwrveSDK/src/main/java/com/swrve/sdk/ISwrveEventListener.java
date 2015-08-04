package com.swrve.sdk;

/**
 * Used internally to intercept events raised from the SDK.
 */
public interface ISwrveEventListener {

    void onEvent(String eventName);

}
