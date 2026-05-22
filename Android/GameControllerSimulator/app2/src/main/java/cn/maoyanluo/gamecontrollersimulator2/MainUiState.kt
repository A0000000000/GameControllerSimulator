package cn.maoyanluo.gamecontrollersimulator2

import android.bluetooth.BluetoothDevice

sealed interface MainUiState {
    data object NoPermissionPage : MainUiState
    data object SelectPage : MainUiState
    data class ConnectingPage(
        val device: BluetoothDevice,
        val isGATTAvailable: Boolean = false,
        val isRFCOMMAvailable: Boolean = false
    ) : MainUiState
    data object GamepadPage : MainUiState
}
