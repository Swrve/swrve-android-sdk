package com.swrve.sdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveConversationCampaign;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.ui.SwrveInAppMessageActivity;
import com.swrve.sdk.messaging.view.SwrveButtonView;
import com.swrve.sdk.messaging.view.SwrveMessageView;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.Pair;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class SwrveInAppMessagesUnitTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        SwrveTestUtils.disableRestClientExecutor(swrveSpy);
        swrveSpy.init(mActivity);
    }

    @Test
    public void testAppStoreURLForGame() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");

        assertNotNull(swrveSpy.getAppStoreURLForApp(150));
        assertNull(swrveSpy.getAppStoreURLForApp(250));

        SwrveMessage message = swrveSpy.getMessageForId(165);
        assertNotNull(message);
        Iterator<SwrveButton> buttonsIt = message.getFormats().get(0).getButtons().iterator();
        boolean correct = false;
        while (buttonsIt.hasNext()) {
            SwrveButton button = buttonsIt.next();
            if (button.getActionType() == SwrveActionType.Install && button.getAppId() == 150) {
                correct = true;
            }
        }

        assertTrue(correct);
    }

    @Test
    public void testAppStoreURLEmpty() {
        assertNull(swrveSpy.getAppStoreURLForApp(150));
        assertNull(swrveSpy.getAppStoreURLForApp(250));
    }

    @Test
    public void testDownloadingEnabled() throws Exception {

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();
        config.setAutoDownloadCampaignsAndResources(true);
        swrveSpy = SwrveTestUtils.createSpyInstance(config);

        final AtomicBoolean callbackBool = new AtomicBoolean(false);
        SwrveAssetsManager assetsManager = new SwrveAssetsManager() {
            @Override
            public void setCdnImages(String cdnImages) {
            }

            @Override
            public void setCdnFonts(String cdnFonts) {
            }

            @Override
            public void setStorageDir(File storageDir) {
            }

            @Override
            public File getStorageDir() {
                return null;
            }

            @Override
            public Set<String> getAssetsOnDisk() {
                return null;
            }

            @Override
            public void downloadAssets(Set<SwrveAssetsQueueItem> assetsImages, SwrveAssetsCompleteCallback callback) {
                callback.complete();
                callbackBool.set(true);
            }
        };
        SwrveAssetsManager assetsManagerImagesSpy = Mockito.spy(assetsManager);
        swrveSpy.swrveAssetsManager = assetsManagerImagesSpy;

        swrveSpy.init(mActivity);

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");

        await().untilTrue(callbackBool);

        ArgumentCaptor<Set> setArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<SwrveAssetsCompleteCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(SwrveAssetsCompleteCallback.class);
        Mockito.verify(assetsManagerImagesSpy, Mockito.atLeastOnce()).downloadAssets(setArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        assertEquals(3, setArgumentCaptor.getValue().size()); // 2 of the image items are the same, so only 3 total queued instead of 4
    }

    @Test
    public void testDownloadingPersonalizedDynamicUrls() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();

        SwrveInAppMessageConfig inAppConfig = new SwrveInAppMessageConfig.Builder()
                .personalisationProvider(eventPayload -> {
                    Map<String, String> result = new HashMap<>();
                    result.put("test_key_two", "asset2"); // resolve one by personalization
                    return result;
                }).build();
        config.setInAppMessageConfig(inAppConfig);
        config.setAutoDownloadCampaignsAndResources(true);
        swrveSpy = SwrveTestUtils.createSpyInstance(config);

        final AtomicBoolean callbackBool = new AtomicBoolean(false);
        SwrveAssetsManager assetsManager = new SwrveAssetsManager() {
            @Override
            public void setCdnImages(String cdnImages) {
            }

            @Override
            public void setCdnFonts(String cdnFonts) {
            }

            @Override
            public void setStorageDir(File storageDir) {
            }

            @Override
            public File getStorageDir() {
                return null;
            }

            @Override
            public Set<String> getAssetsOnDisk() {
                return null;
            }

            @Override
            public void downloadAssets(Set<SwrveAssetsQueueItem> assetsImages, SwrveAssetsCompleteCallback callback) {
                callback.complete();
                callbackBool.set(true);
            }
        };
        SwrveAssetsManager assetsManagerImagesSpy = Mockito.spy(assetsManager);
        swrveSpy.swrveAssetsManager = assetsManagerImagesSpy;

        swrveSpy.init(mActivity);

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_dynamic_image_url.json");

        await().untilTrue(callbackBool);

        ArgumentCaptor<Set> setArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<SwrveAssetsCompleteCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(SwrveAssetsCompleteCallback.class);
        Mockito.verify(assetsManagerImagesSpy, Mockito.atLeastOnce()).downloadAssets(setArgumentCaptor.capture(), callbackArgumentCaptor.capture());

        HashSet<SwrveAssetsQueueItem> assetsQueueItemHashSet = (HashSet<SwrveAssetsQueueItem>) setArgumentCaptor.getValue();
        assertEquals(4, assetsQueueItemHashSet.size()); // all of the fallback assets are the same. the only new assets are dynamic urls, one should not be present as it fails personalization
        assertTrue(assetsQueueItemHashSet.contains(new SwrveAssetsQueueItem(SwrveHelper.sha1("https://fakeitem/asset1.png".getBytes()), "https://fakeitem/asset1.png", true, true)));
        assertTrue(assetsQueueItemHashSet.contains(new SwrveAssetsQueueItem(SwrveHelper.sha1("https://fakeitem/asset2.png".getBytes()), "https://fakeitem/asset2.png", true, true)));
        assertTrue(assetsQueueItemHashSet.contains(new SwrveAssetsQueueItem(SwrveHelper.sha1("https://fakeitem/asset3.png".getBytes()), "https://fakeitem/asset3.png", true, true)));
        assertTrue(assetsQueueItemHashSet.contains(new SwrveAssetsQueueItem("8721fd4e657980a5e12d498e73aed6e6a565dfca", "8721fd4e657980a5e12d498e73aed6e6a565dfca", true, false)));
    }

    @Test
    public void testGetMessageForId() throws Exception {
        SwrveMessage message = assertGetMessageForIdTest(165);
        assertNotNull(message);
        assertEquals(165, message.getId());
    }

    @Test
    public void testGetMessageForIdNotFound() throws Exception {
        SwrveMessage message = assertGetMessageForIdTest(200);
        assertNull(message);
    }

    private SwrveMessage assertGetMessageForIdTest(int messageId) throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e"
        );
        return swrveSpy.getMessageForId(messageId);
    }

    @Test
    public void testGetMessageForIdEmpty() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_empty.json");
        SwrveMessage message = swrveSpy.getMessageForId(165);
        assertNull(message);
    }

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
            assertNotNull(message);
            swrveSpy.messageWasShownToUser(message.getFormats().get(0));

            // Add 2 minutes 'later' every time we iterate
            later = new Date(later.getTime() + 120000l);
            doReturn(later).when(swrveSpy).getNow();
        }

        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);
    }

    @Test
    public void testGetMessageForNonExistingTrigger() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb",
                "8721fd4e657980a5e12d498e73aed6e6a565dfca",
                "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e"
        );

        Date oneMinLater = new Date(System.currentTimeMillis() + 60000l);
        doReturn(oneMinLater).when(swrveSpy).getNow();
        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("Swrve.SDK.Android.invented_event");
        assertNull(message);
    }

    @Test
    public void testGetMessageWithFutureCampaign() throws Exception {

        Date pastDate = SwrveTestUtils.parseDate("2010/1/1 00:00");
        doReturn(pastDate).when(swrveSpy).getNow();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_future.json");

        Date bitlater = new Date(pastDate.getTime() + 61000l);
        doReturn(bitlater).when(swrveSpy).getNow();

        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);
    }

    @Test
    public void testGetMessageWithPastCampaign() throws Exception {

        Date pastDate = SwrveTestUtils.parseDate("2016/1/1 00:00");
        doReturn(pastDate).when(swrveSpy).getNow();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_past.json");

        Date bitlater = new Date(pastDate.getTime() + 61000l);
        doReturn(bitlater).when(swrveSpy).getNow();

        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);
    }

    @Test
    public void testGetMessageWithRoundRobin() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_robin.json");
        for (int i = 1; i < 6; i++) {
            SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
            assertNotNull(message);
            assertEquals(i, message.getId());
            swrveSpy.messageWasShownToUser(message.getFormats().get(0));
        }
    }

    @Test
    public void testGetMessageWithRandom() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_random.json");
        int hits = 0;
        int sameMessage = 0;
        int messageIndex = 1;
        int previousIndex = 0;
        Date date = new Date(System.currentTimeMillis());
        for (int i = 1; i < 60; i++) {
            date = new Date(date.getTime() + 60000l);
            doReturn(date).when(swrveSpy).getNow();
            SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
            assertNotNull(message);
            if (message.getCampaign().getMessages().size() != 1 && message.getId() == previousIndex) {
                sameMessage++;
            }
            if (message.getId() == messageIndex) {
                hits++;
            }
            messageIndex++;
            previousIndex = message.getId();
            swrveSpy.messageWasShownToUser(message.getFormats().get(0));
        }

        assertFalse("Message order should be random, we have shown the messages in order", hits == 59);
        assertFalse("Message order should be random, we have shown the same message each time", sameMessage == 58); // We have shown the same message 60 times
    }

    @Test
    public void testMessagePriority() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_priority_2.json");
        // Highest priority first
        SwrveMessage message1 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message1);
        assertEquals(1, message1.getId());
        swrveSpy.messageWasShownToUser(message1.getFormats().get(0));

        // Second highest priority (first in round robin)
        SwrveMessage message2 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message2);
        assertEquals(2, message2.getId());
        swrveSpy.messageWasShownToUser(message2.getFormats().get(0));

        // Round robin later
        SwrveMessage message3 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message3);
        assertEquals(3, message3.getId());
        swrveSpy.messageWasShownToUser(message2.getFormats().get(0));

        // Round robin again
        message2 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message2);
        assertEquals(2, message2.getId());
    }

    @Test
    public void testMessagePriorityInverse() throws Exception {
        // https://swrvedev.jira.com/browse/SWRVE-10432
        // We were not clearing the bucket of candidate messages, ever...

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_priority_reverse.json");
        // Highest priority first (first in round robin)
        SwrveMessage message1 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message1);
        assertEquals(2, message1.getId());
        swrveSpy.messageWasShownToUser(message1.getFormats().get(0));

        // Round robin later
        SwrveMessage message2 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message2);
        assertEquals(3, message2.getId());
        swrveSpy.messageWasShownToUser(message2.getFormats().get(0));

        // Lowest priority (first message in JSON)
        message2 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message2);
        assertEquals(1, message2.getId());
    }

    @Test
    public void testMessageCenterWithOnlyNonMessageCenterCampaigns() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_and_conversation_event.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        assertEquals(0, swrveSpy.getMessageCenterCampaigns().size());
    }

    @Test
    public void testIAMMessageCenter() throws Exception {

        Date futureDate = SwrveTestUtils.parseDate("2035/1/1 00:00");
        doReturn(futureDate).when(swrveSpy).getNow();

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_and_conversation_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        // No MessageCenter campaigns as they have both finished
        assertEquals(0, swrveSpy.getMessageCenterCampaigns().size());

        Date today = new Date();
        doReturn(today).when(swrveSpy).getNow();
        assertEquals(2, swrveSpy.getMessageCenterCampaigns().size());
        assertEquals(2, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Both).size());
        assertEquals(2, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape).size());
        // One IAM does not support the portrait orientation
        assertEquals(1, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Portrait).size());

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        SwrveInAppCampaign campaign = (SwrveInAppCampaign) swrveSpy.getMessageCenterCampaigns().get(0);
        assertEquals(SwrveCampaignState.Status.Unseen, campaign.getStatus());
        assertEquals("IAM subject", campaign.getSubject());
        swrveSpy.showMessageCenterCampaign(campaign);
        Robolectric.flushForegroundThreadScheduler();

        // Next activity started should be the a SwrveInAppMessageActivity
        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, SwrveInAppMessageActivity.class));
        createIAMActivityFromIntent(nextIntent);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals(1, campaign.getImpressions());
        assertEquals(SwrveCampaignState.Status.Seen, campaign.getStatus());

        // Simulate no assets, we should not get the campaign
        Set<String> assetsOnDisk = swrveSpy.getAssetsOnDisk();
        Set<String> previousAssets = new HashSet<>(assetsOnDisk);
        assetsOnDisk.clear();
        assertEquals(0, swrveSpy.getMessageCenterCampaigns().size());
        assetsOnDisk.addAll(previousAssets);

        // We can still get the IAM, even though the rules specify a limit of 1 impression
        assertEquals(campaign.getId(), swrveSpy.getMessageCenterCampaigns().get(0).getId());

        // Remove the campaign, we will never get it again
        swrveSpy.removeMessageCenterCampaign(campaign);
        assertFalse(swrveSpy.getMessageCenterCampaigns().contains(campaign));
        assertEquals(SwrveCampaignState.Status.Deleted, campaign.getStatus());
    }

    private Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> createIAMActivityFromIntent(Intent intent) {
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, intent);
        return new Pair(activityController, activityController.create().start().visible().get());
    }

    @Test
    public void testIAMMessageCenterWithPersonalisation() throws Exception {

        Date futureDate = SwrveTestUtils.parseDate("2035/1/1 00:00");
        doReturn(futureDate).when(swrveSpy).getNow();

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_and_conversation_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        // No MessageCenter campaigns as they have both finished
        assertEquals(0, swrveSpy.getMessageCenterCampaigns().size());

        Date today = new Date();
        doReturn(today).when(swrveSpy).getNow();
        assertEquals(2, swrveSpy.getMessageCenterCampaigns().size());
        assertEquals(2, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Both).size());
        assertEquals(2, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape).size());
        // One IAM does not support the portrait orientation
        assertEquals(1, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Portrait).size());

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        SwrveInAppCampaign campaign = (SwrveInAppCampaign) swrveSpy.getMessageCenterCampaigns().get(0);
        assertEquals(SwrveCampaignState.Status.Unseen, campaign.getStatus());
        assertEquals("IAM subject", campaign.getSubject());

        HashMap<String, String> testProperties = new HashMap<>();
        testProperties.put("test", "working");

        swrveSpy.showMessageCenterCampaign(campaign, testProperties);
        Robolectric.flushForegroundThreadScheduler();

        // Next activity started should be the a SwrveInAppMessageActivity
        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);

        Bundle extras = nextIntent.getExtras();
        HashMap<String, String> personalisationResponse = (HashMap<String, String>) extras.getSerializable(SwrveInAppMessageActivity.SWRVE_PERSONALISATION_KEY);
        assertNotNull(personalisationResponse);
        assertEquals("working", testProperties.get("test"));

        assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, SwrveInAppMessageActivity.class));
        createIAMActivityFromIntent(nextIntent);
        Robolectric.flushForegroundThreadScheduler();

        assertEquals(1, campaign.getImpressions());
        assertEquals(SwrveCampaignState.Status.Seen, campaign.getStatus());

        // Simulate no assets, we should not get the campaign
        Set<String> assetsOnDisk = swrveSpy.getAssetsOnDisk();
        Set<String> previousAssets = new HashSet<>(assetsOnDisk);
        assetsOnDisk.clear();
        assertEquals(0, swrveSpy.getMessageCenterCampaigns().size());
        assetsOnDisk.addAll(previousAssets);

        // We can still get the IAM, even though the rules specify a limit of 1 impression
        assertEquals(campaign.getId(), swrveSpy.getMessageCenterCampaigns().get(0).getId());

        // Remove the campaign, we will never get it again
        swrveSpy.removeMessageCenterCampaign(campaign);
        assertFalse(swrveSpy.getMessageCenterCampaigns().contains(campaign));
        assertEquals(SwrveCampaignState.Status.Deleted, campaign.getStatus());
    }

    @Test
    public void testIAMMessageCenterProgrammaticallySeen() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_and_conversation_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        SwrveInAppCampaign campaign = (SwrveInAppCampaign) swrveSpy.getMessageCenterCampaigns().get(0);
        assertEquals(SwrveCampaignState.Status.Unseen, campaign.getStatus());

        // Mark the campaign as seen programmatically (for custom IAM renderers)
        swrveSpy.markMessageCenterCampaignAsSeen(campaign);
        assertEquals(SwrveCampaignState.Status.Seen, campaign.getStatus());
    }

    @Test
    public void testConversationsActivityHonoursPortraitOrientation() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_and_conversation_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        assertEquals(2, swrveSpy.getMessageCenterCampaigns().size());

        swrveSpy.config.setOrientation(SwrveOrientation.Portrait);
        SwrveConversationCampaign campaign = (SwrveConversationCampaign) swrveSpy.getMessageCenterCampaigns().get(1);
        swrveSpy.showMessageCenterCampaign(campaign);
        // Next activity started should be the ConversationActivity
        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, ConversationActivity.class));
        Robolectric.flushForegroundThreadScheduler();
        ActivityController<ConversationActivity> activityController = Robolectric.buildActivity(ConversationActivity.class, nextIntent);
        ConversationActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);
        assertEquals(activity.getRequestedOrientation(), ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
    }

    @Test
    public void testConversationMessageCenter() throws Exception {

        Date futureDate = SwrveTestUtils.parseDate("2035/1/1 00:00");
        doReturn(futureDate).when(swrveSpy).getNow();

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_and_conversation_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        // No MessageCenter campaigns as they have both finished
        List<SwrveBaseCampaign> campaigns = swrveSpy.getMessageCenterCampaigns();
        assertEquals(0, campaigns.size());

        Date today = new Date();
        doReturn(today).when(swrveSpy).getNow();
        assertEquals(2, swrveSpy.getMessageCenterCampaigns().size());

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        SwrveConversationCampaign campaign = (SwrveConversationCampaign) swrveSpy.getMessageCenterCampaigns().get(1);
        assertEquals(SwrveCampaignState.Status.Unseen, campaign.getStatus());
        assertEquals("Conversation subject", campaign.getSubject());
        swrveSpy.showMessageCenterCampaign(campaign);
        // Next activity started should be the ConversationActivity
        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, ConversationActivity.class));
        Robolectric.flushForegroundThreadScheduler();

        assertEquals(1, campaign.getImpressions());
        assertEquals(SwrveCampaignState.Status.Seen, campaign.getStatus());
        // We can still get the conversation, even though the rules specify a limit of 1 impression
        assertEquals(campaign.getId(), swrveSpy.getMessageCenterCampaigns().get(1).getId());

        // Remove the campaign, we will never get it again
        swrveSpy.removeMessageCenterCampaign(campaign);
        assertFalse(swrveSpy.getMessageCenterCampaigns().contains(campaign));
        assertEquals(SwrveCampaignState.Status.Deleted, campaign.getStatus());
    }

    @Test
    public void testDisplayConversationOrMessageForEvent() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_and_conversation_event.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        assertEquals(2, swrveSpy.campaigns.size());

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        swrveSpy._event("Swrve.currency_given");

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        // Test has a conversation and IAM triggered with Swrve.currency_given, but only the conversation should be started.
        // Next activity started should be the ConversationActivity
        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, ConversationActivity.class));
    }

    @Test
    public void testConversationsActivityHonoursLandscapeOrientation() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_and_conversation_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        assertEquals(2, swrveSpy.getMessageCenterCampaigns().size());

        swrveSpy.config.setOrientation(SwrveOrientation.Landscape);
        SwrveConversationCampaign campaign = (SwrveConversationCampaign) swrveSpy.getMessageCenterCampaigns().get(1);
        swrveSpy.showMessageCenterCampaign(campaign);
        // Next activity started should be the ConversationActivity
        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, ConversationActivity.class));
        Robolectric.flushForegroundThreadScheduler();
        ActivityController<ConversationActivity> activityController = Robolectric.buildActivity(ConversationActivity.class, nextIntent);
        ConversationActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            assertEquals(activity.getRequestedOrientation(), ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        } else {
            assertEquals(activity.getRequestedOrientation(), ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    @Test
    public void testConversationPriorityInverse() throws Exception {
        // https://swrvedev.jira.com/browse/SWRVE-10432
        // We were not clearing the bucket of candidate messages, ever...
        // Test that the same problem does not affect conversations

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_priority_reverse.json");

        // Highest priority conversation first
        SwrveConversation conversation1 = swrveSpy.getConversationForEvent("Swrve.buy_in", null);
        assertNotNull(conversation1);
        assertEquals(103, conversation1.getId());
        conversation1.getCampaign().messageWasShownToUser();

        // Second highest conversation
        SwrveConversation conversation2 = swrveSpy.getConversationForEvent("Swrve.buy_in", null);
        assertNotNull(conversation2);
        assertEquals(104, conversation2.getId());
        conversation2.getCampaign().messageWasShownToUser();

        // Lowest conversation (out of 3)
        SwrveConversation conversation3 = swrveSpy.getConversationForEvent("Swrve.buy_in", null);
        assertNotNull(conversation3);
        assertEquals(102, conversation3.getId());
        conversation3.getCampaign().messageWasShownToUser();

        // Highest IAM
        SwrveMessage message1 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message1);
        assertEquals(1, message1.getId());
    }

    @Test
    public void testConversationPriority() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_priority.json");

        // Highest priority conversation first
        SwrveConversation conversation1 = swrveSpy.getConversationForEvent("Swrve.buy_in", null);
        assertNotNull(conversation1);
        assertEquals(103, conversation1.getId());
        conversation1.getCampaign().messageWasShownToUser();

        // Second highest conversation
        SwrveConversation conversation2 = swrveSpy.getConversationForEvent("Swrve.buy_in", null);
        assertNotNull(conversation2);
        assertEquals(102, conversation2.getId());
        conversation2.getCampaign().messageWasShownToUser();

        // Lowest conversation (out of 3)
        SwrveConversation conversation3 = swrveSpy.getConversationForEvent("Swrve.buy_in", null);
        assertNotNull(conversation3);
        assertEquals(104, conversation3.getId());
        conversation3.getCampaign().messageWasShownToUser();

        // Highest IAM
        SwrveMessage message1 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message1);
        assertEquals(1, message1.getId());
    }

    @Test
    public void testDisplayConversationOrMessageForAutoShow() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_and_conversation_auto.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        assertEquals(2, swrveSpy.campaigns.size());

        swrveSpy.campaignsAndResourcesInitialized = true;
        swrveSpy.autoShowMessagesEnabled = true;
        swrveSpy.autoShowMessages();
        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        // Test has a conversation and IAM triggered with Swrve.Messages.showAtSessionStart, but only the conversation should be started.
        // Next activity started should be the ConversationActivity
        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(new ComponentName(mActivity, ConversationActivity.class), nextIntent.getComponent());
        assertFalse(swrveSpy.autoShowMessagesEnabled);
    }

    @Test
    public void testAutomaticDisplayEvents() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_autoshow.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");
        for (int i = 0; i < 3; i++) {

            swrveSpy.campaignsAndResourcesInitialized = true;
            swrveSpy.autoShowMessagesEnabled = true;
            swrveSpy.autoShowMessages();

            Intent nextIntent = mShadowActivity.getNextStartedActivity();
            if (i == 2) {
                assertNull(nextIntent);
            } else {
                assertNotNull(nextIntent);
                assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, SwrveInAppMessageActivity.class));

                // Should get an in-app message (and display it)
                Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> pair = createIAMActivityFromIntent(nextIntent);
                dismissInAppMessage(pair.second);
            }
        }
    }

    @Test
    public void testMessageLeftOpen() throws Exception {

        Date today = new Date();
        doReturn(today).when(swrveSpy).getNow();

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        Date later60 = new Date(today.getTime() + 61000l);
        doReturn(later60).when(swrveSpy).getNow();

        swrveSpy._event("Swrve.currency_given");
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
        SwrveMessageView view = (SwrveMessageView) parentView.getChildAt(0);
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

    @Test
    public void testTooSoonAfterLaunchRuleAfterReload() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        // Do not return any until delay_first_message
        SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // To the future!
        Date later61 = new Date(System.currentTimeMillis() + 61000);
        doReturn(later61).when(swrveSpy).getNow();
        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
        assertEquals(165, message.getId());

        swrveSpy.refreshCampaignsAndResources();

        message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
        assertEquals(165, message.getId());
    }

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

    @Test
    public void testCacheFolder() {
        File newCacheDir = new File("this_is_it");
        swrveSpy.config.setCacheDir(newCacheDir);

        Context context = spy(mActivity);
        when(context.checkPermission(eq(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), anyInt(), anyInt())).thenReturn(PackageManager.PERMISSION_DENIED);
        assertEquals(mActivity.getCacheDir(), swrveSpy.getCacheDir(context));

        when(context.checkPermission(eq(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), anyInt(), anyInt())).thenReturn(PackageManager.PERMISSION_GRANTED);
        assertEquals(newCacheDir, swrveSpy.getCacheDir(context));
    }

    @Test
    public void testReloadCampaigns() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");
        swrveSpy.refreshCampaignsAndResources();
        // Same number of campaigns
        assertEquals(1, swrveSpy.campaigns.size());
    }

    @Test
    public void testImpressions() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_impressions.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");

        Date later = new Date(System.currentTimeMillis() + 61000l);
        doReturn(later).when(swrveSpy).getNow();
        for (int i = 0; i < 2; i++) {
            SwrveMessage message = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
            assertNotNull(message);
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
}
