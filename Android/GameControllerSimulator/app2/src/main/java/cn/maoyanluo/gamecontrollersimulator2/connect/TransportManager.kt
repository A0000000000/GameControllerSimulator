package cn.maoyanluo.gamecontrollersimulator2.connect

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import cn.maoyanluo.bluetooth_library.socket.BluetoothSocketClient
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.log_library.LogUtils
import cn.maoyanluo.network_library.tcp.TcpSocketClient
import cn.maoyanluo.socket_common_library.IClientTransport
import cn.maoyanluo.socket_common_library.SocketClientCallback
import kotlinx.coroutines.launch
import java.util.EnumMap
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.atomics.AtomicInt


class TransportManager(
    private val coroutineManager: CoroutineManager,
    private val callback: TransportManagerCallback
    ) {

    companion object {
        const val TAG = "TransportManager"
    }

    private val transports: EnumMap<ConnectionType, IClientTransport> = EnumMap(ConnectionType::class.java)
    private var availableCallbackCount: AtomicInteger = AtomicInteger(0)

    fun initRFCOMM(adapter: BluetoothAdapter, device: BluetoothDevice, uuid: UUID) {
        LogUtils.d(TAG, "init RFCOMM. uuid = $uuid")
        coroutineManager.getIOScope().launch {
            synchronized(transports) {
                if (transports[ConnectionType.BLE] != null) {
                    return@launch
                }
                transports[ConnectionType.BLE] = BluetoothSocketClient(
                    adapter,
                    device,
                    uuid,
                    SocketClientCallbackImpl(this@TransportManager, ConnectionType.BLE),
                    coroutineManager
                ).also {
                    it.connect()
                }
            }
        }
    }

    fun initTcpSocket(host: String, port: Int) {
        LogUtils.d(TAG, "init tcp. host = $host, port = $port")
        coroutineManager.getIOScope().launch {
            synchronized(transports) {
                if (transports[ConnectionType.TCP] != null) {
                    return@launch
                }
                transports[ConnectionType.TCP] = TcpSocketClient(
                    host, port, SocketClientCallbackImpl(
                        this@TransportManager,
                        ConnectionType.TCP
                    ), coroutineManager
                ).also {
                    it.connect()
                }
            }
        }
    }

    fun initUdpPacket(host: String, port: Int) {
        coroutineManager.getIOScope().launch {
            synchronized(transports) {
                if (transports[ConnectionType.UDP] != null) {
                    return@launch
                }
                // Todo 实现UDP传输
            }
        }
    }

    /**
     * 尽量保证使用调用方要求的方式，如果不可用，fallback到一个可用的方式
     */
    fun sendData(data: ByteArray, type: ConnectionType, id: Int = -1) {
        LogUtils.i(TAG, "send data. id = $id, except type = $type")
        synchronized(transports) {
                transports[type]?.takeIf { it.available }
                    ?: transports.values.firstOrNull { it.available }
            }?.sendData(data, id)
    }

    fun isAvailable(type: ConnectionType): Boolean {
        return transports[type]?.state == IClientTransport.SocketClientState.CONNECTED
    }

    fun destroy() {
        LogUtils.d(TAG, "destroy")
        synchronized(transports) {
            for (item in transports) {
                item.value?.disconnect()
            }
            transports.clear()
        }
    }

    private fun onDataReady(data: ByteArray, type: ConnectionType) {
        LogUtils.i(TAG, "onDataReady type = $type")
        callback.onDataReady(data, type)
    }

    private fun onConnectionAvailableChange(available: Boolean, type: ConnectionType) {
        LogUtils.d(TAG, "onConnectionAvailableChange available = $available, type = $type")
        if (!available) {
            synchronized(transports) {
                transports.remove(type)?.disconnect()
            }
        }
        callback.onConnectionAvailableChange(available, type)
        if (available) {
            if (availableCallbackCount.incrementAndGet() == 1) {
                LogUtils.i(TAG, "onConnectionAvailableChange onAvailable")
                callback.onAvailableChange(true)
            }
        } else {
            if (availableCallbackCount.decrementAndGet() == 0) {
                LogUtils.i(TAG, "onConnectionAvailableChange onUnavailable")
                callback.onAvailableChange(true)
            }
        }
    }

    private fun removeConnection(type: ConnectionType) {
        synchronized(transports) {
            transports.remove(type)?.disconnect()
        }
    }

    private fun onFault(msg: String, e: Exception, params: Map<String, Any>? = null) {
        Log.w(TAG, "onFault msg = $msg, params = $params", e)
        callback.onFault(msg, e, params)
    }

    private class SocketClientCallbackImpl(
        private val manager: TransportManager,
        private val type: ConnectionType
    ) : SocketClientCallback {

        companion object {
            const val TAG = "SocketClientCallbackImpl"
        }

        override fun onConnectSuccess() {
            LogUtils.i(TAG, "onConnectSuccess type = $type")
            manager.onConnectionAvailableChange(true, type)
        }

        override fun onConnectException(e: Exception) {
            LogUtils.e(TAG, "onConnectException type = $type", e)
            manager.removeConnection(type)
        }

        override fun onSendDataException(e: Exception, id: Int) {
            LogUtils.e(TAG, "onSendDataException id = $id, type = $type", e)
            manager.onFault("onSendDataException", e, mapOf(Pair("id", id)))
        }

        override fun onDisconnect() {
            LogUtils.i(TAG, "onDisconnect type = $type")
            manager.onConnectionAvailableChange(false, type)
        }

        override fun onDataReady(data: ByteArray) {
            LogUtils.i(TAG, "onDataReady type = $type")
            manager.onDataReady(data, type)
        }

        override fun onDataRevException(e: Exception) {
            LogUtils.e(TAG, "onDataRevException type = $type", e)
            manager.onFault("onDataRevException", e)
        }

    }

    interface TransportManagerCallback {
        fun onDataReady(data: ByteArray, type: ConnectionType)
        fun onAvailableChange(available: Boolean)
        fun onConnectionAvailableChange(available: Boolean, type: ConnectionType)
        fun onFault(msg: String, e: Exception, params: Map<String, Any>? = null)
    }

}
enum class ConnectionType {
    BLE, TCP, UDP
}

