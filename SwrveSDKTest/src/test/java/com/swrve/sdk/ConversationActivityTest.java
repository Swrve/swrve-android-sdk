package com.swrve.sdk;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.conversations.ui.ConversationFragment;
import com.swrve.sdk.conversations.ui.ConversationRelativeLayout;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConversationActivityTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        swrveSpy.init(mActivity);
        SwrveCommon.setSwrveCommon(swrveSpy);
    }

    @Test
    public void testActivityLifecycle() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("conversation", conversation);
        ActivityController<ConversationActivity> activityController = Robolectric.buildActivity(ConversationActivity.class, intent);
        ConversationActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        activityController.resume();

        assertFalse(activity.isFinishing());
        activityController.pause().stop().destroy();
    }

    @Test
    public void testCreateActivityWithNoConvAndFinishes() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ConversationActivity.class);
        ActivityController<ConversationActivity> activityController = Robolectric.buildActivity(ConversationActivity.class, intent);
        ConversationActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testOnBackPressed() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("conversation", conversation);
        ActivityController<ConversationActivity> activityController = Robolectric.buildActivity(ConversationActivity.class, intent);
        ConversationActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        assertFalse(activity.isFinishing());
        activity.onBackPressed();
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testOnPauseSendsQueuedEvents() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("conversation", conversation);
        ActivityController<ConversationActivity> activityController = Robolectric.buildActivity(ConversationActivity.class, intent);
        ConversationActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);
        activityController.resume();

        // reset the swrveSpy mock and verify that sendQueuedEvents has been called zero times
        Mockito.reset(swrveSpy);
        verify(swrveSpy, times(0)).sendQueuedEvents();

        // pause activity
        activityController.pause();

        // verify sendQueuedEvents called once
        verify(swrveSpy, times(1)).sendQueuedEvents();
    }

    @Test
    public void testConfigurationChange() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        Intent intent1 = new Intent(ApplicationProvider.getApplicationContext(), ConversationActivity.class);
        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent1.putExtra("conversation", conversation);
        ActivityController<ConversationActivity> activityController1 = Robolectric.buildActivity(ConversationActivity.class, intent1);
        activityController1.create().start().visible();
        ConversationActivity activityOrientation1 = activityController1.get();
        assertNotNull(activityOrientation1);

        // toggle orientation
        int currentOrientation = activityOrientation1.getResources().getConfiguration().orientation;
        boolean isPortraitOrUndefined = currentOrientation == Configuration.ORIENTATION_PORTRAIT || currentOrientation == Configuration.ORIENTATION_UNDEFINED;
        int toOrientation = isPortraitOrUndefined ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
        ApplicationProvider.getApplicationContext().getResources().getConfiguration().orientation = toOrientation;

        Bundle bundle = new Bundle();
        activityController1.saveInstanceState(bundle).pause().stop().destroy();

        Intent intent2 = new Intent(ApplicationProvider.getApplicationContext(), ConversationActivity.class);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent2.putExtra("conversation", conversation);
        ActivityController<ConversationActivity> activityController2 = Robolectric.buildActivity(ConversationActivity.class, intent2);
        activityController2.create(bundle).start().visible().restoreInstanceState(bundle).resume();
        ConversationActivity activityOrientation2 = activityController2.get();
        assertNotNull(activityOrientation2);
    }

    @Test
    @Ignore
    public void testConversationStartedFromSDK() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
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
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        assertNotNull(swrveSpy.campaigns);
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("conversation", conversation);
        ActivityController<ConversationActivity> activityController = Robolectric.buildActivity(ConversationActivity.class, intent);
        ConversationActivity activity = activityController.create().start().visible().get();
        assertNotNull(activity);

        int maxModalWidthPx = mActivity.getResources().getDimensionPixelSize(com.swrve.sdk.conversations.R.dimen.swrve__conversation_max_modal_width);

        ConversationFragment fragment = activity.getConversationFragment();
        assertTrue(fragment.getView() instanceof ConversationRelativeLayout);
        ConversationRelativeLayout conversationRelativeLayout = (ConversationRelativeLayout)fragment.getView();

        // Set width to less than the max for modal activation and refresh the layout
        ViewGroup.LayoutParams outsideParams = conversationRelativeLayout.getLayoutParams();
        outsideParams.width = maxModalWidthPx/2;
        conversationRelativeLayout.requestLayout();

        View conversationLayoutModal = conversationRelativeLayout.findViewById(com.swrve.sdk.conversations.R.id.swrve__conversation_modal);
        RelativeLayout.LayoutParams modalParams = (RelativeLayout.LayoutParams) conversationLayoutModal.getLayoutParams();
        assertEquals(25, modalParams.topMargin);
        assertEquals(25, modalParams.bottomMargin);

        // increase the width to over the max and refresh the layout
        outsideParams.width = maxModalWidthPx + 1;
        conversationRelativeLayout.requestLayout();

        Shadows.shadowOf(activity.getMainLooper()).idle();

        conversationLayoutModal = conversationRelativeLayout.findViewById(com.swrve.sdk.conversations.R.id.swrve__conversation_modal);
        modalParams = (RelativeLayout.LayoutParams) conversationLayoutModal.getLayoutParams();
        int topBottomPaddingPx = mActivity.getResources().getDimensionPixelSize(com.swrve.sdk.conversations.R.dimen.swrve__conversation_min_modal_top_bottom_padding);
        assertEquals(topBottomPaddingPx, modalParams.topMargin);
        assertEquals(topBottomPaddingPx, modalParams.bottomMargin);
        assertEquals(365, modalParams.width);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, modalParams.height);
    }

    @Test
    public void testShowConversation() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);

        boolean success = ConversationActivity.showConversation(mActivity, conversation, null);
        assertTrue("Failed to show conversation. Check logs.", success);
        Intent intent = mShadowActivity.getNextStartedActivity();
        assertEquals(intent.getComponent().getClassName(), "com.swrve.sdk.conversations.ui.ConversationActivity");

        ConversationAtom unknownAtom = new ConversationAtom("tag", ConversationAtom.TYPE.UNKNOWN, new ConversationStyle());

        // Add an unknown atom to first page
        ArrayList<ConversationAtom> conversationAtoms = conversation.getFirstPage().getContent();
        conversationAtoms.add(unknownAtom);
        success = ConversationActivity.showConversation(mActivity, conversation, null);
        assertFalse("Conversation should not be shown with UNKNOWN atoms", success);

        // Remove unknown atom and test again
        conversationAtoms.remove(conversationAtoms.size() - 1);
        success = ConversationActivity.showConversation(mActivity, conversation, null);
        assertTrue("Failed to show conversation. Check logs.", success);
        intent = mShadowActivity.getNextStartedActivity();
        assertEquals(intent.getComponent().getClassName(), "com.swrve.sdk.conversations.ui.ConversationActivity");

        // Add to a new page with unknown content and test again
        ConversationPage newPage = new ConversationPage();
        ArrayList<ConversationAtom> contentList = new ArrayList<>();
        contentList.add(unknownAtom);
        newPage.setContent(contentList);
        conversation.getPages().add(newPage);
        success = ConversationActivity.showConversation(mActivity, conversation, null);
        assertFalse("Conversation should not be shown with UNKNOWN atoms", success);
    }
}
