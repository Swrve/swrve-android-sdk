package com.swrve.sdk;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.app.Activity;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedMessageListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveOrientation;

import java.util.List;
import java.util.Map;

/**
 * This class will trigger a swrve campaign if available.
 */
class SwrveEventListener implements ISwrveEventListener {

    private final SwrveBase<?, ?> sdk;
    private final SwrveEmbeddedMessageListener embeddedMessageListener;

    public SwrveEventListener(SwrveBase<?, ?> sdk, SwrveEmbeddedMessageListener embeddedMessageListener) {
        this.sdk = sdk;
        this.embeddedMessageListener = embeddedMessageListener;
    }

    @Override
    public void onEvent(String eventName, Map<String, String> payload) {
        if (SwrveHelper.isNullOrEmpty(eventName)) {
            return;
        }

        handleNotificationPermissionEvents(sdk.getActivityContext(), eventName);

        SwrveConversation conversation = sdk.getConversationForEvent(eventName, payload);
        if (conversation != null) {
            ConversationActivity.showConversation(sdk.getContext(), conversation, sdk.config.getOrientation());
            conversation.getCampaign().messageWasShownToUser();
            QaUser.campaignTriggeredMessageNoDisplay(eventName, payload);
            return;
        }

        SwrveOrientation deviceOrientation = SwrveOrientation.parse(sdk.getContext().getResources().getConfiguration().orientation);
        SwrveBaseMessage message = sdk.getBaseMessageForEvent(eventName, payload, deviceOrientation);
        if (message != null) {
            sdk.lastEventPayloadUsed = payload; // Save the last used payload for personalization
            if (message instanceof SwrveMessage) {
                sdk.displaySwrveMessage((SwrveMessage) message, null);
            } else if (message instanceof SwrveEmbeddedMessage && embeddedMessageListener != null) {
                Map<String, String> personalizationProperties = sdk.retrievePersonalizationProperties(payload, null);
                embeddedMessageListener.onMessage(sdk.getContext(), (SwrveEmbeddedMessage) message, personalizationProperties);
            }
            sdk.lastEventPayloadUsed = null; // Remove ref
        }
    }

    private void handleNotificationPermissionEvents(Activity activity, String eventName) {
        if (sdk.config.getNotificationConfig() == null) {
            return;
        }
        List<String> notificationPermissionEvents = sdk.config.getNotificationConfig().getPushNotificationPermissionEvents();
        if (SwrveHelper.isNullOrEmpty(eventName) || notificationPermissionEvents == null || activity == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(activity, POST_NOTIFICATIONS) == PERMISSION_DENIED) {
            for (String notificationPermissionEvent : notificationPermissionEvents) {
                if (notificationPermissionEvent.equals(eventName)) {
                    int notificationPermissionAnsweredTimes = sdk.getPermissionAnsweredTime(POST_NOTIFICATIONS);
                    if (notificationPermissionAnsweredTimes >= 2) {
                        SwrveLogger.v("SwrveSDK: notificationPermissionEvent triggered, however already showed notification prompt twice so do not attempt to show again.");
                        return;
                    } else {
                        SwrveLogger.v("SwrveSDK: notificationPermissionEvent triggered. Attempting to show notification permission prompt.");
                        requestNotificationPermission(activity);
                        return;
                    }
                }
            }
        }
    }

    // exposed for testing
    protected void requestNotificationPermission(Activity activity) {
        SwrvePermissionRequesterActivity.requestPermission(activity, POST_NOTIFICATIONS);
    }
}
