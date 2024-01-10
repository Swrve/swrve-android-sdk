package com.swrve.sdk;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveEmbeddedMessageConfig;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveCampaignState;
import com.swrve.sdk.messaging.SwrveEmbeddedCampaign;
import com.swrve.sdk.messaging.SwrveMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.manipulation.Ordering;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Any;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SwrveCampaignHoldoutTests extends SwrveBaseTest {

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
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(any(Runnable.class)); // disable rest
        swrveSpy.init(mActivity);
    }

    @Test
    public void testEmbeddedMessageHoldoutCallback() throws Exception {
        final AtomicBoolean embeddedCallbackBool = new AtomicBoolean(false);

        SwrveEmbeddedMessageConfig embeddedMessageConfig = new SwrveEmbeddedMessageConfig.Builder().embeddedListener((context, message, personalizationProperties, isControl) -> {
            assertTrue(isControl);
            embeddedCallbackBool.set(true);
        }).build();

        config.setEmbeddedMessageConfig(embeddedMessageConfig);
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_holdouts.json");
        swrveSpy.event("trigger_embedded");
        // we don't send the impression event if they have implemented the embedded listener and its a control campaign.
        verify(swrveSpy, Mockito.never()).queueMessageImpressionEvent(anyInt(), anyString());
        await().untilTrue(embeddedCallbackBool);
    }

    @Test
    public void testEmbeddedMessageHoldoutNoCallback() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_holdouts.json");

        Map campaignsStateList = swrveSpy.campaignsState;
        SwrveCampaignState campaignState = (SwrveCampaignState) campaignsStateList.get(165); // 165 is id for embedded campaign in above json file
        assertEquals(campaignState.toJSON().get("status"), SwrveCampaignState.Status.Unseen.toString() );
        swrveSpy.event("trigger_embedded");
        assertEquals(campaignState.toJSON().get("status"), SwrveCampaignState.Status.Seen.toString() );

        // control campaign must triggers saveCampaignsState method
        verify(swrveSpy, Mockito.atLeastOnce()).saveCampaignsState(anyString());
        verify(swrveSpy, Mockito.atLeastOnce()).queueMessageImpressionEvent(20, "true");
    }

    @Test
    public void testIAMMessageHoldout() throws Exception {
        initSDK();
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_holdouts.json");

        Map campaignsStateList = swrveSpy.campaignsState;
        SwrveCampaignState campaignState = (SwrveCampaignState) campaignsStateList.get(21); // 21 is id for embedded campaign in above json file
        assertEquals(campaignState.toJSON().get("status"), SwrveCampaignState.Status.Unseen.toString() );
        swrveSpy.event("trigger_iam");
        assertEquals(campaignState.toJSON().get("status"), SwrveCampaignState.Status.Seen.toString() );

        // control campaign must triggers saveCampaignsState method
        verify(swrveSpy, Mockito.atLeastOnce()).saveCampaignsState(anyString());

        verify(swrveSpy, Mockito.atLeastOnce()).queueMessageImpressionEvent(165, "false");
        verify(swrveSpy, Mockito.never()).displaySwrveMessage(any(SwrveMessage.class), anyMap());
    }
}
