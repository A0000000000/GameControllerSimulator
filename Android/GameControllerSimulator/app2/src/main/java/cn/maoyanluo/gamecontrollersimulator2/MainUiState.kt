package cn.maoyanluo.gamecontrollersimulator2

import android.bluetooth.BluetoothDevice

sealed interface MainUiState {
    data object NoPermissionPage: MainUiState
    data object SelectPage: MainUiState
    data class ConnectingPage(val device: BluetoothDevice): MainUiState
    data class GamepadPage(val device: BluetoothDevice): MainUiState

}