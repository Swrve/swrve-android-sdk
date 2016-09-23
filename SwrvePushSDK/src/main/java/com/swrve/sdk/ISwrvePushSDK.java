package com.swrve.sdk;

import android.content.Context;
import android.os.Bundle;

interface ISwrvePushSDK {
    String initialisePushSDK(Context context, ISwrvePushSDKListener listener, String senderId);
}

