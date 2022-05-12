package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.EVENT_TYPE_GENERIC_CAMPAIGN;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_DISMISS;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_NAVIGATION;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_PAGE_VIEW;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_IAM;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.common.collect.Maps;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveBaseInteractableView;
import com.swrve.sdk.messaging.SwrveButtonView;
import com.swrve.sdk.messaging.SwrveImageView;
import com.swrve.sdk.messaging.SwrveInAppWindowListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageView;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.SwrveTextImageView;
import com.swrve.sdk.messaging.SwrveTextView;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
        doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        doNothing().when(swrveSpy).sendEventsInBackground(any(Context.class), anyString(), any(ArrayList.class));
        swrveSpy.init(mActivity);
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

        SwrveMessageView view = getSwrveMessageView(activity);
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

        SwrveMessageView view = getSwrveMessageView(activity);
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

        SwrveMessageView view = getSwrveMessageView(activity);
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
        SwrveMessageView view = getSwrveMessageView(activity);
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

        SwrveMessageView view = getSwrveMessageView(activity);
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

        SwrveMessageView view = getSwrveMessageView(activity);
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

        SwrveMessageView view = getSwrveMessageView(activity);
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
        SwrveMessageView view = getSwrveMessageView(activity);
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

        SwrveMessageView view = getSwrveMessageView(activity);

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
    public void testMultiPageNoSwipe() throws Exception {

        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "multipage_campaign_no_swipe.json", "asset1", "asset2", "asset3", "asset4", "asset5", "asset_install");
        // Trigger IAM
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> eventsCaptor = ArgumentCaptor.forClass(ArrayList.class);

        SwrveMessageView view = getSwrveMessageView(activity);
        assertEquals(123, view.getPage().getPageId());

        verify(swrveSpy, times(1)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());
        assertPageViewEvent(eventsCaptor, 0, "123", "page1");

        // Navigate to page 2
        view = clickButton(activity, view, SwrveActionType.PageLink);
        assertEquals(456, view.getPage().getPageId());

        view = getSwrveMessageView(activity);
        assertEquals(456, view.getPage().getPageId());

        // Swrve.Messages.Message-165.impression
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        payload.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);

        // Press dismiss button
        clickButton(activity, view, SwrveActionType.Dismiss);

        assertTrue(activity.isFinishing());

        // Dismiss button event
        verify(swrveSpy, times(4)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());
        ArrayList events = eventsCaptor.getValue(); // latest value
        JSONObject event = new JSONObject((String) events.get(0));
        Map<String, String> expectedPayload = new HashMap<>();
        expectedPayload.put("buttonName", "close");
        expectedPayload.put("pageName", "page2");
        expectedPayload.put("buttonId", "999");
        SwrveTestUtils.assertGenericEvent(event.toString(), "456", GENERIC_EVENT_CAMPAIGN_TYPE_IAM, GENERIC_EVENT_ACTION_TYPE_DISMISS, expectedPayload);
    }

    @Test
    public void testMultiPageEventsOnlyOnce() throws Exception {

        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "multipage_campaign_no_swipe.json", "asset1", "asset2", "asset3", "asset4", "asset5", "asset_install");
        // Trigger IAM
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> eventsCaptor = ArgumentCaptor.forClass(ArrayList.class);

        SwrveMessageView view = getSwrveMessageView(activity);
        assertEquals(123, view.getPage().getPageId());

        verify(swrveSpy, times(1)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());
        assertPageViewEvent(eventsCaptor, 0, "123", "page1");

        reset(swrveSpy);

        // Navigate to page 2
        view = clickButton(activity, view, SwrveActionType.PageLink);
        assertEquals(456, view.getPage().getPageId());

        // sendEventsInBackground 2 times: navigation, pageView
        verify(swrveSpy, times(2)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());
        assertNavigationEvent(eventsCaptor, 1, "123", "page1", 111l, 456l);
        assertPageViewEvent(eventsCaptor, 2, "456", "page2");

        reset(swrveSpy);

        // Navigate back to page 1
        view = clickButton(activity, view, SwrveActionType.PageLink);
        assertEquals(123, view.getPage().getPageId());

        verify(swrveSpy, times(1)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());
        assertNavigationEvent(eventsCaptor, 3, "456", "page2", 222l, 123l);

        reset(swrveSpy);

        // Navigate to page 2
        view = clickButton(activity, view, SwrveActionType.PageLink);
        assertEquals(456, view.getPage().getPageId());

        // no more page view or navigation events
        verify(swrveSpy, never()).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());

        // Navigate back to page 1
        view = clickButton(activity, view, SwrveActionType.PageLink);
        assertEquals(123, view.getPage().getPageId());

        // no more page view or navigation events
        verify(swrveSpy, never()).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());
    }

    @Test
    public void testMultiPageButtonClick() throws Exception {

        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "multipage_campaign_no_swipe.json", "asset1", "asset2", "asset3", "asset4", "asset5", "asset_install");
        // Trigger IAM
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        SwrveMessageView view = getSwrveMessageView(activity);
        assertEquals(123, view.getPage().getPageId());

        // Press install button
        clickButton(activity, view, SwrveActionType.Install);

        assertTrue(activity.isFinishing());

        // Swrve.Messages.Message-165.impression
        Map<String, Object> parametersImpression = new HashMap<>();
        Map<String, Object> payloadImpression = new HashMap<>();
        parametersImpression.put("name", "Swrve.Messages.Message-165.impression");
        payloadImpression.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parametersImpression, payloadImpression);

        // Swrve.Messages.Message-165.click
        Map<String, Object> parametersClick = new HashMap<>();
        Map<String, Object> payloadClick = new HashMap<>();
        parametersClick.put("name", "Swrve.Messages.Message-165.click");
        payloadClick.put("embedded", "false");
        payloadClick.put("name", "install button");
        payloadClick.put("contextId", "123");
        payloadClick.put("pageName", "page1");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parametersClick, payloadClick);
    }

    @Test
    public void testMultiPageSwipe() throws Exception {

        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "multipage_campaign_swipe.json", "asset1", "asset2", "asset3", "asset4", "asset5");
        // Trigger IAM
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        SwrveInAppMessageActivity.ScreenSlidePagerAdapter adapter = activity.adapter;
        assertEquals(2, adapter.trunk.size());

        SwrveMessageView view = getSwrveMessageView(activity);
        assertEquals(123, view.getPage().getPageId());

        int page1Index = activity.viewPager2.getCurrentItem();
        assertEquals(0, page1Index);

        // Press page link button
        view = clickButton(activity, view, SwrveActionType.PageLink);

        int page2Index = activity.viewPager2.getCurrentItem();
        assertEquals(1, page2Index);

        // Swrve.Messages.Message-165.impression
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        payload.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);
    }

    @Test
    public void testMultiPageSwipeDismiss() throws Exception {

        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "multipage_campaign_swipe.json", "asset1", "asset2", "asset3", "asset4", "asset5");
        // Trigger IAM
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        SwrveInAppMessageActivity.ScreenSlidePagerAdapter adapter = (SwrveInAppMessageActivity.ScreenSlidePagerAdapter)activity.adapter;
        assertEquals(2, adapter.trunk.size());

        SwrveMessageView view = getSwrveMessageView(activity);
        assertEquals(123, view.getPage().getPageId());

        int page1Index = activity.viewPager2.getCurrentItem();
        assertEquals(0, page1Index);

        // Swrve.Messages.Message-165.impression
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> payload = new HashMap<>();
        parameters.put("name", "Swrve.Messages.Message-165.impression");
        payload.put("embedded", "false");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, payload);

        // Press dismiss button
        clickButton(activity, view, SwrveActionType.Dismiss);

        assertTrue(activity.isFinishing());

        // Dismiss button event
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> events = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, Mockito.atLeastOnce()).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), events.capture());

        ArrayList dismissEvent = events.getValue(); // get the last event
        Map<String, String> expectedPayload = new HashMap<>();
        Map<String, String> expectedPayloadDismiss = new HashMap<>();
        expectedPayloadDismiss.put("buttonName", "close");
        expectedPayloadDismiss.put("pageName", "page2");
        SwrveTestUtils.assertGenericEvent((String) dismissEvent.get(0), "123", GENERIC_EVENT_CAMPAIGN_TYPE_IAM, GENERIC_EVENT_ACTION_TYPE_DISMISS, expectedPayload);
    }

    @Test
    public void testMultiPageCircularLoop() throws Exception {

        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "multipage_campaign_circular_loop.json", "asset1", "asset2", "asset3", "asset4", "asset5");
        // Trigger IAM
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        assertTrue(activity.isFinishing());
    }

    @Test
    public void testMultiPageRotateDevice() throws Exception {

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> eventsCaptor = ArgumentCaptor.forClass(ArrayList.class);

        // start in portrait mode
        RuntimeEnvironment.setQualifiers("+port");

        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "multipage_both_orientation.json", "asset1");
        // Trigger IAM
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        SwrveMessageView view = getSwrveMessageView(activity);
        assertEquals(123, view.getPage().getPageId());
        assertEquals("PortraitFormat", activity.inAppMessageHandler.format.getName());

        // Navigate to page 2
        clickButton(activity, view, SwrveActionType.PageLink);
        view = getSwrveMessageView(activity);
        assertEquals(456, view.getPage().getPageId());

        // sendEventsInBackground 3 times: pageView, navigation, pageView
        verify(swrveSpy, times(3)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());
        assertPageViewEvent(eventsCaptor, 0, "123", "page1");
        assertNavigationEvent(eventsCaptor, 1, "123", "page1", 111222l, 456l);
        assertPageViewEvent(eventsCaptor, 2, "456", "page2");

        reset(swrveSpy);

        // change to landscape mode, call configuration change but create the use the saved bundle instance
        RuntimeEnvironment.setQualifiers("+land");
        pair.first.configurationChange();
        Bundle bundle = new Bundle();
        pair.first.saveInstanceState(bundle).pause().stop().destroy();
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity()).create(bundle).start().restoreInstanceState(bundle).resume();
        SwrveInAppMessageActivity activityLandscape = activityController.get();
        assertNotEquals(activity, activityLandscape);

        // Should be on the landscape format and on page 2
        assertEquals("LandscapeFormat", activityLandscape.inAppMessageHandler.format.getName());
        view = getSwrveMessageView(activityLandscape);
        assertEquals(456, view.getPage().getPageId()); // still on same page

        // Navigate to page 1
        clickButton(activityLandscape, view, SwrveActionType.PageLink);
        view = getSwrveMessageView(activityLandscape);
        assertEquals(123l, view.getPage().getPageId());

        // Navigate back to page 2
        clickButton(activityLandscape, view, SwrveActionType.PageLink);
        view = getSwrveMessageView(activityLandscape);
        assertEquals(456l, view.getPage().getPageId());

        // Just the one event which is the button navigation from page 2 to page 1
        verify(swrveSpy, times(1)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());
        assertNavigationEvent(eventsCaptor, 3, "456", "page2", 222111l, 123l);
    }

    @Test
    public void testMultiPageSwipeRotateDevice() throws Exception {

        // start in portrait mode
        RuntimeEnvironment.setQualifiers("+port");

        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "multipage_campaign_swipe.json", "asset1", "asset2", "asset3", "asset4", "asset5");
        // Trigger IAM
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        SwrveMessageView view = getSwrveMessageView(activity);
        assertEquals(123, view.getPage().getPageId());
        assertEquals("PortraitFormat", activity.inAppMessageHandler.format.getName());
        assertEquals(2, activity.adapter.trunk.size());

        // Navigate to page 2
        int page1Index = activity.viewPager2.getCurrentItem();
        activity.viewPager2.setCurrentItem(page1Index + 1);

        // change to landscape mode, call configuration change but create the use the saved bundle instance
        RuntimeEnvironment.setQualifiers("+land");
        pair.first.configurationChange();
        Bundle bundle = new Bundle();
        pair.first.saveInstanceState(bundle).pause().stop().destroy();
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity()).create(bundle).start().restoreInstanceState(bundle).resume();
        SwrveInAppMessageActivity activityLandscape = activityController.get();
        assertNotEquals(activity, activityLandscape);

        // Should be on the landscape format and trunk size should be the same.
        assertEquals("LandscapeFormat", activityLandscape.inAppMessageHandler.format.getName());
        assertEquals(2, activityLandscape.adapter.trunk.size());
        assertEquals(456, activityLandscape.inAppMessageHandler.getStartingPageId());
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

        SwrveMessageView view = getSwrveMessageView(activity);

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
        SwrveMessageView view = getSwrveMessageView(activity);

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

        SwrveMessageView view = getSwrveMessageView(activity);

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
    public void testDismissButtonListener() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().dismissButtonListener((campaignSubject, buttonName) -> testDismissButtonName = buttonName);
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        // Trigger IAM
        swrveSpy.currencyGiven("gold", 20);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        SwrveMessageView view = getSwrveMessageView(activity);

        // Press dismiss button
        SwrveButtonView swrveButtonView = findButton(view, SwrveActionType.Dismiss);
        swrveButtonView.performClick();

        assertEquals("close", testDismissButtonName);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, times(2)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());

        // page_view
        ArrayList events = arrayListCaptor.getAllValues().get(0); // index 0
        JSONObject event = new JSONObject((String) events.get(0));
        assertEquals(EVENT_TYPE_GENERIC_CAMPAIGN, event.get("type"));
        assertEquals("165", event.get("id"));
        assertFalse(event.has("payload")); // for older campaigns there are no pages. back button has no name
        SwrveTestUtils.assertGenericEvent(event.toString(), "0", GENERIC_EVENT_CAMPAIGN_TYPE_IAM, GENERIC_EVENT_ACTION_TYPE_PAGE_VIEW, null);

        // dismiss
        events = arrayListCaptor.getAllValues().get(1); // index 1
        event = new JSONObject((String) events.get(0));
        assertEquals(EVENT_TYPE_GENERIC_CAMPAIGN, event.get("type"));
        assertEquals("165", event.get("id"));
        Map<String, String> expectedPayloadNav = new HashMap<>();
        expectedPayloadNav.put("buttonName", "close");
        SwrveTestUtils.assertGenericEvent(event.toString(), "0", GENERIC_EVENT_CAMPAIGN_TYPE_IAM, GENERIC_EVENT_ACTION_TYPE_DISMISS, expectedPayloadNav);
    }

    @Test
    public void testDismissButtonListenerBackButton() throws Exception {
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

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> arrayListCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, times(2)).sendEventsInBackground(contextCaptor.capture(), userIdStringCaptor.capture(), arrayListCaptor.capture());

        // page_view
        ArrayList events = arrayListCaptor.getAllValues().get(0); // index 0
        JSONObject event = new JSONObject((String) events.get(0));
        assertEquals(EVENT_TYPE_GENERIC_CAMPAIGN, event.get("type"));
        assertEquals("165", event.get("id"));
        assertFalse(event.has("payload")); // for older campaigns there are no pages. back button has no name
        SwrveTestUtils.assertGenericEvent(event.toString(), "0", GENERIC_EVENT_CAMPAIGN_TYPE_IAM, GENERIC_EVENT_ACTION_TYPE_PAGE_VIEW, null);

        // dismiss
        events = arrayListCaptor.getAllValues().get(1); // index 1
        event = new JSONObject((String) events.get(0));
        assertEquals(EVENT_TYPE_GENERIC_CAMPAIGN, event.get("type"));
        assertEquals("165", event.get("id"));
        assertFalse(event.has("payload")); // for older campaigns there are no pages. back button has no name
        SwrveTestUtils.assertGenericEvent(event.toString(), "0", GENERIC_EVENT_CAMPAIGN_TYPE_IAM, GENERIC_EVENT_ACTION_TYPE_DISMISS, null);
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

        SwrveMessageView view = getSwrveMessageView(activity);

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

        SwrveMessageView view = getSwrveMessageView(activity);

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

        SwrveMessageView view = getSwrveMessageView(activity);
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

        SwrveMessageView view = getSwrveMessageView(activity);
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

        SwrveMessageView view = getSwrveMessageView(activity);
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
        assertEquals(1, textView.getLineSpacingMultiplier(), 0.1);

        assertEquals(Color.parseColor("#00FF00"), ((ColorDrawable) textView.getBackground()).getColor());
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

        SwrveMessageView view = getSwrveMessageView(activity);
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

        SwrveMessageView view = getSwrveMessageView(activity);
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

        SwrveMessageView view = getSwrveMessageView(activity);
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

        assertEquals(SwrveOrientation.Portrait, activity.inAppMessageHandler.format.getOrientation());
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

        assertEquals(SwrveOrientation.Landscape, activity.inAppMessageHandler.format.getOrientation());
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
    
    @Test
    public void testAccessibility() throws Exception {
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().personalizationProvider(eventPayload -> {
            Map values = Maps.newHashMap();
            values.put("test_1", "TEST123");
            values.put("user", "Jose");
            return values;
        });
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_access.json", "1111111111111111111111111");

        // Trigger IAM
        swrveSpy.event("trigger_iam");
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        SwrveMessageView view = getSwrveMessageView(activity);
        assertNotNull(view);
        
        SwrveImageView background = (SwrveImageView) view.getChildAt(0);
        //tests fallback
        assertEquals("Decorative Purple Background personalized", background.getContentDescription());
        assertTrue(background.isImportantForAccessibility());
        
        SwrveTextView swrveTextView1 = (SwrveTextView) view.getChildAt(1);
        assertEquals("This is a longer piece of text over several lines that will scroll up and down.", swrveTextView1.getContentDescription());
        assertTrue(swrveTextView1.isImportantForAccessibility());
        
        SwrveTextView swrveTextView2 = (SwrveTextView) view.getChildAt(2);
        assertEquals("Text that auto fits", swrveTextView2.getContentDescription());
        assertTrue(swrveTextView2.isImportantForAccessibility());
        
        SwrveTextImageView swrveTextImageView = (SwrveTextImageView) view.getChildAt(3);
        assertEquals("Copy code to clipboard 01234566789", swrveTextImageView.getContentDescription());
        assertTrue(swrveTextImageView.isImportantForAccessibility());
        
        SwrveButtonView swrveButtonView = (SwrveButtonView) view.getChildAt(4);
        assertEquals(swrveButtonView.getContentDescription(), "Dismiss Message Jose");
        assertTrue(swrveButtonView.isImportantForAccessibility());
        
        SwrveTextImageView swrveTextImageView2 = (SwrveTextImageView) view.getChildAt(5);
        assertEquals(swrveTextImageView2.getContentDescription(), "Launch google");
        assertTrue(swrveTextImageView2.isImportantForAccessibility());
        
        SwrveTextImageView swrveTextImageView3 = (SwrveTextImageView) view.getChildAt(6);
        assertEquals(swrveTextImageView3.getContentDescription(), "Text TEST123");
        assertTrue(swrveTextImageView3.isImportantForAccessibility());
        
    }

    // Helpers

    private SwrveMessageView clickButton(SwrveInAppMessageActivity activity, SwrveMessageView view, SwrveActionType actionType) {
        SwrveButtonView swrveButton = findButton(view, actionType);
        swrveButton.performClick();
        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)
        return getSwrveMessageView(activity);
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
                SwrveBaseInteractableView swrveBaseInteractableView = (SwrveBaseInteractableView) childView;
                if (swrveBaseInteractableView.getType() == actionType) {
                    return swrveBaseInteractableView;
                }
            }
        }
        Assert.fail("Could not find custom button");
        return null;
    }

    private Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> createActivityFromPeekIntent(Intent intent) {
        assertNotNull(intent);
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, intent);
        return new Pair(activityController, activityController.create().start().resume().visible().get());
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

    private SwrveMessageView getSwrveMessageView(SwrveInAppMessageActivity activity) {
        ViewGroup parentView = activity.findViewById(android.R.id.content);
        LinearLayout linearLayout = (LinearLayout) parentView.getChildAt(0);
        // ViewPager2 and FrameLayout are both children linearlayout. But one of these is hidden depending on isSwipeable
        FrameLayout frameLayout;
        if (activity.isSwipeable) {
            assertEquals(View.GONE, linearLayout.getChildAt(1).getVisibility()); // index 1 is the second child which should be gone.
            ViewPager2 viewPager2 = (ViewPager2) linearLayout.getChildAt(0);
            RecyclerView recyclerView = (RecyclerView) viewPager2.getChildAt(0);
            frameLayout = (FrameLayout) recyclerView.getChildAt(0);
        } else {
            assertEquals(View.GONE, linearLayout.getChildAt(0).getVisibility());
            frameLayout = (FrameLayout) linearLayout.getChildAt(1); // index 1 because its the second child. Viewpager is first, but gone.
        }
        SwrveMessageView view = (SwrveMessageView) frameLayout.getChildAt(0);
        return view;
    }

    private void assertPageViewEvent(ArgumentCaptor<ArrayList> eventsCaptor, int eventIndex, String pageId, String pageName) throws Exception {
        ArrayList events = eventsCaptor.getAllValues().get(eventIndex);
        JSONObject event = new JSONObject((String) events.get(0));
        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("pageName", pageName);
        SwrveTestUtils.assertGenericEvent(event.toString(), pageId, GENERIC_EVENT_CAMPAIGN_TYPE_IAM, GENERIC_EVENT_ACTION_TYPE_PAGE_VIEW, expectedPayload);
    }

    private void assertNavigationEvent(ArgumentCaptor<ArrayList> eventsCaptor, int eventIndex, String pageId, String pageName, long buttonId, long toPageId) throws Exception {
        ArrayList events = eventsCaptor.getAllValues().get(eventIndex);
        JSONObject event = new JSONObject((String) events.get(0));
        Map<String, Object> expectedPayload = new HashMap<>();
        expectedPayload.put("pageName", pageName);
        expectedPayload.put("buttonId", buttonId);
        expectedPayload.put("to", toPageId);
        SwrveTestUtils.assertGenericEvent(event.toString(), pageId, GENERIC_EVENT_CAMPAIGN_TYPE_IAM, GENERIC_EVENT_ACTION_TYPE_NAVIGATION, expectedPayload);
    }
}
