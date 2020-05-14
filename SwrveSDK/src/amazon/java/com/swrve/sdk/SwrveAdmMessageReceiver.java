package com.swrve.sdk;

import com.amazon.device.messaging.ADMMessageReceiver;

public class SwrveAdmMessageReceiver extends ADMMessageReceiver {
    private static Boolean ADMLatestAvailable;
    private static final int JOB_ID = 459352;

    public SwrveAdmMessageReceiver() {
        // This is needed for backward compatibility
        super(SwrveAdmIntentService.class);

        // Where available, prefer using the new job based
        if (checkADMLatestAvailable()) {
            registerJobServiceClass(SwrveAdmHandlerJobService.class, JOB_ID);
        }
    }

    protected static boolean checkADMLatestAvailable() {
        if (ADMLatestAvailable == null) {
            ADMLatestAvailable = false;
            try {
                Class.forName("com.amazon.device.messaging.ADMMessageHandlerJobBase");
                ADMLatestAvailable = true;
            } catch (ClassNotFoundException ignored) {
            }
        }
        return ADMLatestAvailable;
    }
}
