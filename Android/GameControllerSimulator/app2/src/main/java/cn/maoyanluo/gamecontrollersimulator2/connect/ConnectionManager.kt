package cn.maoyanluo.gamecontrollersimulator2.connect

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.text.TextUtils
import cn.maoyanluo.bluetooth_library.socket.BluetoothSocketClient
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityId
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityType
import cn.maoyanluo.network_library.tcp.TcpSocketClient
import cn.maoyanluo.socket_common_library.SocketClient
import cn.maoyanluo.socket_common_library.SocketClientCallback
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.Closeable
import java.util.UUID

/**
 * 链接管理器，通过是用蓝牙来进行连接，然后交换TCP/UDP信息来创建网络连接
 *
 * 由上层自主选择传输数据使用的方式
 */
class ConnectionManager(
    private val callback: ConnectionCallback,
    private val coroutineManager: CoroutineManager
): Closeable {

    enum class ConnectionType {
        BLE, TCP, UDP
    }

    companion object {
        val INTERNAL_ID_ARRAY = arrayOf(EntityId.CONNECTION_MANAGER_INTERNAL_ID)
    }

    var connectionType = ConnectionType.BLE
        set(value) {
            if (value == ConnectionType.UDP) {
                throw IllegalStateException("Not support type: $value.")
            }
            field = value
        }

    private val gson = Gson()

    private var clientId = ""
        set(value) {
            val current = isAvailable
            field = value
            onAvailableChange(current, isAvailable)
        }

    var isAvailable = false
        private set
        get() = (bluetoothAvailable || tcpAvailable || udpAvailable) && !TextUtils.isEmpty(clientId)

    var bluetoothAvailable = false
        private set(value) {
            if (value && !field) {
                callback.onConnectionAvailable(true, ConnectionType.BLE)
            }
            if (!value && field) {
                callback.onConnectionAvailable(false, ConnectionType.BLE)
            }
            val current = isAvailable
            field = value
            onAvailableChange(current, isAvailable)
        }
    var tcpAvailable = false
        private set(value) {
            if (value && !field) {
                callback.onConnectionAvailable(true, ConnectionType.TCP)
            }
            if (!value && field) {
                callback.onConnectionAvailable(false, ConnectionType.TCP)
            }
            val current = isAvailable
            field = value
            onAvailableChange(current, isAvailable)
        }
    var udpAvailable = false
        private set(value) {
            if (value && !field) {
                callback.onConnectionAvailable(true, ConnectionType.UDP)
            }
            if (!value && field) {
                callback.onConnectionAvailable(false, ConnectionType.UDP)
            }
            val current = isAvailable
            field = value
            onAvailableChange(current, isAvailable)
        }

    private var bluetoothSocketClient: BluetoothSocketClient? = null
    private val bluetoothClientCallback: SocketClientCallback = object : SocketClientCallback {

        override fun onConnectSuccess() {
            bluetoothAvailable = true
            requestClientId()
        }

        override fun onConnectException(e: Exception) {
            bluetoothAvailable = false
        }

        override fun onSendDataException(e: Exception, id: Int) = callback.onSendDataException(e, id, ConnectionType.BLE)

        override fun onDisconnect() {
            bluetoothAvailable = false
        }

        override fun onDataReady(data: ByteArray) {
            this@ConnectionManager.onDataReady(data)
        }

        override fun onDataRevException(e: Exception) = callback.onDataRevException(e, ConnectionType.BLE)

    }

    fun initRFCOMM(adapter: BluetoothAdapter, device: BluetoothDevice, uuid: UUID) {
        coroutineManager.getIOScope().launch {
            if (bluetoothAvailable) {
                return@launch
            }
            bluetoothSocketClient = BluetoothSocketClient(
                adapter,
                device,
                uuid,
                bluetoothClientCallback,
                coroutineManager
            )
            bluetoothSocketClient?.connect()
        }
    }

    private var tcpSocketClient: TcpSocketClient? = null
    private val tcpClientCallback: SocketClientCallback = object : SocketClientCallback {
        override fun onConnectSuccess() {
            tcpAvailable = true
            requestClientId()
        }

        override fun onConnectException(e: Exception) {
            tcpAvailable = false
        }

        override fun onSendDataException(e: Exception, id: Int) = callback.onSendDataException(e, id, ConnectionType.TCP)

        override fun onDisconnect() {
            tcpAvailable = false
        }

        override fun onDataReady(data: ByteArray) {
            this@ConnectionManager.onDataReady(data)
        }

        override fun onDataRevException(e: Exception) = callback.onDataRevException(e, ConnectionType.TCP)
    }

    fun initTcpSocket(host: String, port: Int) {
        coroutineManager.getIOScope().launch {
            if (tcpAvailable) {
                return@launch
            }
            tcpSocketClient = TcpSocketClient(host, port, tcpClientCallback, coroutineManager)
            tcpSocketClient?.connect()
        }
    }

    fun initUdpPacket(host: String, port: Int) {
        coroutineManager.getIOScope().launch {
            if (udpAvailable) {
                return@launch
            }
        }
    }

    fun <T> sendData(data: BaseEntity<T>, id: Int = -1) {
        when (connectionType) {
            ConnectionType.BLE if bluetoothAvailable -> bluetoothSocketClient?.sendData(gson.toJson(data).toByteArray(), id)
            ConnectionType.TCP if tcpAvailable -> tcpSocketClient?.sendData(gson.toJson(data).toByteArray(), id)
            else -> {
                when {
                    bluetoothAvailable -> bluetoothSocketClient?.sendData(gson.toJson(data).toByteArray(), id)
                    tcpAvailable -> tcpSocketClient?.sendData(gson.toJson(data).toByteArray(), id)
                    else -> callback.onSendDataException(IllegalStateException("Not support type: $connectionType."), -1, connectionType)
                }
            }
        }
    }

    fun destroy() {
        canRequestClientId = true
        coroutineManager.getIOScope().launch {
            if (isAvailable) {
                unregisterClientId()
            }
        }
    }

    @Volatile
    private var canRequestClientId = true
    private fun requestClientId() {
        synchronized(this) {
            if (canRequestClientId) {
                canRequestClientId = false
                sendData(
                    BaseEntity<JsonElement>(
                        type = EntityType.TYPE_REQUEST_CLIENT_ID,
                        id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                        timestamp = System.currentTimeMillis(),
                        null
                    )
                )
            }
        }
    }

    private fun <T: Closeable> registerNewType(client: SocketClient<T>) {
        client.sendData(gson.toJson(BaseEntity(
            type = EntityType.TYPE_NEW_TYPE_CONNECT,
            id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
            timestamp = System.currentTimeMillis(),
            clientId
        )).toByteArray())
    }

    private fun unregisterClientId() = sendData(
        BaseEntity(
            type = EntityType.TYPE_UNREGISTER_CLIENT_ID,
            id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
            timestamp = System.currentTimeMillis(),
            clientId
        )
    )

    private fun filterById(entity: BaseEntity<*>) = entity.id in INTERNAL_ID_ARRAY

    private fun getTypeClass(type: Int): Class<*> {
        val typeClass = callback.getTypeClass(type)
        if (typeClass != JsonElement::class.java) {
            return typeClass
        }
        return EntityType.TYPE_MAPPING[type] ?: JsonElement::class.java
    }

    private fun onDataReady(data: ByteArray) {
        val jsonStr = String(data)
        val jsonObject = gson.fromJson(jsonStr, JsonObject::class.java)
        val type = jsonObject.get("type")?.asInt ?: -1
        val typeTokenType = if (type == -1) {
            TypeToken.getParameterized(BaseEntity::class.java, JsonElement::class.java).type
        } else {
            TypeToken.getParameterized(BaseEntity::class.java, getTypeClass(type)).type
        }
        val entity = gson.fromJson<BaseEntity<*>>(jsonStr, typeTokenType)
        if (filterById(entity)) {
            onDataReady(entity)
        } else {
            callback.onDataReady(entity)
        }
    }

    private fun onDataReady(entity: BaseEntity<*>) {
        when (entity.type) {
            EntityType.TYPE_REQUEST_CLIENT_ID_RESULT -> {
                clientId = entity.data?.toString() ?: ""
                bluetoothSocketClient?.let {
                    registerNewType(it)
                }
                tcpSocketClient?.let {
                    registerNewType(it)
                }
            }

            EntityType.TYPE_UNREGISTER_CLIENT_ID_RESULT -> {
                canRequestClientId = true
                bluetoothSocketClient?.disconnect()
                bluetoothSocketClient = null
                tcpSocketClient?.disconnect()
                tcpSocketClient = null
                clientId = ""
            }
        }
    }

    private fun onAvailableChange(current: Boolean, now: Boolean) {
        if (!current && now) {
            callback.onManagerAvailableChange(true)
        }
        if (current && !now) {
            callback.onManagerAvailableChange(false)
        }
    }

    override fun close() = destroy()

}
