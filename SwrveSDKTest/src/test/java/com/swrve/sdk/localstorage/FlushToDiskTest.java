package com.swrve.sdk.localstorage;

import android.os.Build;
import android.util.Log;

import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.test.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.LinkedHashMap;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.N_MR1)
public class FlushToDiskTest  {
    private LocalStorage primaryLocalStorage;
    private LocalStorage secondaryLocalStorage;
    private SwrveMultiLayerLocalStorage multiLayerLocalStorage;

    @Before
    public void setUp() throws Exception {
        SwrveLogger.setLogLevel(Log.VERBOSE);
        ShadowLog.stream = System.out;
        primaryLocalStorage = new InMemoryLocalStorage();
        secondaryLocalStorage = new SQLiteLocalStorage(RuntimeEnvironment.application, "flushToDiskTest", 2024 * 2024 * 2024);
        multiLayerLocalStorage = new SwrveMultiLayerLocalStorage(primaryLocalStorage);
        multiLayerLocalStorage.setSecondaryStorage(secondaryLocalStorage);
    }

    @Test
    public void testFlushToDisk() throws Exception {
        String userId = "userId";
        int amount = 2000;
        for (int i = 0; i < amount; i++) {
            multiLayerLocalStorage.addEvent(userId, "event" + i);
        }
        LinkedHashMap<LocalStorage, LinkedHashMap<Long, String>> events = multiLayerLocalStorage.getCombinedFirstNEvents(amount, userId);
        int eventCount = events.get(primaryLocalStorage).size();
        int primaryEventCount = primaryLocalStorage.getFirstNEvents(amount, userId).size();
        int secondaryEventCount = secondaryLocalStorage.getFirstNEvents(amount, userId).size();
        assertEquals(1, events.size());
        assertEquals(amount, eventCount);
        assertEquals(amount, primaryEventCount);
        assertEquals(0, secondaryEventCount);

        multiLayerLocalStorage.flush();

        events = multiLayerLocalStorage.getCombinedFirstNEvents(amount, userId);
        eventCount = events.get(secondaryLocalStorage).size();
        primaryEventCount = primaryLocalStorage.getFirstNEvents(amount, userId).size();
        secondaryEventCount = secondaryLocalStorage.getFirstNEvents(amount, userId).size();
        assertEquals(1, events.size());
        assertEquals(amount, eventCount);
        assertEquals(0, primaryEventCount);
        assertEquals(amount, secondaryEventCount);
    }
}
