package com.swrve.sdk;

import com.amazon.device.messaging.ADMMessageReceiver;

public class SwrveAdmMessageReceiver extends ADMMessageReceiver {
    public SwrveAdmMessageReceiver() {
        super(SwrveAdmIntentService.class);
    }
}
