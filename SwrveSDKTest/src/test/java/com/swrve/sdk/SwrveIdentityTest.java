package com.swrve.sdk;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.localstorage.LocalStorage;
import com.swrve.sdk.localstorage.SQLiteLocalStorage;
import com.swrve.sdk.rest.IRESTClient;
import com.swrve.sdk.rest.IRESTResponseListener;
import com.swrve.sdk.rest.RESTResponse;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.swrve.sdk.ISwrveCommon.EVENT_FIRST_SESSION;
import static com.swrve.sdk.SwrveTrackingState.EVENT_SENDING_PAUSED;
import static com.swrve.sdk.SwrveTrackingState.ON;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

public class SwrveIdentityTest extends SwrveBaseTest {

    private Swrve swrveSpy;
    private SwrveProfileManager profileManagerSpy;
    private IRESTClient profileManagerRestClientSpy;
    private int appId = 1;
    private String testExternalId = "ExternalUserId";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveCommon.setSwrveCommon(swrveSpy);
        doReturn(true).when(swrveSpy).restClientExecutorExecute(any(Runnable.class)); // disable rest
        profileManagerSpy = Mockito.spy(swrveSpy.profileManager);
        swrveSpy.profileManager = profileManagerSpy;

        profileManagerRestClientSpy = Mockito.spy(profileManagerSpy.restclient);
        profileManagerSpy.restclient = profileManagerRestClientSpy;
        swrveSpy.init(mActivity);
    }

    @Test
    public void testIdentityCanGetCachedUserBefore_SDKInit() throws Exception {

        //  Add current swrve user to cache to mock that it has been identified.
        SwrveUser unVerifiedUser = new SwrveUser(swrveSpy.getUserId(), testExternalId, true);
        swrveSpy.multiLayerLocalStorage.getSecondaryStorage().saveUser(unVerifiedUser);

        SwrveTestUtils.shutdownAndRemoveSwrveSDKSingletonInstance();

        // create a new instance , but dont call init.
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(ApplicationProvider.getApplicationContext(), 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);

        final String cachedSwrveId = swrveSpy.getUserId();
        final AtomicBoolean identityCallback = new AtomicBoolean(false);
        swrveSpy.identify(testExternalId, new SwrveIdentityResponse() {
            @Override
            public void onSuccess(String status, String swrveId) {
                assertEquals(swrveId, cachedSwrveId);
                assertEquals(status, "Loaded from cache");
                identityCallback.set(true);
            }

            @Override
            public void onError(int responseCode, String errorMessage) {
                fail("Couldn't get user from cache. errorMessage:" + errorMessage);
                identityCallback.set(true);
            }
        });
        await().untilTrue(identityCallback);
    }

    @Test
    public void testIdentityResponseSuccess() {

        final String currentSwrveUserId = swrveSpy.getUserId();

        int responseCode = 200;
        String response = "{\"swrve_id\" : " + currentSwrveUserId + ", \"status\" : \"some_server_response_status\"}";
        mockRestResponse(responseCode, response);

        final AtomicBoolean identityCallback = new AtomicBoolean(false);
        TestSwrveIdentityResponse testSwrveIdentityResponse = new TestSwrveIdentityResponse(identityCallback);
        TestSwrveIdentityResponse testSwrveIdentityResponseSpy = Mockito.spy(testSwrveIdentityResponse);
        swrveSpy._identify("SomeExternalId", testSwrveIdentityResponseSpy);
        await().untilTrue(identityCallback);

        Mockito.verify(testSwrveIdentityResponseSpy).onSuccess("some_server_response_status", currentSwrveUserId);
        Mockito.verify(testSwrveIdentityResponseSpy, never()).onError(anyInt(), anyString());
    }

    @Test
    public void testIdentityResponseNull() {
        try {
            String currentSwrveUserId = swrveSpy.getUserId();

            // test success case by creating success response's below

            swrveSpy.profileManager = new SwrveProfileManagerIdentifySuccess("user1", mActivity, 1, "apiKey", new SwrveConfig(), null);
            swrveSpy._identify("", null); // test null identityResponse with blank externalUserId
            assertEquals(currentSwrveUserId, swrveSpy.getUserId());

            swrveSpy.profileManager = new SwrveProfileManagerIdentifySuccess("user2", mActivity, 1, "apiKey", new SwrveConfig(), null);
            swrveSpy._identify("user2", null); // test null identityResponse with new externalUserId
            assertEquals("user2", swrveSpy.getUserId());

            swrveSpy._identify("user2", null); // test null identityResponse with existing externalUserId
            assertEquals("user2", swrveSpy.getUserId());

            // test error case by creating error response's below

            swrveSpy.profileManager = new SwrveProfileManagerIdentifyError(mActivity, 1, "apiKey", new SwrveConfig(), null);
            currentSwrveUserId = swrveSpy.getUserId();

            swrveSpy._identify("DiffExternalId", null); // test null identityResponse and error response
            assertNotEquals(currentSwrveUserId, swrveSpy.getUserId()); // new userid should be generated

        } catch (Exception ex) {
            SwrveLogger.e("Null SwrveIdentityResponse may have caused an exception", ex);
            fail("Null SwrveIdentityResponse may have caused an exception:" + ex.getMessage());
        }
    }

    @Test
    public void testIdentityResponseError() {

        mockRestException(new Exception("some error message"));

        final AtomicBoolean identityCallback = new AtomicBoolean(false);
        TestSwrveIdentityResponse testSwrveIdentityResponse = new TestSwrveIdentityResponse(identityCallback);
        TestSwrveIdentityResponse testSwrveIdentityResponseSpy = Mockito.spy(testSwrveIdentityResponse);
        swrveSpy._identify("SomeExternalId", testSwrveIdentityResponseSpy);
        await().untilTrue(identityCallback);

        Mockito.verify(testSwrveIdentityResponseSpy, never()).onSuccess(anyString(), anyString());
        Mockito.verify(testSwrveIdentityResponseSpy).onError(503, "some error message");
    }

    @Test
    public void testIdentityResponseErrorWithNullExternalUserId() {

        mockRestException(new Exception("some error message"));

        final AtomicBoolean identityCallback = new AtomicBoolean(false);
        TestSwrveIdentityResponse testSwrveIdentityResponse = new TestSwrveIdentityResponse(identityCallback);
        TestSwrveIdentityResponse testSwrveIdentityResponseSpy = Mockito.spy(testSwrveIdentityResponse);
        swrveSpy._identify(null, testSwrveIdentityResponseSpy);
        await().untilTrue(identityCallback);

        Mockito.verify(testSwrveIdentityResponseSpy, never()).onSuccess(anyString(), anyString());
        Mockito.verify(testSwrveIdentityResponseSpy).onError(-1, "External user id cannot be null or empty");
    }

    @Test
    public void testIdentityResponseErrorWithBlankNullExternalUserId() {

        mockRestException(new Exception("some error message"));

        final AtomicBoolean identityCallback = new AtomicBoolean(false);
        TestSwrveIdentityResponse testSwrveIdentityResponse = new TestSwrveIdentityResponse(identityCallback);
        TestSwrveIdentityResponse testSwrveIdentityResponseSpy = Mockito.spy(testSwrveIdentityResponse);
        swrveSpy._identify("", testSwrveIdentityResponseSpy);
        await().untilTrue(identityCallback);

        Mockito.verify(testSwrveIdentityResponseSpy, never()).onSuccess(anyString(), anyString());
        Mockito.verify(testSwrveIdentityResponseSpy).onError(-1, "External user id cannot be null or empty");
    }

    @Test
    public void testIdentify_Forbidden_Email2() {

        final String currentSwrveUserId = swrveSpy.getUserId();

        mockRestResponse(403, "some_response");
        identifyAndWait("email@gmail.com");
        assertEquals(currentSwrveUserId, swrveSpy.getUserId());
        assertEquals(0, getAllSwrveUsers().size());

        mock200RestResponseWithSameUserId();
        identifyAndWait("NewExternalUser");
        assertEquals(currentSwrveUserId, swrveSpy.getUserId());
        assertEquals(1, getAllSwrveUsers().size());

        mockRestResponse(403, "some_response");
        identifyAndWait("anotherEmail@gmail.com");
        assertEquals(1, getAllSwrveUsers().size());
        assertThat(currentSwrveUserId, not(equalTo(swrveSpy.getUserId())));
    }

    @Test
    public void testIdentify_SwrveError() {

        final String currentSwrveUserId = swrveSpy.getUserId();

        //  Add current swrve user to cache to mock that it has been identified.
        SwrveUser unVerifiedUser = new SwrveUser(swrveSpy.getUserId(), testExternalId, true);
        swrveSpy.multiLayerLocalStorage.getSecondaryStorage().saveUser(unVerifiedUser);

        mockRestResponse(404, "some_response");
        identifyAndWait("NewExternalUser");
        // switch users failed, so should be tracking under a new anonymous swrve user id
        assertThat(currentSwrveUserId, not(equalTo(swrveSpy.getUserId())));
    }

    @Test
    public void testIdentify_Success_SameUserId() {
        final String currentSwrveUserId = swrveSpy.getUserId();
        mock200RestResponseWithSameUserId();
        identifyAndWait(testExternalId);
        assertEquals(currentSwrveUserId, swrveSpy.getUserId());
        assertEquals(1, getAllSwrveUsers().size());
    }

    @Test
    public void testIdentify_Success_NewUserId() {
        //Identify with User1 , then User2 , then back to User1

        //User1 will return SwrverUser1
        String response = "{\"swrve_id\" : \"SwrveUser1\", \"status\" : \"existing_external_id_with_matching_swrve_id\"}";
        mockRestResponse(200, response);
        identifyAndWait("User1");
        assertEquals(swrveSpy.getUserId(), "SwrveUser1");
        verify(swrveSpy, atLeastOnce()).clearAllAuthenticatedNotifications(); // verify clearAllAuthenticatedNotifications is called when user is switched

        //User2 will return SwrverUser2
        response = "{\"swrve_id\" : \"SwrveUser2\", \"status\" : \"existing_external_id_with_matching_swrve_id\"}";
        mockRestResponse(200, response);
        identifyAndWait("User2");
        assertEquals(swrveSpy.getUserId(), "SwrveUser2");
        verify(swrveSpy, atLeastOnce()).clearAllAuthenticatedNotifications(); // verify clearAllAuthenticatedNotifications is called when user is switched

        //User1 will return SwrverUser1
        response = "{\"swrve_id\" : \"SwrveUser1\", \"status\" : \"existing_external_id_with_matching_swrve_id\"}";
        mockRestResponse(200, response);
        identifyAndWait("User1");
        assertEquals(swrveSpy.getUserId(), "SwrveUser1");
        verify(swrveSpy, atLeastOnce()).clearAllAuthenticatedNotifications(); // verify clearAllAuthenticatedNotifications is called when user is switched
    }

    @Test
    public void testIdentify_SwrveUser_Cache_Object_Updated() {

        mockRestException(new Exception());
        identifyAndWait("User1");
        SwrveUser user1 = swrveSpy.multiLayerLocalStorage.getUserByExternalUserId("User1");
        assertEquals(user1.isVerified(), false);
        assertEquals(user1.getExternalUserId(), "User1");
        assertEquals(user1.getSwrveUserId(), swrveSpy.getUserId());

        String response = "{\"swrve_id\" : \"SwrveUser1\", \"status\" : \"existing_external_id_with_matching_swrve_id\"}";
        mockRestResponse(200, response);
        identifyAndWait("User1");
        user1 = swrveSpy.multiLayerLocalStorage.getUserByExternalUserId("User1");
        assertEquals(user1.isVerified(), true);
        assertEquals(user1.getExternalUserId(), "User1");
        assertEquals(user1.getSwrveUserId(), "SwrveUser1");
    }

    @Test
    public void testIdentify_Url() {
        String identityUrl = swrveSpy.profileManager.getIdentityUrl();
        String identityUrlExpected = "https://" + appId + ".identity.swrve.com/identify";
        assertEquals(identityUrlExpected, identityUrl);
    }

    @Test
    public void testIdentify_Body() throws Exception {
        SwrveProfileManager profileManager = new SwrveProfileManager(mActivity, 1, "apiKey", new SwrveConfig(), null);
        String postString = profileManager.getIdentityBody("ExternalUserId", "SwrveUserId", "deviceId");
        JSONObject postObject = new JSONObject(postString);
        assertEquals(postObject.getString("swrve_id"), "SwrveUserId");
        assertEquals(postObject.getString("external_user_id"), "ExternalUserId");
        assertEquals(postObject.getString("unique_device_id"), "deviceId");
        assertEquals(postObject.getString("api_key"), "apiKey");
    }

    @Test
    public void testIdentify_With_Null_ExternalId() {
        // Test:  Anonymous -> Identify New User
        // Confirm no change if nil or empty

        SQLiteLocalStorage sqLiteLocalStorage = (SQLiteLocalStorage) swrveSpy.multiLayerLocalStorage.getSecondaryStorage();
        SwrveUser user = new SwrveUser(swrveSpy.getUserId(), "SomeExternalId", true);
        sqLiteLocalStorage.saveUser(user);

        final String userIdBeforeIdentityCall = swrveSpy.getUserId();
        mockRestException(new Exception());
        identifyAndWait(null);
        assertEquals(userIdBeforeIdentityCall, (swrveSpy.getUserId()));
        assertEquals(1, getAllSwrveUsers().size());

        final String userIdBeforeIdentityCall2 = swrveSpy.getUserId();
        mock200RestResponseWithSameUserId();
        identifyAndWait(testExternalId );
        assertNotEquals(userIdBeforeIdentityCall2, swrveSpy.getUserId());
        assertEquals(2, getAllSwrveUsers().size());
    }

    @Test
    public void testFirstSessionEvents() {

        // as part of setup a new user is created and first session event is sent
        Mockito.verify(swrveSpy, times(1))._event(EVENT_FIRST_SESSION);

        // identify and respond with same userId which shouldn't send another first session event
        mock200RestResponseWithSameUserId();
        identifyAndWait("NewExternalUser");
        Mockito.verify(swrveSpy, times(1))._event(EVENT_FIRST_SESSION);

        // identify and respond with diff userId which shouldn't send another first session event
        String response = "{\"swrve_id\" : \"SwrveUser1\", \"status\" : \"existing_external_id_with_matching_swrve_id\"}";
        mockRestResponse(200, response);
        identifyAndWait("User1");
        Mockito.verify(swrveSpy, times(1))._event(EVENT_FIRST_SESSION);

        // identify again and this time respond with same userId which SHOULD send a first session event
        mock200RestResponseWithSameUserId();
        identifyAndWait("User245646");
        Mockito.verify(swrveSpy, times(2))._event(EVENT_FIRST_SESSION);
    }

    @Test
    public void testTrackingState() {
        SwrveTrackingState trackingState = swrveSpy.trackingState;
        assertTrue(trackingState == ON);

        swrveSpy.pauseEventSending();
        trackingState = swrveSpy.trackingState;
        assertTrue(trackingState == EVENT_SENDING_PAUSED);

        swrveSpy.enableEventSending();
        trackingState = swrveSpy.trackingState;
        assertTrue(trackingState == ON);
    }

    @Test
    public void testQueueEventWhileEventSendingPaused() {

        reset(swrveSpy); // reset the setup init calls on swrveSpy so the times()/never() test can be done below
        assertNotNull(swrveSpy.pausedEvents);
        assertEquals(swrveSpy.pausedEvents.size(), 0);

        swrveSpy.pauseEventSending();
        assertTrue(swrveSpy.trackingState == EVENT_SENDING_PAUSED);

        SwrveSDK.event("someEvent");
        SwrveSDK.event("anotherEvent", new HashMap<>());
        assertEquals(swrveSpy.pausedEvents.size(), 2);

        swrveSpy.enableEventSending();
        swrveSpy.queuePausedEvents();
        assertEquals(swrveSpy.pausedEvents.size(), 0);
        verify(swrveSpy, atLeast(2)).storageExecutorExecute(any(Runnable.class));
        verify(swrveSpy, atLeastOnce()).sendQueuedEvents();
    }

    @Test
    public void testGetExternalId() {

        assertEquals("", SwrveSDK.getExternalUserId()); // no external id to start with

        String response = "{\"swrve_id\" : \"SwrveUser1\", \"status\" : \"new_external_id\"}";
        mockRestResponse(200, response);

        identifyAndWait("User1");
        assertEquals("User1", SwrveSDK.getExternalUserId());

        response = "{\"swrve_id\" : \"SwrveUser2\", \"status\" : \"new_external_id\"}";
        mockRestResponse(200, response);
        identifyAndWait("User2");
        assertEquals("User2", SwrveSDK.getExternalUserId());
    }

    @Test
    public void testSwitchUserClearAuthenticatedNotifications() {

        ISwrveCommon swrveCommon = SwrveCommon.getInstance();
        assertNumberOfNotification(0);

        // fire 2 authenticated notifications and 1 regular non authenticated notification
        fireNotification("authenticated notification", "personalized message 1", 123);
        swrveCommon.saveNotificationAuthenticated(123);
        fireNotification("authenticated notification", "personalized message 2", 456);
        swrveCommon.saveNotificationAuthenticated(456);
        fireNotification("Non authenticated notification", "generic broadcast all message", 789);

        assertNumberOfNotification(3);

        LocalStorage localStorage = new SQLiteLocalStorage(ApplicationProvider.getApplicationContext(), "test", 2024 * 1024 * 1024);
        SQLiteLocalStorage sqLiteLocalStorage = (SQLiteLocalStorage) localStorage;

        SwrveUser user = new SwrveUser(swrveSpy.getUserId(), "SomeExternalId", true);
        sqLiteLocalStorage.saveUser(user);

        swrveSpy.switchUser("someuser1");

        assertNumberOfNotification(1); // should only be one non authenticated notification left
    }

    @Test
    public void testSwitchUserAutoShowMessagesReset() {
        assertEquals("AutoDisplayMessages should be true upon sdk init.", true, swrveSpy.autoShowMessagesEnabled);

        swrveSpy.autoShowMessagesEnabled = false;
        assertEquals("AutoDisplayMessages should be false because it was just set false from above.", false, swrveSpy.autoShowMessagesEnabled);

        swrveSpy.switchUser("someuser1");
        assertEquals("AutoDisplayMessages should be reset to true after identify.", true, swrveSpy.autoShowMessagesEnabled);

        // do the test again with different user
        swrveSpy.autoShowMessagesEnabled = false;
        assertEquals("AutoDisplayMessages should be false because it was just set false from above.", false, swrveSpy.autoShowMessagesEnabled);

        swrveSpy.switchUser("someuser2");
        assertEquals("AutoDisplayMessages should be reset to true after identify.", true, swrveSpy.autoShowMessagesEnabled);
    }

    @Test
    public void testSwitchUserDiffUserId() {

        reset(swrveSpy); // reset the setup init calls on swrveSpy so the times() test can be done below
        swrveSpy.trackingState = EVENT_SENDING_PAUSED;
        assertEquals("Setup of this test will be that event sending is paused.", EVENT_SENDING_PAUSED, swrveSpy.trackingState);

        swrveSpy.switchUser("some_diff_userId"); // call switchuser with the diff userId

        assertEquals("switchUser should always enable event sending.", ON, swrveSpy.trackingState);
        Mockito.verify(swrveSpy, times(1)).clearAllAuthenticatedNotifications();
        Mockito.verify(swrveSpy, times(1)).init(any(Activity.class));
        Mockito.verify(swrveSpy, times(1)).queuePausedEvents();
    }

    @Test
    public void testSwitchUserSameUserId() {

        reset(swrveSpy); // reset the setup init calls on swrveSpy so the never() test can be done below

        final String currentUserId = swrveSpy.getUserId();
        swrveSpy.trackingState = EVENT_SENDING_PAUSED;
        assertEquals("Setup of this test will be that event sending is paused.", EVENT_SENDING_PAUSED, swrveSpy.trackingState);

        swrveSpy.switchUser(currentUserId); // call switchuser with the current userId (simulates identify twice in a row with same userId)

        assertEquals("switchUser should always enable event sending.", ON, swrveSpy.trackingState);
        Mockito.verify(swrveSpy, never()).clearAllAuthenticatedNotifications();
        Mockito.verify(swrveSpy, never()).init(any(Activity.class));
        Mockito.verify(swrveSpy, times(1)).queuePausedEvents();
    }

    // Helper methods below

    private void assertNumberOfNotification(int numberOfNotifications) {
        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(numberOfNotifications, notifications.size());
    }

    private void fireNotification(String title, String message, int notificationId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("456", "channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mActivity, "456");
        PendingIntent pendingIntent = PendingIntent.getActivity(mActivity, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setSmallIcon(com.swrve.sdk.test.R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent);
        Notification notification = mBuilder.build();
        NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }

    private void mockRestResponse(int responseCode, String response) {
        doAnswer((Answer<Void>) invocation -> {
            IRESTResponseListener callback = (IRESTResponseListener) invocation.getArguments()[2];
            callback.onResponse(new RESTResponse(responseCode, response, null));
            return null;
        }).when(profileManagerRestClientSpy).post(anyString(), anyString(), any(IRESTResponseListener.class), anyString());
    }

    private void mock200RestResponseWithSameUserId() {
        doAnswer((Answer<Void>) invocation -> {
            String encodedBody = (String) invocation.getArguments()[1];
            String userId = null;
            try {
                JSONObject jsonObj = new JSONObject(encodedBody);
                userId = jsonObj.getString("swrve_id");
            } catch (Exception e) {
                SwrveLogger.e("Error getting swrve id from json string", e);
            }
            int responseCode = 200;
            String response = "{\"swrve_id\" : " + userId + ", \"status\" : \"new_external_id\"}";

            IRESTResponseListener callback = (IRESTResponseListener) invocation.getArguments()[2];
            callback.onResponse(new RESTResponse(responseCode, response, null));
            return null;
        }).when(profileManagerRestClientSpy).post(anyString(), anyString(), any(IRESTResponseListener.class), anyString());
    }

    private void mockRestException(Exception ex) {
        doAnswer((Answer<Void>) invocation -> {
            IRESTResponseListener callback = (IRESTResponseListener) invocation.getArguments()[2];
            callback.onException(ex);
            return null;
        }).when(profileManagerRestClientSpy).post(anyString(), anyString(), any(IRESTResponseListener.class), anyString());
    }

    private void identifyAndWait(String externalUserId) {
        final AtomicBoolean identityCallback = new AtomicBoolean(false);
        swrveSpy._identify(externalUserId, new SwrveIdentityResponse() {
            @Override
            public void onSuccess(String status, String swrveId) {
                identityCallback.set(true);
            }

            @Override
            public void onError(int responseCode, String errorMessage) {
                identityCallback.set(true);
            }
        });
        await().untilTrue(identityCallback);
    }

    private class TestSwrveIdentityResponse implements SwrveIdentityResponse {
        final AtomicBoolean identityCallback;

        public TestSwrveIdentityResponse(AtomicBoolean identityCallback) {
            this.identityCallback = identityCallback;
        }

        @Override
        public void onSuccess(String status, String swrveId) {
            identityCallback.set(true);
        }

        @Override
        public void onError(int responseCode, String errorMessage) {
            identityCallback.set(true);
        }
    }

    private class SwrveProfileManagerIdentifySuccess extends SwrveProfileManager<SwrveConfig> {

        final String testUserId;

        protected SwrveProfileManagerIdentifySuccess(String userId, Context context, int appId, String apiKey, SwrveConfig config, IRESTClient restClient) {
            super(context, appId, apiKey, config, restClient);
            this.testUserId = userId;
        }

        @Override
        protected void identify(final String externalUserId, final String userId, final String deviceId, final SwrveIdentityResponse identityResponse) {
            identityResponse.onSuccess("status", testUserId);
        }
    }

    private class SwrveProfileManagerIdentifyError extends SwrveProfileManager<SwrveConfig> {

        protected SwrveProfileManagerIdentifyError(Context context, int appId, String apiKey, SwrveConfig config, IRESTClient restClient) {
            super(context, appId, apiKey, config, restClient);
        }

        @Override
        protected void identify(final String externalUserId, final String userId, final String deviceId, final SwrveIdentityResponse identityResponse) {
            identityResponse.onError(1, "error message");
        }
    }

    private ArrayList<SwrveUser> getAllSwrveUsers() {
        File dbFile = mActivity.getDatabasePath("swrve.db");
        SQLiteDatabase database = SQLiteDatabase.openDatabase(dbFile.getPath(), null, 0);
        ArrayList<SwrveUser> allUsers = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.rawQuery("SELECT swrve_user_id, external_user_id, verified FROM users", null);
            if (cursor.moveToFirst()) {
                do {
                    String swrveUserId = cursor.getString(0);
                    String externalId = cursor.getString(1);
                    int verified = cursor.getInt(2);
                    SwrveUser swrveUser = new SwrveUser(swrveUserId, externalId, verified == 1);
                    allUsers.add(swrveUser);
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            SwrveLogger.e("Exception occurred getting all user", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            database.close();
        }
        return allUsers;
    }
}
