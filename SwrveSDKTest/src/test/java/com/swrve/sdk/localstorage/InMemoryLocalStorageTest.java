package com.swrve.sdk.localstorage;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InMemoryLocalStorageTest extends BaseLocalStorage {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        localStorage = new InMemoryLocalStorage();
    }

    @Test
    public void testInitEmpty() {
        assertEquals(0, localStorage.getFirstNEvents(10, "userId").size());
    }
}