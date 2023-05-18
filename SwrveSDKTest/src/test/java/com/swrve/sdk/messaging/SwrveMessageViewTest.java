package com.swrve.sdk.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.Swrve;
import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveTestUtils;
import com.swrve.sdk.config.SwrveConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

public class SwrveMessageViewTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        SwrveTestUtils.initSwrve(swrveSpy, mActivity);
    }

    @Test
    public void testBuildLayoutCreation() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        SwrveMessage message = swrveSpy.getMessageForId(165);
        assertNotNull(message);

        SwrveMessageView view = new SwrveMessageView(ApplicationProvider.getApplicationContext(), new SwrveConfig(), message,  message.getFormats().get(0), null, 0);
        assertNotNull(view);
        assertEquals(4, view.getChildCount());

        assertEquals(3, getButtonCount(view));
        assertEquals(1, getImageCount(view));
    }

    @Test
    public void testBuildLayoutCreationWithPersonalization() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_personalization.json", "1111111111111111111111111");
        SwrveMessage message = swrveSpy.getMessageForId(165);

        assertNotNull(message);

        HashMap<String, String> personalization = new HashMap<>();
        personalization.put("test_cp", "shows up");

        SwrveMessageView view = new SwrveMessageView(ApplicationProvider.getApplicationContext(), new SwrveConfig(), message,  message.getFormats().get(0), personalization, 0);
        assertNotNull(view);
        assertEquals(4, view.getChildCount());
        assertEquals(2, getPersonalizedButtonCount(view)); // personalization gets counted as buttons
        assertEquals(1, getImageCount(view));
    }

    @Test
    public void testRenderView() throws Exception {

        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_right_away.json", "1111111111111111111111111");
        SwrveMessage message = swrveSpy.getMessageForId(165);
        assertNotNull(message);

        SwrveMessageView view = new SwrveMessageView(ApplicationProvider.getApplicationContext(), new SwrveConfig(), message,  message.getFormats().get(0), null, 0);

        String base64MD5Screenshot = SwrveHelper.md5(SwrveTestUtils.takeScreenshot(view));
        assertNotNull(base64MD5Screenshot);
    }

    private int getButtonCount(SwrveMessageView view) {
        int buttons = 0;
        for (int i = 0; i < view.getChildCount(); i++) {
            Class<?> viewClass = view.getChildAt(i).getClass();
            if (viewClass == SwrveButtonView.class) {
                buttons++;
            }
        }
        return buttons;
    }

    private int getPersonalizedButtonCount(SwrveMessageView view) {
        int buttons = 0;
        for (int i = 0; i < view.getChildCount(); i++) {
            if (view.getChildAt(i) instanceof SwrveTextImageView) {
                buttons++;
            }
        }
        return buttons;
    }

    private int getImageCount(SwrveMessageView view) {
        int images = 0;
        for (int i = 0; i < view.getChildCount(); i++) {
            Class<?> viewClass = view.getChildAt(i).getClass();
            if (viewClass == SwrveImageView.class) {
                images++;
            }
        }
        return images;
    }
}
