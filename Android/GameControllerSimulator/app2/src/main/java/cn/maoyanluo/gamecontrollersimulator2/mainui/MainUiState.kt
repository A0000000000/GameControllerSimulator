package cn.maoyanluo.gamecontrollersimulator2.mainui

import android.bluetooth.BluetoothDevice


sealed interface MainUiState {
    data object NoPermissionPage : MainUiState
    data class SelectPage(val data: SelectPageModel) : MainUiState
    data class ConnectingPage(val data: ConnectingPageModel) : MainUiState
    data object GamepadPage : MainUiState
}

data class SelectPageModel(
    val devices: List<BluetoothDevice> = emptyList()
)

data class ConnectStatusModel(
    val isAvailable: Boolean = false,
    val isSelect: Boolean = false,
    val info: String = "",
)

data class ConnectingPageModel(
    val deviceName: String = "",
    val isGATTAvailable: Boolean = false,
    val isAvailable: Boolean = false,
    val rfcommStatus: ConnectStatusModel = ConnectStatusModel(),
    val tcpStatus: ConnectStatusModel = ConnectStatusModel(),
    val udpStatus: ConnectStatusModel = ConnectStatusModel()
)