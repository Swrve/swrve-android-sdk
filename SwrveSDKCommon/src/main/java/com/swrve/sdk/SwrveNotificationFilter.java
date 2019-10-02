package com.swrve.sdk;

import android.app.Notification;
import android.support.v4.app.NotificationCompat;

public interface SwrveNotificationFilter {

    /**
     * Method which can be implemented to filter incoming remote notifications. If filtering Geo Notifications,
     * please use the SwrveGeoCustomFilter instead.
     * Return null to prevent the notification from displaying.
     *
     * @param builder             Notification builder with the Swrve generated notification
     * @param id                  Id that the notification would be displayed with
     * @param notificationDetails Notification details such as title and text
     * @param jsonPayload         Custom json payload
     * @return Your customized notification, or builder.build() with/without modifications, or null to suppress
     */
    Notification filterNotification(NotificationCompat.Builder builder, int id, SwrveNotificationDetails notificationDetails, String jsonPayload);
}