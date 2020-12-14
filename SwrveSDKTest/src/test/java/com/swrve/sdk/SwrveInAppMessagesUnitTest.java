package com.swrve.sdk;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

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
        swrveSpy.getNow();
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
    public void testGetMessageForNonExistingTrigger() throws Exception  {
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

}
