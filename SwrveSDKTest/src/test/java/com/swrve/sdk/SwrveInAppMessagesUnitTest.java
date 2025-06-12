package com.swrve.sdk;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static com.swrve.sdk.SwrveTrackingState.STARTED;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveButtonView;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveInAppCampaign;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageView;
import com.swrve.sdk.messaging.SwrveOrientation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowApplication;

import java.io.File;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SwrveInAppMessagesUnitTest extends SwrveBaseTest {

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
                callback.complete(null, true);
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
        verify(assetsManagerImagesSpy, Mockito.atLeastOnce()).downloadAssets(setArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        assertEquals(3, setArgumentCaptor.getValue().size()); // 2 of the image items are the same, so only 3 total queued instead of 4
    }

    @Test
    public void testAssetDownloadLimits() throws Exception {

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
                Set<String> assetsDownloaded = new HashSet<>();
                assetsDownloaded.add("8721fd4e657980a5e12d498e73aed6e6a565dfca");
                assetsDownloaded.add("97c5df26c8e8fcff8dbda7e662d4272a6a94af7e");
                callback.complete(assetsDownloaded, true);
                callbackBool.set(true);
            }
        };
        SwrveAssetsManager assetsManagerImagesSpy = Mockito.spy(assetsManager);
        swrveSpy.swrveAssetsManager = assetsManagerImagesSpy;
        assertEquals(150, swrveSpy.DEFAULT_ASSET_DOWNLOAD_LIMIT);

        swrveSpy.init(mActivity);

        // "campaign.json" has 4 assets, 2 of which are the same - so a total of 3 required:
        // "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca" and "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e"
        for (int i = 0; i < swrveSpy.DEFAULT_ASSET_DOWNLOAD_LIMIT + 1; i++) {
            swrveSpy.multiLayerLocalStorage.incrementAssetDownloadCount("42e6e1cb07e0841aeae695be94f4355b67ee6cdb", System.currentTimeMillis());
        }
        assertEquals(151, swrveSpy.multiLayerLocalStorage.getAssetDownloadCount("42e6e1cb07e0841aeae695be94f4355b67ee6cdb"));
        assertEquals(0, swrveSpy.multiLayerLocalStorage.getAssetDownloadCount("8721fd4e657980a5e12d498e73aed6e6a565dfca"));
        assertEquals(0, swrveSpy.multiLayerLocalStorage.getAssetDownloadCount("97c5df26c8e8fcff8dbda7e662d4272a6a94af7e"));

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign.json");
        await().untilTrue(callbackBool);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "Swrve.asset_download_limit_exceeded");
        SwrveTestUtils.assertQueueEvent(swrveSpy, "event", parameters, null);

        ArgumentCaptor<Set> setArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<SwrveAssetsCompleteCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(SwrveAssetsCompleteCallback.class);
        verify(assetsManagerImagesSpy, Mockito.atLeastOnce()).downloadAssets(setArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        Set<SwrveAssetsQueueItem> set = setArgumentCaptor.getValue();
        assertEquals(2, set.size()); // only 2 queued
        assertTrue(set.contains(new SwrveAssetsQueueItem(102, "8721fd4e657980a5e12d498e73aed6e6a565dfca", "8721fd4e657980a5e12d498e73aed6e6a565dfca", true, false)));
        assertTrue(set.contains(new SwrveAssetsQueueItem(102, "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", true, false)));

        assertEquals(151, swrveSpy.multiLayerLocalStorage.getAssetDownloadCount("42e6e1cb07e0841aeae695be94f4355b67ee6cdb"));
        assertEquals(1, swrveSpy.multiLayerLocalStorage.getAssetDownloadCount("8721fd4e657980a5e12d498e73aed6e6a565dfca"));
        assertEquals(1, swrveSpy.multiLayerLocalStorage.getAssetDownloadCount("97c5df26c8e8fcff8dbda7e662d4272a6a94af7e"));
    }

    @Test
    public void testDownloadingPersonalizedDynamicUrls() throws Exception {
        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();
        SwrveConfig config = new SwrveConfig();

        SwrveInAppMessageConfig inAppConfig = new SwrveInAppMessageConfig.Builder()
                .personalizationProvider(eventPayload -> {
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
                callback.complete(null, true);
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
        verify(assetsManagerImagesSpy, Mockito.atLeastOnce()).downloadAssets(setArgumentCaptor.capture(), callbackArgumentCaptor.capture());

        HashSet<SwrveAssetsQueueItem> assetsQueueItemHashSet = (HashSet<SwrveAssetsQueueItem>) setArgumentCaptor.getValue();
        assertEquals(4, assetsQueueItemHashSet.size()); // all of the fallback assets are the same. the only new assets are dynamic urls, one should not be present as it fails personalization
        int randomInt = new Random().nextInt(1000); // random int verifies the set.contains is based off of name/digest/isImage properties only, and campaignId is not used.
        assertTrue(assetsQueueItemHashSet.contains(new SwrveAssetsQueueItem(randomInt, SwrveHelper.sha1("https://fakeitem/asset1.png".getBytes()), "https://fakeitem/asset1.png", true, true)));
        assertTrue(assetsQueueItemHashSet.contains(new SwrveAssetsQueueItem(randomInt, SwrveHelper.sha1("https://fakeitem/asset2.png".getBytes()), "https://fakeitem/asset2.png", true, true)));
        assertTrue(assetsQueueItemHashSet.contains(new SwrveAssetsQueueItem(randomInt, SwrveHelper.sha1("https://fakeitem/asset3.png".getBytes()), "https://fakeitem/asset3.png", true, true)));
        assertTrue(assetsQueueItemHashSet.contains(new SwrveAssetsQueueItem(randomInt, "8721fd4e657980a5e12d498e73aed6e6a565dfca", "8721fd4e657980a5e12d498e73aed6e6a565dfca", true, false)));
    }

    @Test
    public void testCheckAssetDownloadLimits() throws Exception {

        swrveSpy.init(mActivity);

        // filterExcessiveAssetDownloads is skipped
        Set<SwrveAssetsQueueItem> assetsQueue = getDummyAssetQueue(4);
        swrveSpy.campaignsAndResourcesAssetDownloadLimit = -1;
        swrveSpy.checkAssetDownloadLimits(assetsQueue);
        assertTrue(assetsQueue.size() == 4);
        verify(swrveSpy, never()).filterExcessiveAssetDownloads(assetsQueue);

        // asset queue is wiped
        swrveSpy.campaignsAndResourcesAssetDownloadLimit = 0;
        assetsQueue = getDummyAssetQueue(4);
        swrveSpy.checkAssetDownloadLimits(assetsQueue);
        assertTrue(assetsQueue.size() == 0);
        verify(swrveSpy, never()).filterExcessiveAssetDownloads(assetsQueue);

        // filterExcessiveAssetDownloads is called
        swrveSpy.campaignsAndResourcesAssetDownloadLimit = 10;
        assetsQueue = getDummyAssetQueue(4);
        swrveSpy.checkAssetDownloadLimits(assetsQueue);
        assertTrue(assetsQueue.size() == 4);
        verify(swrveSpy, atLeastOnce()).filterExcessiveAssetDownloads(assetsQueue);
    }

    private Set<SwrveAssetsQueueItem> getDummyAssetQueue(int numAssets) {
        Set<SwrveAssetsQueueItem> assetsQueue = new HashSet<>();
        for (int i = 0; i < numAssets; i++) {
            assetsQueue.add(new SwrveAssetsQueueItem(102, "asset" + i, "asset" + i, true, false));
        }
        return assetsQueue;
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
    public void testMessagePriority() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_priority_2.json");
        // Highest priority first
        SwrveMessage message1 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message1);
        assertEquals(1, message1.getId());
        swrveSpy.messageWasShownToUser(message1.getFormats().get(0));

        // Second highest priority
        SwrveMessage message2 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message2);
        assertEquals(2, message2.getId());
        swrveSpy.messageWasShownToUser(message2.getFormats().get(0));

    }

    @Test
    public void testMessagePriorityInverse() throws Exception {
        // https://emailabove.jira.com/browse/MOBILE-10432
        // We were not clearing the bucket of candidate messages, ever...

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_priority_reverse.json");
        // Highest priority first
        SwrveMessage message1 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message1);
        assertEquals(2, message1.getId());
        swrveSpy.messageWasShownToUser(message1.getFormats().get(0));

        SwrveMessage message2 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message2);
        assertEquals(2, message2.getId());
        swrveSpy.messageWasShownToUser(message2.getFormats().get(0));

        SwrveMessage message3 = (SwrveMessage) swrveSpy.getBaseMessageForEvent("Swrve.buy_in");
        assertNotNull(message3);
        assertEquals(1, message3.getId());
        swrveSpy.messageWasShownToUser(message3.getFormats().get(0));
    }

    @Test
    public void testMessageCenterWithOnlyNonMessageCenterCampaigns() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_event.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        assertEquals(0, swrveSpy.getMessageCenterCampaigns().size());
        assertEquals(0, swrveSpy.getInAppMessageCenterCampaigns(SwrveOrientation.Both, null).size());
    }

    @Test
    public void testIAMMessageCenter() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        Date today = new Date();
        doReturn(today).when(swrveSpy).getNow();
        assertEquals(1, swrveSpy.getMessageCenterCampaigns().size());
        assertEquals(1, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Both).size());
        assertEquals(1, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape).size());
        assertEquals(1, SwrveSDK.getInAppMessageCenterCampaigns(SwrveOrientation.Both, null).size());

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        SwrveInAppCampaign campaign = (SwrveInAppCampaign) swrveSpy.getMessageCenterCampaign(102, null);
        assertEquals(SwrveCampaignState.Status.Unseen, campaign.getStatus());
        assertNotNull(campaign.getDownloadDate());
        assertEquals("IAM subject", campaign.getMessageCenterDetails().getSubject());
        assertEquals(5, campaign.getPriority());
        assertEquals("Kindle", campaign.getName());

        assertEquals(5, campaign.getPriority());
        assertEquals(1362671700000L, campaign.getStartDate().getTime());
        assertEquals(1927994560000L, campaign.getEndDate().getTime());
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

    @Test
    public void testIAMMessageCenterIsActive() throws Exception {

        Date dateUTC = Date.from(Instant.parse("2024-09-10T12:00:00Z")); // Ensure Z is added for UTC
        doReturn(dateUTC).when(swrveSpy).getNow();
        swrveSpy.initialisedTime = dateUTC;

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_blackouts_and_intervals.json", "dummyAsset");

        // Get the active campaign and assert if can be shown
        SwrveBaseCampaign campaign = SwrveSDK.getMessageCenterCampaign(100, null);
        assertNotNull(campaign);
        assertTrue(SwrveSDK.showMessageCenterCampaign(campaign));

        // campaign black out date is 2024-09-11T00:00:00Z so move current time to that date
        dateUTC = Date.from(Instant.parse("2024-09-11T12:00:00Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        assertFalse(SwrveSDK.showMessageCenterCampaign(campaign));

        // campaign end date is 2050-09-10T00:00:00Z so move current time just BEFORE that
        dateUTC = Date.from(Instant.parse("2050-09-09T12:00:00Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        assertTrue(SwrveSDK.showMessageCenterCampaign(campaign));

        // campaign end date is 2050-09-10T00:00:00Z so move current time just beyond that
        dateUTC = Date.from(Instant.parse("2050-09-10T12:00:00Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        assertFalse(SwrveSDK.showMessageCenterCampaign(campaign));
    }

    private Pair<ActivityController<SwrveInAppMessageActivity>, SwrveInAppMessageActivity> createIAMActivityFromIntent(Intent intent) {
        ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, intent);
        return new Pair(activityController, activityController.create().start().visible().get());
    }

    @Test
    public void testIAMMessageCenterWithPersonalization() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        Date today = new Date();
        doReturn(today).when(swrveSpy).getNow();
        assertEquals(1, swrveSpy.getMessageCenterCampaigns().size());
        assertEquals(1, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Both).size());
        assertEquals(1, swrveSpy.getMessageCenterCampaigns(SwrveOrientation.Landscape).size());

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        SwrveInAppCampaign campaign = (SwrveInAppCampaign) swrveSpy.getMessageCenterCampaign(102, null);
        assertEquals(SwrveCampaignState.Status.Unseen, campaign.getStatus());
        assertEquals("IAM subject", campaign.getMessageCenterDetails().getSubject());
        assertEquals(5, campaign.getPriority());

        HashMap<String, String> testProperties = new HashMap<>();
        testProperties.put("test", "working");

        swrveSpy.showMessageCenterCampaign(campaign, testProperties);
        Robolectric.flushForegroundThreadScheduler();

        // Next activity started should be the a SwrveInAppMessageActivity
        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);

        Bundle extras = nextIntent.getExtras();
        HashMap<String, String> personalizationResponse = (HashMap<String, String>) extras.getSerializable(SwrveInAppMessageActivity.SWRVE_PERSONALISATION_KEY);
        assertNotNull(personalizationResponse);
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
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");

        SwrveInAppCampaign campaign = (SwrveInAppCampaign) swrveSpy.getMessageCenterCampaign(102, null);
        assertEquals(SwrveCampaignState.Status.Unseen, campaign.getStatus());

        // Mark the campaign as seen programmatically (for custom IAM renderers)
        swrveSpy.markMessageCenterCampaignAsSeen(campaign);
        assertEquals(SwrveCampaignState.Status.Seen, campaign.getStatus());
    }

    @Test
    public void testMarkMessageCenterCampaignAsSeen() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        List<SwrveInAppCampaign> campaigns =  SwrveSDK.getInAppMessageCenterCampaigns(SwrveOrientation.Both, null);
        assertNotNull(campaigns);
        assertEquals(1, campaigns.size());
        SwrveInAppCampaign campaign = campaigns.get(0);
        assertEquals(102, campaign.getId());
        assertEquals(SwrveCampaignState.Status.Unseen, campaign.getStatus());
        SwrveSDK.markMessageCenterCampaignAsSeen(campaign.getId());
        assertEquals(SwrveCampaignState.Status.Seen, campaign.getStatus());
    }

    @Test
    public void testRemoveMessageCenterCampaign() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_message_center.json",
                "42e6e1cb07e0841aeae695be94f4355b67ee6cdb", "8721fd4e657980a5e12d498e73aed6e6a565dfca", "97c5df26c8e8fcff8dbda7e662d4272a6a94af7e", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        List<SwrveInAppCampaign> campaigns =  SwrveSDK.getInAppMessageCenterCampaigns(SwrveOrientation.Both, null);
        assertNotNull(campaigns);
        assertEquals(1, campaigns.size());
        SwrveInAppCampaign campaign = campaigns.get(0);
        assertEquals(102, campaign.getId());
        assertNotNull(campaign);
        SwrveSDK.removeMessageCenterCampaign(campaign.getId());
        campaigns =  SwrveSDK.getInAppMessageCenterCampaigns(SwrveOrientation.Both, null);
        assertNotNull(campaigns);
        assertEquals(0, campaigns.size());
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
        swrveSpy.refreshContent(null);
        // Same number of campaigns
        assertEquals(1, swrveSpy.campaigns.size());
    }

    @Test
    public void testFilterCampaignWithGrantedCapabilityButton() throws Exception {
        ShadowApplication shadowApplication = Shadows.shadowOf(mActivity.getApplication());
        doReturn(false).when(swrveSpy).filterStartGeoSDKCampaign(); // mock that the geosdk is integrated

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_capabilities.json", "13c1f62022e248dac7c9a6fac01255140a8360a1");
        // there are 5 campaigns in test file, 2 request capability buttons and one open app settings and one open notification permission settings, and one start geo
        assertEquals(5, swrveSpy.getMessageCenterCampaigns().size());

        verifyCampaign(1, "open app settings", true);
        verifyCampaign(2, "request notification permission", true);
        verifyCampaign(3, "open permission settings", true);
        verifyCampaign(4, "request camera permission", true);
        verifyCampaign(5, "start geo", true);

        shadowApplication.grantPermissions(new String[]{POST_NOTIFICATIONS}); // grant the POST_NOTIFICATIONS permission
        verifyCampaign(1, "open app settings", true);
        verifyCampaign(2, "request notification permission", false);
        verifyCampaign(3, "open permission settings", true);
        verifyCampaign(4, "request camera permission", true);
        verifyCampaign(5, "start geo", true);

        shadowApplication.grantPermissions(new String[]{CAMERA}); // grant the CAMERA permission
        verifyCampaign(1, "open app settings", true);
        verifyCampaign(2, "request notification permission", false);
        verifyCampaign(3, "open permission settings", true);
        verifyCampaign(4, "request camera permission", false);
        verifyCampaign(5, "start geo", true);

        shadowApplication.denyPermissions(new String[]{CAMERA, POST_NOTIFICATIONS}); // deny the POST_NOTIFICATIONS and CAMERA permission
        verifyCampaign(1, "open app settings", true);
        verifyCampaign(2, "request notification permission", true);
        verifyCampaign(3, "open permission settings", true);
        verifyCampaign(4, "request camera permission", true);
        verifyCampaign(5, "start geo", true);

        doReturn(true).when(swrveSpy).filterStartGeoSDKCampaign(); // mock that the geosdk is integrated and already started
        verifyCampaign(1, "open app settings", true);
        verifyCampaign(2, "request notification permission", true);
        verifyCampaign(3, "open permission settings", true);
        verifyCampaign(4, "request camera permission", true);
        verifyCampaign(5, "start geo", false);
    }

    @Test
    public void testFilterCampaignWithStartGeoSDKButton() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_capabilities.json", "13c1f62022e248dac7c9a6fac01255140a8360a1");

        // there are 5 campaigns in test file, 2 request capability buttons and one open app settings and one open notification permission settings, and one start geo
        // Only 4 campaigns will be returned because the 5th one is start geo which is redundant.
        assertEquals(4, swrveSpy.getMessageCenterCampaigns().size());
    }

    private void verifyCampaign(int campaignId, String name, boolean show) {
        SwrveInAppCampaign campaign = (SwrveInAppCampaign) swrveSpy.getMessageCenterCampaign(campaignId, null);
        swrveSpy.showMessageCenterCampaign(campaign);

        Intent nextIntent = mShadowActivity.peekNextStartedActivity();
        if (show) {
            assertNotNull(nextIntent);
            assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, SwrveInAppMessageActivity.class));
            Robolectric.flushForegroundThreadScheduler();
            ActivityController<SwrveInAppMessageActivity> activityController = Robolectric.buildActivity(SwrveInAppMessageActivity.class, nextIntent);
            SwrveInAppMessageActivity activity = activityController.create().start().visible().get();
            assertNotNull(activity);
            assertEquals(campaignId, activity.inAppMessageHandler.message.getCampaign().getId());
            assertEquals(name, activity.inAppMessageHandler.message.getCampaign().getName());
            activity.finish();
            mShadowActivity.clearNextStartedActivities();
        } else {
            assertNull(nextIntent);
        }
    }

    @Test
    public void testMultiLineAssetSystemFontDownload() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_multiline_font_assets.json");
        SwrveMessage message = swrveSpy.getMessageForId(165);
        assertNotNull(message);

        HashSet assets = new HashSet();
        assertFalse(message.areAssetsReady(assets, null));  // missing SomeNativeFont

        assets = new HashSet();
        assets.add("_system_font_");
        assertFalse(message.areAssetsReady(assets, null));  // missing SomeNativeFont

        assets = new HashSet();
        assets.add("SomeNativeFont");
        assertTrue(message.areAssetsReady(assets, null));  // _system_font_ not needed

        assets = new HashSet();
        assets.add("SomeNativeFont");
        assets.add("_system_font_");
        assertTrue(message.areAssetsReady(assets, null));
    }
}
