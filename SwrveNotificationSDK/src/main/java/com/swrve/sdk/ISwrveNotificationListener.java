package com.swrve.sdk;

import android.os.Bundle;

public interface ISwrveNotificationListener {

    void onRegistrationIdUpdated(String registrationId);

    void onMessageReceived(Bundle msg);

    void onPushEngaged(Bundle msg);
}
