package com.swrve.sdk;

import android.content.Intent;
import android.os.Bundle;

public interface SwrveNotificationIntentListener {
    /**
     * Called when a notification is rendered and used to launch an Activity when notification is
     * engaged. Use this to configure the main activity to start and apply custom intent flags
     * and extras. If not configured then the SDK will apply default.
     *
     * @param pushBundle The push notification bundle and custom properties.
     * @param deeplink The deeplink that was pressed. Null if engage was not a deeplink.
     * @return Ensure a valid Activity Intent is returned that can launched when the notification is engaged with.
     */
    Intent onNotificationEngage(Bundle pushBundle, String deeplink);
}
