package com.swrve.sdk.test;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VideoContentTest extends SwrveBaseTest {

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
    }

    @Test
    public void testYoutubeIdFetching() {
        String expectedID = "6-SfjPGTu8s";
        String[] youtubeUrls = {
                "https://www.youtube.com/embed/6-SfjPGTu8s?rel=0",
                "http://www.youtube.com/embed/6-SfjPGTu8s?rel=0",
                "https://www.youtube.com/watch?v=6-SfjPGTu8s&feature=feedrec_grec_index",
                "http://www.youtube.com/watch?v=6-SfjPGTu8s&feature=feedrec_grec_index",
                "https://www.youtube.com/user/SomeUser#p/a/u/1/6-SfjPGTu8s",
                "http://www.youtube.com/user/SomeUser#p/a/u/1/6-SfjPGTu8s",
                "https://www.youtube.com/v/6-SfjPGTu8s?fs=1&amp;hl=en_US&amp;rel=0",
                "http://www.youtube.com/v/6-SfjPGTu8s?fs=1&amp;hl=en_US&amp;rel=0",
                "https://www.youtube.com/watch?v=6-SfjPGTu8s#t=0m10s",
                "http://www.youtube.com/watch?v=6-SfjPGTu8s#t=0m10s",
                "https://www.youtube.com/watch?v=6-SfjPGTu8s",
                "http://www.youtube.com/watch?v=6-SfjPGTu8s",
                "https://youtu.be/6-SfjPGTu8s",
                "http://youtu.be/6-SfjPGTu8s"
        };

        Content videoContent = new Content("tag", ConversationAtom.TYPE.CONTENT_VIDEO, null, "", "");
        for (String url : youtubeUrls) {
            videoContent.setHeight("200");
            videoContent.setValue(url);
            String youtubeId = videoContent.getYoutubeVideoId();

            String message = "Expected: " + expectedID + ". Got: " + youtubeId + ". Url: " + url;
            assertNotNull(expectedID);
            assertNotNull(message, youtubeId);
            assertEquals(message, expectedID, youtubeId);
            assert(youtubeId.equals(expectedID));
        }
    }
}
