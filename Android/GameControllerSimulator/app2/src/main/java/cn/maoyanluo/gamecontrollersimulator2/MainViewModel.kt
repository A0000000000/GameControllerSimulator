package cn.maoyanluo.gamecontrollersimulator2

import android.app.Application
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
import cn.maoyanluo.gamecontrollersimulator2.mainui.ConnectingPageModel
import cn.maoyanluo.gamecontrollersimulator2.mainui.MainUiEffect
import cn.maoyanluo.gamecontrollersimulator2.mainui.MainUiIntent
import cn.maoyanluo.gamecontrollersimulator2.mainui.MainUiState
import cn.maoyanluo.gamecontrollersimulator2.mainui.SelectPageModel
import cn.maoyanluo.log_library.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TAG = "MainViewModel"
    }

    var mainUiState by mutableStateOf<MainUiState>(MainUiState.NoPermissionPage)
        private set

    private val _mainUiEffect = MutableSharedFlow<MainUiEffect>()
    val mainUiEffect = _mainUiEffect.asSharedFlow()

    private val coroutineManager = CoroutineManager()
    private val connectionCoordinator = ConnectionCoordinator(application, coroutineManager)
    private var connectionPageModel = ConnectingPageModel()

    init {
        LogUtils.i(TAG, "init")
        coroutineManager.init()
        val job = coroutineManager.getMainScope().launch {
            LogUtils.i(TAG, "collect connectionCoordinator event")
            connectionCoordinator.event.collect {
                onConnectionCoordinatorEvent(it)
            }
        }
        addCloseable {
            LogUtils.i(TAG, "close")
            job.cancel()
            connectionCoordinator.destroy()
            coroutineManager.destroy()
        }
    }

    private fun onConnectionCoordinatorEvent(event: CoordinatorEvent) {
        LogUtils.i(TAG, "event is ${event::class.simpleName}, event data is $event")
        when (event) {
            is CoordinatorEvent.GattAvailableEvent -> {
                connectionPageModel = if (event.available) {
                    connectionPageModel.copy(
                        deviceName = connectionCoordinator.getDeviceName(),
                        isGATTAvailable = true
                    )
                } else {
                    ConnectingPageModel(isAvailable = false)
                }
            }

            is CoordinatorEvent.ConnectionTypeAvailableEvent -> {
                connectionPageModel = when (event.type) {
                    ConnectionType.BLE -> {
                        connectionPageModel.copy(
                            rfcommStatus = connectionPageModel.rfcommStatus.copy(
                                isAvailable = event.available,
                                isSelect = connectionCoordinator.getConnectType() == ConnectionType.BLE
                            )
                        )
                    }

                    ConnectionType.TCP -> {
                        connectionPageModel.copy(
                            tcpStatus = connectionPageModel.tcpStatus.copy(
                                isAvailable = event.available,
                                isSelect = connectionCoordinator.getConnectType() == ConnectionType.TCP
                            )
                        )
                    }

                    ConnectionType.UDP -> {
                        connectionPageModel.copy(
                            udpStatus = connectionPageModel.udpStatus.copy(
                                isAvailable = event.available,
                                isSelect = connectionCoordinator.getConnectType() == ConnectionType.UDP
                            )
                        )
                    }
                }
            }

            is CoordinatorEvent.ConnectionAvailableEvent -> {
                connectionPageModel = connectionPageModel.copy(isAvailable = event.available)
            }

            is CoordinatorEvent.RFCOMMDataReadyEvent -> {
                connectionPageModel = connectionPageModel.copy(
                    rfcommStatus = connectionPageModel.rfcommStatus.copy(info = event.data)
                )
            }

            is CoordinatorEvent.TCPDataReadyEvent -> {
                connectionPageModel =
                    connectionPageModel.copy(tcpStatus = connectionPageModel.tcpStatus.copy(info = event.data))
            }

            is CoordinatorEvent.UDPDataReadyEvent -> {
                connectionPageModel =
                    connectionPageModel.copy(udpStatus = connectionPageModel.udpStatus.copy(info = event.data))
            }

            is CoordinatorEvent.RttResultEvent -> {
                emitEffect(MainUiEffect.RttResultEffect(event.diff, event.type))
                LogUtils.d(TAG, "onRttResult. diff = ${event.diff}, type = ${event.type}")
            }

            is CoordinatorEvent.FaultEvent -> {
                LogUtils.d(TAG, "onFault. msg = ${event.msg}, params = ${event.params}", event.e)
            }
        }
        val currentPage = mainUiState
        when (currentPage) {
            MainUiState.NoPermissionPage -> {

            }

            is MainUiState.SelectPage -> {

            }

            is MainUiState.ConnectingPage -> {
                mainUiState =
                    if (connectionPageModel.isAvailable || connectionPageModel.isGATTAvailable) {
                        MainUiState.ConnectingPage(connectionPageModel.copy())
                    } else {
                        MainUiState.SelectPage(SelectPageModel(connectionCoordinator.getBoundDevices()))
                    }
            }

            MainUiState.GamepadPage -> {
                if (!connectionPageModel.isAvailable) {
                    mainUiState =
                        MainUiState.SelectPage(SelectPageModel(connectionCoordinator.getBoundDevices()))
                }
            }
        }
    }

    fun onUiIntent(uiIntent: MainUiIntent) {
        coroutineManager.getIOScope().launch {
            val currentPage = mainUiState
            var nextPage: MainUiState? = null
            when (uiIntent) {
                is MainUiIntent.PermissionResultIntent -> {
                    if (uiIntent.grant) {
                        if (currentPage is MainUiState.NoPermissionPage) {
                            nextPage =
                                MainUiState.SelectPage(SelectPageModel(connectionCoordinator.getBoundDevices()))
                        }
                    } else {
                        connectionCoordinator.destroy()
                        connectionPageModel = ConnectingPageModel()
                        nextPage = MainUiState.NoPermissionPage
                    }
                }

                is MainUiIntent.OnDeviceSelectedIntent -> {
                    connectionCoordinator.setDevice(uiIntent.device)
                    connectionPageModel =
                        connectionPageModel.copy(deviceName = connectionCoordinator.getDeviceName())
                    nextPage = MainUiState.ConnectingPage(connectionPageModel.copy())
                }

                MainUiIntent.OnDeviceListFlush -> {
                    if (currentPage is MainUiState.SelectPage) {
                        emitEffect(MainUiEffect.DeviceFlushEffect(false))
                        nextPage =
                            MainUiState.SelectPage(SelectPageModel(connectionCoordinator.getBoundDevices()))
                        emitEffect(MainUiEffect.DeviceFlushEffect(true))
                    }
                }

                MainUiIntent.OnEnterGamepadIntent -> {
                    if (currentPage is MainUiState.ConnectingPage && connectionCoordinator.isConnectionAvailable()) {
                        nextPage = MainUiState.GamepadPage
                    }
                    if (!connectionCoordinator.isConnectionAvailable()) {
                        nextPage =
                            MainUiState.SelectPage(SelectPageModel(connectionCoordinator.getBoundDevices()))
                    }
                }

                is MainUiIntent.OnSelectConnectTypeIntent -> {
                    connectionCoordinator.setConnectType(uiIntent.type)
                    connectionPageModel = connectionPageModel.copy(
                        rfcommStatus = connectionPageModel.rfcommStatus.copy(isSelect = connectionCoordinator.getConnectType() == ConnectionType.BLE),
                        tcpStatus = connectionPageModel.tcpStatus.copy(isSelect = connectionCoordinator.getConnectType() == ConnectionType.TCP),
                        udpStatus = connectionPageModel.udpStatus.copy(isSelect = connectionCoordinator.getConnectType() == ConnectionType.UDP),
                    )
                    if (mainUiState is MainUiState.ConnectingPage) {
                        mainUiState = MainUiState.ConnectingPage(connectionPageModel.copy())
                    }
                }

                is MainUiIntent.OnRequestRttIntent -> {
                    connectionCoordinator.requestRtt(uiIntent.type)
                }

                MainUiIntent.OnBackFromGamepad -> {
                    nextPage = if (connectionCoordinator.isConnectionAvailable()) {
                        MainUiState.ConnectingPage(connectionPageModel.copy())
                    } else {
                        connectionPageModel = ConnectingPageModel()
                        MainUiState.SelectPage(SelectPageModel(connectionCoordinator.getBoundDevices()))
                    }
                }

                is MainUiIntent.OnGamepadEvent -> {
                    connectionCoordinator.sendData(
                        BaseEntity(
                            type = EntityType.TYPE_SEND_GAME_EVENT,
                            id = EntityId.GAMEPAD_PAGE_EVENT,
                            timestamp = System.currentTimeMillis(),
                            data = Base64.encodeToString(uiIntent.data, Base64.NO_WRAP)
                        )
                    )
                }

                MainUiIntent.OnDisconnect -> {
                    connectionCoordinator.clearConnection()
                    connectionPageModel = ConnectingPageModel()
                    nextPage =
                        MainUiState.SelectPage(SelectPageModel(connectionCoordinator.getBoundDevices()))
                }
            }
            withContext(Dispatchers.Main) {
                nextPage?.let {
                    mainUiState = it
                }
            }
        }
    }

    private fun emitEffect(effect: MainUiEffect) {
        coroutineManager.getIOScope().launch {
            _mainUiEffect.emit(effect)
        }
    }

    fun getGamepadEventGenerator() = connectionCoordinator.getGamepadEventGenerator()


}
