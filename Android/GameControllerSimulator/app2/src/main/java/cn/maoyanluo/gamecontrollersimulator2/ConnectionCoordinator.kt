package cn.maoyanluo.gamecontrollersimulator2

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import cn.maoyanluo.bluetooth_library.BluetoothManagerWrapper
import cn.maoyanluo.bluetooth_library.gatt.BluetoothGATTManager
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.MainViewModel.Companion.TAG
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.bean.DeviceInfo
import cn.maoyanluo.gamecontrollersimulator2.bean.FeedbackReceived
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionController
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionType
import cn.maoyanluo.gamecontrollersimulator2.connect.RouterHandler
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityId
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityType
import cn.maoyanluo.gamecontrollersimulator2.constant.UUIDConstant
import cn.maoyanluo.gamecontrollersimulator2.generator.GamepadEventGenerator
import cn.maoyanluo.log_library.LogUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class ConnectionCoordinator(
    private val ctx: Context,
    private val coroutineManager: CoroutineManager,
) {
    private val gamepadEventGenerator = GamepadEventGenerator(coroutineManager)
    private val bluetoothManagerWrapper = BluetoothManagerWrapper(ctx)
    private var bluetoothGATTManager: BluetoothGATTManager? = null
    private val connectionController = ConnectionController(coroutineManager, ConnectionControllerCallbackImpl(this))
    private val routerHandlers = listOf(
        RouterHandler(
            id = EntityId.GAMEPAD_PAGE_EVENT,
            type = EntityType.TYPE_QUERY_CLIENT_INFO,
            handler = { data, type ->
                connectionController.sendData(BaseEntity(
                    type = EntityType.TYPE_QUERY_CLIENT_INFO_RESULT,
                    id = EntityId.GAMEPAD_PAGE_EVENT,
                    timestamp = System.currentTimeMillis(),
                    data = DeviceInfo()
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
    )

    private val _event = MutableSharedFlow<CoordinatorEvent>(extraBufferCapacity = 16)
    val event = _event.asSharedFlow()

    fun setDevice(device: BluetoothDevice) {
        clearConnection()
        connectionController.init()
        connectionController.registerHandler(routerHandlers)
        bluetoothGATTManager = BluetoothGATTManager(ctx, device, coroutineManager, bluetoothGATTManagerCallback).also { it.init() }
    }

    fun getDeviceName() = bluetoothGATTManager?.device?.name ?: "未知设备"

    fun isGATTAvailable() = bluetoothGATTManager?.isAvailable == true

    fun isConnectionAvailable() = connectionController.available

    fun isConnectionTypeAvailable(type: ConnectionType) = connectionController.isAvailable(type)

    fun setConnectType(type: ConnectionType) {
        connectionController.selectType = type
    }

    fun getConnectType() = connectionController.selectType

    fun sendData(entity: BaseEntity<*>) = connectionController.sendData(entity)

    fun requestRtt(type: ConnectionType) = connectionController.requestRtt(type)

    fun getBoundDevices() = bluetoothManagerWrapper.getBondedDevice()

    fun getGamepadEventGenerator() = gamepadEventGenerator

    fun clearConnection() {
        connectionController.destroy()
        bluetoothGATTManager?.destroy()
    }

    fun destroy() {
        clearConnection()
    }

    private val bluetoothGATTManagerCallback = object : BluetoothGATTManager.BluetoothGATTManagerCallback {
        override fun onAvailable(device: BluetoothDevice) {
            coroutineManager.getIOScope().launch {
                _event.emit(CoordinatorEvent.GattAvailableEvent(true))
            }
            bluetoothGATTManager?.readCharacteristic(UUIDConstant.GATT_FUN_UUID, UUIDConstant.GATT_DATA_RFCOMM_UUID)
            bluetoothGATTManager?.readCharacteristic(UUIDConstant.GATT_FUN_UUID, UUIDConstant.TCP_INFO_UUID)
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
                        val rfcommUuid = String(data)
                        connectionController.initRFCOMM(bluetoothManagerWrapper.getAdapter(), device, UUID.fromString(rfcommUuid))
                        coroutineManager.getIOScope().launch {
                            _event.emit((CoordinatorEvent.RFCOMMDataReadyEvent(rfcommUuid)))
                        }
                    }
                    UUIDConstant.TCP_INFO_UUID -> {
                        val tcpInfo = String(data)
                        val tcpInfos = tcpInfo.split(":")
                        connectionController.initTcpSocket(tcpInfos[0], tcpInfos[1].toInt())
                        coroutineManager.getIOScope().launch {
                            _event.emit(CoordinatorEvent.TCPDataReadyEvent(tcpInfo))
                        }
                    }
                    UUIDConstant.UDP_INFO_UUID -> {

                    }
                }
            }
        }

        override fun onFault(device: BluetoothDevice) {
            coroutineManager.getIOScope().launch {
                _event.emit(CoordinatorEvent.GattAvailableEvent(false))
            }
        }

        override fun onDestroy() {
            coroutineManager.getIOScope().launch {
                _event.emit(CoordinatorEvent.GattAvailableEvent(false))
            }
        }

    }

    private class ConnectionControllerCallbackImpl(private val coordinator: ConnectionCoordinator): ConnectionController.ConnectionControllerCallback {
        override fun onAvailableChange(available: Boolean) {
            coordinator.coroutineManager.getIOScope().launch {
                coordinator._event.emit(CoordinatorEvent.ConnectionAvailableEvent(available))
            }
        }

        override fun onConnectionAvailableChange(
            available: Boolean,
            type: ConnectionType
        ) {
            coordinator.coroutineManager.getIOScope().launch {
                coordinator._event.emit(CoordinatorEvent.ConnectionTypeAvailableEvent(available, type))
            }
        }

        override fun onRttResult(diff: Long, type: ConnectionType) {
            coordinator.coroutineManager.getIOScope().launch {
                coordinator._event.emit(CoordinatorEvent.RttResultEvent(diff, type))
            }
        }

        override fun onFault(
            msg: String,
            e: Exception,
            params: Map<String, Any>?
        ) {
            coordinator.coroutineManager.getIOScope().launch {
                coordinator._event.emit(CoordinatorEvent.FaultEvent(msg, e, params))
            }
        }

    }

    sealed interface CoordinatorEvent {
        data class GattAvailableEvent(val available: Boolean): CoordinatorEvent
        data class ConnectionTypeAvailableEvent(val available: Boolean, val type: ConnectionType): CoordinatorEvent
        data class ConnectionAvailableEvent(val available: Boolean): CoordinatorEvent
        data class RFCOMMDataReadyEvent(val data: String): CoordinatorEvent
        data class TCPDataReadyEvent(val data: String): CoordinatorEvent
        data class UDPDataReadyEvent(val data: String): CoordinatorEvent
        data class RttResultEvent(val diff: Long, val type: ConnectionType): CoordinatorEvent
        data class FaultEvent(val msg: String, val e: Exception, val params: Map<String, Any>?): CoordinatorEvent
    }

}