package cn.maoyanluo.gamecontrollersimulator2

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import cn.maoyanluo.bluetooth_library.BluetoothManagerWrapper
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionCallback
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionManager
import com.google.gson.JsonElement
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var mainUiState by mutableStateOf<MainUiState>(MainUiState.NoPermissionPage)
        private set

    private val bluetoothManagerWrapper = BluetoothManagerWrapper(application)
    private val coroutineManager = CoroutineManager()
    private var connectionManager: ConnectionManager? = null

    private val connectionCallback = object : ConnectionCallback {
        override fun onManagerAvailable() {
            updateConnectionAvailability(true)
        }

        override fun onManagerUnavailable() {
            updateConnectionAvailability(false)
        }

        override fun getTypeClass(type: Int): Class<*> {
            return JsonElement::class.java
        }

        override fun onDataReady(data: BaseEntity<*>?) {
        }

        override fun onSendDataException(
            e: Exception,
            id: Int,
            connectionType: ConnectionManager.ConnectionType
        ) {
            coroutineManager.getMainScope().launch {
                Toast.makeText(
                    application,
                    "onSendDataException e: ${e.message}, id = $id",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onDataRevException(
            e: Exception,
            connectionType: ConnectionManager.ConnectionType
        ) {
            coroutineManager.getMainScope().launch {
                Toast.makeText(
                    application,
                    "onDataRevException e: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    init {
        coroutineManager.init()
        addCloseable {
            clearConnection()
            coroutineManager.destroy()
        }
    }

    fun setPermission(grantAll: Boolean) {
        mainUiState = if (grantAll) {
            when (mainUiState) {
                MainUiState.NoPermissionPage -> MainUiState.SelectPage
                else -> mainUiState
            }
        } else {
            clearConnection()
            MainUiState.NoPermissionPage
        }
    }

    fun onDeviceSelected(device: BluetoothDevice) {
        clearConnection()
        mainUiState = MainUiState.ConnectingPage(
            device = device,
            isAvailable = false
        )
        connectionManager = ConnectionManager(
            device,
            bluetoothManagerWrapper.getAdapter(),
            connectionCallback,
            coroutineManager
        ).also { it.init() }
    }

    fun onEnterGamepad() {
        val currentState = mainUiState
        if (currentState is MainUiState.ConnectingPage && currentState.isAvailable) {
            mainUiState = MainUiState.GamepadPage(currentState.device)
        }
    }

    fun onBackFromGamepad() {
        val currentState = mainUiState
        if (currentState is MainUiState.GamepadPage) {
            mainUiState = MainUiState.ConnectingPage(
                device = currentState.device,
                isAvailable = connectionManager?.isAvailable == true
            )
        }
    }

    fun disconnect() {
        clearConnection()
        mainUiState = MainUiState.SelectPage
    }

    fun getBoundDevices() = bluetoothManagerWrapper.getBondedDevice()

    private fun updateConnectionAvailability(isAvailable: Boolean) {
        when (val currentState = mainUiState) {
            is MainUiState.ConnectingPage -> {
                mainUiState = currentState.copy(isAvailable = isAvailable)
            }
            is MainUiState.GamepadPage -> {
                if (!isAvailable) {
                    mainUiState = MainUiState.ConnectingPage(
                        device = currentState.device,
                        isAvailable = false
                    )
                }
            }
            else -> Unit
        }
    }

    private fun clearConnection() {
        connectionManager?.destroy()
        connectionManager = null
    }
}
