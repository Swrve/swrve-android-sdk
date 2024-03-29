package com.swrve.sdk;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Browser;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.ChoiceInputItem;
import com.swrve.sdk.conversations.engine.model.ChoiceInputResponse;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ControlActions;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.StarRating;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.conversations.engine.model.styles.ConversationColorStyle;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.conversations.ui.ConversationButton;
import com.swrve.sdk.conversations.ui.ConversationFragment;
import com.swrve.sdk.conversations.ui.ConversationImageView;
import com.swrve.sdk.conversations.ui.ConversationRatingBar;
import com.swrve.sdk.conversations.ui.ConversationRoundedLinearLayout;
import com.swrve.sdk.conversations.ui.HtmlSnippetView;
import com.swrve.sdk.conversations.ui.MultiValueInputControl;
import com.swrve.sdk.conversations.ui.video.YoutubeVideoView;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ConversationFragmentTest extends SwrveBaseTest {

    private SwrveConversation partialMockSwrveConversation;
    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        Mockito.doReturn(true).when(swrveSpy).restClientExecutorExecute(Mockito.any(Runnable.class)); // disable rest
        SwrveCommon.setSwrveCommon(swrveSpy);
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        assertNotNull(swrveSpy.campaigns);
        SwrveConversation realSwrveConversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(realSwrveConversation);
        partialMockSwrveConversation = spy(realSwrveConversation);
        doReturn(true).when(partialMockSwrveConversation).areAssetsReady(any(Set.class));
    }

    @Test
    public void testControls0Button() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 0, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = fragment.getView().findViewById(R.id.swrve__controls);
        assertEquals(controls.getVisibility(), (View.VISIBLE));
        assertEquals(controls.getChildCount(), (0));
    }

    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testControls1Button() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = fragment.getView().findViewById(R.id.swrve__controls);
        assertEquals(controls.getVisibility(), (View.VISIBLE));
        assertEquals(controls.getChildCount(), (1));
        ConversationButton conversationButton = (ConversationButton) controls.getChildAt(0);
        assertTrue(conversationButton.getModel().getActions().isCall());
        assertEquals(conversationButton.getModel().getActions().getCallUri().toString(), ("tel:0"));
    }

    @Test
    public void testControls2Button() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 2, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = fragment.getView().findViewById(R.id.swrve__controls);
        assertEquals(controls.getVisibility(), (View.VISIBLE));
        assertEquals(controls.getChildCount(), (2));
        ConversationButton conversationButton0 = (ConversationButton) controls.getChildAt(0);
        assertTrue(conversationButton0.getModel().getActions().isCall());
        assertEquals(conversationButton0.getModel().getActions().getCallUri().toString(), ("tel:0"));
        ConversationButton conversationButton1 = (ConversationButton) controls.getChildAt(1);
        assertTrue(conversationButton1.getModel().getActions().isCall());
        assertEquals(conversationButton1.getModel().getActions().getCallUri().toString(), ("tel:1"));
    }

    @Test
    public void testControls3Button() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = fragment.getView().findViewById(R.id.swrve__controls);
        assertEquals(controls.getVisibility(), (View.VISIBLE));
        assertEquals(controls.getChildCount(), (3));
        ConversationButton conversationButton0 = (ConversationButton) controls.getChildAt(0);
        assertTrue(conversationButton0.getModel().getActions().isCall());
        assertEquals(conversationButton0.getModel().getActions().getCallUri().toString(), ("tel:0"));
        ConversationButton conversationButton1 = (ConversationButton) controls.getChildAt(1);
        assertTrue(conversationButton1.getModel().getActions().isCall());
        assertEquals(conversationButton1.getModel().getActions().getCallUri().toString(), ("tel:1"));
        ConversationButton conversationButton2 = (ConversationButton) controls.getChildAt(2);
        assertTrue(conversationButton2.getModel().getActions().isCall());
        assertEquals(conversationButton2.getModel().getActions().getCallUri().toString(), ("tel:2"));
    }

    @Test
    public void testControls3ButtonV4() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = fragment.getView().findViewById(R.id.swrve__controls);
        assertEquals(controls.getVisibility(), (View.VISIBLE));
        assertEquals(controls.getChildCount(), (3));

        ConversationButton conversationButton0 = (ConversationButton) controls.getChildAt(0);
        assertTrue(conversationButton0.getModel().getActions().isCall());
        assertEquals(conversationButton0.getModel().getActions().getCallUri().toString(), ("tel:0"));
        assertEquals("assert gravity of button 0", conversationButton0.getGravity(), (Gravity.CENTER | Gravity.LEFT));
        assertEquals("assert text size of button 0", conversationButton0.getTextSize(), 123.0, 0.0);

        ConversationButton conversationButton1 = (ConversationButton) controls.getChildAt(1);
        assertTrue(conversationButton1.getModel().getActions().isCall());
        assertEquals(conversationButton1.getModel().getActions().getCallUri().toString(), ("tel:1"));
        assertEquals("assert gravity of button 1", conversationButton1.getGravity(), (Gravity.CENTER | Gravity.CENTER));
        assertEquals("assert text size of button 1", conversationButton1.getTextSize(), 123.0, 0.0);

        ConversationButton conversationButton2 = (ConversationButton) controls.getChildAt(2);
        assertTrue(conversationButton2.getModel().getActions().isCall());
        assertEquals(conversationButton2.getModel().getActions().getCallUri().toString(), ("tel:2"));
        assertEquals("assert gravity of button 2", conversationButton2.getGravity(), (Gravity.CENTER | Gravity.RIGHT));
        assertEquals("assert text size of button 2", conversationButton2.getTextSize(), 123.0, 0.0);
    }

    @Test
    public void testCurvedButton() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 10));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = fragment.getView().findViewById(R.id.swrve__controls);
        ConversationButton conversationButton = (ConversationButton) controls.getChildAt(1);
        assertEquals(conversationButton.getBorderRadius(), 2.5, 0.0);

        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "outline", 20));
        fragment = createConversationFragment(partialMockSwrveConversation);
        controls = fragment.getView().findViewById(R.id.swrve__controls);
        conversationButton = (ConversationButton) controls.getChildAt(1);
        assertEquals(conversationButton.getBorderRadius(), 5.0, 0.0);

        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "outline", 100));
        fragment = createConversationFragment(partialMockSwrveConversation);
        controls = fragment.getView().findViewById(R.id.swrve__controls);
        conversationButton = (ConversationButton) controls.getChildAt(1);
        assertEquals(conversationButton.getBorderRadius(), 25.0, 0.0);

        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "outline", 200));
        fragment = createConversationFragment(partialMockSwrveConversation);
        controls = fragment.getView().findViewById(R.id.swrve__controls);
        conversationButton = (ConversationButton) controls.getChildAt(1);
        assertEquals(conversationButton.getBorderRadius(), 25.0, 0.0);
    }

    @Test
    public void testRoundedPage() {
        ArrayList<ConversationPage> pages = getMockContentConversationPages(1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50);
        ConversationColorStyle bgStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#ffffff");
        ConversationColorStyle lbStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#5f68a3");
        ConversationStyle pageStyle0 = new ConversationStyle(20, ConversationStyle.TYPE_SOLID, bgStyle, null, lbStyle); // border 20
        when(pages.get(0).getStyle()).thenReturn(pageStyle0);
        partialMockSwrveConversation.setPages(pages);

        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        ConversationRoundedLinearLayout modal = fragment.getView().findViewById(R.id.swrve__conversation_modal);
        // border 20 means 20% of 25pixels == 5
        assertEquals(modal.getRadius(), 5.0, 0.0);
    }

    @Test
    public void testContentImage() {
        // write a fake image file
        SwrveTestUtils.writeFileToCache(ApplicationProvider.getApplicationContext().getCacheDir(), "value0");
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = fragment.getView().findViewById(R.id.swrve__content);
        assertEquals(content.getVisibility(), (View.VISIBLE));
        assertEquals(content.getChildCount(), (1));
        assertTrue(content.getChildAt(0) instanceof ConversationImageView);
    }

    @Test
    public void testContentHtml() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_HTML, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = fragment.getView().findViewById(R.id.swrve__content);
        assertEquals(content.getVisibility(), (View.VISIBLE));
        assertEquals(content.getChildCount(), (1));
        assertTrue(content.getChildAt(0) instanceof HtmlSnippetView);
    }

    @Test
    public void testContentVideo() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_VIDEO, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = fragment.getView().findViewById(R.id.swrve__content);
        assertEquals(content.getVisibility(), (View.VISIBLE));
        assertEquals(content.getChildCount(), (1));
        assertTrue(content.getChildAt(0) instanceof YoutubeVideoView);
    }

    @Test
    public void testContentSpacer() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_SPACER, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = fragment.getView().findViewById(R.id.swrve__content);
        assertEquals(content.getVisibility(), (View.VISIBLE));
        assertEquals(content.getChildCount(), (1));
        assertEquals(content.getChildAt(0).getLayoutParams().height, (123));
    }

    @Test
    public void testMultiValue() {
        partialMockSwrveConversation.setPages(getMockInputConversationPages(1, ConversationAtom.TYPE.INPUT_MULTIVALUE, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = fragment.getView().findViewById(R.id.swrve__content);
        assertEquals(content.getVisibility(), (View.VISIBLE));
        assertEquals(content.getChildCount(), (1));
        assertTrue(content.getChildAt(0) instanceof MultiValueInputControl);
        MultiValueInputControl multiValueInputControl = (MultiValueInputControl) content.getChildAt(0);

        assertEquals(multiValueInputControl.getChildCount(), (2));
        assertTrue(multiValueInputControl.getChildAt(0) instanceof android.widget.TextView);
        TextView tv = (TextView) multiValueInputControl.getChildAt(0);
        assertEquals(tv.getText().toString(), ("description0"));
        assertEquals(tv.getTextSize(), 123.0, 0.0);

        assertTrue(multiValueInputControl.getChildAt(1) instanceof RadioButton);
        RadioButton rb = (RadioButton) multiValueInputControl.getChildAt(1);
        assertEquals(rb.getText().toString(), ("answer_text0"));
        assertEquals(rb.getTextSize(), 123.0, 0.0);
    }

    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testStarRatingKitKat() {
        partialMockSwrveConversation.setPages(getMockInputConversationPages(1, ConversationAtom.TYPE.INPUT_STARRATING, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout contentLinearLayout = fragment.getView().findViewById(R.id.swrve__content);
        assertEquals(contentLinearLayout.getChildCount(), (1));
        assertTrue(contentLinearLayout.getChildAt(0) instanceof ConversationRatingBar);
        ConversationRatingBar conversationRatingBar = (ConversationRatingBar) contentLinearLayout.getChildAt(0);
        assertEquals(conversationRatingBar.getModel().getValue(), ("value0"));
        assertEquals(conversationRatingBar.getModel().getTag(), ("partialMockSwrveConversation"));
        assertEquals(conversationRatingBar.getModel().getStarColor(), ("#ff0000"));
        assertEquals(conversationRatingBar.getModel().getValue(), ("value0"));
        assertEquals(conversationRatingBar.getRatingBar().getNumStars(), (5));
        assertEquals(conversationRatingBar.getRatingBar().getStepSize(), 0.01, 0.02);

        // test less than zero selected but value only goes to 1.0
        conversationRatingBar.onRatingChanged(conversationRatingBar.getRatingBar(), 0.5f, true);
        assertEquals(conversationRatingBar.getRatingBar().getRating(), 1.0, 0.0);
    }

    @Test
    public void testStarRating() {
        partialMockSwrveConversation.setPages(getMockInputConversationPages(1, ConversationAtom.TYPE.INPUT_STARRATING, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout contentLinearLayout = fragment.getView().findViewById(R.id.swrve__content);
        assertEquals(contentLinearLayout.getChildCount(), (1));
        assertTrue(contentLinearLayout.getChildAt(0) instanceof ConversationRatingBar);
        ConversationRatingBar conversationRatingBar = (ConversationRatingBar) contentLinearLayout.getChildAt(0);
        assertEquals(conversationRatingBar.getModel().getValue(), ("value0"));
        assertEquals(conversationRatingBar.getModel().getTag(), ("partialMockSwrveConversation"));
        assertEquals(conversationRatingBar.getModel().getStarColor(), ("#ff0000"));
        assertEquals(conversationRatingBar.getModel().getValue(), ("value0"));
        assertEquals(conversationRatingBar.getRatingBar().getNumStars(), (5));
        assertEquals(conversationRatingBar.getRatingBar().getStepSize(), 0.01, 0.02);

        // test less than zero selected but value only goes to 1.0
        conversationRatingBar.onRatingChanged(conversationRatingBar.getRatingBar(), 0.5f, true);
        assertEquals(conversationRatingBar.getRatingBar().getRating(), 1.0, 0.0);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testLightboxDefaultColor() {
        ArrayList<ConversationPage> pages = getMockContentConversationPages(2, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50);
        partialMockSwrveConversation.setPages(pages);
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);

        // assert default colors
        Drawable drawable = fragment.getActivity().getWindow().getDecorView().getBackground();
        assertTrue(drawable instanceof ColorDrawable);
        ColorDrawable colorDrawable = (ColorDrawable) drawable;
        int defaultColor = Color.parseColor(ConversationStyle.DEFAULT_LB_COLOR);
        assertEquals(defaultColor, (colorDrawable.getColor()));

        // move to next page
        ConversationPage page1 = pages.get(1);
        fragment.setPage(page1);
        fragment.onResume();

        drawable = fragment.getActivity().getWindow().getDecorView().getBackground();
        assertTrue(drawable instanceof ColorDrawable);
        colorDrawable = (ColorDrawable) drawable;
        defaultColor = Color.parseColor(ConversationStyle.DEFAULT_LB_COLOR);
        assertEquals(defaultColor, (colorDrawable.getColor()));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testLightboxColor() {
        ArrayList<ConversationPage> pages = getMockContentConversationPages(2, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50);
        ConversationColorStyle bgStyle0 = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#ffffff");
        ConversationColorStyle lbStyle0 = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#5f68a3");
        ConversationStyle pageStyle0 = new ConversationStyle(0, ConversationStyle.TYPE_SOLID, bgStyle0, null, lbStyle0);
        when(pages.get(0).getStyle()).thenReturn(pageStyle0);
        ConversationColorStyle bgStyle1 = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#ffffff");
        ConversationColorStyle lbStyle1 = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#a20932");
        ConversationStyle pageStyle1 = new ConversationStyle(0, ConversationStyle.TYPE_SOLID, bgStyle1, null, lbStyle1);
        when(pages.get(1).getStyle()).thenReturn(pageStyle1);

        partialMockSwrveConversation.setPages(pages);
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);

        // assert default colors
        Drawable drawable = fragment.getActivity().getWindow().getDecorView().getBackground();
        assertTrue(drawable instanceof ColorDrawable);
        ColorDrawable colorDrawable = (ColorDrawable) drawable;
        int color = Color.parseColor("#5f68a3");
        assertEquals(color, (colorDrawable.getColor()));

        // move to next page
        ConversationPage page1 = pages.get(1);
        fragment.setPage(page1);
        fragment.onResume();

        drawable = fragment.getActivity().getWindow().getDecorView().getBackground();
        assertTrue(drawable instanceof ColorDrawable);
        colorDrawable = (ColorDrawable) drawable;
        color = Color.parseColor("#a20932");
        assertEquals(color, (colorDrawable.getColor()));
    }

    @Test
    public void testOnResume() {
        ArrayList<ConversationPage> pages = getMockContentConversationPages(2, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50);
        partialMockSwrveConversation.setPages(pages);
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        // first page0
        ConversationPage page0 = pages.get(0);
        assertNotNull(page0);
        fragment.onResume();

        // Go to next page and resume
        ConversationPage page1 = pages.get(1);
        fragment.setPage(page1);
        fragment.onResume();
    }

    @Test
    public void testOnResumeWithMultiValueInput() {
        ArrayList<ConversationPage> pages = getMockInputConversationPages(2, ConversationAtom.TYPE.INPUT_MULTIVALUE, "solid");
        partialMockSwrveConversation.setPages(pages);
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        ConversationPage page0 = pages.get(0);
        assertNotNull(page0);
        fragment.onResume();

        LinearLayout content = fragment.getView().findViewById(R.id.swrve__content);
        MultiValueInputControl multiValueInputControl = (MultiValueInputControl) content.getChildAt(0);
        assertEquals(((android.widget.TextView) multiValueInputControl.getChildAt(0)).getText().toString(), ("description0"));
        assertTrue(multiValueInputControl.getChildAt(1) instanceof RadioButton);
        RadioButton radioButton1 = (RadioButton) multiValueInputControl.getChildAt(1);
        assertEquals(radioButton1.isChecked(), (Boolean.FALSE));
        RadioButton radioButton2 = (RadioButton) multiValueInputControl.getChildAt(2);
        assertEquals(radioButton2.isChecked(), (Boolean.FALSE));

        radioButton2.setChecked(true); // enter user content

        fragment.onResume();

        LinearLayout content2 = fragment.getView().findViewById(R.id.swrve__content);
        MultiValueInputControl multiValueInputControl2 = (MultiValueInputControl) content2.getChildAt(0);
        RadioButton radioButton21 = (RadioButton) multiValueInputControl2.getChildAt(1);
        assertEquals(radioButton21.isChecked(), (Boolean.FALSE));
        RadioButton radioButton22 = (RadioButton) multiValueInputControl2.getChildAt(2);
        assertEquals(radioButton22.isChecked(), (Boolean.TRUE));
    }

    @Test
    public void testOnResumeWithRatingBar() {
        ArrayList<ConversationPage> pages = getMockInputConversationPages(2, ConversationAtom.TYPE.INPUT_STARRATING, "solid");
        partialMockSwrveConversation.setPages(pages);
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        ConversationPage page0 = pages.get(0);
        assertNotNull(page0);
        fragment.onResume();

        LinearLayout contentLinearLayout = fragment.getView().findViewById(R.id.swrve__content);
        assertEquals(contentLinearLayout.getChildCount(), (1));
        assertTrue(contentLinearLayout.getChildAt(0) instanceof ConversationRatingBar);
        ConversationRatingBar conversationRatingBar = (ConversationRatingBar) contentLinearLayout.getChildAt(0);

        // enter user content
        conversationRatingBar.getRatingBar().setRating(3.0f);
        conversationRatingBar.onRatingChanged(conversationRatingBar.getRatingBar(), 3.0f, true);

        fragment.onResume();

        LinearLayout content2 = fragment.getView().findViewById(R.id.swrve__content);
        assertEquals(content2.getChildCount(), (1));
        assertTrue(content2.getChildAt(0) instanceof ConversationRatingBar);
        ConversationRatingBar conversationRatingBar2 = (ConversationRatingBar) content2.getChildAt(0);
        assertEquals(conversationRatingBar2.getRatingBar().getRating(), 3.0, 0.0);
    }

    @Test
    public void testOnBackPressed() {
        partialMockSwrveConversation.setPages(getMockInputConversationPages(1, ConversationAtom.TYPE.INPUT_MULTIVALUE, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);

        HashMap<String, UserInputResult> userData = new HashMap<>();
        UserInputResult result = new UserInputResult();
        result.type = "type";
        result.conversationId = 123;
        result.fragmentTag = "fragmentTag";
        result.pageTag = "pageTag";
        result.result = "result";
        userData.put("key", result);
        fragment.setUserInteractionData(userData);

        fragment.onBackPressed();
    }

    @Ignore("Ignored for now. Threads issue. If you break after performClick it seems to work")
    @Test
    public void testButtonClickWithActionCall() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = fragment.getView().findViewById(R.id.swrve__controls);
        ConversationButton conversationButton = (ConversationButton) controls.getChildAt(0);
        conversationButton.performClick();

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        ConversationActivity activity = Robolectric.buildActivity(ConversationActivity.class).get();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent nextStartedActivity = shadowActivity.getNextStartedActivity(); // for some reason this line is needed before calling peekNextStartedActivity?
        assertNotNull(nextStartedActivity);
        Intent nextIntent = shadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getAction(), (Intent.ACTION_DIAL));
        assertEquals(nextIntent.getDataString(), ("tel:0"));
    }

    @Ignore("Ignored for now. Threads issue. If you break after performClick it seems to work")
    @Test
    public void testButtonClickWithActionVisit() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, ControlActions.VISIT_URL_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = fragment.getView().findViewById(R.id.swrve__controls);
        ConversationButton conversationButton = (ConversationButton) controls.getChildAt(0);
        conversationButton.performClick();

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        ConversationActivity activity = Robolectric.buildActivity(ConversationActivity.class).get();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent nextStartedActivity = shadowActivity.getNextStartedActivity(); // for some reason this line is needed before calling peekNextStartedActivity?
        assertNotNull(nextStartedActivity);
        Intent nextIntent = shadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getAction(), (Intent.ACTION_VIEW));
        assertEquals(nextIntent.getDataString(), ("http://www.swrve.com"));
        assertTrue(nextIntent.hasExtra(Browser.EXTRA_HEADERS));
        // assertEquals(nextIntent.getBundleExtra(Browser.EXTRA_HEADERS), ("some referrer"));
    }

    @Ignore("Ignored for now. Threads issue. If you break after performClick it seems to work")
    @Test
    public void testButtonClickWithActionDeepLink() {
        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, ControlActions.DEEPLINK_ACTION, ConversationAtom.TYPE.CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = fragment.getView().findViewById(R.id.swrve__controls);
        ConversationButton conversationButton = (ConversationButton) controls.getChildAt(0);
        conversationButton.performClick();

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        ConversationActivity activity = Robolectric.buildActivity(ConversationActivity.class).get();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent nextStartedActivity = shadowActivity.getNextStartedActivity(); // for some reason this line is needed before calling peekNextStartedActivity?
        assertNotNull(nextStartedActivity);
        Intent nextIntent = shadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(nextIntent.getAction(), (Intent.ACTION_VIEW));
        assertEquals(nextIntent.getDataString(), ("http://www.swrve.com"));
        assertEquals(nextIntent.getFlags(), (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Test
    public void testCommitMultiInputValueToEvents() {
        partialMockSwrveConversation.setPages(getMockInputConversationPages(2, ConversationAtom.TYPE.INPUT_MULTIVALUE, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);

        LinearLayout content = fragment.getView().findViewById(R.id.swrve__content);
        MultiValueInputControl multiValueInputControl = (MultiValueInputControl) content.getChildAt(0);
        assertEquals(multiValueInputControl.getRadioButtons().size(), (2));
        RadioButton radioButton = multiValueInputControl.getRadioButtons().get(1);
        radioButton.setChecked(true);
        multiValueInputControl.onCheckedChanged(radioButton, true);

        assertEquals(fragment.getUserInteractionData().size(), (1));
        UserInputResult userInputResult = fragment.getUserInteractionData().get("pageTag-partialMockSwrveConversation");
        assertEquals(userInputResult.getType(), ("choice"));
        assertEquals(userInputResult.getConversationId(), (82));
        assertEquals(userInputResult.getPageTag(), ("pageTag"));
        assertEquals(userInputResult.getFragmentTag(), ("partialMockSwrveConversation"));
        assertTrue(userInputResult.getResult() instanceof ChoiceInputResponse);
        ChoiceInputResponse choiceInputResponse = (ChoiceInputResponse) userInputResult.getResult();
        assertEquals(choiceInputResponse.getQuestionID(), ("partialMockSwrveConversation"));
        assertEquals(choiceInputResponse.getAnswerID(), ("answer_id1"));
        assertEquals(choiceInputResponse.getAnswerText(), ("answer_text1"));
        assertEquals(choiceInputResponse.getFragmentTag(), ("partialMockSwrveConversation"));

        fragment.commitUserInputsToEvents();
        assertEquals(fragment.getUserInteractionData().size(), (0));
    }

    @Test
    public void testCommitRatingBarToEvents() {
        partialMockSwrveConversation.setPages(getMockInputConversationPages(1, ConversationAtom.TYPE.INPUT_STARRATING, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout contentLinearLayout = fragment.getView().findViewById(R.id.swrve__content);
        assertEquals(contentLinearLayout.getChildCount(), (1));
        assertTrue(contentLinearLayout.getChildAt(0) instanceof ConversationRatingBar);
        ConversationRatingBar conversationRatingBar = (ConversationRatingBar) contentLinearLayout.getChildAt(0);

        conversationRatingBar.getRatingBar().setRating(3.0f);
        conversationRatingBar.onRatingChanged(conversationRatingBar.getRatingBar(), 3.0f, true);

        assertEquals(fragment.getUserInteractionData().size(), (1));
        UserInputResult userInputResult = fragment.getUserInteractionData().get("pageTag-partialMockSwrveConversation");
        assertEquals(userInputResult.getType(), ("star-rating"));
        assertEquals(userInputResult.getConversationId(), (82));
        assertEquals(userInputResult.getPageTag(), ("pageTag"));
        assertEquals(userInputResult.getFragmentTag(), ("partialMockSwrveConversation"));
        assertTrue(userInputResult.getResult() instanceof Float);
        assertEquals((float)userInputResult.getResult(), 3.0, 0.0);

        fragment.commitUserInputsToEvents();
        assertEquals(fragment.getUserInteractionData().size(), (0));
    }

    @Test
    public void testSystemFont() {
        partialMockSwrveConversation.setPages(getMockInputConversationPages(1, ConversationAtom.TYPE.INPUT_MULTIVALUE, "solid", SwrveConversationConstants.SYSTEM_FONT, ConversationStyle.FONT_NATIVE_STYLE.NORMAL));
        testSystemFontTypeface(Typeface.NORMAL);

        partialMockSwrveConversation.setPages(getMockInputConversationPages(1, ConversationAtom.TYPE.INPUT_MULTIVALUE, "solid", SwrveConversationConstants.SYSTEM_FONT, ConversationStyle.FONT_NATIVE_STYLE.BOLD));
        testSystemFontTypeface(Typeface.BOLD);

        partialMockSwrveConversation.setPages(getMockInputConversationPages(1, ConversationAtom.TYPE.INPUT_MULTIVALUE, "solid", SwrveConversationConstants.SYSTEM_FONT, ConversationStyle.FONT_NATIVE_STYLE.ITALIC));
        testSystemFontTypeface(Typeface.ITALIC);

        partialMockSwrveConversation.setPages(getMockInputConversationPages(1, ConversationAtom.TYPE.INPUT_MULTIVALUE, "solid", SwrveConversationConstants.SYSTEM_FONT, ConversationStyle.FONT_NATIVE_STYLE.BOLDITALIC));
        testSystemFontTypeface(Typeface.BOLD_ITALIC);
    }

    private void testSystemFontTypeface(int expectedTypeface) {
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = fragment.getView().findViewById(R.id.swrve__controls);
        assertEquals(controls.getVisibility(), (View.VISIBLE));
        assertEquals(controls.getChildCount(), (1));
        ConversationButton conversationButton0 = (ConversationButton) controls.getChildAt(0);
        assertEquals(expectedTypeface, conversationButton0.getTypeface().getStyle());
        assertEquals(Typeface.defaultFromStyle(expectedTypeface), conversationButton0.getTypeface());
    }

    private ConversationFragment createConversationFragment(SwrveConversation swrveConversation) {
        ConversationFragment fragment = ConversationFragment.create(swrveConversation);
        assertNotNull(fragment);
        ConversationActivity conversationActivity = Robolectric.buildActivity(ConversationActivity.class).create().start().resume().get();
        fragment.commitConversationFragment(conversationActivity.getSupportFragmentManager());

        Shadows.shadowOf(conversationActivity.getMainLooper()).idle();

        return fragment;
    }

    // Mock pages of type CONTENT
    private ArrayList<ConversationPage> getMockContentConversationPages(int numPages, final int numButtonControls, final Object action, ConversationAtom.TYPE contentType,
                                                                        final String buttonType, final int cornerRadiusPerCent) {
        ConversationColorStyle bgStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#ffffff");
        ConversationColorStyle lbStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, ConversationStyle.DEFAULT_LB_COLOR);
        ConversationStyle pageStyle = new ConversationStyle(0, ConversationStyle.TYPE_SOLID, bgStyle, null, lbStyle);

        ArrayList<ConversationPage> pages = new ArrayList<>();
        for (int i = 0; i < numPages; i++) {
            ConversationPage page = mock(ConversationPage.class);
            when(page.getTag()).thenReturn("pageTag");
            when(page.getTitle()).thenReturn("pageTitle" + i);
            when(page.getStyle()).thenReturn(pageStyle);

            ArrayList<ButtonControl> controls = new ArrayList<ButtonControl>() {
                {
                    for (int j = 0; j < numButtonControls; j++) {
                        ButtonControl buttonControl = mock(ButtonControl.class);
                        ConversationStyle.ALIGNMENT alignment = ConversationStyle.ALIGNMENT.LEFT;
                        alignment = j == 1 ? ConversationStyle.ALIGNMENT.CENTER : alignment;
                        alignment = j == 2 ? ConversationStyle.ALIGNMENT.RIGHT : alignment;
                        when(buttonControl.getStyle()).thenReturn(getDummyAtomStyle(alignment, buttonType, cornerRadiusPerCent));
                        ControlActions controlActions = mock(ControlActions.class);
                        if (action.toString().equals(ControlActions.CALL_ACTION)) {
                            when(controlActions.getCallUri()).thenReturn(Uri.parse("tel:" + j));
                            when(controlActions.isCall()).thenReturn(true);
                        } else if (action.toString().equals(ControlActions.VISIT_URL_ACTION)) {
                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put(ControlActions.VISIT_URL_URI_KEY.toString(), "http://www.swrve.com");
                            hashMap.put(ControlActions.VISIT_URL_REFERER_KEY.toString(), "some referrer");
                            when(controlActions.getVisitDetails()).thenReturn(hashMap);
                            when(controlActions.isVisit()).thenReturn(true);
                        } else if (action.toString().equals(ControlActions.DEEPLINK_ACTION)) {
                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put(ControlActions.DEEPLINK_URL_URI_KEY.toString(), "http://www.swrve.com");
                            when(controlActions.getDeepLinkDetails()).thenReturn(hashMap);
                            when(controlActions.isDeepLink()).thenReturn(true);
                        }
                        when(buttonControl.getActions()).thenReturn(controlActions);
                        when(buttonControl.hasActions()).thenReturn(true);
                        add(buttonControl);
                    }
                }
            };
            when(page.getControls()).thenReturn(controls);

            ArrayList<ConversationAtom> contents = new ArrayList<>();
            Content content = mock(Content.class);
            when(content.getType()).thenReturn(contentType);
            when(content.getHeight()).thenReturn("123");
            when(content.getValue()).thenReturn("value" + i);
            when(content.getStyle()).thenReturn(getDummyAtomStyle(ConversationStyle.ALIGNMENT.LEFT, buttonType, 0));
            contents.add(content);
            when(page.getContent()).thenReturn(contents);

            pages.add(page);
        }
        return pages;
    }

    // Mock pages of type INPUT
    private ArrayList<ConversationPage> getMockInputConversationPages(final int num, ConversationAtom.TYPE contentType, final String buttonType) {
        return getMockInputConversationPages(num, contentType, buttonType, "my_funky_font", ConversationStyle.FONT_NATIVE_STYLE.NORMAL);
    }

    // Mock pages of type INPUT
    private ArrayList<ConversationPage> getMockInputConversationPages(final int num, ConversationAtom.TYPE contentType, final String buttonType,
                                                                      String fontFile, ConversationStyle.FONT_NATIVE_STYLE fontNativeStyle) {
        ConversationColorStyle bgStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#ffffff");
        ConversationColorStyle lbStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, ConversationStyle.DEFAULT_LB_COLOR);
        ConversationStyle pageStyle = new ConversationStyle(0, ConversationStyle.TYPE_SOLID, bgStyle, null, lbStyle);

        ArrayList<ConversationPage> pages = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            ConversationPage page = mock(ConversationPage.class);
            when(page.getTag()).thenReturn("pageTag");
            when(page.getTitle()).thenReturn("pageTitle" + i);
            when(page.getStyle()).thenReturn(pageStyle);

            ArrayList<ButtonControl> controls = new ArrayList<>();
            controls.add(mock(ButtonControl.class));
            when(page.getControls()).thenReturn(controls);

            for (int j = 0; j < page.getControls().size(); j++) {
                ButtonControl control = page.getControls().get(j);
                ConversationStyle.ALIGNMENT alignment = ConversationStyle.ALIGNMENT.LEFT;
                alignment = j == 1 ? ConversationStyle.ALIGNMENT.CENTER : alignment;
                alignment = j == 2 ? ConversationStyle.ALIGNMENT.RIGHT : alignment;
                when(control.getStyle()).thenReturn(getDummyAtomStyle(alignment, buttonType, 0, fontFile, fontNativeStyle));
            }

            ArrayList<ConversationAtom> contents = new ArrayList<>();
            if (contentType.equals(ConversationAtom.TYPE.INPUT_MULTIVALUE)) {
                MultiValueInput inputBase = mock(MultiValueInput.class);
                when(inputBase.getType()).thenReturn(contentType);
                when(inputBase.getDescription()).thenReturn("description" + i);
                when(inputBase.getTag()).thenReturn("partialMockSwrveConversation");

                final ConversationStyle style = getDummyAtomStyle(ConversationStyle.ALIGNMENT.LEFT, buttonType, 0, fontFile, fontNativeStyle);
                ArrayList<ChoiceInputItem> list = new ArrayList<ChoiceInputItem>() {
                    {
                        for (int j = 0; j < num; j++) {
                            add(new ChoiceInputItem("answer_id" + j, "answer_text" + j, style));
                        }
                    }
                };
                when(inputBase.getValues()).thenReturn(list);
                when(inputBase.getStyle()).thenReturn(style);
                contents.add(inputBase);
            } else {
                StarRating starRating = mock(StarRating.class);
                when(starRating.getType()).thenReturn(contentType);
                when(starRating.getTag()).thenReturn("partialMockSwrveConversation");
                when(starRating.getValue()).thenReturn("value" + i);
                when(starRating.getStarColor()).thenReturn("#ff0000");
                when(starRating.getStyle()).thenReturn(getDummyAtomStyle(ConversationStyle.ALIGNMENT.LEFT, buttonType, 0, fontFile, fontNativeStyle));
                contents.add(starRating);
            }
            when(page.getContent()).thenReturn(contents);

            pages.add(page);
        }
        return pages;
    }

    private ConversationStyle getDummyAtomStyle(ConversationStyle.ALIGNMENT alignment, String type, int cornerRadiusPerCent) {
        return getDummyAtomStyle(alignment, type, cornerRadiusPerCent, "my_funky_font", ConversationStyle.FONT_NATIVE_STYLE.NORMAL);
    }

    private ConversationStyle getDummyAtomStyle(ConversationStyle.ALIGNMENT alignment, String type, int cornerRadiusPerCent, String fontFile, ConversationStyle.FONT_NATIVE_STYLE fontNativeStyle) {
        ConversationColorStyle bgStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#00ff00");
        ConversationColorStyle fgStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#0000ff");
        SwrveTestUtils.writeFileToCache(mActivity.getCacheDir(), fontFile);
        return new ConversationStyle(cornerRadiusPerCent, type, bgStyle, fgStyle, null, fontFile, fontFile, "my_funky_family", 123, alignment, fontNativeStyle);
    }

}
