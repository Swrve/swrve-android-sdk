package com.swrve.sdk.adm;

import com.amazon.device.messaging.ADMMessageReceiver;

//SwrveMessageReceiver listens for messages from ADM
public class SwrveAdmMessageReceiver extends ADMMessageReceiver {
    public SwrveAdmMessageReceiver() {
        super(SwrveAdmIntentService.class);
    }
}
