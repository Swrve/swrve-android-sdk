package com.swrve.sdk;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.StarRating;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConversationModelTest extends SwrveBaseTest {

    private Swrve swrve;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrve = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey");
        swrve.init(mActivity);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        SwrveTestUtils.removeSwrveSDKSingletonInstance();
    }

    @Test
    public void testConversationModel() throws Exception {
        loadCampaignsFromAssets("conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversation conversation = swrve.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());

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
        assertThat(content1.getType(), equalTo(ConversationAtom.TYPE_INPUT_MULTIVALUE));

        ConversationAtom content2 = page1.getContent().get(2);
        assertThat(content2.getType(), equalTo(ConversationAtom.TYPE_INPUT_STARRATING));
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
    public void test12887LightBoxRadius() throws Exception {
        loadCampaignsFromAssets("conversation_campaign.json", "8d4f969706e6bf2aa344d6690496ecfdefc89f1f");
        SwrveConversation conversation = swrve.getConversationForEvent("swrve.messages.showatsessionstart", new HashMap<String, String>());

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

    private void loadCampaignsFromAssets(String assetName, String... imageAssets) throws Exception {
        String json = SwrveTestUtils.getAssetAsText(mActivity, assetName);
        JSONObject jsonObject = new JSONObject(json);
        swrve.loadCampaignsFromJSON(jsonObject, swrve.campaignsState);
        if (imageAssets.length > 0) {
            Set<String> assetsOnDisk = new HashSet<>();
            assetsOnDisk.add(imageAssets[0]);
            swrve.assetsOnDisk = assetsOnDisk;
        }
    }

}
