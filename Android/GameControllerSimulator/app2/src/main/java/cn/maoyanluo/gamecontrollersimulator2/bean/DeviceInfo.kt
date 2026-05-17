package cn.maoyanluo.gamecontrollersimulator2.bean

import com.google.gson.annotations.SerializedName

data class DeviceInfo(
    @SerializedName("os_version")
    val osVersion: String,
    @SerializedName("sdk")
    val sdk: String,
    @SerializedName("brand")
    val brand: String,
    @SerializedName("manufacturer")
    val manufacturer: String,
    @SerializedName("model")
    val model: String,
    @SerializedName("device")
    val device: String,
    @SerializedName("product")
    val product: String,
    @SerializedName("board")
    val board: String,
    @SerializedName("hardware")
    val hardware: String,
    @SerializedName("codename")
    val codename: String,
    @SerializedName("build_id")
    val buildId: String,
    @SerializedName("fingerprint")
    val fingerprint: String,
    @SerializedName("supported_abis")
    val supportedAbis: String
)
