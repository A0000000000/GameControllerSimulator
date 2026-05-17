package cn.maoyanluo.gamecontrollersimulator2.bean

import com.google.gson.annotations.SerializedName


data class BaseEntity<T>(
    @SerializedName("type")
    val type: Int,
    @SerializedName("id")
    val id: Int,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("data")
    val data: T?
)
