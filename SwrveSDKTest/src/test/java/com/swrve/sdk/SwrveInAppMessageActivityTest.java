package com.swrve.sdk;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.ui.SwrveInAppMessageActivity;
import com.swrve.sdk.messaging.view.SwrveBaseInteractableView;
import com.swrve.sdk.messaging.view.SwrveButtonView;
import com.swrve.sdk.messaging.view.SwrveImageView;
import com.swrve.sdk.messaging.view.SwrveMessageView;
import com.swrve.sdk.messaging.view.SwrvePersonalisedTextView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
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
    private String testDismissButtonName;
    private boolean testDismissButtonBackButton;

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
        SwrveMessageView view = new SwrveMessageView(activity, message, message.getFormats().get(0), 1, new SwrveInAppMessageConfig.Builder().build(), null);
        assertNotNull(view);
        assertEquals(4, view.getChildCount());

        assertEquals(3, getButtonCount(view));
        assertEquals(1, getImageCount(view));
    }

    @Test
    public void testBuildLayoutCreationWithPersonalisation() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalisation.json", "1111111111111111111111111");
        SwrveMessage message = swrveSpy.getMessageForEvent("show.personalised", new HashMap<>(), SwrveOrientation.Both);

        assertNotNull(message);

        SwrveInAppMessageActivity activity = Robolectric.buildActivity(SwrveInAppMessageActivity.class).create().get();

        HashMap<String, String> personalisation = new HashMap<>();
        personalisation.put("test_cp", "shows up");

        SwrveMessageView view = new SwrveMessageView(activity, message, message.getFormats().get(0), 1, new SwrveInAppMessageConfig.Builder().build(), personalisation);
        assertNotNull(view);
        assertEquals(4, view.getChildCount());
        assertEquals(2, getPersonalisedButtonCount(view)); // personalisation gets counted as buttons
        assertEquals(1, getImageCount(view));
    }

    @Test
    public void testBuildLayoutWithDefaultColor() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().defaultBackgroundColor(Color.RED);
        swrveSpy.config.setInAppMessageConfig(inAppConfigBuilder.build());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");

        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);
        assertEquals(Color.RED, ((ColorDrawable) view.getBackground()).getColor());
    }

    @Test
    public void testBuildLayoutColor() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().defaultBackgroundColor(Color.BLUE);
        swrveSpy.config.setInAppMessageConfig(inAppConfigBuilder.build());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_color.json", "1111111111111111111111111");

        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);
        assertEquals(Color.parseColor("#EC9D78"), ((ColorDrawable) view.getBackground()).getColor());
    }

    @Test
    public void testConfigurationOfButtonFocusClickColors() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().focusColor(Color.BLUE).clickColor(Color.GREEN);
        swrveSpy.config.setInAppMessageConfig(inAppConfigBuilder.build());
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
    }

    @Test
    public void testConfigurationOfButtonFocusClickAndPersonalisationOptions() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder()
                .focusColor(Color.BLUE)
                .clickColor(Color.GREEN)
                .personalisedTextBackgroundColor(Color.RED)
                .personalisedTextForegroundColor(Color.YELLOW)
                .personalisedTextTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        swrveSpy.config.setInAppMessageConfig(inAppConfigBuilder.build());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalisation_mc.json", "1111111111111111111111111");

        // Trigger IAM
        HashMap<String, String> personalisation = new HashMap<>();
        personalisation.put("test_cp", "test_coupon");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0), personalisation);

        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);

        // Assert image has personalised text
        assertTrue(view.getChildAt(1) instanceof SwrvePersonalisedTextView);
        SwrvePersonalisedTextView imageView = (SwrvePersonalisedTextView) view.getChildAt(1);
        assertEquals("Image test_coupon", imageView.getText());

        // Assert button has personalised text
        assertTrue(view.getChildAt(2) instanceof SwrvePersonalisedTextView);
        SwrvePersonalisedTextView swrveButtonView = (SwrvePersonalisedTextView) view.getChildAt(2);
        assertEquals("Button test_coupon", swrveButtonView.getText());
        assertEquals(Color.GREEN, swrveButtonView.clickColor);
        assertEquals(Color.BLUE, swrveButtonView.focusColor);
        assertEquals(Color.RED, swrveButtonView.inAppConfig.getPersonalisedTextBackgroundColor());
        assertEquals(Color.YELLOW, swrveButtonView.inAppConfig.getPersonalisedTextForegroundColor());
        assertNotNull("Typeface should be set", swrveButtonView.inAppConfig.getPersonalisedTextTypeface());

        // Assert button to clipboard
        assertTrue(view.getChildAt(3) instanceof SwrveButtonView);
        SwrveButtonView swrveC2CButtonView = (SwrveButtonView) view.getChildAt(3);
        assertEquals(SwrveActionType.CopyToClipboard, swrveC2CButtonView.getType());
        assertEquals("hello", swrveC2CButtonView.getAction());

        // Assert custom button
        assertTrue(view.getChildAt(4) instanceof SwrveButtonView);
        SwrveButtonView swrveCustomButtonView = (SwrveButtonView) view.getChildAt(4);
        assertEquals(SwrveActionType.Custom, swrveCustomButtonView.getType());
        assertEquals("http://www.google.com?a=hello_other", swrveCustomButtonView.getAction());
    }

    @Test
    public void testCustomButtonActionPersonalisation() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder()
                .focusColor(Color.BLUE)
                .clickColor(Color.GREEN)
                .personalisedTextBackgroundColor(Color.RED)
                .personalisedTextForegroundColor(Color.YELLOW)
                .personalisedTextTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        swrveSpy.config.setInAppMessageConfig(inAppConfigBuilder.build());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalisation_mc.json", "1111111111111111111111111");

        // Trigger IAM
        HashMap<String, String> personalisation = new HashMap<>();
        personalisation.put("test_cp", "test_coupon");
        personalisation.put("test_deeplink", "user-name");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0), personalisation);

        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);
        assertTrue(view.getChildAt(4) instanceof SwrveButtonView);
        SwrveButtonView swrveButtonView = (SwrveButtonView) view.getChildAt(4);

        assertEquals("http://www.google.com?a=user-name", swrveButtonView.getAction());
    }

    @Test
    public void testRenderView() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        SwrveMessage message = swrveSpy.getMessageForId(165);
        assertNotNull(message);
        SwrveInAppMessageActivity activity = Robolectric.buildActivity(SwrveInAppMessageActivity.class).create().get();
        SwrveMessageView view = new SwrveMessageView(activity, message, message.getFormats().get(0), 1, new SwrveInAppMessageConfig.Builder().build(), null);

        String base64MD5Screnshot = SwrveHelper.md5(SwrveTestUtils.takeScreenshot(view));
        assertNotNull(base64MD5Screnshot);
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
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().installButtonListener(appStoreUrl -> {
            testInstallButtonSuccess = appStoreUrl;
            return false;
        });
        swrveSpy.config.setInAppMessageConfig(inAppConfigBuilder.build());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);

        // Press install button
        SwrveButtonView swrveButtonView = findButton(view, SwrveActionType.Install);
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
        SwrveButtonView swrveButtonView = findButton(view, SwrveActionType.Install);
        swrveButtonView.performClick();

        // Detect intent from url
        String expectedUrl = swrveSpy.getAppStoreURLForApp(150);
        Intent nextIntent = mShadowActivity.getNextStartedActivity();
        assertNotNull(nextIntent);
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
    public void testClipboardButtonCopiesText() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalisation_mc.json", "1111111111111111111111111");

        // Trigger IAM
        HashMap<String, String> personalisation = new HashMap<>();
        personalisation.put("test_cp", "test_coupon");
        personalisation.put("test_1", "test_coupon_in_action");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0), personalisation);

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);

        // Press clipboard button
        ImageView swrveButtonView = findButtonWithAction(view, SwrveActionType.CopyToClipboard);
        swrveButtonView.performClick();

        // Verify button copies text to system clipboard
        ClipboardManager clipboard = (ClipboardManager) activity.getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
        try {

            // Make sure we have something in the clipboard
            ClipData clipData = clipboard.getPrimaryClip();
            Assert.assertNotNull(clipData);

            // Make sure its what was passed into the properties
            String clipboardContents = clipData.getItemAt(0).getText().toString();
            Assert.assertEquals(clipboardContents, "test_coupon_in_action");

        } catch (Exception e) {
            Assert.fail("clipboard.getPrimaryClip() threw Exception: " + e.toString());
        }
    }

    @Test
    public void testCustomButtonListenerIntercept() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().customButtonListener(customAction -> {
            testCustomButtonSuccess = customAction;
        });
        swrveSpy.config.setInAppMessageConfig(inAppConfigBuilder.build());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);

        // Press custom button
        SwrveButtonView swrveButtonView = findButton(view, SwrveActionType.Custom);
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
    public void testDismissButtonListenerIntercept() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().dismissButtonListener((campaignSubject, buttonName) -> testDismissButtonName = buttonName);
        swrveSpy.config.setInAppMessageConfig(inAppConfigBuilder.build());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);

        // Press dismiss button
        SwrveButtonView swrveButtonView = findButton(view, SwrveActionType.Dismiss);
        swrveButtonView.performClick();

        assertEquals("close", testDismissButtonName);
    }

    @Test
    public void testDismissButtonListenerInterceptBackButton() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().dismissButtonListener((campaignSubject, buttonName) -> {
            if (buttonName == null) {
                testDismissButtonBackButton = true;
            }
        });
        swrveSpy.config.setInAppMessageConfig(inAppConfigBuilder.build());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        // Press back button
        activity.onBackPressed();

        assertTrue(testDismissButtonBackButton);
    }

    private SwrveButtonView findButton(SwrveMessageView view, SwrveActionType actionType) {
        for (int i = 0; i < view.getChildCount(); i++) {
            View childView = view.getChildAt(i);
            if (childView instanceof SwrveButtonView) {
                SwrveButtonView swrveButtonView = (SwrveButtonView) childView;
                if (swrveButtonView.getType() == actionType) {
                    return swrveButtonView;
                }
            }
        }
        Assert.fail("Could not find custom button");
        return null;
    }

    private ImageView findButtonWithAction(SwrveMessageView view, SwrveActionType actionType) {
        for (int i = 0; i < view.getChildCount(); i++) {
            View childView = view.getChildAt(i);
            if (childView instanceof SwrveBaseInteractableView) {
                SwrveBaseInteractableView swrvePersonalisedTextView = (SwrveBaseInteractableView) childView;
                if (swrvePersonalisedTextView.getType() == actionType) {
                    return swrvePersonalisedTextView;
                }
            }
        }
        Assert.fail("Could not find custom button");
        return null;
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
        SwrveButtonView swrveButtonView = findButton(view, SwrveActionType.Custom);
        swrveButtonView.performClick();

        // Detect intent from url
        Intent nextIntent = mShadowActivity.getNextStartedActivity();
        assertNotNull(nextIntent);
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

    private int getPersonalisedButtonCount(SwrveMessageView view) {
        int buttons = 0;
        for (int i = 0; i < view.getChildCount(); i++) {
            Class<?> viewClass = view.getChildAt(i).getClass();
            if (viewClass == SwrvePersonalisedTextView.class) {
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
        assertNotNull(intent);
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, intent);
        return new Pair(activityController, activityController.create().start().visible().get());
    }

    /**
     * Look through the arguments sent to queueEvent and search for an event of certain name. Presume this is a unique event.
     *
     * @param eventName  The event name
     * @param parameters Map conating the event name
     * @param payload    Map of payload parameters
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
