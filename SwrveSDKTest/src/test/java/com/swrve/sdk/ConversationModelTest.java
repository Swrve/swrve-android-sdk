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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        assertEquals(conversation.getPages().size(), (1));

        ConversationPage page1 = conversation.getPages().get(0);
        assertEquals(page1.getTitle(), ("Page 1 of x"));
        assertNotNull(page1.getTag());
        assertNotNull(page1.getContent());
        assertEquals(page1.getContent().size(), (3));
        assertNotNull(page1.getControls());
        assertEquals(page1.getControls().size(), (2));
        assertStyle(page1.getStyle(), 44, null, null, null, null, 0, null); // page style does not have any text styling

        ConversationAtom content1 = page1.getContent().get(0);
        assertNotNull(content1);
        assertNotNull(content1.getTag());
        assertEquals(content1.getType(), (ConversationAtom.TYPE.INPUT_MULTIVALUE));
        assertEquals(content1 instanceof MultiValueInput, (true));
        MultiValueInput multiValueInput = (MultiValueInput) content1;
        assertEquals(multiValueInput.getDescription(), ("Please select an answer from the radio buttons below 1:"));
        assertStyle(multiValueInput.getStyle(), 0, "2617fb3c279e30dd7c180de8679a2e2d33cf3552", "my_awesome_font", "my_awesome_font_postscript_name", null, 123, null);
        assertEquals(multiValueInput.getValues().size(), (3));
        for (int i = 0; i < multiValueInput.getValues().size(); i++) {
            ChoiceInputItem item = multiValueInput.getValues().get(i);
            assertStyle(item.getStyle(), 0, "2617fb3c279e30dd7c180de8679a2e2d33cf3552", "my_awesome_font_" + i, "my_awesome_font_postscript_name_" + i, null, i, null);
        }

        ConversationAtom content2 = page1.getContent().get(2);
        assertEquals(content2.getType(), (ConversationAtom.TYPE.INPUT_STARRATING));
        StarRating starRating = (StarRating)content2;
        assertEquals(starRating.getValue(), ("<h1>Customer Service</h1>"));
        assertEquals(starRating.getStarColor(), ("#F8F8F8"));
        assertEquals(starRating.getTag(), ("1427199673126-star-rating"));
        assertStyle(content2.getStyle(), 0, "2617fb3c279e30dd7c180de8679a2e2d33cf3552", "my_awesome_font", "my_awesome_font_postscript_name", null, 123, LEFT);

        ConversationAtom controls1 = page1.getControls().get(0);
        assertNotNull(controls1);
        assertTrue(controls1 instanceof ButtonControl);
        ButtonControl buttonControl = (ButtonControl)controls1;
        assertEquals(buttonControl.getDescription(), ("Call a number"));
        assertNotNull(buttonControl.getTag());
        assertEquals(controls1.getStyle().getBorderRadius(), (50));
        assertStyle(controls1.getStyle(), 50, "_system_font_", "", "", ConversationStyle.FONT_NATIVE_STYLE.BOLDITALIC, 123, LEFT);
    }

    @Test
    public void testConversationModel_v3() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_v3.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertEquals(conversation.getPages().size(), (3));

        ConversationPage page1 = conversation.getPages().get(0);
        assertEquals(page1.getTitle(), ("Page 1 of x"));
        assertNotNull(page1.getTag());
        assertNotNull(page1.getContent());
        assertEquals(page1.getContent().size(), (4));
        assertNotNull(page1.getControls());
        assertEquals(page1.getControls().size(), (2));

        ConversationAtom content1 = page1.getContent().get(0);
        assertNotNull(content1);
        assertNotNull(content1.getTag());
        assertEquals(content1.getType(), (ConversationAtom.TYPE.INPUT_MULTIVALUE));

        ConversationAtom content2 = page1.getContent().get(2);
        assertEquals(content2.getType(), (ConversationAtom.TYPE.INPUT_STARRATING));
        StarRating starRating = (StarRating)content2;
        assertEquals(starRating.getValue(), ("<h1>Customer Service</h1>"));
        assertEquals(starRating.getStarColor(), ("#F8F8F8"));
        assertEquals(starRating.getTag(), ("1427199673126-star-rating"));

        ConversationAtom controls1 = page1.getControls().get(0);
        assertNotNull(controls1);
        assertTrue(controls1 instanceof ButtonControl);
        ButtonControl buttonControl = (ButtonControl)controls1;
        assertEquals(buttonControl.getDescription(), ("Call a number"));
        assertNotNull(buttonControl.getTag());
        assertEquals(controls1.getStyle().getBorderRadius(), (50));

        ConversationPage page2 = conversation.getPages().get(1);
        assertEquals(page2.getTitle(), ("Page 2 of x"));
    }

    @Test
    public void test12887LightBoxRadius_v3() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign_v3.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertEquals(conversation.getPages().size(), (3));

        ConversationPage page1 = conversation.getPages().get(0);
        assertEquals(page1.getStyle().getBorderRadius(), (44));
        assertEquals(page1.getStyle().getLb().getValue(), ("#FFFFFF"));

        // No LB or border_radius attributes on second page so load defaults.
        ConversationPage page2 = conversation.getPages().get(1);
        assertEquals(page2.getStyle().getBorderRadius(), (0));
        assertEquals(page2.getStyle().getLb().getValue(), (ConversationStyle.DEFAULT_LB_COLOR));
    }

    @Test
    public void test12887LightBoxRadius() throws Exception {
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f", "2617fb3c279e30dd7c180de8679a2e2d33cf3552");
        SwrveConversation conversation = swrveSpy.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());
        assertNotNull(conversation);
        assertEquals(conversation.getPages().size(), (1));

        ConversationPage page1 = conversation.getPages().get(0);
        assertEquals(page1.getStyle().getBorderRadius(), (44));
        assertEquals(page1.getStyle().getLb().getValue(), ("#FFFFFF"));
    }

    private void assertStyle(ConversationStyle style, int borderRadius, String fontFile, String fontFamily, String fontPostscriptName,
                             ConversationStyle.FONT_NATIVE_STYLE fontNativeStyle, int textSize, ConversationStyle.ALIGNMENT alignment) {
        assertEquals("border radius is wrong", style.getBorderRadius(), (borderRadius));
        assertEquals("font file is wrong", style.getFontFile(), (fontFile));
        assertEquals("font family is wrong", style.getFontFamily(), (fontFamily));
        assertEquals("font name is wrong", style.getFontPostscriptName(), (fontPostscriptName));
        assertEquals("text size is wrong", style.getTextSize(), (textSize));
        assertEquals("font native style is wrong", style.getFontNativeStyle(), (fontNativeStyle));
        assertEquals("alignment is wrong", style.getAlignment(), (alignment));
    }
}
