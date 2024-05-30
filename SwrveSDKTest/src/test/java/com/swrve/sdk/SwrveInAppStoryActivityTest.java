package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_DISMISS;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_IAM;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveButtonView;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveInAppStoryButton;
import com.swrve.sdk.messaging.SwrveInAppStoryView;
import com.swrve.sdk.messaging.SwrveMessageFocusListener;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessageView;
import com.swrve.sdk.messaging.SwrveStorySettings;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SwrveInAppStoryActivityTest extends SwrveBaseTest{

    private Swrve swrveSpy;
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
    public void testInAppStoryViewProgressBar() throws Exception{
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_in_app_story.json", "asset1", "asset2");
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity.class);
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(intent);
        ActivityController activityController = pair.first;
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);
        SwrveMessageView messageView = getSwrveMessageView(activity);
        assertNotNull(messageView);

        FrameLayout frameLayout = (FrameLayout)messageView.getParent().getParent();

        //Container should have a 3rd child (the progressbar) & Dismiss
        assertEquals(4, frameLayout.getChildCount());

        SwrveInAppStoryView storyView = (SwrveInAppStoryView) frameLayout.getChildAt(2);
        //Number of segments on bar should match what's in story campaign json
        assertEquals(3,storyView.getNumberOfSegments());

        //Check that it animates ok
        AtomicBoolean segmentIsCompleted = new AtomicBoolean(false);
        SwrveInAppStoryView.SwrveInAppStorySegmentListener listener = new SwrveInAppStoryView.SwrveInAppStorySegmentListener() {
            @Override
            public void segmentFinished(int segmentIndex) {
                segmentIsCompleted.set(true);
                assertEquals(100, storyView.getSegmentProgress());
            }
        };
        storyView.setListener(listener);
        await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> segmentIsCompleted.get());

        //Test pause (backgrounding) of the activity
        //Run for a second, pause, wait another second, and check progress remains the same
        storyView.startSegmentAtIndex(1);
        segmentIsCompleted.set(false);
        await().pollDelay(Duration.ofSeconds(1)).until(() -> { return true;});
        activityController.pause();
        AtomicInteger pausedProgress = new AtomicInteger(storyView.getSegmentProgress());
        await().pollDelay(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(2))
                .until(() -> {
                    return pausedProgress.get() == storyView.getSegmentProgress();
                });
        //Resume, and check segment animation completes
        activityController.resume();
        await().atMost(Duration.ofSeconds(10))
                .until(() -> segmentIsCompleted.get());
    }
    @Test
    public void testInAppStoryViewNavigation() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_in_app_story.json", "asset1", "asset2");
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity.class);
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(intent);
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);

        SwrveMessageView messageView = getSwrveMessageView(activity);
        assertNotNull(messageView);

        assertEquals(0, messageView.getFormat().getIndexForPageId(messageView.getPage().getPageId()));

        //TODO - is this test redudant, thanks to last progression tests?
        //Check that it auto-progresses to next page
        messageView = waitForStoryProgression(activity, 2);

        assertEquals(1, messageView.getFormat().getIndexForPageId(messageView.getPage().getPageId()));
    }
    @Test
    public void testInAppStoryLastPageStop() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_in_app_story.json", "asset1", "asset2");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        assertEquals(2, campaigns.size());
        SwrveInAppCampaign campaign = (SwrveInAppCampaign) campaigns.get(0);
        SwrveMessageFormat format = campaign.getMessage().getFormats().get(0);
        format.getStorySettings().setLastPageProgression(SwrveStorySettings.LastPageProgression.STOP);

        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);
        SwrveMessageView messageView = getSwrveMessageView(activity);
        assertNotNull(messageView);

        //Wait until progresses to last page
        messageView = waitForStoryProgression(activity, 2);
        assertEquals(1, messageView.getFormat().getIndexForPageId(messageView.getPage().getPageId()));
        messageView = waitForStoryProgression(activity, 3);
        assertEquals(2, messageView.getFormat().getIndexForPageId(messageView.getPage().getPageId()));

        //Confirm it stays on last page after page duration
        await().pollInSameThread()
                .pollDelay(Duration.ofMillis(format.getStorySettings().getPageDuration() + 500))
                .until(() -> {
                    Robolectric.flushForegroundThreadScheduler();
                    return activity.currentPageIdNonSwipe == 3;
                });

        // Press button on last page which should go to page 2 but the page progression should continue
        messageView = clickButton(activity, messageView, SwrveActionType.PageLink);
        assertEquals(2, messageView.getPage().getPageId());
        waitForStoryProgression(activity, 3);
    }
    @Test
    public void testInAppStoryLastPageDismiss() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_in_app_story.json", "asset1", "asset2");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        assertEquals(2, campaigns.size());
        SwrveInAppCampaign campaign = (SwrveInAppCampaign) campaigns.get(0);
        SwrveMessageFormat format = campaign.getMessage().getFormats().get(0);
        format.getStorySettings().setLastPageProgression(SwrveStorySettings.LastPageProgression.DISMISS);

        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);
        SwrveMessageView messageView = getSwrveMessageView(activity);
        assertNotNull(messageView);

        //Wait for the story to progress to the last page
        messageView = waitForStoryProgression(activity, 3);

        //Confirm activity is dismissed after last page duration
        await().pollInSameThread()
                .pollDelay(Duration.ofMillis(format.getStorySettings().getPageDuration() + 500))
                .until(() -> {
                    Robolectric.flushForegroundThreadScheduler();
                    return activity.currentPageIdNonSwipe == 3;
                });

        //Confirm IAM window is dismissed/finishing
        assertTrue(activity.isFinishing());

        //Verify dismiss event sent
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> eventsCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, times(1)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());
        ArrayList events = eventsCaptor.getValue();
        JSONObject event = new JSONObject((String) events.get(0));
        Map<String, String> expectedPayload = new HashMap<>();
        expectedPayload.put("buttonName", "Auto Dismiss?");
        expectedPayload.put("pageName", "page3");
        expectedPayload.put("buttonId", "12345");
        SwrveTestUtils.assertGenericEvent(event.toString(), "3", GENERIC_EVENT_CAMPAIGN_TYPE_IAM, GENERIC_EVENT_ACTION_TYPE_DISMISS, expectedPayload);
    }
    @Test
    public void testInAppStoryLastPageLoop() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_in_app_story.json", "asset1", "asset2");
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        assertEquals(2, campaigns.size());
        SwrveInAppCampaign campaign = (SwrveInAppCampaign) campaigns.get(0);
        SwrveMessageFormat format = campaign.getMessage().getFormats().get(0);
        format.getStorySettings().setLastPageProgression(SwrveStorySettings.LastPageProgression.LOOP);

        swrveSpy.showMessageCenterCampaign(campaigns.get(0));

        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, mShadowActivity.peekNextStartedActivity());
        SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);
        SwrveMessageView messageView = getSwrveMessageView(activity);
        assertNotNull(messageView);

        //Wait until progresses to last page
        messageView = waitForStoryProgression(activity, 3);
        assertEquals(2, messageView.getFormat().getIndexForPageId(messageView.getPage().getPageId()));
        //Confirm next is back to frist page, and on..
        messageView = waitForStoryProgression(activity, 1);
        assertEquals(0, messageView.getFormat().getIndexForPageId(messageView.getPage().getPageId()));
        messageView = waitForStoryProgression(activity, 2);
        assertEquals(1, messageView.getFormat().getIndexForPageId(messageView.getPage().getPageId()));
    }

    @Test
    public void testInAppStoryHandleTaps() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_in_app_story.json", "asset1", "asset2");
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity.class);
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(intent);
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);
        SwrveMessageView messageView = getSwrveMessageView(activity);
        assertNotNull(messageView);
        FrameLayout frameLayout = (FrameLayout)messageView.getParent().getParent();
        SwrveInAppStoryView storyView = (SwrveInAppStoryView) frameLayout.getChildAt(2);

        ViewGroup parentView = activity.findViewById(android.R.id.content);
        LinearLayout linearLayout = (LinearLayout) parentView.getChildAt(0);
        FrameLayout swrveLayout = (FrameLayout)linearLayout.getChildAt(0);

        Point leftTapPoint = new Point((int)(storyView.getWidth() * 0.25), storyView.getWidth() / 2);
        Point rightTapPoint = new Point((int)(storyView.getWidth() * 0.75), storyView.getWidth() / 2);

        // Ensure start on page 1
        await().atMost(Duration.ofSeconds(1)).until(() -> storyView.getCurrentIndex() == 0);

        // Tap left should remain on page 1
        simulateTap(swrveLayout, leftTapPoint);
        await().atMost(Duration.ofSeconds(1)).until(() -> storyView.getCurrentIndex() == 0);

        // Tap right to page 2
        simulateTap(swrveLayout, rightTapPoint);
        await().atMost(Duration.ofSeconds(1)).until(() -> storyView.getCurrentIndex() == 1);

        // .. and on to page 3
        simulateTap(swrveLayout, rightTapPoint);
        await().atMost(Duration.ofSeconds(1)).until(() -> storyView.getCurrentIndex() == 2);

        // tap right again should remain on page 3 (the last page)
        simulateTap(swrveLayout, rightTapPoint);
        await().atMost(Duration.ofSeconds(1)).until(() -> storyView.getCurrentIndex() == 2);

        // tap left back to page 2
        simulateTap(swrveLayout, leftTapPoint);
        await().atMost(Duration.ofSeconds(1)).until(() -> storyView.getCurrentIndex() == 1);

    }

    @Test
    public void testInAppStoryDismissButton() throws Exception {
        AtomicBoolean focusListenerCalled = new AtomicBoolean(false);
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().messageFocusListener(new SwrveMessageFocusListener() {
            @Override
            public void onFocusChanged(View view, boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
                focusListenerCalled.set(gainFocus);
            }
        });
        config.setInAppMessageConfig(inAppConfigBuilder.build());

        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_in_app_story.json", "asset1", "asset2");
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity.class);

        //Test Story campaign with no dismiss button
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 166);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(intent);
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);
        SwrveMessageView messageView = getSwrveMessageView(activity);
        assertNotNull(messageView);
        FrameLayout storyContainer = (FrameLayout)messageView.getParent().getParent();
        SwrveInAppStoryView storyView = (SwrveInAppStoryView) storyContainer.getChildAt(2);
        //Should only be a 4th child (the dismiss button) if set in campaign json
        assertTrue(storyContainer.getChildCount() == 3);

        //Test Story campaign with a dismiss button
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165);
        pair = createActivityFromPeekIntent(intent);
        activity = pair.second;
        assertNotNull(activity);
        messageView = getSwrveMessageView(activity);
        assertNotNull(messageView);
        storyContainer = (FrameLayout)messageView.getParent().getParent();
        storyView = (SwrveInAppStoryView) storyContainer.getChildAt(2);
        assertNotNull(storyView);

        //Test the button appears/disappears if in settings json
        SwrveInAppStoryButton dismissView = (SwrveInAppStoryButton) storyContainer.getChildAt(3);
        assertNotNull(dismissView);
        assertTrue(dismissView.getVisibility() == View.VISIBLE);
        assertTrue(dismissView.getWidth() == 50);
        assertEquals("Dismiss", dismissView.getContentDescription());

        //Test focus listener called
        dismissView.requestFocus();
        assertTrue(focusListenerCalled.get());

        //Test tap on dismissbutton closes message
        dismissView.performClick();
        Robolectric.flushForegroundThreadScheduler();
        SwrveInAppMessageActivity finalActivity = activity;
        await().atMost(Duration.ofSeconds(1)).until(() -> finalActivity.isFinishing());

        //And test it sends dismiss event
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ArrayList> eventsCaptor = ArgumentCaptor.forClass(ArrayList.class);
        verify(swrveSpy, times(3)).sendEventsInBackground(contextCaptor.capture(), userIdCaptor.capture(), eventsCaptor.capture());
        ArrayList events = eventsCaptor.getValue();
        JSONObject event = new JSONObject((String) events.get(0));
        Map<String, String> expectedPayload = new HashMap<>();
        expectedPayload.put("buttonName", "Dismiss?");
        expectedPayload.put("pageName", "page1");
        expectedPayload.put("buttonId", "12345678");
        SwrveTestUtils.assertGenericEvent(event.toString(), "1", GENERIC_EVENT_CAMPAIGN_TYPE_IAM, GENERIC_EVENT_ACTION_TYPE_DISMISS, expectedPayload);
    }

    @Test
    public void testInAppStoryCustomDismissButton() throws Exception {
        StateListDrawable stateDrawables = new StateListDrawable();
        Drawable drawable = new ShapeDrawable();
        stateDrawables.addState(new int[]{android.R.attr.state_enabled}, drawable);
        stateDrawables.addState(new int[]{android.R.attr.state_focused}, drawable);
        stateDrawables.addState(new int[]{android.R.attr.state_pressed}, drawable);
        stateDrawables.addState(new int[]{}, drawable);
        SwrveInAppMessageConfig.Builder inAppConfigBuilder = new SwrveInAppMessageConfig.Builder().storyDismissButton(stateDrawables);
        config.setInAppMessageConfig(inAppConfigBuilder.build());
        initSDK();

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_in_app_story.json", "asset1", "asset2");
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity.class);

        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(intent);
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);
        SwrveMessageView messageView = getSwrveMessageView(activity);
        assertNotNull(messageView);
        FrameLayout storyContainer = (FrameLayout)messageView.getParent().getParent();

        //Should be a dismiss button, because is requested in campaign json
        assertTrue(storyContainer.getChildCount() == 4);
        SwrveInAppStoryButton dismissButton = (SwrveInAppStoryButton) storyContainer.getChildAt(3);
        assertNotNull(dismissButton);

        //Confirm that the story dismiss button is using the custom button
        //(should have drawables set for 4 of the states, instead of the default 3 states)
        StateListDrawable buttonDrawable = (StateListDrawable) dismissButton.getBackground();
        assertEquals(4, buttonDrawable.getStateCount());
    }

    @Test
    public void testStoryDifferentPageDurations() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_in_app_story_durations.json", "asset1", "asset2");
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SwrveInAppMessageActivity.class);

        //Test Story campaign with no dismiss button
        intent.putExtra(SwrveInAppMessageActivity.MESSAGE_ID_KEY, 165);
        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createActivityFromPeekIntent(intent);
        SwrveInAppMessageActivity activity = pair.second;
        assertNotNull(activity);
        SwrveMessageView messageView = getSwrveMessageView(activity);
        assertNotNull(messageView);
        FrameLayout storyContainer = (FrameLayout)messageView.getParent().getParent();
        SwrveInAppStoryView storyView = (SwrveInAppStoryView) storyContainer.getChildAt(2);

        //Ensure on first at start
        assertEquals(0,storyView.getCurrentIndex());

        //Second page should appear after 0.5 sec
        await().pollInSameThread().pollDelay(Duration.ofMillis(600)).atMost(Duration.ofSeconds(1)).until(() -> {
            Robolectric.flushForegroundThreadScheduler();
            return storyView.getCurrentIndex() == 1;
        });

        //Third page should appear (only) after a further 4 secs
        //Wait and ensure still on same page after 3 seconds (the campaign's general duration is 2.5secs)
        await().pollInSameThread().pollDelay(Duration.ofMillis(3000)).atMost(Duration.ofMillis(3500)).until(() -> {
            Robolectric.flushForegroundThreadScheduler();
            return true;
        });
        assertEquals(1,storyView.getCurrentIndex());
        //Check page has moved on after the 4 seconds are passed.
        await().pollInSameThread().pollDelay(Duration.ofMillis(1500)).atMost(Duration.ofMillis(2000)).until(() -> {
            Robolectric.flushForegroundThreadScheduler();
            return true;
        });
        assertEquals(2,storyView.getCurrentIndex());
        activity.finish();
    }

    private SwrveMessageView waitForStoryProgression(SwrveInAppMessageActivity activity, long toPageId) {
        await().pollInSameThread()
                .pollInterval(Duration.ofMillis(500))
                .atMost(Duration.ofSeconds(10))
                .until(() -> {
                    Robolectric.flushForegroundThreadScheduler();
                    return activity.currentPageIdNonSwipe == toPageId;
                });
        return getSwrveMessageView(activity);
    }

    private Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> createActivityFromPeekIntent(Intent intent) {
        assertNotNull(intent);
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, intent);
        return new Pair(activityController, activityController.create().start().resume().visible().get());
    }

    private void simulateTap(View view, Point point) {
        long downTime = System.currentTimeMillis();
        long eventTime = downTime;
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, point.x, point.y, 0);
        view.dispatchTouchEvent(event);
        event = MotionEvent.obtain(downTime, eventTime + 100, MotionEvent.ACTION_UP, point.x, point.y, 0);
        view.dispatchTouchEvent(event);
        //Wait long enough so gestureDestector knows its not a double tap (only then is OnSingleTapConfirmed fired)
        await().pollInSameThread().pollDelay(Duration.ofMillis(ViewConfiguration.getDoubleTapTimeout()))
                .until(() -> {
                    Robolectric.flushForegroundThreadScheduler();
                    return true;
                });
        Robolectric.flushForegroundThreadScheduler();
    }

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

    private SwrveMessageView getSwrveMessageView(SwrveInAppMessageActivity activity) {
        ViewGroup parentView = activity.findViewById(android.R.id.content);
        LinearLayout linearLayout = (LinearLayout) parentView.getChildAt(0);
        FrameLayout swrveLayout = (FrameLayout)linearLayout.getChildAt(0);
        // ViewPager2 and FrameLayout are both children linearlayout. But one of these is hidden depending on isSwipeable
        FrameLayout frameLayout;
        if (activity.isSwipeable) {
            assertEquals(View.GONE, swrveLayout.getChildAt(1).getVisibility()); // index 1 is the second child which should be gone.
            ViewPager2 viewPager2 = (ViewPager2) swrveLayout.getChildAt(0);
            RecyclerView recyclerView = (RecyclerView) viewPager2.getChildAt(0);
            frameLayout = (FrameLayout) recyclerView.getChildAt(0);
        } else {
            assertEquals(View.GONE, swrveLayout.getChildAt(0).getVisibility());
            frameLayout = (FrameLayout) swrveLayout.getChildAt(1); // index 1 because its the second child. Viewpager is first, but gone.
        }
        SwrveMessageView view = (SwrveMessageView) frameLayout.getChildAt(0);
        return view;
    }
}
