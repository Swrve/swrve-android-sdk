package com.swrve.sdk

import android.content.Context
import com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_PIM_ENGAGED
import com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_PIM_READ
import com.swrve.sdk.SwrvePushInboxListenerResult.ResultCode.ERROR
import com.swrve.sdk.SwrvePushInboxListenerResult.ResultCode.ERROR_UNKNOWN
import com.swrve.sdk.SwrvePushInboxListenerResult.ResultCode.SUCCESS
import com.swrve.sdk.rest.IRESTClient
import com.swrve.sdk.rest.IRESTResponseListener
import com.swrve.sdk.rest.RESTResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.Date

class SwrvePushInboxManager(val context: Context,
                            var restClient: IRESTClient,
                            val apiKey: String,
                            val userId: String,
                            val contentUrl : String) {
    companion object {
        const val STATE_UPDATE_API = "/api/1/push_inbox_update"
        const val STATE_UPDATE_PARAM_READ = "R"
        const val STATE_UPDATE_PARAM_DELETE = "D"
        const val REST_MAX_ATTEMPTS = 3;
    }

    private var messages: List<SwrvePushInboxMessage> = Collections.synchronizedList(ArrayList())

    fun getMessages(): List<SwrvePushInboxMessage> {
        return messages
    }

    fun loadMessages(pimJsonArray: JSONArray) {
        SwrveLogger.i("SwrveSDK: Push Inbox Messages JSON array: %s", pimJsonArray)
        val newMessages = ArrayList<SwrvePushInboxMessage>()
        for (i in 0 until pimJsonArray.length()) {
            try {
                val pushInbox = pimJsonArray.getJSONObject(i)
                newMessages.add(SwrvePushInboxMessage(pushInbox))
            } catch (ex: Exception) {
                SwrveLogger.e("SwrveSDK: Error parsing push inbox message JSON", ex)
                continue
            }
        }
        messages = newMessages
    }

    fun getFilteredMessages(): List<SwrvePushInboxMessage> {
        val result: MutableList<SwrvePushInboxMessage> = java.util.ArrayList()
        if (messages.isEmpty()) {
            return result
        }

        synchronized(messages) {
            val now = Date()
            for (message in messages) {
                val messageEndDate = Date(message.endDate)
                if (messageEndDate.before(now)) {
                    SwrveLogger.i("SwrveSDK: Push Inbox Message %s end date has expired.", message.messageId)
                    continue
                }
                result.add(message)
            }
        }

        return result
    }

    fun readMessage(messageId: Long, listener: SwrvePushInboxListener?) {
        val body = getStateUpdateBody(messageId, STATE_UPDATE_PARAM_READ)
        val callback = object : IRESTResponseListener {
            override fun onResponse(response: RESTResponse) {
                val result: SwrvePushInboxListenerResult
                if (SwrveHelper.successResponseCode(response.responseCode)) {
                    updateSuccess(messageId, SwrvePushInboxMessageState.READ, response.responseBody)
                    result = SwrvePushInboxListenerResult(SUCCESS, "", response.responseCode)
                } else {
                    val errorMessage = "Push Inbox Message $messageId failed to mark as read. Server response code:${response.responseCode}"
                    result = SwrvePushInboxListenerResult(ERROR, errorMessage, response.responseCode)
                }
                listener?.onComplete(messageId, result)
            }

            override fun onException(e: Exception) {
                SwrveLogger.e("SwrveSDK: Error marking Push Inbox Message %s as read.", messageId, e)
                if (listener != null) {
                    val errorMessage = "Push Inbox Message error marking as Read:${e.message}"
                    val result = SwrvePushInboxListenerResult(ERROR_UNKNOWN, errorMessage, 0)
                    listener.onComplete(messageId, result)
                }
            }
        }

        executePushInboxRestRequest(body, callback)
    }

    fun engageMessage(messageId: Long, listener: SwrvePushInboxListener) {
        val message: SwrvePushInboxMessage? = getPushInboxMessage(messageId)
        if (message != null) {
            sendEvent(GENERIC_EVENT_ACTION_TYPE_PIM_ENGAGED, message.variantId, messageId, message.state, message.trackingData)
        }
        readMessage(messageId, listener)
    }

    fun deleteMessage(messageId: Long, listener: SwrvePushInboxListener?) {
        val body = getStateUpdateBody(messageId, STATE_UPDATE_PARAM_DELETE)
        val callback = object : IRESTResponseListener {
            override fun onResponse(response: RESTResponse) {
                val result: SwrvePushInboxListenerResult
                if (SwrveHelper.successResponseCode(response.responseCode)) {
                    updateSuccess(messageId, SwrvePushInboxMessageState.DELETED, response.responseBody)
                    result = SwrvePushInboxListenerResult(SUCCESS, "", response.responseCode)
                } else {
                    val errorMessage = "Push Inbox Message $messageId failed to delete. Server response code:${response.responseCode}"
                    result = SwrvePushInboxListenerResult(ERROR, errorMessage, response.responseCode)
                }
                listener?.onComplete(messageId, result)
            }

            override fun onException(e: Exception) {
                SwrveLogger.e("SwrveSDK: Error deleting Push Inbox Message %s.", messageId, e)
                if (listener != null) {
                    val errorMessage = "Push Inbox Message error deleting:${e.message}"
                    val result = SwrvePushInboxListenerResult(ERROR_UNKNOWN, errorMessage, 0)
                    listener.onComplete(messageId, result)
                }
            }
        }

        executePushInboxRestRequest(body, callback)
    }

    fun getPushInboxMessage(messageId: Long): SwrvePushInboxMessage? {
        synchronized(messages) {
            return messages.firstOrNull { it.messageId == messageId }
        }
    }

    private fun updateSuccess(messageId: Long, state: SwrvePushInboxMessageState, responseBody: String?) {
        SwrveLogger.v("SwrveSDK: Push Inbox Message %s marked as %s.", messageId, state)
        val message: SwrvePushInboxMessage = getPushInboxMessage(messageId) ?: return

        // Send event with current state (before updating local state) if the response body contains json {"state": "modified"}
        if (responseBody != null) {
            val jsonResponse = JSONObject(responseBody)
            if (jsonResponse.optString("state") == "modified") {
                if (state == SwrvePushInboxMessageState.READ) {
                    sendEvent(GENERIC_EVENT_ACTION_TYPE_PIM_READ, message.variantId, messageId, message.state, message.trackingData)
                } else if (state == SwrvePushInboxMessageState.DELETED) {
                    sendEvent(ISwrveCommon.GENERIC_EVENT_ACTION_TYPE_PIM_DELETE, message.variantId, messageId, message.state, message.trackingData)
                }
            }
        }

        // Update the local state of the message or delete
        if (state == SwrvePushInboxMessageState.READ) {
            message.state = SwrvePushInboxMessageState.READ
        } else if (state == SwrvePushInboxMessageState.DELETED) {
            synchronized(messages) {
                messages = messages.toMutableList().apply { remove(message) }
            }
        }
    }

    private fun sendEvent(actionType: String, variantId: Long, messageId: Long, state: SwrvePushInboxMessageState, trackingData: String) {
        val time = System.currentTimeMillis()
        val id = variantId.toString()
        val campaignType = ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PIM
        val campaignId = ""
        val payload: MutableMap<String, Any?> = HashMap()
        payload[ISwrveCommon.GENERIC_EVENT_PAYLOAD_PIM_MESSAGE_ID] = messageId.toString()
        if (actionType != GENERIC_EVENT_ACTION_TYPE_PIM_READ) {
            val stateString = if (state == SwrvePushInboxMessageState.READ) "read" else "unread"
            payload[ISwrveCommon.GENERIC_EVENT_PAYLOAD_PIM_STATE] = stateString
        }
        if (trackingData.isNotEmpty()) {
            payload[ISwrveCommon.GENERIC_EVENT_PAYLOAD_TRACKING_DATA] = trackingData
        }
        val swrveCommon = SwrveCommon.getInstance()
        val seqNum: Int = swrveCommon.getNextSequenceNumber()
        val events = EventHelper.createGenericEvent(time, id, campaignType, actionType, null, campaignId, payload, seqNum)
        swrveCommon.sendEventsInBackground(context, swrveCommon.getUserId(), events)
    }

    private fun getStateUpdateBody(messageId: Long, state: String): String {
        val params: MutableMap<String, String> = java.util.HashMap()
        params["api_key"] = apiKey
        params["user"] = userId
        params["message_id"] = messageId.toString()
        params["state"] = state
        return SwrveHelper.encodeParameters(params)
    }

    private fun executePushInboxRestRequest(body: String, listener: IRESTResponseListener) {

        CoroutineScope(Dispatchers.IO).launch {

            var attempt = 1;

            val retryWrapperCallback = object : IRESTResponseListener {
                override fun onResponse(response: RESTResponse) {
                    if (SwrveHelper.successResponseCode(response.responseCode)
                        || attempt++ >= REST_MAX_ATTEMPTS ) {
                            SwrveLogger.v("SwrveSDK: Push Inbox REST Result ${response.responseCode} on attempt ${attempt-1}")
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    listener.onResponse(response);
                                } catch (e: Exception) {
                                    SwrveLogger.e("SwrveSDK: Listener Response Exception.", e)
                                }
                            }
                    } else {
                        SwrveLogger.v("SwrveSDK: Retrying Push Inbox REST error ${response.responseCode} on attempt ${attempt-1}")
                        restClient.post(getUpdateEndpoint(), body, this, "application/x-www-form-urlencoded")
                    }
                }

                override fun onException(e: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            listener.onException(e);
                        } catch (e: Exception) {
                            SwrveLogger.e("Listener Exception Handling error.", e)
                        }
                    }
                }
            }

            restClient.post(getUpdateEndpoint(), body, retryWrapperCallback, "application/x-www-form-urlencoded")
        }
    }

    private fun getUpdateEndpoint() : String {
        return contentUrl + STATE_UPDATE_API
    }
}