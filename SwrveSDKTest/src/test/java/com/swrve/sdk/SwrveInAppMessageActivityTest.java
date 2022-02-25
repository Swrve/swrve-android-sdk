package com.swrve.sdk;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.util.Pair;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.Maps;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveInAppWindowListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.ui.SwrveInAppMessageActivity;
import com.swrve.sdk.messaging.view.SwrveBaseInteractableView;
import com.swrve.sdk.messaging.view.SwrveButtonView;
import com.swrve.sdk.messaging.view.SwrveImageView;
import com.swrve.sdk.messaging.view.SwrveMessageView;
import com.swrve.sdk.messaging.view.SwrveTextImageView;
import com.swrve.sdk.messaging.view.SwrveTextView;
import com.swrve.sdk.messaging.view.SwrveTextViewStyle;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SwrveInAppMessageActivityTest extends SwrveBaseTest {

    private Swrve swrveSpy;
    private String testInstallButtonSuccess;
    private String testCustomButtonSuccess;
    private String testDismissButtonName;
    private boolean testDismissButtonBackButton;

    private SwrveConfig config;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        config = new SwrveConfig();
    }

    private void initSDK() throws Exception {
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey", config);
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        swrveSpy.init(mActivity);
    }

    @Test
    public void testBuildLayoutCreation() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given", new HashMap<String, String>(), SwrveOrientation.Both);
        assertNotNull(message);

        SwrveInAppMessageActivity activity = Robolectric.buildActivity(SwrveInAppMessageActivity.class).create().get();
        SwrveMessageView view = new SwrveMessageView(activity, message, message.getFormats().get(0), 1, new SwrveInAppMessageConfig.Builder().build(), null);
        assertNotNull(view);
        assertEquals(4, view.getChildCount());

        assertEquals(3, getButtonCount(view));
        assertEquals(1, getImageCount(view));
    }

    @Test
    public void testBuildLayoutCreationWithPersonalization() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization.json", "1111111111111111111111111");
        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("show.personalized", new HashMap<>(), SwrveOrientation.Both);

        assertNotNull(message);

        SwrveInAppMessageActivity activity = Robolectric.buildActivity(SwrveInAppMessageActivity.class).create().get();

        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_cp", "shows up");

        SwrveMessageView view = new SwrveMessageView(activity, message, message.getFormats().get(0), 1, new SwrveInAppMessageConfig.Builder().build(), personalization);
        assertNotNull(view);
        assertEquals(4, view.getChildCount());
        assertEquals(2, getPersonalizedButtonCount(view)); // personalization gets counted as buttons
        assertEquals(1, getImageCount(view));
    }

    @Test
    public void testBuildLayoutWithDefaultColor() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().defaultBackgroundColor(Color.RED);
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
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
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
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
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
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
    public void testConfigurationOfButtonFocusClickAndPersonalizationOptions() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder()
                .focusColor(Color.BLUE)
                .clickColor(Color.GREEN)
                .personalizedTextBackgroundColor(Color.RED)
                .personalizedTextForegroundColor(Color.YELLOW)
                .personalizedTextTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization_mc.json", "1111111111111111111111111");

        // Trigger IAM
        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_cp", "test_coupon");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns(personalization);
        swrveSpy.showMessageCenterCampaign(campaigns.get(0), personalization);

        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);

        // Assert image has personalized text
        assertTrue(view.getChildAt(1) instanceof SwrveTextImageView);
        SwrveTextImageView imageView = (SwrveTextImageView) view.getChildAt(1);
        assertEquals("Image test_coupon", imageView.getText());

        // Assert button has personalized text
        assertTrue(view.getChildAt(2) instanceof SwrveTextImageView);
        SwrveTextImageView swrveButtonView = (SwrveTextImageView) view.getChildAt(2);
        assertEquals("Button test_coupon", swrveButtonView.getText());
        assertEquals(Color.GREEN, swrveButtonView.clickColor);
        assertEquals(Color.BLUE, swrveButtonView.focusColor);
        assertEquals(Color.RED, swrveButtonView.inAppConfig.getPersonalizedTextBackgroundColor());
        assertEquals(Color.YELLOW, swrveButtonView.inAppConfig.getPersonalizedTextForegroundColor());
        assertNotNull("Typeface should be set", swrveButtonView.inAppConfig.getPersonalizedTextTypeface());

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
    public void testCustomButtonActionPersonalization() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder()
                .focusColor(Color.BLUE)
                .clickColor(Color.GREEN)
                .personalizedTextBackgroundColor(Color.RED)
                .personalizedTextForegroundColor(Color.YELLOW)
                .personalizedTextTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization_mc.json", "1111111111111111111111111");

        // Trigger IAM
        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_cp", "test_coupon");
        personalization.put("test_deeplink", "user-name");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns(personalization);
        swrveSpy.showMessageCenterCampaign(campaigns.get(0), personalization);

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
    public void testPersonalizationFromRealTimeUserProperties() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder()
                .focusColor(Color.BLUE)
                .clickColor(Color.GREEN)
                .personalizedTextBackgroundColor(Color.RED)
                .personalizedTextForegroundColor(Color.YELLOW)
                .personalizedTextTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();

        HashMap<String, String> rtups = new HashMap<>();
        rtups.put("test_cp", "test_coupon");
        rtups.put("test_deeplink", "user-name");
        swrveSpy.realTimeUserProperties = rtups;

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization_rtups_mc.json", "1111111111111111111111111");

        // Trigger IAM
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0), null);

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
    public void testImagePersonalizationFromMessageCenter() throws Exception {
        initSDK();
        String personalAssetSha1 = SwrveHelper.sha1("https://fakeitem/asset1.png".getBytes());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization_image_mc.json", "1111111111111111111111111", personalAssetSha1);

        // Trigger IAM
        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_cp", "shows up");
        personalization.put("test_image_key", "asset1");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns(personalization);
        swrveSpy.showMessageCenterCampaign(campaigns.get(0), personalization);

        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);
    }

    @Test
    public void testImagePersonalizationFromMessageCenterDoesNotShow() throws Exception {
        initSDK();
        String personalAssetSha1 = SwrveHelper.sha1("https://fakeitem/asset1.png".getBytes());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization_image_mc.json", "1111111111111111111111111", personalAssetSha1);

        // retrieve the list with the correct personalization
        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_cp", "shows up");
        personalization.put("test_image_key", "asset1");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns(personalization);

        // Attempt to Trigger the IAM without the wrong personalization
        HashMap<String, String> wrongPersonalization = new HashMap<>();
        wrongPersonalization.put("test_cp", "shows up");
        swrveSpy.showMessageCenterCampaign(campaigns.get(0), wrongPersonalization);

        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNull(view);

    }

    @Test
    public void testPersonalizedMessageDoesNotShow() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization.json", "1111111111111111111111111");
        swrveSpy.event("currency_given"); // deliberately wrong event

        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNull(view);
    }

    @Test
    public void testRenderView() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        SwrveMessage message = swrveSpy.getMessageForId(165);
        assertNotNull(message);
        SwrveInAppMessageActivity activity = Robolectric.buildActivity(SwrveInAppMessageActivity.class).create().get();
        SwrveMessageView view = new SwrveMessageView(activity, message, message.getFormats().get(0), 1, new SwrveInAppMessageConfig.Builder().build(), null);

        String base64MD5Screenshot = SwrveHelper.md5(SwrveTestUtils.takeScreenshot(view));
        assertNotNull(base64MD5Screenshot);
    }

    @Test
    public void testActivityLifecycle() throws Exception {
        initSDK();
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
        Map<String, Object> payload = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        payload.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testCreateActivityWithNoMessageAndFinishes() throws Exception {
        initSDK();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ConversationActivity.class);
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, intent);
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testOnBackPressed() throws Exception {
        initSDK();
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
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
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
        Map<String, Object> payload = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        payload.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);

        // Swrve.Messages.Message-165.click
        parameters.clear();
        payload.clear();
        parameters.put("name", "Swrve.Messages.Message-165.click");
        payload.put("name", "accept");
        payload.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testInstallButtonLaunchesUrl() throws Exception {
        initSDK();
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
        Map<String, Object> payload = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        payload.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);

        // Swrve.Messages.Message-165.click
        parameters.clear();
        payload.clear();
        parameters.put("name", "Swrve.Messages.Message-165.click");
        payload.put("name", "accept");
        payload.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testClipboardButtonCopiesText() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization_mc.json", "1111111111111111111111111");

        // Trigger IAM
        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_cp", "test_coupon");
        personalization.put("test_1", "test_coupon_in_action");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns(personalization);
        swrveSpy.showMessageCenterCampaign(campaigns.get(0), personalization);

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
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
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
        Map<String, Object> payload = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        payload.put("embedded", "false");

        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);

        // Swrve.Messages.Message-165.click
        parameters.clear();
        payload.clear();
        parameters.put("name", "Swrve.Messages.Message-165.click");
        payload.put("name", "custom");
        payload.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testDismissButtonListenerIntercept() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().dismissButtonListener((campaignSubject, buttonName) -> testDismissButtonName = buttonName);
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
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
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
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

    @Test
    public void testCustomButtonLaunchesUrl() throws Exception {
        initSDK();
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
        Map<String, Object> payload = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        payload.put("embedded", "false");

        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);

        // Swrve.Messages.Message-165.click
        parameters.clear();
        payload.clear();
        parameters.put("name", "Swrve.Messages.Message-165.click");
        payload.put("name", "custom");
        payload.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testButtonFocusability() throws Exception {
        initSDK();
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

    @Test
    public void testPersonalizationProvider() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().personalizationProvider(eventPayload -> {
            if (eventPayload != null && !eventPayload.isEmpty()) {
                Map values = Maps.newHashMap();
                values.put("test_id", "Replaced " + eventPayload.get("payload1"));
                return values;
            } else {
                return null;
            }
        });
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalized_trigger.json", "1111111111111111111111111");

        // Trigger IAM
        Map<String, String> eventPayload = Maps.newHashMap();
        eventPayload.put("payload1", "payloadValue");
        swrveSpy.event("trigger_iam", eventPayload);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);

        assertTrue(view.getChildAt(2) instanceof SwrveTextImageView);
        SwrveTextImageView imageView = (SwrveTextImageView) view.getChildAt(2);
        assertEquals("Replaced payloadValue value", imageView.getText());
    }

    @Test
    public void testMessageCenterListWithTextPersonalization() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization_mc.json", "1111111111111111111111111");

        assertEquals(0, swrveSpy.getMessageCenterCampaigns().size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape).size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Portrait).size());

        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_cp", "shows up");

        assertEquals(1, swrveSpy.getMessageCenterCampaigns(personalization).size());
        assertEquals(1, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape, personalization).size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Portrait, personalization).size());

        HashMap<String, String> other = new HashMap<>();
        other.put("other_value", "shows up");
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(other).size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape, other).size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Portrait, other).size());
    }

    @Test
    public void testPersonalizationProviderWithMultiLineView() throws Exception {
        Typeface tf = Typeface.create("sans-serif-thin", Typeface.NORMAL);
        SwrveInAppMessageConfig inAppConfigBuilder = new SwrveInAppMessageConfig.Builder()
                .personalizationProvider(eventPayload -> {
                    HashMap<String, String> values = Maps.newHashMap();
                    values.put("test_line", "newline");
                    return values;
                })
                .personalizedTextBackgroundColor(Color.RED)
                .personalizedTextForegroundColor(Color.BLUE)
                .defaultBackgroundColor(Color.BLUE)
                .personalizedTextTypeface(tf)
                .build();
        config.setInAppMessageConfig(inAppConfigBuilder);
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_multiline_trigger.json", "1111111111111111111111111");

        // Trigger IAM
        swrveSpy.event("trigger_iam");
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);

        assertTrue(view.getChildAt(0) instanceof SwrveTextView);
        SwrveTextView textView = (SwrveTextView) view.getChildAt(0);
        assertEquals("triggered multiline Text \\n with a newline.", textView.getText().toString());

        assertEquals(textView.getCurrentTextColor(), Color.BLUE);
        ColorDrawable cd = (ColorDrawable) textView.getBackground();
        int colorCode = cd.getColor();
        assertEquals(colorCode, Color.RED);
        assertEquals(textView.getTypeface(), tf);
    }

    @Test
    public void testMessageCenterListWithMultiLineTextView() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_multiline_mc.json", "1111111111111111111111111");

        assertEquals(0, swrveSpy.getMessageCenterCampaigns().size());

        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_line", "newline");

        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns(personalization);
        swrveSpy.showMessageCenterCampaign(campaigns.get(0), personalization);

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);

        assertTrue(view.getChildAt(0) instanceof SwrveTextView);
        SwrveTextView textView = (SwrveTextView) view.getChildAt(0);
        assertEquals("Multiline Text \\n with a newline.", textView.getText().toString());

        assertEquals(15, textView.getPaddingBottom());
        assertEquals(15, textView.getPaddingTop());
        assertEquals(15, textView.getPaddingStart());
        assertEquals(15, textView.getPaddingEnd());
        assertEquals(1, textView.getTextAlignment());

        // the mutliper in the json is 1.5 but we convert this to line spacing.
        assertEquals(1, textView.getLineSpacingMultiplier(),0.1);

        assertEquals(Color.parseColor( "#00FF00"), ((ColorDrawable) textView.getBackground()).getColor());
        assertEquals(Color.parseColor("#0000FF"), textView.getCurrentTextColor());
    }

    @Test
    public void testMessageCenterListWithImagePersonalization() throws Exception {
        initSDK();
        String personalAssetSha1 = SwrveHelper.sha1("https://fakeitem/asset1.png".getBytes());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization_image_mc.json", "1111111111111111111111111", personalAssetSha1);

        // no personalization values provided
        assertEquals(0, swrveSpy.getMessageCenterCampaigns().size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape).size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Portrait).size());

        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_cp", "shows up");
        personalization.put("test_image_key", "asset1");

        assertEquals(1, swrveSpy.getMessageCenterCampaigns(personalization).size());
        assertEquals(1, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape, personalization).size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Portrait, personalization).size());

        HashMap<String, String> other = new HashMap<>();
        other.put("test_cp", "shows up");
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(other).size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape, other).size());
        assertEquals(0, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Portrait, other).size());
    }

    @Test
    public void testDynamicImageWithPersonalizationProvider() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().personalizationProvider(eventPayload -> {
            Map values = Maps.newHashMap();
            values.put("test_key_with_fallback", "asset1");
            values.put("test_key_no_fallback", "asset2");
            // third value has fallback in the url text
            return values;
        });
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();

        String testAsset1 = SwrveHelper.sha1(("https://fakeitem/asset1.png").getBytes());
        String testAsset2 = SwrveHelper.sha1(("https://fakeitem/asset2.gif").getBytes());
        String testAsset3 = SwrveHelper.sha1(("https://fakeitem/asset3.jpg").getBytes());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalized_image_trigger.json", "1111111111111111111111111", testAsset1, testAsset2, testAsset3);

        // Trigger IAM
        Map<String, String> eventPayload = Maps.newHashMap();
        //eventPayload.put("payload1", "payloadValue");
        swrveSpy.event("trigger_iam", eventPayload);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);

        // ensure that it loaded the right dynamic views
        assertTrue(view.getChildAt(0) instanceof SwrveImageView);
        SwrveImageView imageView = (SwrveImageView) view.getChildAt(0);
        assertEquals(ImageView.ScaleType.FIT_CENTER, imageView.getScaleType());
        assertTrue("dynamic image url images should have adjustViewBounds Enabled", imageView.getAdjustViewBounds());

        assertTrue(view.getChildAt(1) instanceof SwrveButtonView);
        SwrveButtonView buttonView = (SwrveButtonView) view.getChildAt(1);
        assertEquals(ImageView.ScaleType.FIT_CENTER, buttonView.getScaleType());
        assertTrue("dynamic image url images should have adjustViewBounds Enabled", buttonView.getAdjustViewBounds());

        assertTrue(view.getChildAt(2) instanceof SwrveButtonView);
        buttonView = (SwrveButtonView) view.getChildAt(2);
        assertEquals(ImageView.ScaleType.FIT_CENTER, buttonView.getScaleType());
        assertTrue("dynamic image url images should have adjustViewBounds Enabled", buttonView.getAdjustViewBounds());

        assertTrue(view.getChildAt(3) instanceof SwrveButtonView);
        buttonView = (SwrveButtonView) view.getChildAt(3);
        assertEquals(ImageView.ScaleType.FIT_XY, buttonView.getScaleType());
        assertFalse("regular images should have adjustViewBounds Disabled", buttonView.getAdjustViewBounds());

    }

    @Test
    public void testDynamicImageWithPersonalizationProviderImageFallback() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().personalizationProvider(eventPayload -> {
            Map values = Maps.newHashMap();
            // have no value for test_key_with_fallback
            values.put("test_key_no_fallback", "asset2");
            return values;
        });
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
        String testAsset1 = SwrveHelper.sha1(("https://fakeitem/asset1.gif").getBytes());
        String testAsset2 = SwrveHelper.sha1(("https://fakeitem/asset2.gif").getBytes());
        String testAsset3 = SwrveHelper.sha1(("https://fakeitem/asset3.jpg").getBytes());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalized_image_trigger.json", "1111111111111111111111111", testAsset1, testAsset2, testAsset3);

        // Trigger IAM
        Map<String, String> eventPayload = Maps.newHashMap();
        //eventPayload.put("payload1", "payloadValue");
        swrveSpy.event("trigger_iam", eventPayload);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);

        // message still appears with fallback image as background
        assertTrue(view.getChildAt(0) instanceof SwrveImageView);
        SwrveImageView imageView = (SwrveImageView) view.getChildAt(0);
        assertEquals(ImageView.ScaleType.FIT_XY, imageView.getScaleType());
        assertFalse("regular images should have adjustViewBounds Disabled", imageView.getAdjustViewBounds());

        assertTrue(view.getChildAt(1) instanceof SwrveButtonView);
        SwrveButtonView buttonView = (SwrveButtonView) view.getChildAt(1);
        assertEquals(ImageView.ScaleType.FIT_CENTER, buttonView.getScaleType());
        assertTrue("dynamic image url images should have adjustViewBounds Enabled", buttonView.getAdjustViewBounds());

        assertTrue(view.getChildAt(2) instanceof SwrveButtonView);
        buttonView = (SwrveButtonView) view.getChildAt(2);
        assertEquals(ImageView.ScaleType.FIT_CENTER, buttonView.getScaleType());
        assertTrue("dynamic image url images should have adjustViewBounds Enabled", buttonView.getAdjustViewBounds());

        assertTrue(view.getChildAt(3) instanceof SwrveButtonView);
        buttonView = (SwrveButtonView) view.getChildAt(3);
        assertEquals(ImageView.ScaleType.FIT_XY, buttonView.getScaleType());
        assertFalse("regular images should have adjustViewBounds Disabled", buttonView.getAdjustViewBounds());
    }

    @Test
    public void testDynamicImageWithPersonalizationProviderImageMissingAssetFallback() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().personalizationProvider(eventPayload -> {
            Map values = Maps.newHashMap();
            values.put("test_key_with_fallback", "asset1");
            values.put("test_key_no_fallback", "asset2");
            return values;
        });
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();

        // purposefully do not have testAsset1 in the cache
        String testAsset2 = SwrveHelper.sha1(("https://fakeitem/asset2.gif").getBytes());
        String testAsset3 = SwrveHelper.sha1(("https://fakeitem/asset3.jpg").getBytes());
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalized_image_trigger.json", "1111111111111111111111111", testAsset2, testAsset3);

        // Trigger IAM
        Map<String, String> eventPayload = Maps.newHashMap();
        //eventPayload.put("payload1", "payloadValue");
        swrveSpy.event("trigger_iam", eventPayload);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
        assertNotNull(view);

        // message still appears with fallback image as background
        assertTrue(view.getChildAt(0) instanceof SwrveImageView);
        SwrveImageView imageView = (SwrveImageView) view.getChildAt(0);
        assertEquals(ImageView.ScaleType.FIT_XY, imageView.getScaleType());
        assertFalse("regular images should have adjustViewBounds Disabled", imageView.getAdjustViewBounds());

        assertTrue(view.getChildAt(1) instanceof SwrveButtonView);
        SwrveButtonView buttonView = (SwrveButtonView) view.getChildAt(1);
        assertEquals(ImageView.ScaleType.FIT_CENTER, buttonView.getScaleType());
        assertTrue("dynamic image url images should have adjustViewBounds Enabled", buttonView.getAdjustViewBounds());

        assertTrue(view.getChildAt(2) instanceof SwrveButtonView);
        buttonView = (SwrveButtonView) view.getChildAt(2);
        assertEquals(ImageView.ScaleType.FIT_CENTER, buttonView.getScaleType());
        assertTrue("dynamic image url images should have adjustViewBounds Enabled", buttonView.getAdjustViewBounds());

        assertTrue(view.getChildAt(3) instanceof SwrveButtonView);
        buttonView = (SwrveButtonView) view.getChildAt(3);
        assertEquals(ImageView.ScaleType.FIT_XY, buttonView.getScaleType());
        assertFalse("regular images should have adjustViewBounds Disabled", buttonView.getAdjustViewBounds());

    }

    @Test
    public void testMessageBothOrientationsPortrait() throws Exception {

        RuntimeEnvironment.setQualifiers("+port");
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_both_orientations.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        swrveSpy.event("trigger_iam");
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        assertEquals(SwrveOrientation.Portrait, activity.getFormat().getOrientation());
        // No requested orientation, as we can flip
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.getRequestedOrientation());
    }

    @Test
    public void testMessageBothOrientationsLandscape() throws Exception {

        RuntimeEnvironment.setQualifiers("+land");
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_both_orientations.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        swrveSpy.event("trigger_iam");
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        assertEquals(SwrveOrientation.Landscape, activity.getFormat().getOrientation());
        // No requested orientation, as we can flip
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.getRequestedOrientation());
    }

    @Test
    public void testMessageOnlyFormatForPortrait() throws Exception {

        RuntimeEnvironment.setQualifiers("+port");
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_portrait_orientation.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        swrveSpy.event("trigger_iam");
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT, activity.getRequestedOrientation());
        } else {
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, activity.getRequestedOrientation());
        }
    }

    @Test
    public void testMessageOnlyFormatForLandscape() throws Exception {

        RuntimeEnvironment.setQualifiers("+land");
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_landscape_orientation.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        swrveSpy.event("trigger_iam");
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE, activity.getRequestedOrientation());
        } else {
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.getRequestedOrientation());
        }
    }

    @Test
    public void testMessageHideToolbarDisabled() throws Exception {

        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().hideToolbar(false);
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);
        assertEquals(R.style.Theme_InAppMessageWithToolbar, getThemeResourceId(activity));
    }

    @Test
    public void testWindowListener() throws Exception {

        final AtomicBoolean callback = new AtomicBoolean(false);
        SwrveInAppWindowListener windowListener = window -> {
            callback.set(true);
        };

        SwrveInAppMessageConfig inAppConfig = new SwrveInAppMessageConfig.Builder()
                .windowListener(windowListener)
                .build();
        config.setInAppMessageConfig(inAppConfig);
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");

        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        await().untilTrue(callback);
    }

    // Helpers

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
                SwrveBaseInteractableView swrveBaseInteractableView = (SwrveBaseInteractableView) childView;
                if (swrveBaseInteractableView.getType() == actionType) {
                    return swrveBaseInteractableView;
                }
            }
        }
        Assert.fail("Could not find custom button");
        return null;
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

    private int getPersonalizedButtonCount(SwrveMessageView view) {
        int buttons = 0;
        for (int i = 0; i < view.getChildCount(); i++) {
            Class<?> viewClass = view.getChildAt(i).getClass();
            if (viewClass == SwrveTextImageView.class) {
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

    private int getThemeResourceId(Activity activity) {
        String TAG = "SwrveInAppMessageActivityTest";
        int themeResId = 0;
        try {
            Class<?> clazz = Context.class;
            Method method = clazz.getMethod("getThemeResId");
            method.setAccessible(true);
            themeResId = (Integer) method.invoke(activity);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Failed to get theme resource ID", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Failed to get theme resource ID", e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to get theme resource ID", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Failed to get theme resource ID", e);
        }
        return themeResId;
    }
}
