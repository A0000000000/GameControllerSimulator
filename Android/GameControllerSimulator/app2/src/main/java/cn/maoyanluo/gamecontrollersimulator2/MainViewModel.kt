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
    private val coordinator = ConnectionCoordinator(application, coroutineManager)
    private var rfcommUuid: String = ""
    private var tcpInfo: String = ""
    private var udpInfo: String = ""


    init {
        coroutineManager.init()
        val job = coroutineManager.getMainScope().launch {
            coordinator.event.collect {
                when (it) {
                    is CoordinatorEvent.GattAvailableEvent -> {
                        mainUiState = if (it.available) {
                            MainUiState.ConnectingPage(
                                deviceName = coordinator.getDeviceName(),
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
                                    isSelect = coordinator.getConnectType() == ConnectionType.BLE,
                                    info = rfcommUuid
                                ))
                                ConnectionType.TCP -> uiState.copy(tcpStatus = ConnectStatus(
                                    isAvailable = it.available,
                                    isSelect = coordinator.getConnectType() == ConnectionType.TCP,
                                    info = tcpInfo
                                ))
                                ConnectionType.UDP -> uiState.copy(udpStatus = ConnectStatus(
                                    isAvailable = it.available,
                                    isSelect = coordinator.getConnectType() == ConnectionType.UDP,
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
            job.cancel()
            coordinator.destroy()
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
            coordinator.clearConnection()
            MainUiState.NoPermissionPage
        }
    }

    fun onDeviceSelected(device: BluetoothDevice) {
        coordinator.setDevice(device)
        mainUiState = MainUiState.ConnectingPage(
            deviceName = coordinator.getDeviceName(),
            isGATTAvailable = false,
        )
    }

    fun onEnterGamepad() {
        val currentState = mainUiState
        if (currentState is MainUiState.ConnectingPage && coordinator.isConnectionAvailable()) {
            mainUiState = MainUiState.GamepadPage
        }
        if (!coordinator.isConnectionAvailable()) {
            mainUiState = MainUiState.SelectPage
        }
    }

    fun onSelectConnectType(type: ConnectionType) {
        coordinator.setConnectType(type)
        if (mainUiState is MainUiState.ConnectingPage) {
            mainUiState = (mainUiState as MainUiState.ConnectingPage).copy(
                rfcommStatus = ConnectStatus(
                    isAvailable = coordinator.isConnectionTypeAvailable(ConnectionType.BLE),
                    isSelect = coordinator.getConnectType() == ConnectionType.BLE,
                    info = rfcommUuid
                ),
                tcpStatus = ConnectStatus(
                    isAvailable = coordinator.isConnectionTypeAvailable(ConnectionType.TCP),
                    isSelect = coordinator.getConnectType() == ConnectionType.TCP,
                    info = tcpInfo
                ),
                udpStatus = ConnectStatus(
                    isAvailable = coordinator.isConnectionTypeAvailable(ConnectionType.UDP),
                    isSelect = coordinator.getConnectType() == ConnectionType.UDP,
                    info = udpInfo
                )
            )
        }
    }

    fun onRequestRtt(type: ConnectionType) = coordinator.requestRtt(type)

    fun onBackFromGamepad() {
        val currentState = mainUiState
        if (currentState is MainUiState.GamepadPage) {
            mainUiState = MainUiState.ConnectingPage.map(
                coordinator.getDeviceName(),
                coordinator.isGATTAvailable(),
                coordinator.isConnectionAvailable(),
                coordinator.getConnectType(),
                rfcommUuid,
                tcpInfo,
                udpInfo,
                coordinator.isConnectionTypeAvailable(ConnectionType.BLE),
                coordinator.isConnectionTypeAvailable(ConnectionType.TCP),
                coordinator.isConnectionTypeAvailable(ConnectionType.UDP)
            )
        }
    }

    fun onGamepadEvent(data: ByteArray) {
        coordinator.sendData(
            BaseEntity(
                type = EntityType.TYPE_SEND_GAME_EVENT,
                id = EntityId.GAMEPAD_PAGE_EVENT,
                timestamp = System.currentTimeMillis(),
                data = Base64.encodeToString(data, Base64.NO_WRAP)
            )
        )
    }

    fun disconnect() {
        coordinator.clearConnection()
        mainUiState = MainUiState.SelectPage
    }

    fun getBoundDevices() = coordinator.getBoundDevices()

    fun getGamepadEventGenerator() = coordinator.getGamepadEventGenerator()


}
