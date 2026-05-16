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

class MainViewModel(application: Application): AndroidViewModel(application) {

    var mainUiState by mutableStateOf<MainUiState>(MainUiState.NoPermissionPage)
    private val bluetoothManagerWrapper = BluetoothManagerWrapper(application)
    private val coroutineManager = CoroutineManager()
    private var connectionManager: ConnectionManager? = null
    var isAvailable by mutableStateOf(false)

    private val connectionCallback = object: ConnectionCallback {
        override fun onManagerAvailable() {
            isAvailable = true
        }

        override fun onManagerUnavailable() {
            isAvailable = false
            val currentState = mainUiState
            if (currentState is MainUiState.GamepadPage) {
                mainUiState = MainUiState.ConnectingPage(currentState.device)
            }
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
                Toast.makeText(application, "onSendDataException e: ${e.message}, id = $id", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onDataRevException(
            e: Exception,
            connectionType: ConnectionManager.ConnectionType
        ) {
            coroutineManager.getMainScope().launch {
                Toast.makeText(application, "onDataRevException e: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    }

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
        connectionManager?.destroy()
        connectionManager = null
    }

    fun getBoundDevices() = bluetoothManagerWrapper.getBondedDevice()

    fun initConnectionManager(device: BluetoothDevice) {
        connectionManager = ConnectionManager(device, bluetoothManagerWrapper.getAdapter(), connectionCallback, coroutineManager)
        connectionManager?.init()
    }



}