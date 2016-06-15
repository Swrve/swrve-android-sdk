package com.swrve.sdk;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.conversations.ui.ConversationFragment;
import com.swrve.sdk.conversations.ui.ConversationRelativeLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.util.ActivityController;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConversationActivityTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        Mockito.doNothing().when(swrveSpy).downloadAssets(Mockito.anySet()); // assets are manually mocked
        swrveSpy.init(mActivity);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testActivityLifecycle() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        ActivityController<ConversationActivity> conversationActivityController = Robolectric.buildActivity(ConversationActivity.class);
        Intent intent = new Intent(RuntimeEnvironment.application, ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("conversation", conversation);
        ConversationActivity conversationActivity = conversationActivityController.withIntent(intent).create().start().visible().get();
        assertNotNull(conversationActivity);

        conversationActivityController.resume();

        assertFalse(conversationActivity.isFinishing());
        conversationActivityController.pause().stop().destroy();
    }

    @Test
    public void testCreateActivityWithNoConvAndFinishes() throws Exception {

        ActivityController<ConversationActivity> conversationActivityController = Robolectric.buildActivity(ConversationActivity.class);
        Intent intent = new Intent(RuntimeEnvironment.application, ConversationActivity.class);
        ConversationActivity conversationActivity = conversationActivityController.withIntent(intent).create().start().visible().get();
        assertNotNull(conversationActivity);
        assertTrue(conversationActivity.isFinishing());
    }

    @Test
    public void testOnBackPressed() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        ActivityController<ConversationActivity> conversationActivityController = Robolectric.buildActivity(ConversationActivity.class);
        Intent intent = new Intent(RuntimeEnvironment.application, ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("conversation", conversation);
        ConversationActivity conversationActivity = conversationActivityController.withIntent(intent).create().start().visible().get();
        assertNotNull(conversationActivity);

        assertFalse(conversationActivity.isFinishing());
        conversationActivity.onBackPressed();
        assertTrue(conversationActivity.isFinishing());
    }

    @Test
    public void testConfigurationChange() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        ActivityController<ConversationActivity> conversationActivityController1 = Robolectric.buildActivity(ConversationActivity.class);
        Intent intent1 = new Intent(RuntimeEnvironment.application, ConversationActivity.class);
        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent1.putExtra("conversation", conversation);
        conversationActivityController1.withIntent(intent1).create().start().visible();
        ConversationActivity conversationActivityOrientation1 = conversationActivityController1.get();
        assertNotNull(conversationActivityOrientation1);

        // toggle orientation
        int currentOrientation = conversationActivityOrientation1.getResources().getConfiguration().orientation;
        boolean isPortraitOrUndefined = currentOrientation == Configuration.ORIENTATION_PORTRAIT || currentOrientation == Configuration.ORIENTATION_UNDEFINED;
        int toOrientation = isPortraitOrUndefined ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
        RuntimeEnvironment.application.getResources().getConfiguration().orientation = toOrientation;

        Bundle bundle = new Bundle();
        conversationActivityController1.saveInstanceState(bundle).pause().stop().destroy();

        ActivityController<ConversationActivity> conversationActivityController2 = Robolectric.buildActivity(ConversationActivity.class);
        Intent intent2 = new Intent(RuntimeEnvironment.application, ConversationActivity.class);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent2.putExtra("conversation", conversation);
        conversationActivityController2.withIntent(intent2).create(bundle).start().visible().restoreInstanceState(bundle).resume();
        ConversationActivity conversationActivityOrientation2 = conversationActivityController2.get();
        assertNotNull(conversationActivityOrientation2);
    }

    @Test
    @Ignore
    public void testConversationStartedFromSDK() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        assertNotNull(swrveSpy.campaigns);
        // Campaign will be displayed on SDK init!
        // Next activity started should be the ConversationActivity
        ShadowActivity shadowMainActivity = Shadows.shadowOf(mActivity);
        Intent nextIntent = shadowMainActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getComponent(), new ComponentName(mActivity, ConversationActivity.class));

        // Impressions should have increased by 1
        assertEquals(1, swrveSpy.campaigns.get(0).getImpressions());
    }

    @Test
    public void testConversationWidths() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        assertNotNull(swrveSpy.campaigns);
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        ActivityController<ConversationActivity> conversationActivityController = Robolectric.buildActivity(ConversationActivity.class);
        Intent intent = new Intent(RuntimeEnvironment.application, ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("conversation", conversation);
        ConversationActivity conversationActivity = conversationActivityController.withIntent(intent).create().start().visible().get();
        assertNotNull(conversationActivity);

        ConversationFragment fragment = conversationActivity.getConversationFragment();
        assertTrue(fragment.getView() instanceof ConversationRelativeLayout);
        ConversationRelativeLayout conversationRelativeLayout = (ConversationRelativeLayout)fragment.getView();

        ViewGroup.LayoutParams outsideParams = conversationRelativeLayout.getLayoutParams();
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, outsideParams.width);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, outsideParams.height);

        View conversationLayoutModal = conversationRelativeLayout.findViewById(com.swrve.sdk.conversations.R.id.swrve__conversation_modal);
        RelativeLayout.LayoutParams modalParams = (RelativeLayout.LayoutParams) conversationLayoutModal.getLayoutParams();
        assertEquals(0, modalParams.topMargin);
        assertEquals(0, modalParams.bottomMargin);

        // increase the width to over the max and refresh the layout
        int maxModalWidthPx = mActivity.getResources().getDimensionPixelSize(com.swrve.sdk.conversations.R.dimen.swrve__conversation_max_modal_width);
        outsideParams.width = maxModalWidthPx + 1;
        conversationRelativeLayout.requestLayout();

        conversationLayoutModal = conversationRelativeLayout.findViewById(com.swrve.sdk.conversations.R.id.swrve__conversation_modal);
        modalParams = (RelativeLayout.LayoutParams) conversationLayoutModal.getLayoutParams();
        int topBottomPaddingPx = mActivity.getResources().getDimensionPixelSize(com.swrve.sdk.conversations.R.dimen.swrve__conversation_min_modal_top_bottom_padding);
        assertEquals(topBottomPaddingPx, modalParams.topMargin);
        assertEquals(topBottomPaddingPx, modalParams.bottomMargin);
        assertEquals(365, modalParams.width);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, modalParams.height);
    }
}
