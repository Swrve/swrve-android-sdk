package com.swrve.sdk.adm;

import com.amazon.device.messaging.ADMMessageReceiver;

public class SwrveAdmMessageReceiver extends ADMMessageReceiver {
    public SwrveAdmMessageReceiver() {
        super(SwrveAdmIntentService.class);
    }
}
