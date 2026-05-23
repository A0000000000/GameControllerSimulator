package cn.maoyanluo.gamecontrollersimulator2.constant

import cn.maoyanluo.gamecontrollersimulator2.bean.DeviceInfo
import cn.maoyanluo.gamecontrollersimulator2.bean.FeedbackReceived
import com.google.gson.JsonElement

object EntityType {
    const val TYPE_REQUEST_CLIENT_ID = 1
    const val TYPE_REQUEST_CLIENT_ID_RESULT = 2
    const val TYPE_UNREGISTER_CLIENT_ID = 3
    const val TYPE_UNREGISTER_CLIENT_ID_RESULT = 4
    const val TYPE_SEND_GAME_EVENT = 5
    const val TYPE_SEND_GAME_EVENT_RESULT = 6
    const val TYPE_FEEDBACK_RECEIVED = 7
    const val TYPE_FEEDBACK_RECEIVED_RESULT = 8
    const val TYPE_QUERY_CLIENT_INFO = 9
    const val TYPE_QUERY_CLIENT_INFO_RESULT = 10
    const val TYPE_NEW_TYPE_CONNECT = 11
    const val TYPE_NEW_TYPE_CONNECT_RESULT = 12
    const val TYPE_ECHO = 13
    const val TYPE_ECHO_RESULT = 14
    const val TYPE_RTT = 15
    const val TYPE_RTT_RESULT = 16

    val TYPE_MAPPING = mapOf(
        Pair(TYPE_REQUEST_CLIENT_ID, JsonElement::class.java),
        Pair(TYPE_REQUEST_CLIENT_ID_RESULT, String::class.java),
        Pair(TYPE_UNREGISTER_CLIENT_ID, String::class.java),
        Pair(TYPE_UNREGISTER_CLIENT_ID_RESULT, JsonElement::class.java),
        Pair(TYPE_SEND_GAME_EVENT, String::class.java),
        Pair(TYPE_SEND_GAME_EVENT_RESULT, JsonElement::class.java),
        Pair(TYPE_FEEDBACK_RECEIVED, FeedbackReceived::class.java),
        Pair(TYPE_FEEDBACK_RECEIVED_RESULT, JsonElement::class.java),
        Pair(TYPE_QUERY_CLIENT_INFO, JsonElement::class.java),
        Pair(TYPE_QUERY_CLIENT_INFO_RESULT, DeviceInfo::class.java),
        Pair(TYPE_NEW_TYPE_CONNECT, String::class.java),
        Pair(TYPE_NEW_TYPE_CONNECT_RESULT, String::class.java),
        Pair(TYPE_ECHO, String::class.java),
        Pair(TYPE_ECHO_RESULT, String::class.java),
        Pair(TYPE_RTT, String::class.java),
        Pair(TYPE_RTT_RESULT, String::class.java)
    )

}