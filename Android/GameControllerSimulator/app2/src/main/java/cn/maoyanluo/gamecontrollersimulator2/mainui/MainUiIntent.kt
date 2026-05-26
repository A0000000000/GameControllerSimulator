package cn.maoyanluo.gamecontrollersimulator2.mainui

import android.bluetooth.BluetoothDevice
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionType

sealed interface MainUiIntent {

    data class PermissionResultIntent(val grant: Boolean): MainUiIntent
    data class OnDeviceSelectedIntent(val device: BluetoothDevice): MainUiIntent
    object OnDeviceListFlush: MainUiIntent
    object OnEnterGamepadIntent: MainUiIntent
    data class OnSelectConnectTypeIntent(val type: ConnectionType): MainUiIntent
    data class OnRequestRttIntent(val type: ConnectionType): MainUiIntent

    object OnBackFromGamepad: MainUiIntent

    data class OnGamepadEvent(val data: ByteArray): MainUiIntent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as OnGamepadEvent

            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }

    object OnDisconnect: MainUiIntent

}