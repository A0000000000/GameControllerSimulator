package cn.maoyanluo.gamecontrollersimulator2.bean

import com.google.gson.annotations.SerializedName

data class FeedbackReceived(
    @SerializedName("large_motor")
    val largeMotor: Int,
    @SerializedName("small_motor")
    val smallMotor: Int,
    @SerializedName("led_number")
    val ledNumber: Int
)
