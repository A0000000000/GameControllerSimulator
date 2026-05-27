package cn.maoyanluo.gamecontrollersimulator2.connect

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.connect.SessionManager.ConnectionManagerState
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityId
import cn.maoyanluo.log_library.LogUtils
import java.util.UUID

/**
 * 带有Session的连接控制器
 */
class ConnectionController(
    coroutineManager: CoroutineManager,
    private val callback: ConnectionControllerCallback
) {

    companion object {
        const val TAG = "ConnectionController"
    }

    var selectType = ConnectionType.BLE

    val available
        get() = sessionManager.state == ConnectionManagerState.READY

    private val transportManagerCallback = object : TransportManager.TransportManagerCallback {
        override fun onDataReady(data: ByteArray, type: ConnectionType) {
            LogUtils.i(TAG, "TransportManagerCallback onDataReady type = $type")
            protocolRouter.dispatcherData(data, type)
        }

        override fun onAvailableChange(available: Boolean) {
            LogUtils.i(TAG, "TransportManagerCallback onAvailableChange available = $available")
            if (!available) {
                sessionManager.onAllConnectionDisconnect()
            } else {
                transportManager.getAnyAvailableConnectionType()?.let {
                    selectType = it
                }
            }
        }

        override fun onConnectionAvailableChange(
            available: Boolean,
            type: ConnectionType
        ) {
            LogUtils.i(TAG, "onConnectionAvailableChange onAvailableChange available = $available, type = $type")
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
            LogUtils.w(TAG, "onConnectionAvailableChange onFault msg = $msg, params = $params", e)
            callback.onFault(msg, e, params)
        }

    }

    private val sessionManagerCallback = object : SessionManager.SessionManagerCallback {
        override fun sendData(data: BaseEntity<*>) {
            LogUtils.i(TAG, "SessionManagerCallback sendData type is selectType: $selectType")
            transportManager.sendData(protocolRouter.encode(data), selectType)
        }

        override fun sendData(
            data: BaseEntity<*>,
            type: ConnectionType
        ) {
            LogUtils.i(TAG, "SessionManagerCallback sendData type is $type")
            transportManager.sendData(protocolRouter.encode(data), type)
        }

        override fun onRttResult(
            diff: Long,
            type: ConnectionType
        ) {
            LogUtils.i(TAG, "SessionManagerCallback onRttResult diff = $diff, type = $type")
            callback.onRttResult(diff, type)
        }

        override fun onSessionAvailableChange(available: Boolean) {
            LogUtils.i(TAG, "SessionManagerCallback onSessionAvailableChange available = $available")
            callback.onAvailableChange(available)
        }

    }

    private val transportManager: TransportManager = TransportManager(coroutineManager, transportManagerCallback)
    private val sessionManager: SessionManager = SessionManager(coroutineManager, sessionManagerCallback)
    private val protocolRouter: ProtocolRouter = ProtocolRouter(coroutineManager)

    fun init() {
        LogUtils.i(TAG, "init (register session handler)")
        protocolRouter.registerHandler(sessionManager.getRouterHandlers())
    }

    fun initRFCOMM(adapter: BluetoothAdapter, device: BluetoothDevice, uuid: UUID) {
        LogUtils.i(TAG, "init RFCOMM uuid = $uuid")
        transportManager.initRFCOMM(adapter, device, uuid)
    }

    fun initTcpSocket(host: String, port: Int) {
        LogUtils.i(TAG, "init tcp socket. host = $host, port = $port")
        transportManager.initTcpSocket(host, port)
    }

    fun initUdpPacket(host: String, port: Int) {
        LogUtils.i(TAG, "init udp packet. host = $host, port = $port")
        transportManager.initUdpPacket(host, port)
    }

    fun isAvailable(type: ConnectionType): Boolean {
        val res = transportManager.isAvailable(type)
        LogUtils.i(TAG, "check $type available is $res")
        return res
    }

    fun sendData(entity: BaseEntity<*>, id: Int = -1) {
        LogUtils.i(TAG, "sendData id = $id")
        transportManager.sendData(protocolRouter.encode(entity), selectType, id)
    }

    /**
     * @return 注册失败的Handler
     */
    fun registerHandler(handlers: List<RouterHandler>): List<RouterHandler> {
        LogUtils.i(TAG, "registerHandler count = ${handlers.size}")
        protocolRouter.registerHandler(handlers.filter { it.id != EntityId.CONNECTION_MANAGER_INTERNAL_ID })
        LogUtils.i(TAG, "success count = ${handlers.filter { it.id != EntityId.CONNECTION_MANAGER_INTERNAL_ID }.size}, failed count = ${handlers.filter { it.id == EntityId.CONNECTION_MANAGER_INTERNAL_ID }.size}")
        return handlers.filter { it.id == EntityId.CONNECTION_MANAGER_INTERNAL_ID }
    }

    fun requestRtt(type: ConnectionType) {
        LogUtils.i(TAG, "requestRtt type = $type")
        sessionManager.requestRtt(type)
    }

    fun destroy() {
        LogUtils.i(TAG, "destroy controller")
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