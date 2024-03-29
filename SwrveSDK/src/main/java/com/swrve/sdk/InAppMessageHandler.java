package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_DISMISS;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_NAVIGATION;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_PAGE_VIEW;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_IAM;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_BUTTON_ID;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_BUTTON_NAME;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_PAGE_NAME;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_TO;
import static com.swrve.sdk.SwrveInAppMessageActivity.MESSAGE_ID_KEY;
import static com.swrve.sdk.SwrveInAppMessageActivity.SWRVE_AD_MESSAGE;
import static com.swrve.sdk.SwrveInAppMessageActivity.SWRVE_PERSONALISATION_KEY;
import static com.swrve.sdk.messaging.SwrveActionType.Dismiss;
import static com.swrve.sdk.messaging.SwrveInAppMessageListener.SwrveMessageAction.CopyToClipboard;
import static com.swrve.sdk.messaging.SwrveInAppMessageListener.SwrveMessageAction.Custom;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageButtonDetails;
import com.swrve.sdk.messaging.SwrveMessageDetails;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveInAppMessageListener;
import com.swrve.sdk.messaging.SwrveMessagePage;
import com.swrve.sdk.messaging.SwrveOrientation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class InAppMessageHandler {

    private static final String SAVE_INSTANCE_STATE_CURRENT_PAGE_ID = "CURRENT_PAGE_ID";
    private static final String SAVE_INSTANCE_STATE_SENT_NAVIGATION_EVENTS = "SENT_NAVIGATION_EVENTS";
    private static final String SAVE_INSTANCE_STATE_SENT_PAGEVIEW_EVENTS = "SENT_PAGEVIEW_EVENTS";
    private static final String BACK_BUTTON_NAME = "os_back_button";

    private final SwrveBase sdk;
    private final Context context;
    private final Bundle savedInstanceState;
    protected Map<String, String> inAppPersonalization;
    protected SwrveMessage message;
    protected SwrveMessageFormat format;
    private final List<Long> sentNavigationEvents; // buttonIds
    private final List<Long> sentPageViewEvents; // pageIds
    protected int customEventDelayQueueSeconds = 2;

    InAppMessageHandler(Context context, Intent intent, Bundle savedInstanceState) {
        this.sdk = (SwrveBase) SwrveSDK.getInstance();
        this.context = context;
        this.savedInstanceState = savedInstanceState;

        this.sentNavigationEvents = new ArrayList<>();
        this.sentPageViewEvents = new ArrayList<>();
        restoreInstanceState();

        if (intent == null || intent.getExtras() == null) {
            return;
        }

        Bundle extras = intent.getExtras();
        int messageId = extras.getInt(MESSAGE_ID_KEY);
        this.message = sdk.getMessageForId(messageId);

        // Check if loaded from SwrveDeeplinkManager
        if (message == null && extras.getBoolean(SWRVE_AD_MESSAGE)) {
            this.message = sdk.getAdMesage();
        }

        if (message == null) {
            return;
        }

        // Choose the current orientation. If it is not possible, pick the first one and set the requested orientation.
        SwrveOrientation deviceOrientation = SwrveOrientation.parse(context.getResources().getConfiguration().orientation);
        this.format = message.getFormat(deviceOrientation);
        if (format == null) {
            this.format = message.getFormats().get(0);
        }

        this.inAppPersonalization = (Map<String, String>) extras.getSerializable(SWRVE_PERSONALISATION_KEY);
    }

    protected void notifyOfImpression(SwrveMessageFormat format) {
        sdk.messageWasShownToUser(format);
    }

    protected void buttonClicked(SwrveButton button, String resolvedAction, String resolvedText, long pageId, String pageName) {
        switch (button.getActionType()) {
            case Dismiss:
                dismissButtonClicked(button, pageId, pageName, resolvedText);
                break;
            case Custom:
                customButtonClicked(button, resolvedAction, pageId, pageName, resolvedText);
                break;
            case Install:
                installButtonClicked(button, pageId, pageName);
                break;
            case CopyToClipboard:
                clipboardButtonClicked(button, resolvedAction, pageId, pageName, resolvedText);
                break;
            case RequestCapabilty:
                requestCapabilityButtonClicked(button, resolvedAction, pageId, pageName);
                break;
            case PageLink:
                sendNavigationEvent(pageId, pageName, Long.parseLong(button.getAction()), button.getButtonId(), button.getName());
                break;
            case OpenNotificationSettings:
                openNotificationSettingsButtonClicked(button, pageId, pageName);
                break;
            case OpenAppSettings:
                openAppSettingsButtonClicked(button, pageId, pageName);
                break;
            case StartGeo:
                startGeoButtonClicked();
                break;
            default:
                break;
        }

        queueButtonEvents(button.getEvents());
        queueButtonUserUpdates(button.getUserUpdates());
    }

    private void installButtonClicked(SwrveButton button, long pageId, String pageName) {
        sdk.queueMessageClickEvent(button, pageId, pageName);
        message.getCampaign().messageDismissed();

        String appInstallLink = sdk.getAppStoreURLForApp(button.getAppId());
        // In case the install link was not set correctly log issue and return early
        // without calling the install button listener not starting the install intent
        if (SwrveHelper.isNullOrEmpty(appInstallLink)) {
            SwrveLogger.e("Could not launch install action as there was no app install link found. Please supply a valid app install link.");
            return;
        }
        boolean freeEvent = true;
        if (sdk.getInstallButtonListener() != null) {
            freeEvent = sdk.getInstallButtonListener().onAction(appInstallLink);
        }
        if (freeEvent) {
            // Launch app store
            try {
                Uri uri = Uri.parse(appInstallLink);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (android.content.ActivityNotFoundException anfe) {
                SwrveLogger.e("Couldn't launch install action. No activity found for: %s", anfe, appInstallLink);
            } catch (Exception exp) {
                SwrveLogger.e("Couldn't launch install action for: %s", exp, appInstallLink);
            }
        }
        qaUserCampaignButtonClicked(button.getAction(), button.getActionType(), button.getName());
    }

    private void customButtonClicked(SwrveButton button, String resolvedAction, long pageId, String pageName, String resolvedText) {
        sdk.queueMessageClickEvent(button, pageId, pageName);
        message.getCampaign().messageDismissed();

        if (sdk.getMessageListener() != null) {
            SwrveMessageDetails messageDetails = getMessageDetails(message, format);
            SwrveMessageButtonDetails selectedButton = new SwrveMessageButtonDetails(button.getName(), resolvedText, button.getActionType(), resolvedAction);
            sdk.getMessageListener().onAction(context.getApplicationContext(), Custom, messageDetails, selectedButton);
            // if this listener is implemented we still want to process deeplinks internally. Customer can overide this by implementing the SwrveDeeplinkListener. Note: This is different behaviour to the deprecated getCustomButtonListener below.
            processDeeplink(resolvedAction, button.getName());
        } else if (sdk.getCustomButtonListener() != null) {
            SwrveLogger.d("SwrveSDK: Passing to CustomButtonListener to open: %s", resolvedAction);
            sdk.getCustomButtonListener().onAction(resolvedAction, message.getName());
        } else {
            processDeeplink(resolvedAction, button.getName());
        }

        qaUserCampaignButtonClicked(button.getAction(), button.getActionType(), button.getName());
    }

    private void processDeeplink(String resolvedAction, String buttonName) {
        Bundle bundle = new Bundle();
        bundle.putString("buttonName", buttonName);
        SwrveIntentHelper.openDeepLink(context, resolvedAction, bundle);
    }

    private void clipboardButtonClicked(SwrveButton button, String resolvedAction, long pageId, String pageName, String resolvedText) {
        // resolvedAction is the string that is copied to the clipboard
        sdk.queueMessageClickEvent(button, pageId, pageName);
        message.getCampaign().messageDismissed();

        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("simple text", resolvedAction);
            clipboard.setPrimaryClip(clip);

            if (sdk.getMessageListener() != null) {
                SwrveMessageDetails messageDetails = getMessageDetails(message, format);
                SwrveMessageButtonDetails selectedButton = new SwrveMessageButtonDetails(button.getName(), resolvedText, button.getActionType(), resolvedAction);
                sdk.getMessageListener().onAction(context.getApplicationContext(), CopyToClipboard, messageDetails, selectedButton);
            } else if (sdk.getClipboardButtonListener() != null) {
                sdk.getClipboardButtonListener().onAction(resolvedAction);
            }
        } catch (Exception e) {
            SwrveLogger.e("Couldn't copy text to clipboard: %s", e, resolvedAction);
        }
    }

    private SwrveMessageDetails getMessageDetails(SwrveMessage message, SwrveMessageFormat messageFormat) {
        String subject;
        if (message.getCampaign() != null && message.getCampaign().getMessageCenterDetails() != null) {
            subject = message.getCampaign().getMessageCenterDetails().getSubject();
        } else {
            subject = message.getCampaign().getSubject();
        }

        List<SwrveMessageButtonDetails> buttons = new ArrayList<>();
        for (Map.Entry<Long, SwrveMessagePage> page : messageFormat.getPages().entrySet()) {
            for (SwrveButton messageButton : page.getValue().getButtons()) {

                String personalizedText = messageButton.getText();
                String personalizedAction = messageButton.getAction();
                try {
                    personalizedText = SwrveTextTemplating.apply(personalizedText, inAppPersonalization);
                    personalizedAction = SwrveTextTemplating.apply(personalizedAction, inAppPersonalization);
                } catch (SwrveSDKTextTemplatingException e) {
                    SwrveLogger.e("Failed to resolve personalization in InAppMessageHandler: getMessageDetails");
                }

                SwrveMessageButtonDetails messageButtonDetails = new SwrveMessageButtonDetails(messageButton.getName(), personalizedText, messageButton.getActionType(), personalizedAction);
                buttons.add(messageButtonDetails);
            }
        }
        SwrveMessageDetails messageDetails = new SwrveMessageDetails(subject, message.getCampaign().getId(), message.getId(), message.getName(), buttons);
        return messageDetails;
    }

    private void requestCapabilityButtonClicked(SwrveButton button, String action, long pageId, String pageName) {
        sdk.queueMessageClickEvent(button, pageId, pageName);
        if (SwrveHelper.isNullOrEmpty(action)) {
            SwrveLogger.e("Swrve requestCapabilityButtonClicked but action is null.");
        } else {
            SwrvePermissionRequesterActivity.requestPermission(context, action);
        }
    }

    private void openAppSettingsButtonClicked(SwrveButton button, long pageId, String pageName) {
        sdk.queueMessageClickEvent(button, pageId, pageName);
        Intent intent = SwrveHelper.getAppSettingsIntent(context);
        context.startActivity(intent);
    }

    protected void startGeoButtonClicked() {
        try {
            Class swrveGeoSDK = Class.forName("com.swrve.sdk.geo.SwrveGeoSDK");
            if (swrveGeoSDK != null && context instanceof Activity) {
                Method method = swrveGeoSDK.getMethod("start", Activity.class);
                method.invoke(null, context);
            }
        } catch (ClassNotFoundException e) {
            SwrveLogger.v("SwrveGeoSDK is not integrated.");
        } catch (Exception e) {
            SwrveLogger.e("SwrveGeoSDK could not be started.", e);
        }
    }

    private void openNotificationSettingsButtonClicked(SwrveButton button, long pageId, String pageName) {
        sdk.queueMessageClickEvent(button, pageId, pageName);
        Intent intent = SwrveHelper.getNotificationPermissionSettingsIntent(context);
        context.startActivity(intent);
    }

    protected void backButtonClicked(long currentPageId) {
        long pageId = 0;
        String pageName = "";
        if (format.getPages().containsKey(currentPageId)) {
            SwrveMessagePage page = format.getPages().get(currentPageId);
            pageId = page.getPageId();
            pageName = page.getPageName();
        }
        dismissButtonClicked(null, pageId, pageName, "");
    }

    protected void dismissMessage(long currentPageId, long buttonId, String buttonName) {
        String pageName = "";
        if (format.getPages().containsKey(currentPageId)) {
            pageName = format.getPages().get(currentPageId).getPageName();
        }
        dismissButtonEventsSend(null, buttonId, buttonName, "dismiss", currentPageId, pageName, "");
    }

    private void dismissButtonClicked(SwrveButton button, long pageId, String pageName, String resolvedText) {
        // button can be null if IAM is dismissed via OS see: backButtonClicked
        String buttonName = (button != null) ? button.getName() : BACK_BUTTON_NAME;
        String action = (button != null) ? button.getAction() : "";
        long buttonId = (button != null) ? button.getButtonId() : 0;

        dismissButtonEventsSend(button, buttonId, buttonName, action, pageId, pageName, resolvedText);
    }

    private void dismissButtonEventsSend(SwrveButton button, long buttonId, String buttonName, String buttonAction, long pageId, String pageName, String resolvedText) {
        sendDismissEvent(message.getId(), pageId, pageName, buttonId, buttonName);
        if (sdk.getMessageListener() != null) {
            SwrveMessageButtonDetails swrveMessageButtonDetails = null;
            if (button != null) {
                swrveMessageButtonDetails = new SwrveMessageButtonDetails(button.getName(), resolvedText, button.getActionType(), button.getAction());
            }
            SwrveMessageDetails messageDetails = getMessageDetails(message, format);
            sdk.getMessageListener().onAction(context.getApplicationContext(), SwrveInAppMessageListener.SwrveMessageAction.Dismiss, messageDetails, swrveMessageButtonDetails);
        } else if (sdk.getDismissButtonListener() != null) {
            sdk.getDismissButtonListener().onAction(message.getCampaign().getSubject(), buttonName, message.getName());
        }
        qaUserCampaignButtonClicked(buttonAction, Dismiss, buttonName);
    }

    private void sendDismissEvent(int variantId, long pageId, String pageName, long buttonId, String buttonName) {
        try {
            long time = System.currentTimeMillis();
            String id = String.valueOf(variantId);
            String campaignType = GENERIC_EVENT_CAMPAIGN_TYPE_IAM;
            String actionType = GENERIC_EVENT_ACTION_TYPE_DISMISS;
            String contextId = String.valueOf(pageId);
            String campaignId = "";

            Map<String, Object> payload = new HashMap<>();
            if (SwrveHelper.isNotNullOrEmpty(pageName)) {
                payload.put(GENERIC_EVENT_PAYLOAD_PAGE_NAME, pageName);
            }
            if (SwrveHelper.isNotNullOrEmpty(buttonName)) {
                payload.put(GENERIC_EVENT_PAYLOAD_BUTTON_NAME, buttonName);
            }
            if (buttonId > 0) {
                payload.put(GENERIC_EVENT_PAYLOAD_BUTTON_ID, "" + buttonId);
            }

            int seqNum = sdk.getNextSequenceNumber();
            ArrayList<String> events = EventHelper.createGenericEvent(time, id, campaignType, actionType, contextId, campaignId, payload, seqNum);
            sdk.sendEventsInBackground(context, sdk.getUserId(), events);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK: Could not send dismiss event for id:%s", e, variantId);
        }
    }

    private void sendNavigationEvent(long pageId, String pageName, long pageToId, long buttonId, String buttonName) {
        if (sentNavigationEvents.contains(buttonId)) {
            SwrveLogger.v("SwrveSDK: Navigation event for button_id %s already sent.", buttonId);
            return;
        }
        try {
            long time = System.currentTimeMillis();
            String id = String.valueOf(message.getId());
            String campaignType = GENERIC_EVENT_CAMPAIGN_TYPE_IAM;
            String actionType = GENERIC_EVENT_ACTION_TYPE_NAVIGATION;
            String contextId = String.valueOf(pageId);
            String campaignId = "";

            Map<String, Object> payload = new HashMap<>();
            if (SwrveHelper.isNotNullOrEmpty(pageName)) {
                payload.put(GENERIC_EVENT_PAYLOAD_PAGE_NAME, pageName);
            }
            if (SwrveHelper.isNotNullOrEmpty(buttonName)) {
                payload.put(GENERIC_EVENT_PAYLOAD_BUTTON_NAME, buttonName);
            }
            if (pageToId > 0) {
                payload.put(GENERIC_EVENT_PAYLOAD_TO, pageToId);
            }
            if (buttonId > 0) {
                payload.put(GENERIC_EVENT_PAYLOAD_BUTTON_ID, buttonId);
            }

            int seqNum = sdk.getNextSequenceNumber();
            ArrayList<String> events = EventHelper.createGenericEvent(time, id, campaignType, actionType, contextId, campaignId, payload, seqNum);
            sdk.sendEventsInBackground(context, sdk.getUserId(), events);

            sentNavigationEvents.add(buttonId);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK: Could not send navigation event for id:%s", e, message.getId());
        }
    }

    protected void sendPageViewEvent(long pageId) {
        if (sentPageViewEvents.contains(pageId)) {
            SwrveLogger.v("SwrveSDK: Page view event for page_id %s already sent.", pageId);
            return;
        }
        try {
            long time = System.currentTimeMillis();
            String id = String.valueOf(message.getId());
            String campaignType = GENERIC_EVENT_CAMPAIGN_TYPE_IAM;
            String actionType = GENERIC_EVENT_ACTION_TYPE_PAGE_VIEW;
            String contextId = String.valueOf(pageId);
            String campaignId = "";

            Map<String, Object> payload = new HashMap<>();
            String pageName = format.getPages().get(pageId).getPageName();
            if (SwrveHelper.isNotNullOrEmpty(pageName)) {
                payload.put(GENERIC_EVENT_PAYLOAD_PAGE_NAME, pageName);
            }

            int seqNum = sdk.getNextSequenceNumber();
            ArrayList<String> events = EventHelper.createGenericEvent(time, id, campaignType, actionType, contextId, campaignId, payload, seqNum);
            sdk.sendEventsInBackground(context, sdk.getUserId(), events);

            sentPageViewEvents.add(pageId);
        } catch (Exception e) {
            SwrveLogger.e("SwrveSDK: Could not send page view event for id:%s", e, String.valueOf(message.getId()));
        }
    }

    private void qaUserCampaignButtonClicked(String action, SwrveActionType swrveActionType, String buttonName) {
        if (!QaUser.isLoggingEnabled()) {
            return;
        }
        int campaignId = message.getCampaign().getId();
        int variantId = message.getCampaign().getVariantId();
        String actionType = "";
        switch (swrveActionType) {
            case Install:
                actionType = "install";
                break;
            case Dismiss:
                actionType = "dismiss";
                break;
            case Custom:
                actionType = "deeplink";
                break;
            case CopyToClipboard:
                actionType = "clipboard";
                break;
            case PageLink:
                actionType = ""; // Not supported as an actionType for campaign-button-clicked qalog event yet
                break;
        }
        String actionValue = SwrveHelper.isNullOrEmpty(action) ? actionType : action;
        QaUser.campaignButtonClicked(campaignId, variantId, buttonName, actionType, actionValue);
    }

    public void saveInstanceState(Bundle bundle, long currentPageId) {
        // save current pageId
        bundle.putLong(SAVE_INSTANCE_STATE_CURRENT_PAGE_ID, currentPageId);

        // save list of sent navigation events
        long[] sentNavigationEventsArray = new long[sentNavigationEvents.size()];
        for (int i = 0; i < sentNavigationEvents.size(); i++) {
            sentNavigationEventsArray[i] = sentNavigationEvents.get(i);
        }
        bundle.putLongArray(SAVE_INSTANCE_STATE_SENT_NAVIGATION_EVENTS, sentNavigationEventsArray);

        // save list of sent page view events
        long[] sentPageViewEventsArray = new long[sentPageViewEvents.size()];
        for (int i = 0; i < sentPageViewEvents.size(); i++) {
            sentPageViewEventsArray[i] = sentPageViewEvents.get(i);
        }
        bundle.putLongArray(SAVE_INSTANCE_STATE_SENT_PAGEVIEW_EVENTS, sentPageViewEventsArray);
    }

    private void restoreInstanceState() {
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_INSTANCE_STATE_SENT_NAVIGATION_EVENTS)) {
            long[] sentNavigationEventsArray = savedInstanceState.getLongArray(SAVE_INSTANCE_STATE_SENT_NAVIGATION_EVENTS);
            for (long buttonId : sentNavigationEventsArray) {
                sentNavigationEvents.add(buttonId);
            }
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_INSTANCE_STATE_SENT_PAGEVIEW_EVENTS)) {
            long[] sentPageViewEventsArray = savedInstanceState.getLongArray(SAVE_INSTANCE_STATE_SENT_PAGEVIEW_EVENTS);
            for (long pageId : sentPageViewEventsArray) {
                sentPageViewEvents.add(pageId);
            }
        }
    }

    // If device has been rotated then starting page might not be the first page
    public long getStartingPageId() {
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_INSTANCE_STATE_CURRENT_PAGE_ID)) {
            return savedInstanceState.getLong(SAVE_INSTANCE_STATE_CURRENT_PAGE_ID);
        } else {
            return format.getFirstPageId();
        }
    }

    private void queueButtonEvents(JSONArray events) {
        if (events == null || events.length() == 0) {
            return;
        }

        // Schedule the button events a short period of time later so it can trigger other campaigns
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.schedule(() -> {
            try {
                for (int i = 0; i < events.length(); i++) {
                    JSONObject eventjson = events.getJSONObject(i);
                    String name = eventjson.getString("name");
                    JSONArray payloadArray = new JSONArray();
                    if (eventjson.has("payload")) {
                        payloadArray = eventjson.getJSONArray("payload");
                    }
                    Map<String, String> eventPayload = new HashMap<>();
                    for (int y = 0; y < payloadArray.length(); y++) {
                        JSONObject keyValueJson = payloadArray.getJSONObject(y);
                        Map<String, String> keyValueMap = SwrveHelper.JSONToMap(keyValueJson);
                        addPersonalizedPayload(eventPayload, keyValueMap);
                    }
                    sdk.event(name, eventPayload);
                }
            } catch (Exception e) {
                SwrveLogger.e("Could not queue custom events associated with this button", e);
            } finally {
                scheduledExecutorService.shutdownNow();
            }
        }, customEventDelayQueueSeconds, TimeUnit.SECONDS);
    }

    private void queueButtonUserUpdates(JSONArray userUpdates) {
        if (userUpdates == null || userUpdates.length() == 0) {
            return;
        }
        try {
            Map<String, String> userPayload = new HashMap<>();
            for (int i = 0; i < userUpdates.length(); i++) {
                JSONObject keyValueJson = userUpdates.getJSONObject(i);
                Map<String, String> keyValueMap = SwrveHelper.JSONToMap(keyValueJson);
                addPersonalizedPayload(userPayload, keyValueMap);
            }
            if (userPayload.size() > 0) {
                sdk.userUpdate(userPayload);
            }
        } catch (Exception e) {
            SwrveLogger.e("Could not queue custom user updates associated with this button", e);
        }
    }

    private void addPersonalizedPayload(Map<String, String> newPayload, Map<String, String> payload) {
        String key = payload.get("key");
        String value = payload.get("value");
        String personalizedText;
        try {
            personalizedText = SwrveTextTemplating.apply(value, inAppPersonalization);
            if (!SwrveHelper.isNullOrEmpty(key) && !SwrveHelper.isNullOrEmpty(personalizedText)) {
                newPayload.put(key, personalizedText);
            }
        } catch (SwrveSDKTextTemplatingException e) {
            SwrveLogger.e("Failed to resolve personalization in InAppMessageHandler for key:%s value:%s", key, value);
        }
    }
}
