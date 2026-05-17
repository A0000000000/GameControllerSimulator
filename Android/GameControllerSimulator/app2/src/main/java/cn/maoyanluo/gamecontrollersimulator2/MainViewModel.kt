package cn.maoyanluo.gamecontrollersimulator2

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import cn.maoyanluo.bluetooth_library.BluetoothManagerWrapper
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.bean.DeviceInfo
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionCallback
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionManager
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityId
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityType
import com.google.gson.JsonElement
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val deviceInfo = DeviceInfo(
        osVersion = "Android ${Build.VERSION.RELEASE}",
        sdk = "${Build.VERSION.SDK_INT}",
        brand = "${Build.BRAND}",
        manufacturer = "${Build.MANUFACTURER}",
        model = "${Build.MODEL}",
        device = "${Build.DEVICE}",
        product = "${Build.PRODUCT}",
        board = "${Build.BOARD}",
        hardware = "${Build.HARDWARE}",
        codename = "${Build.VERSION.CODENAME}",
        buildId = "${Build.ID}",
        fingerprint = "${Build.FINGERPRINT}",
        supportedAbis = Build.SUPPORTED_ABIS.joinToString(),
    )

    var mainUiState by mutableStateOf<MainUiState>(MainUiState.NoPermissionPage)
        private set

    private val bluetoothManagerWrapper = BluetoothManagerWrapper(application)
    val coroutineManager = CoroutineManager()
    private var connectionManager: ConnectionManager? = null

    private val connectionCallback = object : ConnectionCallback {
        override fun onManagerAvailable() {
            updateConnectionAvailability(true)
        }

        override fun onManagerUnavailable() {
            updateConnectionAvailability(false)
        }

        override fun getTypeClass(type: Int): Class<*> {
            return EntityType.TYPE_MAPPING[type] ?: JsonElement::class.java
        }

        override fun onDataReady(data: BaseEntity<*>?) {
            if (data != null) {
                when (data.type) {
                    EntityType.TYPE_QUERY_CLIENT_INFO -> {
                        connectionManager?.sendData(BaseEntity(
                            type = EntityType.TYPE_QUERY_CLIENT_INFO_RESULT,
                            id = EntityId.GAMEPAD_PAGE_EVENT,
                            timestamp = System.currentTimeMillis(),
                            data = deviceInfo
                        ))
                    }

                    EntityType.TYPE_FEEDBACK_RECEIVED -> {

                    }


                }
            }
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

    fun reInitConnectionManager() {
        connectionManager?.init()
    }

    fun onEnterGamepad() {
        val currentState = mainUiState
        val cm = connectionManager
        if (currentState is MainUiState.ConnectingPage && currentState.isAvailable && cm != null) {
            mainUiState = MainUiState.GamepadPage
        }
        if (cm == null) {
            mainUiState = MainUiState.SelectPage
        }
    }

    fun onBackFromGamepad() {
        val currentState = mainUiState
        val cm = connectionManager
        if (currentState is MainUiState.GamepadPage && cm != null) {
            mainUiState = MainUiState.ConnectingPage(
                device = cm.device,
                isAvailable = connectionManager?.isAvailable == true
            )
        }
        if (cm == null) {
            mainUiState = MainUiState.SelectPage
        }
    }

    fun onGamepadEvent(data: ByteArray) {
        connectionManager?.sendData(
            BaseEntity(
                type = EntityType.TYPE_SEND_GAME_EVENT,
                id = EntityId.GAMEPAD_PAGE_EVENT,
                timestamp = System.currentTimeMillis(),
                data = data
            )
        )
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
                val cm = connectionManager
                if (!isAvailable && cm != null) {
                    mainUiState = MainUiState.ConnectingPage(
                        device = cm.device,
                        isAvailable = false
                    )
                }
                if (cm == null) {
                    mainUiState = MainUiState.SelectPage
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
