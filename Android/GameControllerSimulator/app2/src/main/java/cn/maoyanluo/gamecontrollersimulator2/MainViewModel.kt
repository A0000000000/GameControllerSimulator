package cn.maoyanluo.gamecontrollersimulator2

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.os.Build
import android.util.Base64
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import cn.maoyanluo.bluetooth_library.BluetoothManagerWrapper
import cn.maoyanluo.bluetooth_library.gatt.BluetoothGATTManager
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.bean.DeviceInfo
import cn.maoyanluo.gamecontrollersimulator2.bean.FeedbackReceived
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionCallback
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionManager
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityId
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityType
import cn.maoyanluo.gamecontrollersimulator2.constant.UUIDConstant
import cn.maoyanluo.log_library.LogUtils
import com.google.gson.JsonElement
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(private val application: Application) : AndroidViewModel(application) {

    companion object {
        const val TAG = "MainViewModel"
    }

    var mainUiState by mutableStateOf<MainUiState>(MainUiState.NoPermissionPage)
        private set
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
    val coroutineManager = CoroutineManager()
    private val bluetoothManagerWrapper = BluetoothManagerWrapper(application)

    private var rfcommUuid: String = ""
    private var tcpInfo: String = ""
    private var udpInfo: String = ""

    private var bluetoothGATTManager: BluetoothGATTManager? = null
    private val bluetoothGATTManagerCallback = object : BluetoothGATTManager.BluetoothGATTManagerCallback {
        override fun onAvailable(device: BluetoothDevice) {
            bluetoothGATTManager?.readCharacteristic(UUIDConstant.GATT_FUN_UUID, UUIDConstant.GATT_DATA_RFCOMM_UUID)
            bluetoothGATTManager?.readCharacteristic(UUIDConstant.GATT_FUN_UUID, UUIDConstant.TCP_INFO_UUID)
            mainUiState = MainUiState.ConnectingPage(
                device = device,
                isGATTAvailable = true
            )
        }

        override fun onCharacteristicRead(
            data: ByteArray,
            svcUuid: UUID,
            dataUuid: UUID,
            status: Int,
            device: BluetoothDevice
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                LogUtils.e(TAG, "onCharacteristicRead failed. status = $status svcUuid = $svcUuid, dataUuid = $dataUuid")
                return
            }
            if (UUIDConstant.GATT_FUN_UUID == svcUuid)
            {
                when(dataUuid)
                {
                    UUIDConstant.GATT_DATA_RFCOMM_UUID -> {
                        rfcommUuid = String(data)
                        connectionManager.initRFCOMM(
                            bluetoothManagerWrapper.getAdapter(),
                            device,
                            UUID.fromString(rfcommUuid)
                        )
                    }
                    UUIDConstant.TCP_INFO_UUID -> {
                        tcpInfo = String(data)
                        val tcpInfos = tcpInfo.split(":")
                        connectionManager.initTcpSocket(tcpInfos[0], tcpInfos[1].toInt())
                    }
                    UUIDConstant.UDP_INFO_UUID -> {

                    }
                }
            }
        }

        override fun onFault(device: BluetoothDevice) {
            mainUiState = MainUiState.ConnectingPage(
                device = device,
                isGATTAvailable = false,
                isRFCOMMAvailable = false
            )
            connectionManager.initRFCOMM(bluetoothManagerWrapper.getAdapter(), device, UUIDConstant.DEFAULT_RFCOMM_UUID)
        }

        override fun onDestroy() {

        }

    }
    private val connectionManager: ConnectionManager = ConnectionManager(object : ConnectionCallback {
        override fun onManagerAvailableChange(available: Boolean) {
            onConnectionManagerAvailableChange(available)
        }

        override fun onConnectionAvailable(
            available: Boolean,
            type: ConnectionManager.ConnectionType
        ) {
            onConnectionTypeAvailableChange(available, type)
        }


        override fun getTypeClass(type: Int): Class<*> {
            return EntityType.TYPE_MAPPING[type] ?: JsonElement::class.java
        }

        override fun onDataReady(data: BaseEntity<*>?) {
            if (data != null) {
                when (data.type) {
                    EntityType.TYPE_QUERY_CLIENT_INFO -> {
                        connectionManager.sendData(BaseEntity(
                            type = EntityType.TYPE_QUERY_CLIENT_INFO_RESULT,
                            id = EntityId.GAMEPAD_PAGE_EVENT,
                            timestamp = System.currentTimeMillis(),
                            data = deviceInfo
                        ))
                    }

                    EntityType.TYPE_FEEDBACK_RECEIVED -> {
                        val received = data.data as? FeedbackReceived
                        if (received != null) {
                            LogUtils.d(TAG, "Feedback received. received is $received")
                        }
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
    }, coroutineManager)

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
            isGATTAvailable = false,
            isRFCOMMAvailable = false
        )
        bluetoothGATTManager = BluetoothGATTManager(application, device, coroutineManager, bluetoothGATTManagerCallback)
        bluetoothGATTManager?.init()
    }

    fun onEnterGamepad() {
//        connectionManager.connectionType = ConnectionManager.ConnectionType.TCP
        val currentState = mainUiState
        if (currentState is MainUiState.ConnectingPage && connectionManager.isAvailable) {
            mainUiState = MainUiState.GamepadPage
        }
        if (!connectionManager.isAvailable) {
            mainUiState = MainUiState.SelectPage
        }
    }

    fun onBackFromGamepad() {
        val currentState = mainUiState
        val bm = bluetoothGATTManager
        if (currentState is MainUiState.GamepadPage && bm != null) {
            mainUiState = MainUiState.ConnectingPage(
                device = bm.device,
                isGATTAvailable = bluetoothGATTManager?.isAvailable == true,
                isRFCOMMAvailable = connectionManager.isAvailable
            )
        }
        if (bm == null) {
            mainUiState = MainUiState.SelectPage
        }
    }

    fun onGamepadEvent(data: ByteArray) {
        connectionManager.sendData(
            BaseEntity(
                type = EntityType.TYPE_SEND_GAME_EVENT,
                id = EntityId.GAMEPAD_PAGE_EVENT,
                timestamp = System.currentTimeMillis(),
                data = Base64.encodeToString(data, Base64.NO_WRAP)
            )
        )
    }

    fun disconnect() {
        clearConnection()
        mainUiState = MainUiState.SelectPage
    }

    fun getBoundDevices() = bluetoothManagerWrapper.getBondedDevice()

    private fun onConnectionManagerAvailableChange(isAvailable: Boolean) {
        when (val currentState = mainUiState) {
            is MainUiState.ConnectingPage -> {
                mainUiState = currentState.copy(isAvailable = isAvailable)
            }
            is MainUiState.GamepadPage -> {
                val bm = bluetoothGATTManager
                if (!isAvailable && bm != null) {
                    mainUiState = MainUiState.ConnectingPage(
                        device = bm.device,
                        isAvailable = false,
                        isRFCOMMAvailable = connectionManager.bluetoothAvailable,
                        isTcpAvailable = connectionManager.tcpAvailable,
                        isUdpAvailable = connectionManager.udpAvailable,
                        rfcommUuid = rfcommUuid,
                        tcpInfo = tcpInfo,
                        udpInfo = udpInfo
                    )
                }
                if (bm == null) {
                    mainUiState = MainUiState.SelectPage
                }
            }
            else -> Unit
        }
    }

    private fun onConnectionTypeAvailableChange(isAvailable: Boolean, type: ConnectionManager.ConnectionType) {
        when (val currentState = mainUiState) {
            is MainUiState.ConnectingPage -> {
                mainUiState = when (type) {
                    ConnectionManager.ConnectionType.BLE -> currentState.copy(isRFCOMMAvailable = isAvailable, rfcommUuid = rfcommUuid)
                    ConnectionManager.ConnectionType.TCP -> currentState.copy(isTcpAvailable = isAvailable, tcpInfo = tcpInfo)
                    ConnectionManager.ConnectionType.UDP -> currentState.copy(isUdpAvailable = isAvailable, udpInfo = udpInfo)
                }
            }
            else -> Unit
        }
    }

    private fun clearConnection() {
        connectionManager.destroy()
        bluetoothGATTManager?.destroy()
        bluetoothGATTManager = null
    }

}
