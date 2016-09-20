package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

interface ISwrvePushSDK {
    String initialisePushSDK(Context context);

    void setPushSDKListener(ISwrvePushSDKListener listener);

    void showNotification(Context context, Bundle data);
}

