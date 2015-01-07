package io.converser.android.test;

import android.test.AndroidTestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import io.converser.android.ConverserEngine;
import io.converser.android.engine.model.ConversationDetail;
import io.converser.android.engine.model.ConversationItem;
import io.converser.android.engine.model.Conversations;

public class EngineTests extends AndroidTestCase {

    private static final String API_KEY = "7577dbc0-8233-012f-d6a9-12313d2a38e9";
    private static final String REF = "jasonconnery@tapadoo.com";
    private static final String CONV_REF = "4fb60aaad6b1b40010000001";

    public void testEngineInitAndSub() {
        ConverserEngine.init(API_KEY);
        ConverserEngine engine = new ConverserEngine();

        assertNotNull(engine);

        final WaitLock<Boolean> lock = new WaitLock<Boolean>();

        engine.subscribe(getContext(), REF, null, new ConverserEngine.Callback<String>() {

            @Override
            public void onSuccess(String response) {
                assertTrue(true);
                lock.waiting = false;
            }

            @Override
            public void onError(String error) {
                lock.fail = true;
                lock.waiting = false;

            }
        });

        while (lock.waiting) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertTrue(!lock.fail);

    }

    public void testSendFeedback() {
        ConverserEngine.init(API_KEY);
        ConverserEngine engine = new ConverserEngine();
        assertNotNull(engine);

        final WaitLock<?> lock = new WaitLock<Integer>();

        int reaction = (int) (((Math.random() * 100) % 4) + 1);
        String area = "Test Area";
        String text = "Hey, if you can see this, I'm probably working";

        engine.subscribe(getContext(), REF, null, new ConverserEngine.Callback<Integer>() {

            @Override
            public void onSuccess(Integer response) {

            }

            @Override
            public void onError(String error) {

            }
        });

        engine.sendFeedback(reaction, area, text, new ConverserEngine.Callback<Boolean>() {

            @Override
            public void onSuccess(Boolean response) {
                lock.fail = false;
                lock.waiting = false;
            }

            @Override
            public void onError(String error) {

                lock.fail = true;
                lock.waiting = false;
            }
        });

        while (lock.waiting) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertTrue(!lock.fail);

    }

    public void testGetConversations() {
        ConverserEngine.init(API_KEY);
        ConverserEngine engine = new ConverserEngine();
        assertNotNull(engine);

        final WaitLock<Conversations> lock = new WaitLock<Conversations>();


        engine.subscribe(getContext(), REF, null, new ConverserEngine.Callback<Integer>() {

            @Override
            public void onSuccess(Integer response) {
            }

            @Override
            public void onError(String error) {
            }
        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
        }


        engine.getConversations(new ConverserEngine.Callback<Conversations>() {

            @Override
            public void onSuccess(Conversations response) {
                lock.object = response;
                lock.fail = false;
                lock.waiting = false;
            }

            @Override
            public void onError(String error) {

                lock.fail = true;
                lock.waiting = false;
            }
        });

        while (lock.waiting) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertNotNull(lock.object);

        Conversations convsersations = lock.object;

        assertTrue(convsersations.getItems().size() > 0);

        for (ConversationItem ci : convsersations.getItems()) {
            assertTrue(ci.getStatus() != null && ci.getStatus().length() > 0);
            assertTrue(ci.getSubject() != null && ci.getSubject().length() > 0);
            assertTrue(ci.getConversationId() != null && ci.getConversationId().length() > 0);
            assertTrue(ci.getRef() != null && ci.getRef().length() > 0);
            assertTrue(ci.getConversationTrackerId() != null && ci.getConversationTrackerId().length() > 0);
            assertNotNull(ci.getCreatedAt());
            try {
                assertTrue(ci.getCreatedAt().after(new SimpleDateFormat("yyyy-MM-dd").parse("2012-05-17")));
            } catch (ParseException e) {
                e.printStackTrace();
                fail();
            }
        }

        assertTrue(!lock.fail);

    }

    public void testGetConversationDetails() {
        ;

        ConverserEngine.init(API_KEY);
        ConverserEngine engine = new ConverserEngine();
        assertNotNull(engine);

        final WaitLock<ConversationDetail> lock = new WaitLock<ConversationDetail>();


        engine.subscribe(getContext(), REF, null, new ConverserEngine.Callback<Integer>() {

            @Override
            public void onSuccess(Integer response) {

            }

            @Override
            public void onError(String error) {

            }
        });

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
        }

        engine.getConversationDetail(CONV_REF, new ConverserEngine.Callback<ConversationDetail>() {

            @Override
            public void onSuccess(ConversationDetail response) {

                lock.fail = false;
                lock.object = response;
                lock.waiting = false;

            }

            @Override
            public void onError(String error) {

                lock.fail = true;
                lock.waiting = false;
            }
        });

        while (lock.waiting) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertNotNull(lock.object);


    }

    /**
     * I really need to find if there's a proper way to unit test async stuff
     *
     * @author Jason Connery
     */
    private class WaitLock<T> {
        boolean waiting = true;
        boolean fail = false;
        T object = null;
    }
}
