package cn.maoyanluo.gamecontrollersimulator2

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import cn.maoyanluo.coroutine_library.CoroutineManager

class MainViewModel(application: Application): AndroidViewModel(application) {

    var mainUiState by mutableStateOf<MainUiState>(MainUiState.NoPermissionPage)
    val coroutineManager = CoroutineManager()

    init {
        coroutineManager.init()
        addCloseable {
            coroutineManager.destroy()
        }
    }

    fun setPermission(grantAll: Boolean) {
        mainUiState = if (grantAll) {
            MainUiState.SelectPage
        } else {
            MainUiState.NoPermissionPage
        }
    }

    fun setCurrentDevice(device: BluetoothDevice) {
        mainUiState = MainUiState.ConnectingPage(device)
    }

    fun openGamepadPage() {
        val currentState = mainUiState
        if (currentState is MainUiState.ConnectingPage) {
            mainUiState = MainUiState.GamepadPage(currentState.device)
        }
    }

    fun exitGamepadPage() {
        val currentState = mainUiState
        if (currentState is MainUiState.GamepadPage) {
            mainUiState = MainUiState.ConnectingPage(currentState.device)
        }
    }

    fun disconnect() {
        mainUiState = MainUiState.SelectPage
    }

}