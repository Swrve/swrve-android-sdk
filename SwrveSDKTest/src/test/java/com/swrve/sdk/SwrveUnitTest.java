package com.swrve.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.messaging.model.Conditions;
import com.swrve.sdk.messaging.model.Trigger;
import com.swrve.sdk.messaging.model.Triggers;
import com.swrve.sdk.test.BuildConfig;
import com.swrve.sdk.test.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class SwrveUnitTest {

    private Activity mActivity;

    @Before
    public void setUp() throws Exception {
        removeSingletonInstance();
        mActivity = Robolectric.buildActivity(MainActivity.class).create().visible().get();
        ShadowLog.stream = System.out;
    }

    @After
    public void tearDown() throws Exception {
        removeSingletonInstance();
    }

    private void removeSingletonInstance() throws Exception{
        Field instance = SwrveSDKBase.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    public void testInitWithAppVersion() throws Exception {
        String appVersion = "my_version";
        SwrveConfig config = new SwrveConfig();
        config.setAppVersion(appVersion);
        Swrve swrve = (Swrve) SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        assertEquals(appVersion, swrve.appVersion);
    }

    @Test
    public void testLanguage() throws Exception {
        String strangeLanguage = "strange_language";
        String strangeLanguage2 = "strange_language_other";
        SwrveConfig config = new SwrveConfig();
        config.setLanguage(strangeLanguage);
        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        assertEquals(strangeLanguage, swrve.getLanguage());
        swrve.setLanguage(strangeLanguage2);
        assertEquals(strangeLanguage2, swrve.getLanguage());
    }

    @Test
    public void testInitialisationWithUserId() throws Exception {
        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey");
        String userId = swrve.getUserId();
        assertNotNull(userId);

        removeSingletonInstance();

        SwrveConfig config = new SwrveConfig();
        config.setUserId("custom_user_id");
        swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        String userId2 = swrve.getUserId();
        assertNotSame(userId, userId2);
        assertEquals("custom_user_id", userId2);
    }

    @Test
    public void testInitialisationWithNoId() throws Exception {
        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey");
        String userId = swrve.getUserId();
        assertNotNull(userId);

        removeSingletonInstance();

        swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey");
        String userId2 = swrve.getUserId();
        assertEquals(userId, userId2);
    }

    @Test
    public void testGetUserIdForced() {
        SwrveConfig config = new SwrveConfig();
        config.setUserId("forced");
        ISwrve swrve = SwrveSDK.createInstance(mActivity, 1, "apiKey", config);
        assertEquals("forced", swrve.getUserId());
    }

    @Test
    public void testTriggerModelV5() {
        String json = "{ triggers: [ \"song1.played\", \"song2.played\", \"song3.played\"] }";
        Triggers triggers = Triggers.fromJson(json);
        assertNull(triggers);
    }

    @Test
    public void testTriggerModelWithConditions() throws Exception {

        String json = "{\n" +
                "   \"triggers\": [\n" +
                "      {\n" +
                "         \"event_name\": \"song1.played\",\n" +
                "         \"conditions\": {\n" +
                "            \"args\": [\n" +
                "               {\n" +
                "                  \"key\": \"artist\",\n" +
                "                  \"op\": \"eq\",\n" +
                "                  \"value\": \"madonna\"\n" +
                "               },\n" +
                "               {\n" +
                "                  \"key\": \"artist\",\n" +
                "                  \"op\": \"eq\",\n" +
                "                  \"value\": \"queen\"\n" +
                "               }\n" +
                "            ],\n" +
                "            \"op\": \"and\"\n" +
                "         }\n" +
                "      },\n" +
                "      {\n" +
                "         \"event_name\": \"song2.played\",\n" +
                "         \"conditions\": {}\n" +
                "      }\n" +
                "   ]\n" +
                "}";

        Triggers triggers = Triggers.fromJson(json);

        assertNotNull(triggers);
        assertEquals(2, triggers.getTriggers().size());

        Trigger trigger1 = triggers.getTriggers().get(0);
        assertEquals("song1.played", trigger1.getEventName());
        assertNotNull(trigger1.getConditions());
        Conditions conditions = trigger1.getConditions();
        assertEquals("and", conditions.getOp());
        assertEquals(2, conditions.getArgs().size());
        assertEquals("artist", conditions.getArgs().get(0).getKey());
        assertEquals("eq", conditions.getArgs().get(0).getOp());
        assertEquals("madonna", conditions.getArgs().get(0).getValue());
        assertEquals("artist", conditions.getArgs().get(1).getKey());
        assertEquals("eq", conditions.getArgs().get(1).getOp());
        assertEquals("queen", conditions.getArgs().get(1).getValue());

        Trigger trigger2 = triggers.getTriggers().get(1);
        assertEquals("song2.played", trigger2.getEventName());
        assertNotNull(trigger2.getConditions());
        assertNull(trigger2.getConditions().getArgs());
        assertNull(trigger2.getConditions().getOp());
    }

}
