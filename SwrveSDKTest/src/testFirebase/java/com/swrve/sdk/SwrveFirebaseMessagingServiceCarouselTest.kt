package com.swrve.sdk

import android.os.Bundle
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.Robolectric

class SwrveFirebaseMessagingServiceCarouselTest : SwrveBaseTest() {

    private fun newService(): Pair<SwrveFirebaseMessagingService, SwrvePushManagerImp> {
        val serviceSpy = Mockito.spy(Robolectric.setupService(SwrveFirebaseMessagingService::class.java))
        val pushManager = Mockito.mock(SwrvePushManagerImp::class.java)
        doReturn(pushManager).`when`(serviceSpy).swrvePushManager
        return Pair(serviceSpy, pushManager)
    }

    @Test
    fun testCarouselPushIsHandledByPushWorker() {
        val (serviceSpy, pushManager) = newService()
        val workerHelper = Mockito.mock(SwrvePushWorkerHelper::class.java)
        @Suppress("UNCHECKED_CAST")
        val dataCaptor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, String>>
        doReturn(workerHelper).`when`(serviceSpy).getSwrvePushWorkerHelper(dataCaptor.capture())

        val carouselPush = RemoteMessage.Builder("dest")
            .addData(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "3")
            .addData(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, "3")
            .addData(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, """{"version":1,"carousel":[{"image_url":"https://cdn.example.com/0.png"}]}""")
            .setMessageId("456")
            .build()
        serviceSpy.onMessageReceived(carouselPush)

        verify(workerHelper).handle()
        verify(pushManager, never()).processMessage(Mockito.any(Bundle::class.java))
        assertEquals("3", dataCaptor.value["_p"])
        assertEquals("456", dataCaptor.value["provider.message_id"])
        assertEquals("0", dataCaptor.value["provider.priority"])
    }

    @Test
    fun testRichPushIsStillProcessedInline() {
        val (serviceSpy, pushManager) = newService()

        val richPush = RemoteMessage.Builder("dest")
            .addData(SwrveNotificationConstants.SWRVE_TRACKING_KEY, "4")
            .addData(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, """{"version":1,"media":{"type":"image","url":"https://cdn.example.com/m.png"}}""")
            .build()
        serviceSpy.onMessageReceived(richPush)

        verify(pushManager).processMessage(Mockito.any(Bundle::class.java))
        verify(serviceSpy, never()).getSwrvePushWorkerHelper(Mockito.any())
    }

    private fun carouselPush(sid: String) = RemoteMessage.Builder("dest")
        .addData(SwrveNotificationConstants.SWRVE_TRACKING_KEY, sid)
        .addData(SwrveNotificationConstants.SWRVE_UNIQUE_MESSAGE_ID_KEY, sid)
        .addData(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY, """{"version":1,"carousel":[{"image_url":"https://cdn.example.com/0.png"}]}""")
        .build()

    @Test
    fun testCarouselPushIsProcessedInlineWhenWorkerCannotBeQueued() {
        val (serviceSpy, pushManager) = newService()
        val workerHelper = Mockito.mock(SwrvePushWorkerHelper::class.java)
        doReturn(false).`when`(workerHelper).handle()
        doReturn(true).`when`(workerHelper).didFailToQueue()
        doReturn(workerHelper).`when`(serviceSpy).getSwrvePushWorkerHelper(Mockito.any())

        serviceSpy.onMessageReceived(carouselPush("5"))

        verify(pushManager).processMessage(Mockito.any(Bundle::class.java))
    }

    @Test
    fun testDuplicateCarouselPushIsNotProcessedInline() {
        val (serviceSpy, pushManager) = newService()
        val workerHelper = Mockito.mock(SwrvePushWorkerHelper::class.java)
        doReturn(false).`when`(workerHelper).handle()
        doReturn(false).`when`(workerHelper).didFailToQueue()
        doReturn(workerHelper).`when`(serviceSpy).getSwrvePushWorkerHelper(Mockito.any())

        serviceSpy.onMessageReceived(carouselPush("6"))

        verify(pushManager, never()).processMessage(Mockito.any(Bundle::class.java))
    }
}
