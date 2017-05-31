package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

import org.json.JSONObject;

/**
 * Implement this interface to be notified of any Swrve silent push notification to your app.
 */
public interface SwrveSilentPushListener {

    /**
     * This method will be called when a silent push notification is received by your app.
     * @param context The service context
     * @param payload push notification custom payloads.
     */
    void onSilentPush(Context context, JSONObject payload);
}