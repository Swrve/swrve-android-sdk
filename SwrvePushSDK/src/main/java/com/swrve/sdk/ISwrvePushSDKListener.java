package com.swrve.sdk;

import android.os.Bundle;

public interface ISwrvePushSDKListener {

    void onRegistrationIdUpdated(String registrationId);

    void onMessageReceived(String msgId, Bundle msg);

    void onNotificationEngaged(Bundle msg);
}
