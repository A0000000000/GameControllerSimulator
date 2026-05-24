package cn.maoyanluo.gamecontrollersimulator2

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.ConnectionCoordinator.CoordinatorEvent
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionType
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityId
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityType
import cn.maoyanluo.log_library.LogUtils
import kotlinx.coroutines.launch

class MainViewModel(private val application: Application) : AndroidViewModel(application) {

    companion object {
        const val TAG = "MainViewModel"
    }

    var mainUiState by mutableStateOf<MainUiState>(MainUiState.NoPermissionPage)
        private set

    private val coroutineManager = CoroutineManager()
    private val connectionCoordinator = ConnectionCoordinator(application, coroutineManager)
    private var rfcommUuid: String = ""
    private var tcpInfo: String = ""
    private var udpInfo: String = ""


    init {
        LogUtils.i(TAG, "init")
        coroutineManager.init()
        val job = coroutineManager.getMainScope().launch {
            LogUtils.i(TAG, "collect connectionCoordinator event")
            connectionCoordinator.event.collect {
                when (it) {
                    is CoordinatorEvent.GattAvailableEvent -> {
                        mainUiState = if (it.available) {
                            MainUiState.ConnectingPage(
                                deviceName = connectionCoordinator.getDeviceName(),
                                isGATTAvailable = true
                            )
                        } else {
                            MainUiState.SelectPage
                        }
                    }
                    is CoordinatorEvent.ConnectionTypeAvailableEvent -> {
                        if (mainUiState is MainUiState.ConnectingPage) {
                            val uiState = mainUiState as MainUiState.ConnectingPage
                            mainUiState = when(it.type) {
                                ConnectionType.BLE -> uiState.copy(rfcommStatus = ConnectStatus(
                                    isAvailable = it.available,
                                    isSelect = connectionCoordinator.getConnectType() == ConnectionType.BLE,
                                    info = rfcommUuid
                                ))
                                ConnectionType.TCP -> uiState.copy(tcpStatus = ConnectStatus(
                                    isAvailable = it.available,
                                    isSelect = connectionCoordinator.getConnectType() == ConnectionType.TCP,
                                    info = tcpInfo
                                ))
                                ConnectionType.UDP -> uiState.copy(udpStatus = ConnectStatus(
                                    isAvailable = it.available,
                                    isSelect = connectionCoordinator.getConnectType() == ConnectionType.UDP,
                                    info = udpInfo
                                ))
                            }
                        }
                    }
                    is CoordinatorEvent.ConnectionAvailableEvent -> {
                        if (it.available) {
                            if (mainUiState is MainUiState.ConnectingPage ) {
                                val uiState = mainUiState as MainUiState.ConnectingPage
                                mainUiState = uiState.copy(isAvailable = true)
                            }
                        } else {
                            mainUiState = MainUiState.SelectPage
                        }
                    }
                    is CoordinatorEvent.RFCOMMDataReadyEvent -> {
                        rfcommUuid = it.data
                    }
                    is CoordinatorEvent.TCPDataReadyEvent -> {
                        tcpInfo = it.data
                    }
                    is CoordinatorEvent.UDPDataReadyEvent -> {
                        udpInfo = it.data
                    }
                    is CoordinatorEvent.RttResultEvent -> {
                        LogUtils.d(TAG, "onRttResult. diff = ${it.diff}, type = ${it.type}")
                    }
                    is CoordinatorEvent.FaultEvent -> {
                        LogUtils.d(TAG, "onFault. msg = ${it.msg}, params = ${it.params}", it.e)
                    }
                }
            }
        }
        addCloseable {
            LogUtils.i(TAG, "close")
            job.cancel()
            connectionCoordinator.destroy()
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
            connectionCoordinator.clearConnection()
            MainUiState.NoPermissionPage
        }
    }

    fun onDeviceSelected(device: BluetoothDevice) {
        connectionCoordinator.setDevice(device)
        mainUiState = MainUiState.ConnectingPage(
            deviceName = connectionCoordinator.getDeviceName(),
            isGATTAvailable = false,
        )
    }

    fun onEnterGamepad() {
        val currentState = mainUiState
        if (currentState is MainUiState.ConnectingPage && connectionCoordinator.isConnectionAvailable()) {
            mainUiState = MainUiState.GamepadPage
        }
        if (!connectionCoordinator.isConnectionAvailable()) {
            mainUiState = MainUiState.SelectPage
        }
    }

    fun onSelectConnectType(type: ConnectionType) {
        connectionCoordinator.setConnectType(type)
        if (mainUiState is MainUiState.ConnectingPage) {
            mainUiState = (mainUiState as MainUiState.ConnectingPage).copy(
                rfcommStatus = ConnectStatus(
                    isAvailable = connectionCoordinator.isConnectionTypeAvailable(ConnectionType.BLE),
                    isSelect = connectionCoordinator.getConnectType() == ConnectionType.BLE,
                    info = rfcommUuid
                ),
                tcpStatus = ConnectStatus(
                    isAvailable = connectionCoordinator.isConnectionTypeAvailable(ConnectionType.TCP),
                    isSelect = connectionCoordinator.getConnectType() == ConnectionType.TCP,
                    info = tcpInfo
                ),
                udpStatus = ConnectStatus(
                    isAvailable = connectionCoordinator.isConnectionTypeAvailable(ConnectionType.UDP),
                    isSelect = connectionCoordinator.getConnectType() == ConnectionType.UDP,
                    info = udpInfo
                )
            )
        }
    }

    fun onRequestRtt(type: ConnectionType) = connectionCoordinator.requestRtt(type)

    fun onBackFromGamepad() {
        val currentState = mainUiState
        if (currentState is MainUiState.GamepadPage) {
            mainUiState = MainUiState.ConnectingPage.map(
                connectionCoordinator.getDeviceName(),
                connectionCoordinator.isGATTAvailable(),
                connectionCoordinator.isConnectionAvailable(),
                connectionCoordinator.getConnectType(),
                rfcommUuid,
                tcpInfo,
                udpInfo,
                connectionCoordinator.isConnectionTypeAvailable(ConnectionType.BLE),
                connectionCoordinator.isConnectionTypeAvailable(ConnectionType.TCP),
                connectionCoordinator.isConnectionTypeAvailable(ConnectionType.UDP)
            )
        }
    }

    fun onGamepadEvent(data: ByteArray) {
        connectionCoordinator.sendData(
            BaseEntity(
                type = EntityType.TYPE_SEND_GAME_EVENT,
                id = EntityId.GAMEPAD_PAGE_EVENT,
                timestamp = System.currentTimeMillis(),
                data = Base64.encodeToString(data, Base64.NO_WRAP)
            )
        )
    }

    fun disconnect() {
        connectionCoordinator.clearConnection()
        mainUiState = MainUiState.SelectPage
    }

    fun getBoundDevices() = connectionCoordinator.getBoundDevices()

    fun getGamepadEventGenerator() = connectionCoordinator.getGamepadEventGenerator()


}
