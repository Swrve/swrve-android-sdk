package io.converser.android.test;

import android.test.AndroidTestCase;

import com.google.ciogson.Gson;

import io.converser.android.GsonHelper;
import io.converser.android.engine.model.ConversationReply;

public class StaticSerTests extends AndroidTestCase {

    private Gson gson;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        gson = GsonHelper.getConfiguredGson();
    }

    public void testSerConversationReply() {
        ConversationReply reply = new ConversationReply();

        reply.setControl("yes");
        reply.getData().put("some-tag", "some value");
        reply.getData().put("some-other-tag", 5);


        String json = gson.toJson(reply);

        assertNotNull(json);
        System.out.println(json);

        assertTrue(json.length() > 1);


    }
}
