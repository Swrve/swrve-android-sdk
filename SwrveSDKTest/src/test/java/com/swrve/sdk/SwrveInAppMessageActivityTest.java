package com.swrve.sdk;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.ui.SwrveInAppMessageActivity;
import com.swrve.sdk.messaging.view.SwrveButtonView;
import com.swrve.sdk.messaging.view.SwrveImageView;
import com.swrve.sdk.messaging.view.SwrveMessageView;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowView;
import org.robolectric.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SwrveInAppMessageActivityTest extends SwrveBaseTest {

    private Swrve swrveSpy;
    private String testInstallButtonSuccess;
    private String testCustomButtonSuccess;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        swrveSpy.init(mActivity);
    }

    @Test
    public void testBuildLayoutCreation() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        SwrveMessage message = swrveSpy.getMessageForEvent("Swrve.currency_given", new HashMap<String, String>(), SwrveOrientation.Both);
        assertNotNull(message);

        SwrveInAppMessageActivity activity = Robolectric.buildActivity(SwrveInAppMessageActivity.class).create().get();
        SwrveMessageView view = new SwrveMessageView(activity, message, message.getFormats().get(0), 1, 0, 0, 0);
        assertNotNull(view);
        assertEquals(4, view.getChildCount());

        assertEquals(3, getButtonCount(view));
        assertEquals(1, getImageCount(view));
        view.destroy();
    }

    @Test
    public void testBuildLayoutWithDefaultColor() throws Exception {
        swrveSpy.config.setDefaultBackgroundColor(Color.RED);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");

        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);
        ShadowView shadowView = Shadows.shadowOf(view);
        assertEquals(Color.RED, shadowView.getBackgroundColor());
        view.destroy();
    }

    @Test
    public void testBuildLayoutColor() throws Exception {
        swrveSpy.config.setDefaultBackgroundColor(Color.BLUE);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_color.json", "1111111111111111111111111");

        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);
        ShadowView shadowView = Shadows.shadowOf(view);
        assertEquals(Color.parseColor("#EC9D78"), shadowView.getBackgroundColor());
        view.destroy();
    }

    @Test
    public void testConfigurationOfButtonFocusClickColors() throws Exception {
        swrveSpy.config.setInAppMessageClickColor(Color.GREEN);
        swrveSpy.config.setInAppMessageFocusColor(Color.BLUE);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_color.json", "1111111111111111111111111");

        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);
        assertTrue(view.getChildAt(1) instanceof SwrveButtonView);
        SwrveButtonView swrveButtonView = (SwrveButtonView) view.getChildAt(1);

        assertEquals(Color.GREEN, swrveButtonView.clickColor);
        assertEquals(Color.BLUE, swrveButtonView.focusColor);
        view.destroy();
    }


    @Test
    public void testRenderView() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        SwrveMessage message = swrveSpy.getMessageForId(165);
        assertNotNull(message);
        SwrveInAppMessageActivity activity = Robolectric.buildActivity(SwrveInAppMessageActivity.class).create().get();
        SwrveMessageView view = new SwrveMessageView(activity, message, message.getFormats().get(0), 1, 0, 0, 0);

        String base64MD5Screnshot = SwrveHelper.md5(SwrveTestUtils.takeScreenshot(view));
        assertNotNull(base64MD5Screnshot);
        view.destroy();
    }

    @Test
    public void testActivityLifecycle() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        ActivityController<SwrveInAppMessageActivity> activityController = pair.first;
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        activityController.resume();
        assertFalse(activity.isFinishing());
        activityController.pause().stop().destroy();

        // Swrve.Messages.Message-165.impression
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        Map<String, String> payload = new HashMap<>();
        payload.put("format", "Kindle (English (US))");
        payload.put("orientation", "Landscape");
        payload.put("size", "320x240");
        assertQueueEvent("Swrve.Messages.Message-165.impression", parameters, payload);
    }

    @Test
    public void testCreateActivityWithNoMessageAndFinishes() throws Exception {
        Intent intent = new Intent(RuntimeEnvironment.application, ConversationActivity.class);
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, intent);
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testOnBackPressed() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        assertFalse(activity.isFinishing());
        activity.onBackPressed();
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testInstallButtonListenerIntercept() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);

        swrveSpy.setInstallButtonListener(new SwrveInstallButtonListener() {
            @Override
            public boolean onAction(String appStoreUrl) {
                testInstallButtonSuccess = appStoreUrl;
                return false;
            }
        });

        // Press install button
        boolean seekingButton = true;
        SwrveButtonView swrveButtonView = null;
        for (int i = 0; i < view.getChildCount() && seekingButton; i++) {
            View childView = view.getChildAt(i);
            if (childView instanceof SwrveButtonView) {
                swrveButtonView = (SwrveButtonView) childView;
                if (swrveButtonView.getType() == SwrveActionType.Install) {
                    seekingButton = false;
                }
            }
        }
        assertNotNull("Could not find install button", swrveButtonView);
        swrveButtonView.performClick();

        String expectedUrl = swrveSpy.getAppStoreURLForApp(150);
        assertNotNull(expectedUrl);
        assertEquals(expectedUrl, testInstallButtonSuccess);

        // Swrve.Messages.Message-165.impression
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        Map<String, String> payload = new HashMap<>();
        payload.put("format", "Kindle (English (US))");
        payload.put("orientation", "Landscape");
        payload.put("size", "320x240");
        assertQueueEvent("Swrve.Messages.Message-165.impression", parameters, payload);

        // Swrve.Messages.Message-165.click
        parameters.clear();
        parameters.put("name", "Swrve.Messages.Message-165.click");
        payload.clear();
        payload.put("name", "accept");
        assertQueueEvent("Swrve.Messages.Message-165.click", parameters, payload);
    }

    @Test
    public void testInstallButtonLaunchesUrl() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);

        // Press install button
        boolean seekingButton = true;
        SwrveButtonView swrveButtonView = null;
        for (int i = 0; i < view.getChildCount() && seekingButton; i++) {
            View childView = view.getChildAt(i);
            if (childView instanceof SwrveButtonView) {
                swrveButtonView = (SwrveButtonView) childView;
                if (swrveButtonView.getType() == SwrveActionType.Install) {
                    seekingButton = false;
                }
            }
        }
        assertNotNull("Could not find install button", swrveButtonView);
        swrveButtonView.performClick();

        // Detect intent from url
        String expectedUrl = swrveSpy.getAppStoreURLForApp(150);
        Intent nextStartedActivity = mShadowActivity.getNextStartedActivity(); // for some reason this line is needed before calling peekNextStartedActivity?
        assertNotNull(nextStartedActivity);
        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        assertEquals(expectedUrl, nextIntent.getDataString());

        // Swrve.Messages.Message-165.impression
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        Map<String, String> payload = new HashMap<>();
        payload.put("format", "Kindle (English (US))");
        payload.put("orientation", "Landscape");
        payload.put("size", "320x240");
        assertQueueEvent("Swrve.Messages.Message-165.impression", parameters, payload);

        // Swrve.Messages.Message-165.click
        parameters.clear();
        parameters.put("name", "Swrve.Messages.Message-165.click");
        payload.clear();
        payload.put("name", "accept");
        assertQueueEvent("Swrve.Messages.Message-165.click", parameters, payload);
    }

    @Test
    public void testCustomButtonListenerIntercept() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);

        swrveSpy.setCustomButtonListener(new SwrveCustomButtonListener() {
            @Override
            public void onAction(String customAction) {
                testCustomButtonSuccess = customAction;
            }
        });

        // Press custom button
        boolean seekingButton = true;
        SwrveButtonView swrveButtonView = null;
        for (int i = 0; i < view.getChildCount() && seekingButton; i++) {
            View childView = view.getChildAt(i);
            if (childView instanceof SwrveButtonView) {
                swrveButtonView = (SwrveButtonView) childView;
                if (swrveButtonView.getType() == SwrveActionType.Custom) {
                    seekingButton = false;
                }
            }
        }
        assertNotNull("Could not find custom button", swrveButtonView);
        swrveButtonView.performClick();

        assertEquals("custom_action", testCustomButtonSuccess);

        // Swrve.Messages.Message-165.impression
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        Map<String, String> payload = new HashMap<>();
        payload.put("format", "Kindle (English (US))");
        payload.put("orientation", "Landscape");
        payload.put("size", "320x240");
        assertQueueEvent("Swrve.Messages.Message-165.impression", parameters, payload);

        // Swrve.Messages.Message-165.click
        parameters.clear();
        parameters.put("name", "Swrve.Messages.Message-165.click");
        payload.clear();
        payload.put("name", "custom");
        assertQueueEvent("Swrve.Messages.Message-165.click", parameters, payload);
    }

    @Test
    public void testCustomButtonLaunchesUrl() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);

        // Press custom button
        boolean seekingButton = true;
        SwrveButtonView swrveButtonView = null;
        for (int i = 0; i < view.getChildCount() && seekingButton; i++) {
            View childView = view.getChildAt(i);
            if (childView instanceof SwrveButtonView) {
                swrveButtonView = (SwrveButtonView) childView;
                if (swrveButtonView.getType() == SwrveActionType.Custom) {
                    seekingButton = false;
                }
            }
        }
        assertNotNull("Could not find custom button", swrveButtonView);
        swrveButtonView.performClick();

        // Detect intent from url
        Intent nextStartedActivity = mShadowActivity.getNextStartedActivity(); // for some reason this line is needed before calling peekNextStartedActivity?
        assertNotNull(nextStartedActivity);
        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        assertEquals("custom_action", nextIntent.getDataString());


        // Swrve.Messages.Message-165.impression
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        Map<String, String> payload = new HashMap<>();
        payload.put("format", "Kindle (English (US))");
        payload.put("orientation", "Landscape");
        payload.put("size", "320x240");
        assertQueueEvent("Swrve.Messages.Message-165.impression", parameters, payload);

        // Swrve.Messages.Message-165.click
        parameters.clear();
        parameters.put("name", "Swrve.Messages.Message-165.click");
        payload.clear();
        payload.put("name", "custom");
        assertQueueEvent("Swrve.Messages.Message-165.click", parameters, payload);
    }

    @Test
    public void testButtonFocusability() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = (ViewGroup) activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);

        SwrveButtonView swrveButtonView;
        for (int i = 0; i < view.getChildCount(); i++) {
            View childView = view.getChildAt(i);
            if (childView instanceof SwrveButtonView) {
                swrveButtonView = (SwrveButtonView) childView;
                assertTrue(swrveButtonView.isFocusable());
                Log.d("testButtonFocusability()", "Button " + i + " is focusable");
            }
        }
    }

    private int getButtonCount(SwrveMessageView view) {
        int buttons = 0;
        for (int i = 0; i < view.getChildCount(); i++) {
            Class<?> viewClass = view.getChildAt(i).getClass();
            if (viewClass == SwrveButtonView.class) {
                buttons++;
            }
        }
        return buttons;
    }

    private int getImageCount(SwrveMessageView view) {
        int images = 0;
        for (int i = 0; i < view.getChildCount(); i++) {
            Class<?> viewClass = view.getChildAt(i).getClass();
            if (viewClass == SwrveImageView.class) {
                images++;
            }
        }
        return images;
    }

    private Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> createActivityFromPeekIntent(Intent intent) {
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, intent);
        return new Pair(activityController, activityController.create().start().visible().get());
    }

    /**
     * Look through the arguments sent to queueEvent and search for an event of certain name. Presume this is a unique event.
     * @param eventName The event name
     * @param parameters Map conating the event name
     * @param payload Map of payload parameters
     */
    private void assertQueueEvent(String eventName, Map<String, Object> parameters, Map<String, String> payload) {

        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> parametersCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Boolean> triggerEventListenerCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(swrveSpy, Mockito.atLeastOnce()).queueEvent(userIdCaptor.capture(), eventTypeCaptor.capture(), parametersCaptor.capture(), payloadCaptor.capture(), triggerEventListenerCaptor.capture());

        List<String> userIds = userIdCaptor.getAllValues();
        List<String> capturedEventTypes = eventTypeCaptor.getAllValues();
        List<Map> capturedParameters = parametersCaptor.getAllValues();
        List<Map> capturedPayload = payloadCaptor.getAllValues();

        // Find index of correct event that was called, assuming the event is unique
        int index = -1;
        for (int i = 0; i < capturedEventTypes.size(); i++) {
            if (capturedEventTypes.get(i).equals("event")) {
                Map<String, Object> capturedParametersMap = capturedParameters.get(i);
                if (capturedParametersMap.containsKey("name")) {
                    String obj = (String) capturedParametersMap.get("name");
                    if (eventName.equals(obj)) {
                        index = i;
                    }
                }
            }
        }
        assertTrue(index > -1);
        assertEquals(swrveSpy.getUserId(), userIds.get(index));
        assertEquals("event", capturedEventTypes.get(index));
        Map capturedParametersMap = capturedParameters.get(index);
        assertEquals(parameters, capturedParametersMap);
        Map capturedPayloadMap = capturedPayload.get(index);
        assertEquals(payload, capturedPayloadMap);
    }
}
