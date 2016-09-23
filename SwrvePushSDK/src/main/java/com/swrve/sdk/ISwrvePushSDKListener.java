package com.swrve.sdk;

import android.os.Bundle;

public interface ISwrvePushSDKListener {

    void onPushTokenUpdated(String pushToken);

    void onMessageReceived(String msgId, Bundle msg);

    void onNotificationEngaged(Bundle msg);
}
