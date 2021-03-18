package com.swrve.sdk;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.ChoiceInputItem;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.StarRating;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;

import static com.swrve.sdk.conversations.engine.model.styles.ConversationStyle.ALIGNMENT.LEFT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConversationModelTest extends SwrveBaseTest {

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
    }

    @Test
    public void testConversationModel() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertThat(conversation.getPages().size(), equalTo(1));

        ConversationPage page1 = conversation.getPages().get(0);
        assertThat(page1.getTitle(), equalTo("Page 1 of x"));
        assertNotNull(page1.getTag());
        assertNotNull(page1.getContent());
        assertThat(page1.getContent().size(), equalTo(3));
        assertNotNull(page1.getControls());
        assertThat(page1.getControls().size(), equalTo(2));
        assertStyle(page1.getStyle(), 44, null, null, null, null, 0, null); // page style does not have any text styling

        ConversationAtom content1 = page1.getContent().get(0);
        assertNotNull(content1);
        assertNotNull(content1.getTag());
        assertThat(content1.getType(), equalTo(ConversationAtom.TYPE.INPUT_MULTIVALUE));
        assertThat(content1 instanceof MultiValueInput, equalTo(true));
        MultiValueInput multiValueInput = (MultiValueInput) content1;
        assertThat(multiValueInput.getDescription(), equalTo("Please select an answer from the radio buttons below 1:"));
        assertStyle(multiValueInput.getStyle(), 0, "2617fb3c279e30dd7c180de8679a2e2d33cf3552", "my_awesome_font", "my_awesome_font_postscript_name", null, 123, null);
        assertThat(multiValueInput.getValues().size(), equalTo(3));
        for (int i = 0; i < multiValueInput.getValues().size(); i++) {
            ChoiceInputItem item = multiValueInput.getValues().get(i);
            assertStyle(item.getStyle(), 0, "2617fb3c279e30dd7c180de8679a2e2d33cf3552", "my_awesome_font_" + i, "my_awesome_font_postscript_name_" + i, null, i, null);
        }

        ConversationAtom content2 = page1.getContent().get(2);
        assertThat(content2.getType(), equalTo(ConversationAtom.TYPE.INPUT_STARRATING));
        StarRating starRating = (StarRating)content2;
        assertThat(starRating.getValue(), equalTo("<h1>Customer Service</h1>"));
        assertThat(starRating.getStarColor(), equalTo("#F8F8F8"));
        assertThat(starRating.getTag(), equalTo("1427199673126-star-rating"));
        assertStyle(content2.getStyle(), 0, "2617fb3c279e30dd7c180de8679a2e2d33cf3552", "my_awesome_font", "my_awesome_font_postscript_name", null, 123, LEFT);

        ConversationAtom controls1 = page1.getControls().get(0);
        assertNotNull(controls1);
        assertThat(controls1, instanceOf(ButtonControl.class));
        ButtonControl buttonControl = (ButtonControl)controls1;
        assertThat(buttonControl.getDescription(), equalTo("Call a number"));
        assertNotNull(buttonControl.getTag());
        assertThat(controls1.getStyle().getBorderRadius(), equalTo(50));
        assertStyle(controls1.getStyle(), 50, "_system_font_", "", "", ConversationStyle.FONT_NATIVE_STYLE.BOLDITALIC, 123, LEFT);
    }

    @Test
    public void testConversationModel_v3() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_v3.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertThat(conversation.getPages().size(), equalTo(3));

        ConversationPage page1 = conversation.getPages().get(0);
        assertThat(page1.getTitle(), equalTo("Page 1 of x"));
        assertNotNull(page1.getTag());
        assertNotNull(page1.getContent());
        assertThat(page1.getContent().size(), equalTo(4));
        assertNotNull(page1.getControls());
        assertThat(page1.getControls().size(), equalTo(2));

        ConversationAtom content1 = page1.getContent().get(0);
        assertNotNull(content1);
        assertNotNull(content1.getTag());
        assertThat(content1.getType(), equalTo(ConversationAtom.TYPE.INPUT_MULTIVALUE));

        ConversationAtom content2 = page1.getContent().get(2);
        assertThat(content2.getType(), equalTo(ConversationAtom.TYPE.INPUT_STARRATING));
        StarRating starRating = (StarRating)content2;
        assertThat(starRating.getValue(), equalTo("<h1>Customer Service</h1>"));
        assertThat(starRating.getStarColor(), equalTo("#F8F8F8"));
        assertThat(starRating.getTag(), equalTo("1427199673126-star-rating"));

        ConversationAtom controls1 = page1.getControls().get(0);
        assertNotNull(controls1);
        assertThat(controls1, instanceOf(ButtonControl.class));
        ButtonControl buttonControl = (ButtonControl)controls1;
        assertThat(buttonControl.getDescription(), equalTo("Call a number"));
        assertNotNull(buttonControl.getTag());
        assertThat(controls1.getStyle().getBorderRadius(), equalTo(50));

        ConversationPage page2 = conversation.getPages().get(1);
        assertThat(page2.getTitle(), equalTo("Page 2 of x"));
    }

    @Test
    public void test12887LightBoxRadius_v3() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_v3.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertThat(conversation.getPages().size(), equalTo(3));

        ConversationPage page1 = conversation.getPages().get(0);
        assertThat(page1.getStyle().getBorderRadius(), equalTo(44));
        assertThat(page1.getStyle().getLb().getValue(), equalTo("#FFFFFF"));

        // No LB or border_radius attributes on second page so load defaults.
        ConversationPage page2 = conversation.getPages().get(1);
        assertThat(page2.getStyle().getBorderRadius(), equalTo(0));
        assertThat(page2.getStyle().getLb().getValue(), equalTo(ConversationStyle.DEFAULT_LB_COLOR));
    }

    @Test
    public void test12887LightBoxRadius() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertThat(conversation.getPages().size(), equalTo(1));

        ConversationPage page1 = conversation.getPages().get(0);
        assertThat(page1.getStyle().getBorderRadius(), equalTo(44));
        assertThat(page1.getStyle().getLb().getValue(), equalTo("#FFFFFF"));
    }

    private void assertStyle(ConversationStyle style, int borderRadius, String fontFile, String fontFamily, String fontPostscriptName,
                             ConversationStyle.FONT_NATIVE_STYLE fontNativeStyle, int textSize, ConversationStyle.ALIGNMENT alignment) {
        assertThat("border radius is wrong", style.getBorderRadius(), equalTo(borderRadius));
        assertThat("font file is wrong", style.getFontFile(), equalTo(fontFile));
        assertThat("font family is wrong", style.getFontFamily(), equalTo(fontFamily));
        assertThat("font name is wrong", style.getFontPostscriptName(), equalTo(fontPostscriptName));
        assertThat("text size is wrong", style.getTextSize(), equalTo(textSize));
        assertThat("font native style is wrong", style.getFontNativeStyle(), equalTo(fontNativeStyle));
        assertThat("alignment is wrong", style.getAlignment(), equalTo(alignment));
    }
}
