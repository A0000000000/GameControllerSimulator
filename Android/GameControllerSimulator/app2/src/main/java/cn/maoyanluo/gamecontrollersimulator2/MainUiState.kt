package cn.maoyanluo.gamecontrollersimulator2

import android.bluetooth.BluetoothDevice

sealed interface MainUiState {
    data object NoPermissionPage : MainUiState
    data object SelectPage : MainUiState
    data class ConnectingPage(
        val device: BluetoothDevice,
        val isGATTAvailable: Boolean = false,
        val isAvailable: Boolean = false,
        val isRFCOMMAvailable: Boolean = false,
        val isTcpAvailable: Boolean = false,
        val isUdpAvailable: Boolean = false,
        val rfcommUuid: String = "",
        val tcpInfo: String = "",
        val udpInfo: String = ""
    ) : MainUiState
    data object GamepadPage : MainUiState
}
