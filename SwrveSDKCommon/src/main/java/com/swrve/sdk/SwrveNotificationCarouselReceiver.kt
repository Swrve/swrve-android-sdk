package com.swrve.sdk

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors

open class SwrveNotificationCarouselReceiver : BroadcastReceiver() {

    // Rebuilding reads and decodes the cached item image and runs the customer's filter, so it runs off the
    // main thread and goAsync keeps the receiver alive until it's done
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult: PendingResult? = goAsync()
        getExecutor().execute {
            try {
                showCarouselItem(context, intent)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun showCarouselItem(context: Context, intent: Intent) {
        try {
            val extras = intent.extras ?: return
            val msg = extras.getBundle(SwrveNotificationConstants.PUSH_BUNDLE) ?: return
            val notificationId = extras.getInt(SwrveNotificationConstants.PUSH_NOTIFICATION_ID)
            val itemIndex = extras.getInt(SwrveNotificationConstants.CAROUSEL_ITEM_INDEX_KEY)
            val campaignType = extras.getString(SwrveNotificationConstants.CAMPAIGN_TYPE)
            val eventPayload = extras.getBundle(SwrveNotificationConstants.EVENT_PAYLOAD)
            val gifUris = extras.getStringArrayList(NotificationMediaManager.EXTRA_GIF_URIS) ?: arrayListOf()
            // Carried from the first post rather than worked out again, as the image cache may have changed since
            val imageHeightRatio = extras.getFloat(SwrveNotificationConstants.CAROUSEL_IMAGE_HEIGHT_RATIO_KEY)
            // The first item was reported when the notification was posted, so it is already in the sent set.
            // A quick second tap arrives from the previous update's intents, so also check what this process has already sent
            val deliveryId = getDeliveryId(msg)
            val recordedSlideViews = sentSlideViewsInProcess[notificationId]?.takeIf { it.deliveryId == deliveryId }?.slideViews
            val sentSlideViews = (extras.getIntegerArrayList(SwrveNotificationConstants.CAROUSEL_SENT_SLIDE_VIEWS_KEY).orEmpty() +
                    recordedSlideViews.orEmpty()).distinct()
            val isNewSlideView = itemIndex !in sentSlideViews
            val updatedSlideViews = ArrayList(if (isNewSlideView) sentSlideViews + itemIndex else sentSlideViews)

            SwrveCommon.checkInstanceCreated()
            val notificationConfig = SwrveCommon.getInstance().notificationConfig
            val builder = getSwrveNotificationBuilder(context, notificationConfig)
            val compatBuilder = builder.buildCarouselItem(msg, campaignType, eventPayload, notificationId, itemIndex, gifUris, updatedSlideViews, imageHeightRatio)
            val notification = applyCustomFilter(compatBuilder, notificationId, msg, builder, notificationConfig)
            if (notification == null) {
                SwrveLogger.d("SwrveNotificationCarouselReceiver: notification suppressed via custom filter. notificationId: %s", notificationId)
                return
            }
            addAuthenticatedUser(notification, msg)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val activeNotificationIds = notificationManager.activeNotifications.map { it.id }.toSet()
            sentSlideViewsInProcess.keys.retainAll(activeNotificationIds)
            if (notificationId !in activeNotificationIds) {
                SwrveLogger.d("SwrveNotificationCarouselReceiver: notificationId %s no longer showing, not reposting.", notificationId)
                return
            }
            notificationManager.notify(notificationId, notification)
            sentSlideViewsInProcess[notificationId] = SentSlideViews(deliveryId, updatedSlideViews)
            if (isNewSlideView) {
                EventHelper.sendCarouselViewEvent(context, msg, itemIndex, builder.carouselItemUrl)
            }
            SwrveLogger.d("SwrveNotificationCarouselReceiver: showing carousel item %s for notificationId: %s", itemIndex, notificationId)
        } catch (e: Exception) {
            SwrveLogger.e("SwrveNotificationCarouselReceiver exception navigating carousel.", e)
        }
    }

    protected open fun getExecutor(): Executor = executor

    protected open fun getSwrveNotificationBuilder(context: Context, notificationConfig: SwrveNotificationConfig): SwrveNotificationBuilder {
        return SwrveNotificationBuilder(context, notificationConfig)
    }

    private fun applyCustomFilter(
        compatBuilder: NotificationCompat.Builder,
        notificationId: Int,
        msg: Bundle,
        builder: SwrveNotificationBuilder,
        notificationConfig: SwrveNotificationConfig
    ): Notification? {
        val filter = notificationConfig.notificationFilter ?: return compatBuilder.build()
        return try {
            filter.filterNotification(compatBuilder, notificationId, builder.notificationDetails, SwrvePushManagerHelper.getPayload(msg))
        } catch (e: Exception) {
            SwrveLogger.e("Error calling the custom notification filter.", e)
            compatBuilder.build()
        }
    }

    private fun addAuthenticatedUser(notification: Notification, msg: Bundle) {
        val authUser = msg.getString(SwrveNotificationInternalPayloadConstants.SWRVE_AUTH_USER_KEY)
        if (SwrveHelper.isNotNullOrEmpty(authUser)) {
            val extras = notification.extras ?: Bundle()
            extras.putString(SwrveNotificationInternalPayloadConstants.SWRVE_AUTH_USER_KEY, authUser)
            notification.extras = extras
        }
    }

    private fun getDeliveryId(msg: Bundle): String? {
        val sid = msg.getString(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY)
        return if (SwrveHelper.isNotNullOrEmpty(sid)) sid else msg.getString(ISwrveCommon.GENERIC_EVENT_PAYLOAD_MSG_ID)
    }

    private class SentSlideViews(val deliveryId: String?, val slideViews: List<Int>)

    companion object {
        private val executor: Executor = Executors.newSingleThreadExecutor()
        private val sentSlideViewsInProcess = ConcurrentHashMap<Int, SentSlideViews>()
    }
}
