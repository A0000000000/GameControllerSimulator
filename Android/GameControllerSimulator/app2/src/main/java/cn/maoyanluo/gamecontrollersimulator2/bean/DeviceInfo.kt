package cn.maoyanluo.gamecontrollersimulator2.bean

import android.os.Build
import com.google.gson.annotations.SerializedName

class DeviceInfo {
    @SerializedName("os_version")
    val osVersion: String = "Android ${Build.VERSION.RELEASE}"

    @SerializedName("sdk")
    val sdk: String = "${Build.VERSION.SDK_INT}"

    @SerializedName("brand")
    val brand: String = "${Build.BRAND}"

    @SerializedName("manufacturer")
    val manufacturer: String = "${Build.MANUFACTURER}"

    @SerializedName("model")
    val model: String = "${Build.MODEL}"

    @SerializedName("device")
    val device: String = "${Build.DEVICE}"

    @SerializedName("product")
    val product: String = "${Build.PRODUCT}"

    @SerializedName("board")
    val board: String = "${Build.BOARD}"

    @SerializedName("hardware")
    val hardware: String = "${Build.HARDWARE}"

    @SerializedName("codename")
    val codename: String = "${Build.VERSION.CODENAME}"

    @SerializedName("build_id")
    val buildId: String = "${Build.ID}"

    @SerializedName("fingerprint")
    val fingerprint: String = "${Build.FINGERPRINT}"

    @SerializedName("supported_abis")
    val supportedAbis: String = Build.SUPPORTED_ABIS.joinToString()

}
