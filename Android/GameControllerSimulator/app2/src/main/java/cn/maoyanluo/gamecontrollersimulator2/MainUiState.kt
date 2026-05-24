package cn.maoyanluo.gamecontrollersimulator2

import android.bluetooth.BluetoothDevice

data class ConnectStatus(
    val isAvailable: Boolean = false,
    val isSelect: Boolean = false,
    val info: String = "",
)

sealed interface MainUiState {
    data object NoPermissionPage : MainUiState
    data object SelectPage : MainUiState
    data class ConnectingPage(
        val device: BluetoothDevice,
        val isGATTAvailable: Boolean = false,
        val isAvailable: Boolean = false,
        val rfcommStatus: ConnectStatus = ConnectStatus(),
        val tcpStatus: ConnectStatus = ConnectStatus(),
        val udpStatus: ConnectStatus = ConnectStatus()
    ) : MainUiState
    data object GamepadPage : MainUiState
}
