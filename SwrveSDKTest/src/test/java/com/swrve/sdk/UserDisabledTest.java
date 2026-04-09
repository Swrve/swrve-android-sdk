package com.swrve.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.swrve.sdk.config.SwrveConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class UserDisabledTest extends SwrveBaseTest {

    private Swrve swrve;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void event401DisablesCurrentUser() throws Exception {
        AtomicInteger listenerCallCount = new AtomicInteger(0);
        AtomicReference<String> disabledUserIdRef = new AtomicReference<>();
        AtomicReference<String> externalUserIdRef = new AtomicReference<>();

        swrve = createSwarveWithUserDisabledListener(listenerCallCount, disabledUserIdRef, externalUserIdRef);

        String disabledUserId = swrve.getUserId();
        String externalUserId = "external-user";
        seedUserData(disabledUserId, externalUserId);

        swrve.profileManager.handleDisabledUser(disabledResponseBody(), disabledUserId);

        assertEquals(1, listenerCallCount.get());
        assertEquals(disabledUserId, disabledUserIdRef.get());
        assertEquals(externalUserId, externalUserIdRef.get());
        assertNotEquals(disabledUserId, swrve.getUserId());
        assertFalse(swrve.isStarted());
        assertEquals(SwrveTrackingState.STOPPED, swrve.profileManager.getTrackingState());
        assertUserDataDeleted(disabledUserId);
    }

    @Test
    public void handleDisableUserDuplicateDisabledResponseIgnored() throws Exception {
        AtomicInteger listenerCallCount = new AtomicInteger(0);
        AtomicReference<String> disabledUserIdRef = new AtomicReference<>();
        AtomicReference<String> externalUserIdRef = new AtomicReference<>();

        swrve = createSwarveWithUserDisabledListener(listenerCallCount, disabledUserIdRef, externalUserIdRef);

        String disabledUserId = swrve.getUserId();
        String externalUserId = "external-user";
        seedUserData(disabledUserId, externalUserId);

        swrve.profileManager.handleDisabledUser(disabledResponseBody(), disabledUserId);
        String userIdAfterFirst401 = swrve.getUserId();

        swrve.profileManager.handleDisabledUser(disabledResponseBody(), disabledUserId);

        assertEquals(1, listenerCallCount.get());
        assertEquals(disabledUserId, disabledUserIdRef.get());
        assertEquals(externalUserId, externalUserIdRef.get());
        assertEquals(userIdAfterFirst401, swrve.getUserId());
        assertFalse(swrve.isStarted());
        assertUserDataDeleted(disabledUserId);
    }

    @Test
    public void handleDisableUserDoesNotStopTrackingWhenCurrentUserChanged() throws Exception {
        AtomicInteger listenerCallCount = new AtomicInteger(0);
        AtomicReference<String> disabledUserIdRef = new AtomicReference<>();
        AtomicReference<String> externalUserIdRef = new AtomicReference<>();

        swrve = createSwarveWithUserDisabledListener(listenerCallCount, disabledUserIdRef, externalUserIdRef);

        String disabledUserId = swrve.getUserId();
        String activeUserId = "user-2";
        String externalUserId = "external-user";
        seedUserData(disabledUserId, externalUserId);

        swrve.profileManager.setUserId(activeUserId);
        assertEquals(activeUserId, swrve.getUserId());
        assertTrue(swrve.isStarted());

        swrve.profileManager.handleDisabledUser(disabledResponseBody(), disabledUserId);

        assertEquals(1, listenerCallCount.get());
        assertEquals(disabledUserId, disabledUserIdRef.get());
        assertEquals(externalUserId, externalUserIdRef.get());
        assertEquals(activeUserId, swrve.getUserId());
        assertTrue(swrve.isStarted());
        assertUserDataDeleted(disabledUserId);
    }

    private Swrve createSwarveWithUserDisabledListener(
            AtomicInteger listenerCallCount,
            AtomicReference<String> disabledUserIdRef,
            AtomicReference<String> externalUserIdRef) throws Exception {
        SwrveConfig config = new SwrveConfig();
        config.setAutoDownloadCampaignsAndResources(false);
        config.setUserDisabledListener((context, swrveUserId, externalId) -> {
            listenerCallCount.incrementAndGet();
            disabledUserIdRef.set(swrveUserId);
            externalUserIdRef.set(externalId);
        });

        Swrve swrveSpy = SwrveTestUtils.createSpyInstance(config);
        SwrveTestUtils.runSingleThreaded(swrveSpy);
        swrveSpy.init(mActivity);
        return swrveSpy;
    }

    private void seedUserData(String userId, String externalUserId) throws Exception {
        swrve.multiLayerLocalStorage.saveUser(new SwrveUser(userId, externalUserId, true));
        swrve.multiLayerLocalStorage.addEvent(userId, "event1");
        swrve.multiLayerLocalStorage.setCacheEntry(userId, "test_cache", "value");
        assertNotNull(swrve.multiLayerLocalStorage.getUserBySwrveUserId(userId));
        assertEquals("value", swrve.multiLayerLocalStorage.getCacheEntry(userId, "test_cache"));
    }

    private void assertUserDataDeleted(String userId) {
        assertNull(swrve.multiLayerLocalStorage.getUserBySwrveUserId(userId));
        assertNull(swrve.multiLayerLocalStorage.getCacheEntry(userId, "test_cache"));
        assertFalse(swrve.multiLayerLocalStorage.hasQueuedEvents(userId));
    }

    private String disabledResponseBody() {
        return "{ \"message\": \"User access has been disabled\" }";
    }
}
