package com.swrve.sdk;

import static com.swrve.sdk.ISwrveCommon.EVENT_PAYLOAD_KEY;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.swrve.sdk.notifications.model.SwrveNotification;

import org.junit.Assert;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowPendingIntent;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class SwrveNotificationTestUtils {
    public static void assertEngagedEvent(String eventJson, String eventName) {
        Gson gson = new Gson(); // eg: {"type":"event","time":1466519995192,"name":"Swrve.Messages.Push-1.engaged"}
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, String> event = gson.fromJson(eventJson, type);
        assertEquals(4, event.size());
        Assert.assertTrue(event.containsKey("type"));
        assertEquals("event", event.get("type"));
        Assert.assertTrue(event.containsKey("name"));
        assertEquals(eventName, event.get("name"));
        Assert.assertTrue(event.containsKey("time"));
        Assert.assertTrue(event.containsKey("seqnum"));
    }

    public static void assertEngagedEvent(String eventJson, String eventName, Map<String, String> expectedPayload) {
        Gson gson = new Gson(); // eg: {"type":"event","time":1466519995192,"name":"Swrve.Messages.Push-1.engaged"}
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> event = gson.fromJson(eventJson, type);
        assertTrue(event.containsKey("type"));
        assertEquals("event", event.get("type"));
        assertTrue(event.containsKey("name"));
        assertEquals(eventName, event.get("name"));
        assertTrue(event.containsKey("time"));
        assertTrue(event.containsKey("seqnum"));

        if (expectedPayload != null && expectedPayload.size() > 0) {
            assertTrue(event.containsKey(EVENT_PAYLOAD_KEY));
            Map<String, String> actualPayload = (Map) event.get(EVENT_PAYLOAD_KEY);
            assertEquals(expectedPayload, actualPayload);
        }
    }

    public static void displayNotification(Activity mActivity, SwrveNotificationBuilder builderSpy, Bundle bundle) {
        String msgText = bundle.getString(SwrveNotificationConstants.TEXT_KEY);
        NotificationCompat.Builder notificationCompatBuilder = builderSpy.build(msgText, bundle, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, null);
        Notification notification = notificationCompatBuilder.build();
        final NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(123, notification);
    }

    public static Notification assertNotification(String tickerText, String sound, Bundle extras) {
        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(tickerText, notification.tickerText);
        assertEquals(sound, notification.sound == null ? null : notification.sound.toString());
        PendingIntent pendingIntent = notification.contentIntent;
        ShadowPendingIntent shadowPendingIntent = shadowOf(pendingIntent);
        assertNotNull(shadowPendingIntent);
        Intent intent = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertTrue(shadowPendingIntent.isActivityIntent());
            assertEquals(1, shadowPendingIntent.getSavedIntents().length);
            intent = shadowPendingIntent.getSavedIntents()[0];
            assertEquals("com.swrve.sdk.SwrveNotificationEngageActivity", intent.getComponent().getClassName());
        } else {
            assertTrue(shadowPendingIntent.isBroadcastIntent());
            assertEquals(1, shadowPendingIntent.getSavedIntents().length);
            intent = shadowPendingIntent.getSavedIntents()[0];
            assertEquals("com.swrve.sdk.SwrveNotificationEngageReceiver", intent.getComponent().getClassName());
        }
        assertNotNull(intent);
        Bundle intentExtras = intent.getBundleExtra(SwrveNotificationConstants.PUSH_BUNDLE);
        assertNotNull(intentExtras);
        for (String key : extras.keySet()) {
            assertTrue(intentExtras.containsKey(key));
            assertEquals(extras.get(key), intentExtras.getString(key));
        }
        ShadowNotification shadowNotification = shadowOf(notification);
        // In the case of a rich push, we should expect the title to be set to something else
        if (!intentExtras.containsKey(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY)) {
            if (shadowNotification.getContentTitle().equals("androidx.multidex.MultiDexApplication")
                    || shadowNotification.getContentTitle().equals("com.swrve.sdk.test")) {
                // passed
            } else {
                fail();
            }
        } else {
            SwrveNotification extrasPPayload = SwrveNotification.fromJson(extras.getString(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY));
            if (extrasPPayload != null) {
                if (extrasPPayload.getVersion() == SwrveNotificationConstants.SWRVE_PUSH_VERSION) {
                    if (extrasPPayload.getMedia() != null && extrasPPayload.getMedia().getType() != null) {
                        assertEquals(extrasPPayload.getMedia().getTitle(), shadowNotification.getContentTitle());
                        // Robolectric still has to be give the ability to check for subText and body separately
                        //assertEquals(extrasPPayload.getMedia().getSubtitle(), shadowNotification.getSubText());
                        assertEquals(extrasPPayload.getMedia().getBody(), notification.tickerText);
                    }
                }
            }
        }

        return notification;
    }

    public static void assertNumberOfNotifications(int expectedNumberOfNotifications) {
        NotificationManager notificationManager = (NotificationManager) ApplicationProvider.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        assertEquals(expectedNumberOfNotifications, notifications.size());
    }
}
