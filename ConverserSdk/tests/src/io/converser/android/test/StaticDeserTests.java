package io.converser.android.test;

import android.net.Uri;
import android.test.AndroidTestCase;

import com.google.ciogson.Gson;

import io.converser.android.GsonHelper;
import io.converser.android.engine.model.ButtonControl;
import io.converser.android.engine.model.Content;
import io.converser.android.engine.model.ControlBase;
import io.converser.android.engine.model.ConversationAtom;
import io.converser.android.engine.model.ConversationDetail;
import io.converser.android.engine.model.DateSaver;
import io.converser.android.engine.model.MultiValueInput;
import io.converser.android.engine.model.SliderInput;
import io.converser.android.engine.model.TextInput;

public class StaticDeserTests extends AndroidTestCase {

    private Gson gson;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        gson = GsonHelper.getConfiguredGson();
    }


    public void testTextContentDeser() {
        String json = "{\"tag\" : \"some-tag\",\"type\" : \"text\",\"value\" : \"Lorem Ipsum Wha?\"}";

        //Check if it returns a CA
        ConversationAtom obj = gson.fromJson(json, ConversationAtom.class);

        assertNotNull(obj);
        System.out.println("Object returned : " + obj.toString() + " of class " + obj.getClass().getName());
        assertTrue(obj instanceof ConversationAtom);

        //But, seeing as this is also a content type , it should also match that sub class
        assertTrue(obj instanceof Content);

        //And, value should have properly deser'd
        Content cType = (Content) obj;
        assertNotNull(cType.getValue());
        assertEquals("Lorem Ipsum Wha?", cType.getValue());
    }

    public void testHtmlContentDeser() {
        String json = "{\"tag\" : \"some-tag\",\"type\" : \"html-fragment\",\"value\" : \"<h1>Lorem Ipsum Wha?</h1>\"}";

        //Check if it returns a CA
        ConversationAtom obj = gson.fromJson(json, ConversationAtom.class);

        assertNotNull(obj);
        System.out.println("Object returned : " + obj.toString() + " of class " + obj.getClass().getName());
        assertTrue(obj instanceof ConversationAtom);

        //But, seeing as this is also a content type , it should also match that sub class
        assertTrue(obj instanceof Content);

        //And, value should have properly deser'd
        Content cType = (Content) obj;
        assertNotNull(cType.getValue());
        assertEquals("<h1>Lorem Ipsum Wha?</h1>", cType.getValue());
    }

    public void testButtonControlDeser() {
        String json = "{\"tag\" : \"some-tag\",\"description\" : \"OK\"}";
        //Check if it returns a CA
        ConversationAtom obj = gson.fromJson(json, ConversationAtom.class);

        assertNotNull(obj);
        System.out.println("Object returned : " + obj.toString() + " of class " + obj.getClass().getName());
        assertTrue(obj instanceof ConversationAtom);

        //But, seeing as this is also a Control type
        assertTrue(obj instanceof ControlBase);

        //But, also a button
        assertTrue(obj instanceof ButtonControl);

        //And, value should have properly deser'd
        ButtonControl cType = (ButtonControl) obj;
        assertNotNull(cType.getDescription());
        assertEquals("OK", cType.getDescription());
    }

    public void testDateSaverDeser() {
        String json = "{\"tag\" : \"some-tag\", \"type\": \"date-saver\" , \"date\" : \"2010-05-15 23:12:34 UTC\"}";
        //Check if it returns a CA
        ConversationAtom obj = gson.fromJson(json, ConversationAtom.class);

        assertNotNull(obj);
        System.out.println("Object returned : " + obj.toString() + " of class " + obj.getClass().getName());
        assertTrue(obj instanceof ConversationAtom);

        //But, seeing as this is also a Control type
        assertTrue(obj instanceof ControlBase);

        //But, also a button
        assertTrue(obj instanceof DateSaver);

        //And, value should have properly deser'd
        DateSaver cType = (DateSaver) obj;
        assertNotNull(cType.getDate());
        System.out.println("Date : " + cType.getDate());
    }

    public void testConversationDeser() {
        String json = "{\"name\":\"05208\",\"content\":[{\"tag\":\"logo\",\"type\":\"image\",\"value\":\"http://foo.bar.com/images/logo.png\"},{\"tag\":\"tagline\",\"type\":\"html-fragment\",\"value\":\"<h1>Lorum Ipsum dolor Thing\"},{\"tag\":\"reply\",\"type\":\"text-input\",\"placeholder\":\"Tap to enter  your reply here...\"}],\"controls\":[{\"tag\":\"done\",\"description\":\"Done\"}]}";

        ConversationDetail convDetail = gson.fromJson(json, ConversationDetail.class);

        assertNotNull(convDetail);

        assertEquals("05208", convDetail.getName());

        assertEquals(3, convDetail.getContent().size());
        assertEquals(1, convDetail.getControls().size());

        assertTrue(convDetail.getControls().get(0) instanceof ButtonControl);
        assertTrue(convDetail.getContent().get(2) instanceof TextInput);
        assertTrue(convDetail.getContent().get(1) instanceof Content);

    }

    public void testControlActionsDeser() {
        String json = "{\"name\":\"05209c\",\"content\":[{\"tag\":\"header\",\"type\":\"html-fragment\",\"value\":\"When did you last buy one\"},{\"tag\":\"message\",\"type\":\"text\",\"value\":\"Lorum Ipsum dolor Thing\"}],\"controls\":[{\"tag\":\"abutton\",\"description\":\"Save the Date\",\"actions\":{\"call\":\"0862437652\"}}]}";

        ConversationDetail convDetail = gson.fromJson(json, ConversationDetail.class);

        assertNotNull(convDetail);

        ControlBase cb = (ControlBase) convDetail.getControls().get(0);
        assertTrue(cb.hasActions());
        assertTrue(cb.getActions().isCall());
        assertEquals(Uri.parse("tel://0862437652"), cb.getActions().getCallUri());
    }

    public void testMultiValueDeser() {
        String json = "{\"name\":\"05215a\",\"content\":[{\"tag\":\"header\",\"type\":\"html-fragment\",\"value\":\"When did you last buy one\"},{\"tag\":\"message\",\"type\":\"text\",\"value\":\"Lorum Ipsum dolor Thing\"},{\"tag\":\"shares\",\"type\":\"multi-value-input\",\"values\":[{\"Facebook\":\"facebook\"},{\"Twitter\":\"twitta\"},{\"Email\":\"email\"}]}],\"controls\":[{\"tag\":\"share\",\"description\":\"Share\"}]}";
        ConversationDetail convDetail = gson.fromJson(json, ConversationDetail.class);
        assertNotNull(convDetail);

        assertEquals(3, convDetail.getContent().size());

        ConversationAtom a = convDetail.getContent().get(2);

        assertTrue(a instanceof MultiValueInput);
        MultiValueInput mvi = (MultiValueInput) a;

        assertEquals("shares", mvi.getTag());
        assertEquals(3, mvi.getValues().size());

        assertEquals("Twitter", mvi.getValues().get(1).getName());
        assertEquals("twitta", mvi.getValues().get(1).getValue());

    }

    public void testSliderDeser() {
        String json = "{\"name\":\"05216a\",\"content\":[{\"tag\":\"header\",\"type\":\"html-fragment\",\"value\":\"When did you last buy one\"},{\"tag\":\"message\",\"type\":\"text\",\"value\":\"Lorum Ipsum dolor Thing\"},{\"tag\":\"message\",\"type\":\"text\",\"value\":\"This is the question for the...\"},{\"tag\":\"response\",\"type\":\"slider-input\",\"values\":[{\"Poor\":1},{\"Average\":2},{\"Great\":3}]}],\"controls\":[{\"tag\":\"next\",\"description\":\"Next\"}]}";

        ConversationDetail convDetail = gson.fromJson(json, ConversationDetail.class);
        assertNotNull(convDetail);

        assertEquals(4, convDetail.getContent().size());

        ConversationAtom a = convDetail.getContent().get(3);

        assertTrue(a instanceof SliderInput);
        SliderInput sliderInput = (SliderInput) a;

        assertEquals(3, sliderInput.getValues().size());


        assertEquals("Poor", sliderInput.getValues().get(0).getLabel());
        assertEquals(1, sliderInput.getValues().get(0).getValue());
        assertEquals("Average", sliderInput.getValues().get(1).getLabel());
        assertEquals(2, sliderInput.getValues().get(1).getValue());
        assertEquals("Great", sliderInput.getValues().get(2).getLabel());
        assertEquals(3, sliderInput.getValues().get(2).getValue());
    }

}
