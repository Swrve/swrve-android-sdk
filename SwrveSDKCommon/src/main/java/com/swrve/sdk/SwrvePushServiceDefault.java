package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_SENT_TIME;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Map;

@Deprecated
public class SwrvePushServiceDefault {

    /**
     * This method should be used when multiple push providers are integrated and Swrve's default
     * push implementation is not being used. See samples directory in the public repository on how
     * to use it.
     *
     * @param context   A context
     * @param data      A map containing swrve push payload. For firebase this is the remoteMessage.getData().
     * @param messageId For firebase this is the remoteMessage.getMessageId().
     * @param sentTime  For firebase this is the remoteMessage.getSentTime().
     * @return true if it was a swrve push, false if it was another push provider and should be handled by the caller.
     * @deprecated Instead use SwrveSDK.handleSwrvePush(Context context, Map<String, String> data, String messageId, long sentTime)
     */
    @Deprecated
    public static boolean handle(Context context, Map<String, String> data, String messageId, long sentTime) {
        if (data == null) {
            return false;
        }
        data.put(GENERIC_EVENT_PAYLOAD_MSG_ID, messageId);
        data.put(GENERIC_EVENT_PAYLOAD_SENT_TIME, String.valueOf(sentTime));
        SwrvePushWorkerHelper workerHelper = new SwrvePushWorkerHelper(context, SwrvePushManagerWorker.class, data);
        return workerHelper.handle();
    }
}
