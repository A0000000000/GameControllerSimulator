package cn.maoyanluo.gamecontrollersimulator2.mainui

import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionType

sealed interface MainUiEffect {

    data class DeviceFlushEffect(val isEnd: Boolean): MainUiEffect
    data class RttResultEffect(val diff: Long, val type: ConnectionType): MainUiEffect

}