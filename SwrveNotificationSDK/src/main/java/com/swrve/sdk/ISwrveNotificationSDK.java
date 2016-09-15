package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

interface ISwrveNotificationSDK {
    String initialiseNotificationSDK(Context context);

    void setNotificationListener(ISwrveNotificationListener listener);

    void showNotification(Context context, Bundle data);
}

