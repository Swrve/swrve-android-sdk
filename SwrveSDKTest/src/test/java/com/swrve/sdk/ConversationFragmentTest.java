package com.swrve.sdk;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Color;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ConversationFragmentTest extends SwrveBaseTest{

    private SwrveConversation partialMockSwrveConversation;
    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        SwrveCommon.setSwrveCommon(swrveSpy);
        swrveSpy.init(mActivity);
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        assertNotNull(swrveSpy.campaigns);
        SwrveConversation realSwrveConversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(realSwrveConversation);
        partialMockSwrveConversation = spy(realSwrveConversation);
        doReturn(true).when(partialMockSwrveConversation).areAssetsReady(any(Set.class));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        swrveSpy.shutdown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testControls0Button() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 0, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        assertThat(controls.getVisibility(), equalTo(View.VISIBLE));
        assertThat(controls.getChildCount(), equalTo(0));
    }

    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testControls1Button() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        assertThat(controls.getVisibility(), equalTo(View.VISIBLE));
        assertThat(controls.getChildCount(), equalTo(1));
        ConversationButton conversationButton = (ConversationButton) controls.getChildAt(0);
        assertTrue(conversationButton.getModel().getActions().isCall());
        assertThat(conversationButton.getModel().getActions().getCallUri().toString(), equalTo("tel:0"));
    }

    @Test
    public void testControls2Button() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 2, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        assertThat(controls.getVisibility(), equalTo(View.VISIBLE));
        assertThat(controls.getChildCount(), equalTo(2));
        ConversationButton conversationButton0 = (ConversationButton) controls.getChildAt(0);
        assertTrue(conversationButton0.getModel().getActions().isCall());
        assertThat(conversationButton0.getModel().getActions().getCallUri().toString(), equalTo("tel:0"));
        ConversationButton conversationButton1 = (ConversationButton) controls.getChildAt(1);
        assertTrue(conversationButton1.getModel().getActions().isCall());
        assertThat(conversationButton1.getModel().getActions().getCallUri().toString(), equalTo("tel:1"));
    }

    @Test
    public void testControls3Button() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        assertThat(controls.getVisibility(), equalTo(View.VISIBLE));
        assertThat(controls.getChildCount(), equalTo(3));
        ConversationButton conversationButton0 = (ConversationButton) controls.getChildAt(0);
        assertTrue(conversationButton0.getModel().getActions().isCall());
        assertThat(conversationButton0.getModel().getActions().getCallUri().toString(), equalTo("tel:0"));
        ConversationButton conversationButton1 = (ConversationButton) controls.getChildAt(1);
        assertTrue(conversationButton1.getModel().getActions().isCall());
        assertThat(conversationButton1.getModel().getActions().getCallUri().toString(), equalTo("tel:1"));
        ConversationButton conversationButton2 = (ConversationButton) controls.getChildAt(2);
        assertTrue(conversationButton2.getModel().getActions().isCall());
        assertThat(conversationButton2.getModel().getActions().getCallUri().toString(), equalTo("tel:2"));
    }

    @Test
    public void testControls3ButtonV4() {
        int conversationVersation = 4;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersation, 1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        assertThat(controls.getVisibility(), equalTo(View.VISIBLE));
        assertThat(controls.getChildCount(), equalTo(3));

        ConversationButton conversationButton0 = (ConversationButton) controls.getChildAt(0);
        assertTrue(conversationButton0.getModel().getActions().isCall());
        assertThat(conversationButton0.getModel().getActions().getCallUri().toString(), equalTo("tel:0"));
        assertThat("assert gravity of button 0", conversationButton0.getGravity(), equalTo(Gravity.CENTER | Gravity.LEFT));
        assertThat("assert text size of button 0", conversationButton0.getTextSize(), equalTo(123.0f));

        ConversationButton conversationButton1 = (ConversationButton) controls.getChildAt(1);
        assertTrue(conversationButton1.getModel().getActions().isCall());
        assertThat(conversationButton1.getModel().getActions().getCallUri().toString(), equalTo("tel:1"));
        assertThat("assert gravity of button 1", conversationButton1.getGravity(), equalTo(Gravity.CENTER | Gravity.CENTER));
        assertThat("assert text size of button 1", conversationButton1.getTextSize(), equalTo(123.0f));

        ConversationButton conversationButton2 = (ConversationButton) controls.getChildAt(2);
        assertTrue(conversationButton2.getModel().getActions().isCall());
        assertThat(conversationButton2.getModel().getActions().getCallUri().toString(), equalTo("tel:2"));
        assertThat("assert gravity of button 2", conversationButton2.getGravity(), equalTo(Gravity.CENTER | Gravity.RIGHT));
        assertThat("assert text size of button 2", conversationButton2.getTextSize(), equalTo(123.0f));
    }

    @Test
    public void testCurvedButton() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 10));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        ConversationButton conversationButton = (ConversationButton) controls.getChildAt(1);
        assertThat(conversationButton.getBorderRadius(), equalTo(2.5f));

        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "outline", 20));
        fragment = createConversationFragment(partialMockSwrveConversation);
        controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        conversationButton = (ConversationButton) controls.getChildAt(1);
        assertThat(conversationButton.getBorderRadius(), equalTo(5.0f));

        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "outline", 100));
        fragment = createConversationFragment(partialMockSwrveConversation);
        controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        conversationButton = (ConversationButton) controls.getChildAt(1);
        assertThat(conversationButton.getBorderRadius(), equalTo(25.0f));

        partialMockSwrveConversation.setPages(getMockContentConversationPages(1, 1, 3, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "outline", 200));
        fragment = createConversationFragment(partialMockSwrveConversation);
        controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        conversationButton = (ConversationButton) controls.getChildAt(1);
        assertThat(conversationButton.getBorderRadius(), equalTo(25.0f));
    }

    @Test
    public void testRoundedPage() {
        int conversationVersion = 0;
        ArrayList<ConversationPage> pages = getMockContentConversationPages(conversationVersion, 1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50);
        ConversationColorStyle bgStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#ffffff");
        ConversationColorStyle lbStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#5f68a3");
        ConversationStyle pageStyle0 = new ConversationStyle(20, ConversationStyle.TYPE_SOLID, bgStyle, null, lbStyle); // border 20
        when(pages.get(0).getStyle()).thenReturn(pageStyle0);
        partialMockSwrveConversation.setPages(pages);

        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        ConversationRoundedLinearLayout modal = (ConversationRoundedLinearLayout) fragment.getView().findViewById(R.id.swrve__conversation_modal);
        // border 20 means 20% of 25pixels == 5
        assertThat(modal.getRadius(), equalTo(5.0f));
    }

    @Test
    public void testContentImage() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(content.getVisibility(), equalTo(View.VISIBLE));
        assertThat(content.getChildCount(), equalTo(1));
        assertTrue(content.getChildAt(0) instanceof ConversationImageView);
    }

    @Test
    public void testContentHtml() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_HTML, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(content.getVisibility(), equalTo(View.VISIBLE));
        assertThat(content.getChildCount(), equalTo(1));
        assertTrue(content.getChildAt(0) instanceof HtmlSnippetView);
    }

    @Test
    public void testContentVideo() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_VIDEO, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(content.getVisibility(), equalTo(View.VISIBLE));
        assertThat(content.getChildCount(), equalTo(1));
        assertTrue(content.getChildAt(0) instanceof YoutubeVideoView);
    }

    @Test
    public void testContentSpacer() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_SPACER, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(content.getVisibility(), equalTo(View.VISIBLE));
        assertThat(content.getChildCount(), equalTo(1));
        assertThat(content.getChildAt(0).getLayoutParams().height, equalTo(123));
    }

    @Test
    public void testMultiValue() {
        int conversationVersation = 3;
        partialMockSwrveConversation.setPages(getMockInputConversationPages(conversationVersation, 1, ConversationAtom.TYPE_INPUT_MULTIVALUE, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(content.getVisibility(), equalTo(View.VISIBLE));
        assertThat(content.getChildCount(), equalTo(1));
        assertTrue(content.getChildAt(0) instanceof MultiValueInputControl);
        MultiValueInputControl multiValueInputControl = (MultiValueInputControl) content.getChildAt(0);


        assertTrue(multiValueInputControl.getChildAt(0) instanceof android.widget.TextView);
        TextView tv = (TextView)multiValueInputControl.getChildAt(0);
        assertThat(tv.getText().toString(), equalTo("description0"));

        assertTrue(multiValueInputControl.getChildAt(1) instanceof RadioButton);
        RadioButton rb = (RadioButton)multiValueInputControl.getChildAt(1);
        assertThat(rb.getText().toString(), equalTo("answer_text0"));
    }

    @Test
    public void testMultiValueV4() {
        int conversationVersation = 4;
        partialMockSwrveConversation.setPages(getMockInputConversationPages(conversationVersation, 1, ConversationAtom.TYPE_INPUT_MULTIVALUE, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout content = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(content.getVisibility(), equalTo(View.VISIBLE));
        assertThat(content.getChildCount(), equalTo(1));
        assertTrue(content.getChildAt(0) instanceof MultiValueInputControl);
        MultiValueInputControl multiValueInputControl = (MultiValueInputControl) content.getChildAt(0);

        assertThat(multiValueInputControl.getChildCount(), equalTo(2));
        assertTrue(multiValueInputControl.getChildAt(0) instanceof android.widget.TextView);
        TextView tv = (TextView)multiValueInputControl.getChildAt(0);
        assertThat(tv.getText().toString(), equalTo("description0"));
        assertThat(tv.getTextSize(), equalTo(123.0f));

        assertTrue(multiValueInputControl.getChildAt(1) instanceof RadioButton);
        RadioButton rb = (RadioButton)multiValueInputControl.getChildAt(1);
        assertThat(rb.getText().toString(), equalTo("answer_text0"));
        assertThat(rb.getTextSize(), equalTo(123.0f));
    }

    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void testStarRatingKitKat() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockInputConversationPages(conversationVersion, 1, ConversationAtom.TYPE_INPUT_STARRATING, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout contentLinearLayout = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(contentLinearLayout.getChildCount(), equalTo(1));
        assertTrue(contentLinearLayout.getChildAt(0) instanceof ConversationRatingBar);
        ConversationRatingBar conversationRatingBar = (ConversationRatingBar) contentLinearLayout.getChildAt(0);
        assertThat(conversationRatingBar.getModel().getValue(), equalTo("value0"));
        assertThat(conversationRatingBar.getModel().getTag(), equalTo("partialMockSwrveConversation"));
        assertThat(conversationRatingBar.getModel().getStarColor(), equalTo("#ff0000"));
        assertThat(conversationRatingBar.getModel().getValue(), equalTo("value0"));
        assertThat(conversationRatingBar.getRatingBar().getNumStars(), equalTo(5));
        assertThat(conversationRatingBar.getRatingBar().getStepSize(), equalTo(0.01f));

        // test less than zero selected but value only goes to 1.0
        conversationRatingBar.onRatingChanged(conversationRatingBar.getRatingBar(), 0.5f, true);
        assertThat(conversationRatingBar.getRatingBar().getRating(), equalTo(1.0f));
    }

    @Test
    public void testStarRating() {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockInputConversationPages(conversationVersion, 1, ConversationAtom.TYPE_INPUT_STARRATING, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout contentLinearLayout = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(contentLinearLayout.getChildCount(), equalTo(1));
        assertTrue(contentLinearLayout.getChildAt(0) instanceof ConversationRatingBar);
        ConversationRatingBar conversationRatingBar = (ConversationRatingBar) contentLinearLayout.getChildAt(0);
        assertThat(conversationRatingBar.getModel().getValue(), equalTo("value0"));
        assertThat(conversationRatingBar.getModel().getTag(), equalTo("partialMockSwrveConversation"));
        assertThat(conversationRatingBar.getModel().getStarColor(), equalTo("#ff0000"));
        assertThat(conversationRatingBar.getModel().getValue(), equalTo("value0"));
        assertThat(conversationRatingBar.getRatingBar().getNumStars(), equalTo(5));
        assertThat(conversationRatingBar.getRatingBar().getStepSize(), equalTo(0.01f));

        // test less than zero selected but value only goes to 1.0
        conversationRatingBar.onRatingChanged(conversationRatingBar.getRatingBar(), 0.5f, true);
        assertThat(conversationRatingBar.getRatingBar().getRating(), equalTo(1.0f));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testLightboxDefaultColor() {
        int conversationVersion = 0;
        ArrayList<ConversationPage> pages = getMockContentConversationPages(conversationVersion, 2, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50);
        partialMockSwrveConversation.setPages(pages);
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);

        // assert default colors
        Drawable drawable = fragment.getActivity().getWindow().getDecorView().getBackground();
        assertTrue(drawable instanceof ColorDrawable);
        ColorDrawable colorDrawable = (ColorDrawable) drawable;
        int defaultColor = Color.parseColor(ConversationStyle.DEFAULT_LB_COLOR);
        assertThat(defaultColor, equalTo(colorDrawable.getColor()));

        // move to next page
        ConversationPage page1 = pages.get(1);
        fragment.setPage(page1);
        fragment.onResume();

        drawable = fragment.getActivity().getWindow().getDecorView().getBackground();
        assertTrue(drawable instanceof ColorDrawable);
        colorDrawable = (ColorDrawable) drawable;
        defaultColor = Color.parseColor(ConversationStyle.DEFAULT_LB_COLOR);
        assertThat(defaultColor, equalTo(colorDrawable.getColor()));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Test
    public void testLightboxColor() {
        int conversationVersion = 0;
        ArrayList<ConversationPage> pages = getMockContentConversationPages(conversationVersion, 2, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50);
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
        assertThat(color, equalTo(colorDrawable.getColor()));

        // move to next page
        ConversationPage page1 = pages.get(1);
        fragment.setPage(page1);
        fragment.onResume();

        drawable = fragment.getActivity().getWindow().getDecorView().getBackground();
        assertTrue(drawable instanceof ColorDrawable);
        colorDrawable = (ColorDrawable) drawable;
        color = Color.parseColor("#a20932");
        assertThat(color, equalTo(colorDrawable.getColor()));
    }

    @Test
    public void testOnResume() {
        int conversationVersion = 0;
        ArrayList<ConversationPage> pages = getMockContentConversationPages(conversationVersion, 2, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50);
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
        int conversationVersion = 0;
        ArrayList<ConversationPage> pages = getMockInputConversationPages(conversationVersion, 2, ConversationAtom.TYPE_INPUT_MULTIVALUE, "solid");
        partialMockSwrveConversation.setPages(pages);
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        ConversationPage page0 = pages.get(0);
        assertNotNull(page0);
        fragment.onResume();

        LinearLayout content = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        MultiValueInputControl multiValueInputControl = (MultiValueInputControl) content.getChildAt(0);
        assertThat(((android.widget.TextView) multiValueInputControl.getChildAt(0)).getText().toString(), equalTo("description0"));
        assertTrue(multiValueInputControl.getChildAt(1) instanceof RadioButton);
        RadioButton radioButton1 = (RadioButton) multiValueInputControl.getChildAt(1);
        assertThat(radioButton1.isChecked(), equalTo(Boolean.FALSE));
        RadioButton radioButton2 = (RadioButton) multiValueInputControl.getChildAt(2);
        assertThat(radioButton2.isChecked(), equalTo(Boolean.FALSE));

        radioButton2.setChecked(true); // enter user content

        fragment.onResume();

        LinearLayout content2 = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        MultiValueInputControl multiValueInputControl2 = (MultiValueInputControl) content2.getChildAt(0);
        RadioButton radioButton21 = (RadioButton) multiValueInputControl2.getChildAt(1);
        assertThat(radioButton21.isChecked(), equalTo(Boolean.FALSE));
        RadioButton radioButton22 = (RadioButton) multiValueInputControl2.getChildAt(2);
        assertThat(radioButton22.isChecked(), equalTo(Boolean.TRUE));
    }

    @Test
    public void testOnResumeWithRatingBar() {
        int conversationVersion = 0;
        ArrayList<ConversationPage> pages = getMockInputConversationPages(conversationVersion, 2, ConversationAtom.TYPE_INPUT_STARRATING, "solid");
        partialMockSwrveConversation.setPages(pages);
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        ConversationPage page0 = pages.get(0);
        assertNotNull(page0);
        fragment.onResume();

        LinearLayout contentLinearLayout = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(contentLinearLayout.getChildCount(), equalTo(1));
        assertTrue(contentLinearLayout.getChildAt(0) instanceof ConversationRatingBar);
        ConversationRatingBar conversationRatingBar = (ConversationRatingBar) contentLinearLayout.getChildAt(0);

        // enter user content
        conversationRatingBar.getRatingBar().setRating(3.0f);
        conversationRatingBar.onRatingChanged(conversationRatingBar.getRatingBar(), 3.0f, true);

        fragment.onResume();

        LinearLayout content2 = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(content2.getChildCount(), equalTo(1));
        assertTrue(content2.getChildAt(0) instanceof ConversationRatingBar);
        ConversationRatingBar conversationRatingBar2 = (ConversationRatingBar) content2.getChildAt(0);
        assertThat(conversationRatingBar2.getRatingBar().getRating(), equalTo(3.0f));
    }

    @Test
    public void testOnBackPressed() throws Exception{
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockInputConversationPages(conversationVersion, 1, ConversationAtom.TYPE_INPUT_MULTIVALUE, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);

        HashMap<String, UserInputResult> userData = new HashMap<String, UserInputResult>();
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
    public void testButtonClickWithActionCall() throws Exception {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 1, ControlActions.CALL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        ConversationButton conversationButton = (ConversationButton)controls.getChildAt(0);
        conversationButton.performClick();

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        ConversationActivity activity = Robolectric.buildActivity(ConversationActivity.class).get();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent nextStartedActivity = shadowActivity.getNextStartedActivity(); // for some reason this line is needed before calling peekNextStartedActivity?
        assertNotNull(nextStartedActivity);
        Intent nextIntent = shadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertThat(nextIntent.getAction(), equalTo(Intent.ACTION_DIAL));
        assertThat(nextIntent.getDataString(), equalTo("tel:0"));
    }

    @Ignore("Ignored for now. Threads issue. If you break after performClick it seems to work")
    @Test
    public void testButtonClickWithActionVisit() throws Exception {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 1, ControlActions.VISIT_URL_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        ConversationButton conversationButton = (ConversationButton)controls.getChildAt(0);
        conversationButton.performClick();

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        ConversationActivity activity = Robolectric.buildActivity(ConversationActivity.class).get();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent nextStartedActivity = shadowActivity.getNextStartedActivity(); // for some reason this line is needed before calling peekNextStartedActivity?
        assertNotNull(nextStartedActivity);
        Intent nextIntent = shadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertThat(nextIntent.getAction(), equalTo(Intent.ACTION_VIEW));
        assertThat(nextIntent.getDataString(), equalTo("http://www.swrve.com"));
        assertTrue(nextIntent.hasExtra(Browser.EXTRA_HEADERS));
        // assertThat(nextIntent.getBundleExtra(Browser.EXTRA_HEADERS), equalTo("some referrer")); // TODO
    }

    @Ignore("Ignored for now. Threads issue. If you break after performClick it seems to work")
    @Test
    public void testButtonClickWithActionDeepLink() throws Exception {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockContentConversationPages(conversationVersion, 1, 1, ControlActions.DEEPLINK_ACTION, ConversationAtom.TYPE_CONTENT_IMAGE, "solid", 50));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout controls = (LinearLayout) fragment.getView().findViewById(R.id.swrve__controls);
        ConversationButton conversationButton = (ConversationButton)controls.getChildAt(0);
        conversationButton.performClick();

        Robolectric.flushForegroundThreadScheduler(); // allow tasks that added to ui thread to run (like activity.runOnUiThread)

        ConversationActivity activity = Robolectric.buildActivity(ConversationActivity.class).get();
        ShadowActivity shadowActivity = Shadows.shadowOf(activity);
        Intent nextStartedActivity = shadowActivity.getNextStartedActivity(); // for some reason this line is needed before calling peekNextStartedActivity?
        assertNotNull(nextStartedActivity);
        Intent nextIntent = shadowActivity.peekNextStartedActivity();
        assertNotNull(nextIntent);
        assertThat(nextIntent.getAction(), equalTo(Intent.ACTION_VIEW));
        assertThat(nextIntent.getDataString(), equalTo("http://www.swrve.com"));
        assertThat(nextIntent.getFlags(), equalTo(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    @Test
    public void testCommitMultiInputValueToEvents() throws Exception {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockInputConversationPages(conversationVersion, 2, ConversationAtom.TYPE_INPUT_MULTIVALUE, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);

        LinearLayout content = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        MultiValueInputControl multiValueInputControl = (MultiValueInputControl) content.getChildAt(0);
        assertThat(multiValueInputControl.getRadioButtons().size(), equalTo(2));
        RadioButton radioButton = multiValueInputControl.getRadioButtons().get(1);
        radioButton.setChecked(true);
        multiValueInputControl.onCheckedChanged(radioButton, true);

        assertThat(fragment.getUserInteractionData().size(), equalTo(1));
        UserInputResult userInputResult = fragment.getUserInteractionData().get("pageTag-partialMockSwrveConversation");
        assertThat(userInputResult.getType(), equalTo("choice"));
        assertThat(userInputResult.getConversationId(), equalTo(82));
        assertThat(userInputResult.getPageTag(), equalTo("pageTag"));
        assertThat(userInputResult.getFragmentTag(), equalTo("partialMockSwrveConversation"));
        assertTrue(userInputResult.getResult() instanceof ChoiceInputResponse);
        ChoiceInputResponse choiceInputResponse = (ChoiceInputResponse)userInputResult.getResult();
        assertThat(choiceInputResponse.getQuestionID(), equalTo("partialMockSwrveConversation"));
        assertThat(choiceInputResponse.getAnswerID(), equalTo("answer_id1"));
        assertThat(choiceInputResponse.getAnswerText(), equalTo("answer_text1"));
        assertThat(choiceInputResponse.getFragmentTag(), equalTo("partialMockSwrveConversation"));

        fragment.commitUserInputsToEvents();
        assertThat(fragment.getUserInteractionData().size(), equalTo(0));
    }

    @Test
    public void testCommitRatingBarToEvents() throws Exception {
        int conversationVersion = 0;
        partialMockSwrveConversation.setPages(getMockInputConversationPages(conversationVersion, 1, ConversationAtom.TYPE_INPUT_STARRATING, "solid"));
        ConversationFragment fragment = createConversationFragment(partialMockSwrveConversation);
        LinearLayout contentLinearLayout = (LinearLayout) fragment.getView().findViewById(R.id.swrve__content);
        assertThat(contentLinearLayout.getChildCount(), equalTo(1));
        assertTrue(contentLinearLayout.getChildAt(0) instanceof ConversationRatingBar);
        ConversationRatingBar conversationRatingBar = (ConversationRatingBar) contentLinearLayout.getChildAt(0);

        conversationRatingBar.getRatingBar().setRating(3.0f);
        conversationRatingBar.onRatingChanged(conversationRatingBar.getRatingBar(), 3.0f, true);

        assertThat(fragment.getUserInteractionData().size(), equalTo(1));
        UserInputResult userInputResult = fragment.getUserInteractionData().get("pageTag-partialMockSwrveConversation");
        assertThat(userInputResult.getType(), equalTo("star-rating"));
        assertThat(userInputResult.getConversationId(), equalTo(82));
        assertThat(userInputResult.getPageTag(), equalTo("pageTag"));
        assertThat(userInputResult.getFragmentTag(), equalTo("partialMockSwrveConversation"));
        assertTrue(userInputResult.getResult() instanceof Float);
        assertThat((Float)userInputResult.getResult(), equalTo(3.0f));

        fragment.commitUserInputsToEvents();
        assertThat(fragment.getUserInteractionData().size(), equalTo(0));
    }

    private ConversationFragment createConversationFragment(SwrveConversation swrveConversation) {
        ConversationFragment fragment = ConversationFragment.create(swrveConversation);
        assertNotNull(fragment);
        ConversationActivity conversationActivity = Robolectric.buildActivity(ConversationActivity.class).create().start().resume().get();
        fragment.commitConversationFragment(conversationActivity.getSupportFragmentManager());
        SupportFragmentTestUtil.startVisibleFragment(fragment, ConversationActivity.class, android.R.id.content);
        return fragment;
    }

    // Mock pages of type CONTENT
    private ArrayList<ConversationPage> getMockContentConversationPages(final int conversationVersion, int numPages, final int numButtonControls, final Object action, String contentType,
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
                        if (conversationVersion > 3) {
                            when(buttonControl.getStyle()).thenReturn(getDummyAtomStyleV4(j, buttonType, cornerRadiusPerCent));
                        } else {
                            when(buttonControl.getStyle()).thenReturn(getDummyAtomStyleV3(buttonType, cornerRadiusPerCent));
                        }
                        ControlActions controlActions = mock(ControlActions.class);
                        if(action.toString().equals(ControlActions.CALL_ACTION)) {
                            when(controlActions.getCallUri()).thenReturn(Uri.parse("tel:"+j));
                            when(controlActions.isCall()).thenReturn(true);
                        } else if(action.toString().equals(ControlActions.VISIT_URL_ACTION)) {
                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put(ControlActions.VISIT_URL_URI_KEY.toString(), "http://www.swrve.com");
                            hashMap.put(ControlActions.VISIT_URL_REFERER_KEY.toString(), "some referrer");
                            when(controlActions.getVisitDetails()).thenReturn(hashMap);
                            when(controlActions.isVisit()).thenReturn(true);
                        } else if(action.toString().equals(ControlActions.DEEPLINK_ACTION)) {
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
            when(content.getValue()).thenReturn("value"  + i);
            when(content.getStyle()).thenReturn(getDummyAtomStyleV3(buttonType, 0));
            contents.add(content);
            when(page.getContent()).thenReturn(contents);

            pages.add(page);
        }
        return pages;
    }

    // Mock pages of type INPUT
    private ArrayList<ConversationPage> getMockInputConversationPages(final int conversationVersion, final int num, String contentType, final String buttonType) {
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

            for(ButtonControl control : page.getControls()){
                if (conversationVersion > 3) {
                    when(control.getStyle()).thenReturn(getDummyAtomStyleV4(0, buttonType, 0));
                } else {
                    when(control.getStyle()).thenReturn(getDummyAtomStyleV3(buttonType, 0));
                }
            }

            ArrayList<ConversationAtom> contents = new ArrayList<>();
            if (contentType.equals(ConversationAtom.TYPE_INPUT_MULTIVALUE)) {
                MultiValueInput inputBase = mock(MultiValueInput.class);
                when(inputBase.getType()).thenReturn(contentType);
                when(inputBase.getDescription()).thenReturn("description" + i);
                when(inputBase.getTag()).thenReturn("partialMockSwrveConversation");
                final ConversationStyle style;
                if (conversationVersion > 3) {
                    style = getDummyAtomStyleV4(0, buttonType, 0);
                } else {
                    style = getDummyAtomStyleV3(buttonType, 0);
                }

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
                when(starRating.getStyle()).thenReturn(getDummyAtomStyleV3(buttonType, 0));
                contents.add(starRating);
            }
            when(page.getContent()).thenReturn(contents);

            pages.add(page);
        }
        return pages;
    }

    private ConversationStyle getDummyAtomStyleV3(String type, int cornerRadiusPerCent) {
        ConversationColorStyle bgStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#00ff00");
        ConversationColorStyle fgStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#000033");
        return new ConversationStyle(cornerRadiusPerCent, type, bgStyle, fgStyle, null);
    }

    private ConversationStyle getDummyAtomStyleV4(int buttonIndex, String type, int cornerRadiusPerCent) {
        ConversationColorStyle bgStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#00ff00");
        ConversationColorStyle fgStyle = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, "#0000ff");
        ConversationStyle.ALIGNMENT alignment = ConversationStyle.ALIGNMENT.LEFT;
        switch (buttonIndex) {
            case 0: alignment = ConversationStyle.ALIGNMENT.LEFT; break;
            case 1: alignment = ConversationStyle.ALIGNMENT.CENTER; break;
            case 2: alignment = ConversationStyle.ALIGNMENT.RIGHT; break;
        }
        String fontFile = "my_funky_font";
        SwrveTestUtils.writeFileToCache(mActivity.getCacheDir(), fontFile);
        return new ConversationStyle(cornerRadiusPerCent, type, bgStyle, fgStyle, null, fontFile, "my_funky_family", 123, alignment);
    }

}
