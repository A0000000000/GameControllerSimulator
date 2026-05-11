package cn.maoyanluo.gamecontrollersimulator2.bean

import com.google.gson.annotations.SerializedName


data class BaseEntity<T>(
    @SerializedName("type")
    val type: Int,
    @SerializedName("id")
    val id: Int,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("method")
    val method: String,
    @SerializedName("data")
    val data: T
) {

    companion object {
        const val TYPE_REQ = 1
        const val TYPE_QUERY = 2
        const val TYPE_EVENT = 3
        const val METHOD_BLE = "BLE"
        const val METHOD_TCP = "TCP"
        const val METHOD_UDP = "UDP"
    }

}
