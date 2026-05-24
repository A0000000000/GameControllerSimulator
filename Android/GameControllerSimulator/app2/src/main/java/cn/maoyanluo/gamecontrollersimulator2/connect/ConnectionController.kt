package cn.maoyanluo.gamecontrollersimulator2.connect

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.connect.SessionManager.ConnectionManagerState
import java.util.UUID

class ConnectionController(
    private val coroutineManager: CoroutineManager,
    private val callback: ConnectionControllerCallback
) {
    var selectType = ConnectionType.BLE

    val available
        get() = sessionManager.state == ConnectionManagerState.READY

    private val transportManagerCallback = object : TransportManagerCallback {
        override fun onDataReady(data: ByteArray, type: ConnectionType) {
            protocolRouter.dispatcherData(data, type)
        }

        override fun onAvailableChange(available: Boolean) {
            if (!available) {
                sessionManager.onAllConnectionDisconnect()
            }
        }

        override fun onConnectionAvailableChange(
            available: Boolean,
            type: ConnectionType
        ) {
            if (available) {
                sessionManager.onConnectionSuccess(type)
            }
            callback.onConnectionAvailableChange(available, type)
        }

        override fun onFault(
            msg: String,
            e: Exception,
            params: Map<String, Any>?
        ) {
            callback.onFault(msg, e, params)
        }

    }

    private val sessionManagerCallback = object : SessionManager.SessionManagerCallback {
        override fun sendData(data: BaseEntity<*>) {
            transportManager.sendData(protocolRouter.encode(data), selectType)
        }

        override fun sendData(
            data: BaseEntity<*>,
            type: ConnectionType
        ) {
            transportManager.sendData(protocolRouter.encode(data), type)
        }

        override fun onRttResult(
            diff: Long,
            type: ConnectionType
        ) {
            callback.onRttResult(diff, type)
        }

        override fun onSessionAvailableChange(available: Boolean) {
            callback.onAvailableChange(available)
        }

    }

    private val transportManager: TransportManager = TransportManager(coroutineManager, transportManagerCallback)
    private val sessionManager: SessionManager = SessionManager(coroutineManager, sessionManagerCallback)
    private val protocolRouter: ProtocolRouter = ProtocolRouter(coroutineManager)

    fun init() {
        protocolRouter.registerHandler(sessionManager.getRouterHandlers())
    }

    fun initRFCOMM(adapter: BluetoothAdapter, device: BluetoothDevice, uuid: UUID) {
        transportManager.initRFCOMM(adapter, device, uuid)
    }

    fun initTcpSocket(host: String, port: Int) {
        transportManager.initTcpSocket(host, port)
    }

    fun initUdpPacket(host: String, port: Int) {
        transportManager.initUdpPacket(host, port)
    }

    fun isAvailable(type: ConnectionType): Boolean {
        return transportManager.isAvailable(type)
    }

    fun sendData(entity: BaseEntity<*>, id: Int = -1) {
        transportManager.sendData(protocolRouter.encode(entity), selectType, id)
    }

    fun registerHandler(handlers: List<RouterHandler>) {
        protocolRouter.registerHandler(handlers)
    }

    fun requestRtt(type: ConnectionType) {
        sessionManager.requestRtt(type)
    }

    fun destroy() {
        sessionManager.destroy()
        transportManager.destroy()
        protocolRouter.clearHandlers()
    }

    interface ConnectionControllerCallback {
        fun onAvailableChange(available: Boolean)
        fun onConnectionAvailableChange(available: Boolean, type: ConnectionType)
        fun onRttResult(diff: Long, type: ConnectionType)
        fun onFault(msg: String, e: Exception, params: Map<String, Any>?)
    }

}