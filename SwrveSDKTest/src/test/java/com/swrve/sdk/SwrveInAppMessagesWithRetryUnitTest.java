package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.EVENT_FIRST_SESSION;
import static com.swrve.sdk.SwrveTrackingState.STARTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveButtonView;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageView;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SwrveInAppMessagesWithRetryUnitTest extends SwrveBaseTest {

    @Rule
    public RetryRule retryRule = new RetryRule(3); // Retry up to 3 times

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        SwrveTestUtils.disableRestClientExecutor(swrveSpy);
        swrveSpy.profileManager.setTrackingState(STARTED);
        swrveSpy.onActivityCreated(mActivity, null);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Mockito.reset(swrveSpy);
    }

    @Ignore("Ignored for now. Flaky when executed from command line.")
    @Test
    public void testGetMessageForEventWaitFirstTime() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e"
        );

        // Do not return any until delay_first_message
        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // Move to the future
        Date oneMinLater = new Date(System.currentTimeMillis() + 60000l);
        doReturn(oneMinLater).when(swrveSpy).getNow();
        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
        assertEquals(165, message.getId());
    }

    @Ignore("Ignored for now. Flaky when executed from command line.")
    @Test
    public void testGetMessageForEventWaitIfDisplayed() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e"
        );

        Date oneMinLater = new Date(System.currentTimeMillis() + 60000l);
        doReturn(oneMinLater).when(swrveSpy).getNow();
        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
        assertEquals(165, message.getId());
        swrveSpy.messageWasShownToUser(message.getFormats().get(0));

        // Second time is going to wait min_delay_between_messages seconds
        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // To the future
        Date later = new Date(System.currentTimeMillis() + 120000l);
        doReturn(later).when(swrveSpy).getNow();
        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
    }

    @Ignore("Ignored for now. Failing regularly in CI but passing locally ok.")
    @Test
    public void testGetMessageMaxMessagesDisplayed() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e"
        );

        Date oneMinLater = new Date(System.currentTimeMillis() + 60000l);
        doReturn(oneMinLater).when(swrveSpy).getNow();

        // keep track of time so we can keep adding to it
        Date later = new Date(oneMinLater.getTime());

        for (int i = 0; i < 10; i++) {
            SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
            assertNotNull("failed to show message minute later. i:" + i, message);
            swrveSpy.messageWasShownToUser(message.getFormats().get(0));

            // Add 2 minutes 'later' every time we iterate
            later = new Date(later.getTime() + 120000l);
            doReturn(later).when(swrveSpy).getNow();
        }

        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);
    }

    private Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> createIAMActivityFromIntent(Intent intent) {
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, intent);
        return new Pair(activityController, activityController.create().start().visible().get());
    }

    @Ignore("Ignored for now. Failing regularly in CI but passing locally ok.")
    @Test
    public void testMessageLeftOpen() throws Exception {

        Date today = new Date();
        doReturn(today).when(swrveSpy).getNow();

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        Date later60 = new Date(today.getTime() + 61000l);
        doReturn(later60).when(swrveSpy).getNow();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.currency_given");
        swrveSpy.queueEvent(swrveSpy.getUserId(), "event", parameters, null, true);
        Robolectric.flushForegroundThreadScheduler();

        Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createIAMActivityFromIntent(mShadowActivity.getNextStartedActivity());

        // Into the future!
        Date later180 = new Date(today.getTime() + 180000);
        doReturn(later180).when(swrveSpy).getNow();

        // Dismiss
        dismissInAppMessage(pair.second);

        // Last message just closed - should not be able to display a new one
        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);

        Date later180Again = new Date(later180.getTime() + 180000);
        doReturn(later180Again).when(swrveSpy).getNow();

        // Last message closed 180 seconds ago - should be able to display a new one
        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
    }

    private void dismissInAppMessage(SwrveInAppMessageActivity activity) {
        ViewGroup parentView = activity.findViewById(android.R.id.content);
        LinearLayout linearLayout = (LinearLayout)parentView.getChildAt(0);
        FrameLayout swrveLayout = (FrameLayout)linearLayout.getChildAt(0);
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
        // Press install button
        if (view != null) {
            for (int i = 0; i < view.getChildCount(); i++) {
                View childView = view.getChildAt(i);
                if (childView instanceof SwrveButtonView) {
                    SwrveButtonView btn = (SwrveButtonView) childView;
                    btn.performClick();
                    break;
                }
            }
        }
    }

    @Ignore("Ignored for now. Failing regularly in CI but passing locally ok.")
    @Test
    public void testTooSoonAfterLaunchRuleAfterReload() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        Date nowDate = swrveSpy.getNow();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SS");
        String sdkBeginNow = "sdkBeginNow:" + formatter.format(nowDate);
        doReturn(nowDate).when(swrveSpy).getNow();

        // Do not return any until delay_first_message
        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // To the future!
        nowDate = new Date(nowDate.getTime() + 62000);
        doReturn(nowDate).when(swrveSpy).getNow();
        String sdkFutureNow = " sdkFutureNow:" + formatter.format(swrveSpy.getNow());
        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(sdkBeginNow + sdkFutureNow , message);
        assertEquals(165, message.getId());

        swrveSpy.refreshCampaignsAndResources();

        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
        assertEquals(165, message.getId());
    }

    @Ignore("Ignored for now. Failing regularly in CI but passing locally ok.")
    @Test
    public void testRulesAfterReload() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_impressions.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        Date later61 = new Date(System.currentTimeMillis() + 61000);
        doReturn(later61).when(swrveSpy).getNow();
        for (int i = 0; i < 2; i++) {
            SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
            assertNotNull(message);
            swrveSpy.messageWasShownToUser(message.getFormats().get(0));
            // To the future
            Date later180 = new Date(swrveSpy.getNow().getTime() + 180000);
            doReturn(later180).when(swrveSpy).getNow();
        }

        Date later = new Date(swrveSpy.getNow().getTime() + 2000000);
        doReturn(later).when(swrveSpy).getNow();
        swrveSpy.refreshCampaignsAndResources();

        // Should still return no message!
        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);
    }

    @Ignore("Ignored for now. Failing regularly in CI but passing locally ok.")
    @Test
    public void testImpressions() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_impressions.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        long now = System.currentTimeMillis();
        Date later = new Date(now + 61000l);
        doReturn(later).when(swrveSpy).getNow();
        for (int i = 0; i < 2; i++) {
            SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
            assertNotNull("failed to get message i:" + i + " now:" + new Date(now) + " later:" + later, message);
            swrveSpy.messageWasShownToUser(message.getFormats().get(0));
            // To the future
            Date later180 = new Date(swrveSpy.getNow().getTime() + 180000);
            doReturn(later180).when(swrveSpy).getNow();
        }

        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // Fake no campaigns and restart
        tearDown();
        setUp();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_none.json");
        swrveSpy.refreshCampaignsAndResources();
        assertEquals(0, swrveSpy.campaigns.size());

        tearDown();
        setUp();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_impressions.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");
        swrveSpy.refreshCampaignsAndResources();

        // Same campaigns again, should still not be able to return the campaign
        assertEquals(1, swrveSpy.campaigns.size());
        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);
    }

    @Test
    public void testDownloadDate() throws Exception {

        long maxDiffMillis = TimeUnit.SECONDS.toMillis(5);

        Date downloadDate102Expected = new Date();

        // load up campaign json which contains only one campaign
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_download_date1.json", true, true, "fc972adec8076d203cbdfd8ca0e4b1bfa483abfb");

        // verify there's only 1 campaign and the download date is within a short period of time
        assertEquals(1, swrveSpy.getMessageCenterCampaigns().size());
        SwrveInAppCampaign campaign = (SwrveInAppCampaign) swrveSpy.getMessageCenterCampaign(102, null);
        Date dowloadDate102Actual = campaign.getDownloadDate();
        long timeDiffMillis = Math.abs(dowloadDate102Actual.getTime() - downloadDate102Expected.getTime());
        assertTrue("The download date of campaignId 102 is not within allowed tolerance:" + timeDiffMillis, timeDiffMillis <= maxDiffMillis);

        // Shutdown sdk and create a new instance.
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        setUp();

        Date downloadDate103Expected = new Date();

        // load up new campaign json which contains same campaign previously plus one new one
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_download_date2.json", true, true, "fc972adec8076d203cbdfd8ca0e4b1bfa483abfb");

        // verify there's 2 campaigns now
        assertEquals(2, swrveSpy.getMessageCenterCampaigns().size());

        // verify the download date for first one
        campaign = (SwrveInAppCampaign) swrveSpy.getMessageCenterCampaign(102, null);
        assertEquals(dowloadDate102Actual.getTime(), campaign.getDownloadDate().getTime()); // this time use precise comparison

        // verify the download date for second one
        campaign = (SwrveInAppCampaign) swrveSpy.getMessageCenterCampaign(103, null);
        Date dowloadDate103Actual = campaign.getDownloadDate();
        timeDiffMillis = Math.abs(dowloadDate103Actual.getTime() - downloadDate103Expected.getTime());
        assertTrue("The download date of campaignId 103 is not within allowed tolerance:" + timeDiffMillis, timeDiffMillis <= maxDiffMillis);
    }
}
