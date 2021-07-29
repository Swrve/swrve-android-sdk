package com.swrve.sdk;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SwrveHelperTest extends SwrveBaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testCombineTwoStringMaps() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("key1", "replace_me");
        map1.put("key2", "value2");

        Map<String, String> map2 = new HashMap<>();
        map2.put("key1", "value1");
        map2.put("key3", "value3");

        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("key1", "value1");
        expectedMap.put("key2", "value2");
        expectedMap.put("key3", "value3");

        Map<String, String> combinedMap = SwrveHelper.combineTwoStringMaps(map1, map2);
        Assert.assertEquals(expectedMap, combinedMap);

        combinedMap = SwrveHelper.combineTwoStringMaps(map1, null);
        Assert.assertEquals(map1, combinedMap);

        combinedMap = SwrveHelper.combineTwoStringMaps(null, map2);
        Assert.assertEquals(map2, combinedMap);

        // make sure it won't crash
        combinedMap = SwrveHelper.combineTwoStringMaps(null, null);
        Assert.assertEquals(null, combinedMap);
    }
}
