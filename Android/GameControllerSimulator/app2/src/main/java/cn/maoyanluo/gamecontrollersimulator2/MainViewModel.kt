package cn.maoyanluo.gamecontrollersimulator2

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.os.Build
import android.util.Base64
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
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionController
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionType
import cn.maoyanluo.gamecontrollersimulator2.connect.RouterHandler
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityId
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityType
import cn.maoyanluo.gamecontrollersimulator2.constant.UUIDConstant
import cn.maoyanluo.log_library.LogUtils
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
                        connectionController.initRFCOMM(bluetoothManagerWrapper.getAdapter(), device, UUID.fromString(rfcommUuid))
                    }
                    UUIDConstant.TCP_INFO_UUID -> {
                        tcpInfo = String(data)
                        val tcpInfos = tcpInfo.split(":")
                        connectionController.initTcpSocket(tcpInfos[0], tcpInfos[1].toInt())
                    }
                    UUIDConstant.UDP_INFO_UUID -> {

                    }
                }
            }
        }

        override fun onFault(device: BluetoothDevice) {
            mainUiState = MainUiState.ConnectingPage(
                device = device,
                isGATTAvailable = false
            )
            connectionController.initRFCOMM(bluetoothManagerWrapper.getAdapter(), device, UUIDConstant.DEFAULT_RFCOMM_UUID)
        }

        override fun onDestroy() {

        }

    }

    private val connectionController: ConnectionController = ConnectionController(coroutineManager, object : ConnectionController.ConnectionControllerCallback {
        override fun onAvailableChange(available: Boolean) {
            onConnectionAvailableChange(available)
        }

        override fun onConnectionAvailableChange(
            available: Boolean,
            type: ConnectionType
        ) {
            onConnectionTypeAvailableChange(available, type)
        }

        override fun onRttResult(
            diff: Long,
            type: ConnectionType
        ) {
            LogUtils.d(TAG, "onRttResult. diff = $diff, type = $type")
        }

        override fun onFault(
            msg: String,
            e: Exception,
            params: Map<String, Any>?
        ) {

        }
    })

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
        connectionController.init()
        connectionController.registerHandler(listOf(
            RouterHandler(
                id = EntityId.GAMEPAD_PAGE_EVENT,
                type = EntityType.TYPE_QUERY_CLIENT_INFO,
                handler = { data, type ->
                    connectionController.sendData(BaseEntity(
                        type = EntityType.TYPE_QUERY_CLIENT_INFO_RESULT,
                        id = EntityId.GAMEPAD_PAGE_EVENT,
                        timestamp = System.currentTimeMillis(),
                        data = deviceInfo
                    ))
                }
            ),
            RouterHandler(
                id = EntityId.GAMEPAD_PAGE_EVENT,
                type = EntityType.TYPE_FEEDBACK_RECEIVED,
                handler = { data, type ->
                    val received = data.data as? FeedbackReceived
                    if (received != null) {
                        LogUtils.d(TAG, "Feedback received. received is $received")
                    }
                }
            )
        ))
        mainUiState = MainUiState.ConnectingPage(
            device = device,
            isGATTAvailable = false,
        )
        bluetoothGATTManager = BluetoothGATTManager(application, device, coroutineManager, bluetoothGATTManagerCallback)
        bluetoothGATTManager?.init()
    }

    fun onEnterGamepad() {
        val currentState = mainUiState
        if (currentState is MainUiState.ConnectingPage && connectionController.available) {
            mainUiState = MainUiState.GamepadPage
        }
        if (!connectionController.available) {
            mainUiState = MainUiState.SelectPage
        }
    }

    fun onSelectConnectType(type: ConnectionType) {
        connectionController.selectType = type
        if (mainUiState is MainUiState.ConnectingPage) {
            mainUiState = (mainUiState as MainUiState.ConnectingPage).copy(
                rfcommStatus = ConnectStatus(
                    isAvailable = connectionController.isAvailable(ConnectionType.BLE),
                    isSelect = connectionController.selectType == ConnectionType.BLE,
                    info = rfcommUuid
                ),
                tcpStatus = ConnectStatus(
                    isAvailable = connectionController.isAvailable(ConnectionType.TCP),
                    isSelect = connectionController.selectType == ConnectionType.TCP,
                    info = tcpInfo
                ),
                udpStatus = ConnectStatus(
                    isAvailable = connectionController.isAvailable(ConnectionType.UDP),
                    isSelect = connectionController.selectType == ConnectionType.UDP,
                    info = udpInfo
                )
            )
        }
    }

    fun onRequestRtt(type: ConnectionType) {
        connectionController.requestRtt(type)
    }

    fun onBackFromGamepad() {
        val currentState = mainUiState
        val bm = bluetoothGATTManager
        if (currentState is MainUiState.GamepadPage && bm != null) {
            mainUiState = MainUiState.ConnectingPage(
                device = bm.device,
                isAvailable = connectionController.available,
                isGATTAvailable = bluetoothGATTManager?.isAvailable == true,
                rfcommStatus = ConnectStatus(
                    isAvailable = connectionController.isAvailable(ConnectionType.BLE),
                    isSelect = connectionController.selectType == ConnectionType.BLE,
                    info = rfcommUuid
                ),
                tcpStatus = ConnectStatus(
                    isAvailable = connectionController.isAvailable(ConnectionType.TCP),
                    isSelect = connectionController.selectType == ConnectionType.TCP,
                    info = tcpInfo
                ),
                udpStatus = ConnectStatus(
                    isAvailable = connectionController.isAvailable(ConnectionType.UDP),
                    isSelect = connectionController.selectType == ConnectionType.UDP,
                    info = udpInfo
                )
            )
        }
        if (bm == null) {
            mainUiState = MainUiState.SelectPage
        }
    }

    fun onGamepadEvent(data: ByteArray) {
        connectionController.sendData(
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

    private fun onConnectionAvailableChange(isAvailable: Boolean) {
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
                        rfcommStatus = ConnectStatus(
                            isAvailable = connectionController.isAvailable(ConnectionType.BLE),
                            isSelect = connectionController.selectType == ConnectionType.BLE,
                            info = rfcommUuid
                        ),
                        tcpStatus = ConnectStatus(
                            isAvailable = connectionController.isAvailable(ConnectionType.TCP),
                            isSelect = connectionController.selectType == ConnectionType.TCP,
                            info = tcpInfo
                        ),
                        udpStatus = ConnectStatus(
                            isAvailable = connectionController.isAvailable(ConnectionType.UDP),
                            isSelect = connectionController.selectType == ConnectionType.UDP,
                            info = udpInfo
                        )
                    )
                }
                if (bm == null) {
                    mainUiState = MainUiState.SelectPage
                }
            }
            else -> Unit
        }
    }

    private fun onConnectionTypeAvailableChange(isAvailable: Boolean, type: ConnectionType) {
        when (val currentState = mainUiState) {
            is MainUiState.ConnectingPage -> {
                mainUiState = when (type) {
                    ConnectionType.BLE -> currentState.copy(rfcommStatus = ConnectStatus(
                        isAvailable = isAvailable,
                        isSelect = connectionController.selectType == ConnectionType.BLE,
                        info = rfcommUuid
                    ))
                    ConnectionType.TCP -> currentState.copy(tcpStatus = ConnectStatus(
                        isAvailable = isAvailable,
                        isSelect = connectionController.selectType == ConnectionType.TCP,
                        info = tcpInfo
                    ))
                    ConnectionType.UDP -> currentState.copy(udpStatus = ConnectStatus(
                        isAvailable = isAvailable,
                        isSelect = connectionController.selectType == ConnectionType.UDP,
                        info = udpInfo
                    ))
                }
            }
            else -> Unit
        }
    }

    private fun clearConnection() {
        connectionController.destroy()
        bluetoothGATTManager?.destroy()
        bluetoothGATTManager = null
    }

}
