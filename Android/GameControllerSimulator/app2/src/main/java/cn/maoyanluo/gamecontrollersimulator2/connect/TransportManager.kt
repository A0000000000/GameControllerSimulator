package cn.maoyanluo.gamecontrollersimulator2.connect

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import cn.maoyanluo.bluetooth_library.socket.BluetoothSocketClient
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.network_library.tcp.TcpSocketClient
import cn.maoyanluo.socket_common_library.IClientTransport
import cn.maoyanluo.socket_common_library.SocketClientCallback
import kotlinx.coroutines.launch
import java.util.EnumMap
import java.util.UUID


class TransportManager(
    private val coroutineManager: CoroutineManager,
    private val callback: TransportManagerCallback
    ) {

    companion object {
        const val TAG = "TransportManager"
    }

    private val transports: EnumMap<ConnectionType, IClientTransport> = EnumMap(ConnectionType::class.java)
    private var available = false

    fun initRFCOMM(adapter: BluetoothAdapter, device: BluetoothDevice, uuid: UUID) {
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
                ).apply {
                    coroutineManager.getIOScope().launch {
                        receiveData.collect { data ->
                            onDataReady(data, ConnectionType.BLE)
                        }
                    }
                    connect()
                }
            }
        }
    }

    fun initTcpSocket(host: String, port: Int) {
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
                ).apply {
                    coroutineManager.getIOScope().launch {
                        receiveData.collect { data ->
                            onDataReady(data, ConnectionType.TCP)
                        }
                    }
                    connect()
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
        synchronized(transports) {
            transports[type]?.takeIf { it.available }
                ?: transports.values.firstOrNull { it.available }
        }?.sendData(data, id)
    }

    fun isAvailable(type: ConnectionType): Boolean {
        return transports[type]?.state == IClientTransport.SocketClientState.CONNECTED
    }

    fun destroy() {
        synchronized(transports) {
            for (item in transports) {
                item.value?.disconnect()
            }
            transports.clear()
        }
    }

    private fun onDataReady(data: ByteArray, type: ConnectionType) {
        callback.onDataReady(data, type)
    }

    private fun onAvailableChange(available: Boolean) {
        if (available && !this.available) {
            callback.onAvailableChange(true)
        }
        if (!available && this.available) {
            callback.onAvailableChange(false)
        }
        this.available = available
    }

    private fun onConnectionAvailableChange(available: Boolean, type: ConnectionType) {
        if (!available) {
            synchronized(transports) {
                transports.remove(type)?.disconnect()
            }
        } else {
            onAvailableChange(true)
        }
        if (transports.isEmpty()) {
            onAvailableChange(false)
        }
        callback.onConnectionAvailableChange(available, type)
    }

    private fun onFault(msg: String, e: Exception, params: Map<String, Any>? = null) {
        callback.onFault(msg, e, params)
    }

    private class SocketClientCallbackImpl(
        private val manager: TransportManager,
        private val type: ConnectionType
    ) : SocketClientCallback {
        private var clientAvailable = false

        override fun onConnectSuccess() {
            if (!clientAvailable) {
                manager.onConnectionAvailableChange(true, type)
            }
            clientAvailable = true
        }

        override fun onConnectException(e: Exception) {
            if (clientAvailable) {
                manager.onConnectionAvailableChange(false, type)
            }
            clientAvailable = false
        }

        override fun onSendDataException(e: Exception, id: Int) {
            manager.onFault("onSendDataException", e, mapOf(Pair("id", id)))
        }

        override fun onDisconnect() {
            if (clientAvailable) {
                manager.onConnectionAvailableChange(false, type)
            }
            clientAvailable = false
        }

        override fun onDataRevException(e: Exception) {
            manager.onFault("onDataRevException", e)
        }

    }
}
enum class ConnectionType {
    BLE, TCP, UDP
}

interface TransportManagerCallback {
    fun onDataReady(data: ByteArray, type: ConnectionType)
    fun onAvailableChange(available: Boolean)
    fun onConnectionAvailableChange(available: Boolean, type: ConnectionType)
    fun onFault(msg: String, e: Exception, params: Map<String, Any>? = null)
}