package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

/**
 * Implement this interface to handle opening custom deeplink.
 */
public interface SwrveDeeplinkListener {

    /**
     * This method will be called when a deeplink is to be opened.
     *
     * @param context Android context.
     * @param uri     Deeplink uri string to open.
     * @param extras  Extras included with the deeplink. Nullable.
     */
    void handleDeeplink(Context context, String uri, Bundle extras);
}
