package com.swrve.sdk;

import android.os.Bundle;

public class SwrvePushHelper {
    public static void qaUserPushNotification(Bundle msg) {
        String pushId = SwrveHelper.getRemotePushId(msg);
        if(SwrveHelper.isNotNullOrEmpty(pushId)) {
            pushId = SwrveHelper.getSilentPushId(msg);
        }
        QaUser.pushNotification(pushId, msg);
    }
}
