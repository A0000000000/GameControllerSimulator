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

    private val gson = Gson()

    var connectionType = ConnectionType.BLE
        set(value) {
            if (value == ConnectionType.UDP) {
                throw IllegalStateException("Not support type: $value.")
            }
            field = value
        }

    var isAvailable = false
        private set
        get() = (bluetoothAvailable || tcpAvailable || udpAvailable) && !TextUtils.isEmpty(clientId)

    private var clientId = ""
        set(value) {
            val current = isAvailable
            field = value
            onAvailableChange(current, isAvailable)
        }

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
    private var tcpSocketClient: TcpSocketClient? = null

    fun initRFCOMM(adapter: BluetoothAdapter, device: BluetoothDevice, uuid: UUID) {
        coroutineManager.getIOScope().launch {
            if (bluetoothAvailable) {
                return@launch
            }
            bluetoothSocketClient = BluetoothSocketClient(
                adapter,
                device,
                uuid,
                SocketClientCallbackImpl(this@ConnectionManager, ConnectionType.BLE),
                coroutineManager
            )
            bluetoothSocketClient?.connect()
        }
    }

    fun initTcpSocket(host: String, port: Int) {
        coroutineManager.getIOScope().launch {
            if (tcpAvailable) {
                return@launch
            }
            tcpSocketClient = TcpSocketClient(
                host, port, SocketClientCallbackImpl(
                    this@ConnectionManager,
                    ConnectionType.TCP
                ), coroutineManager
            )
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
            ConnectionType.BLE if bluetoothAvailable -> bluetoothSocketClient?.sendData(
                gson.toJson(
                    data
                ).toByteArray(), id
            )

            ConnectionType.TCP if tcpAvailable -> tcpSocketClient?.sendData(
                gson.toJson(data).toByteArray(), id
            )

            else -> {
                when {
                    bluetoothAvailable -> bluetoothSocketClient?.sendData(
                        gson.toJson(data).toByteArray(), id
                    )

                    tcpAvailable -> tcpSocketClient?.sendData(gson.toJson(data).toByteArray(), id)
                    else -> callback.onSendDataException(
                        IllegalStateException("Not support type: $connectionType."),
                        -1,
                        connectionType
                    )
                }
            }
        }
    }

    fun requestRtt(type: ConnectionType) {
        val currentTime = System.currentTimeMillis()
        val rttEntity = BaseEntity(
            id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
            type = EntityType.TYPE_RTT,
            timestamp = currentTime,
            data = currentTime.toString()
        )
        when (type) {
            ConnectionType.BLE -> {
                if (bluetoothAvailable) {
                    bluetoothSocketClient?.sendData(gson.toJson(rttEntity).toByteArray())
                }
            }

            ConnectionType.TCP -> {
                if (tcpAvailable) {
                    tcpSocketClient?.sendData(gson.toJson(rttEntity).toByteArray())
                }
            }

            else -> Unit
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

    private fun <T : Closeable> registerNewType(client: SocketClient<T>) {
        client.sendData(
            gson.toJson(
                BaseEntity(
                    type = EntityType.TYPE_NEW_TYPE_CONNECT,
                    id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                    timestamp = System.currentTimeMillis(),
                    clientId
                )
            ).toByteArray()
        )
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

    private fun onDataReady(data: ByteArray, connType: ConnectionType) {
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
            onDataReadyInner(entity, connType)
        } else {
            callback.onDataReady(entity)
        }
    }

    private fun onDataReadyInner(entity: BaseEntity<*>, type: ConnectionType) {
        when (entity.type) {
            EntityType.TYPE_REQUEST_CLIENT_ID_RESULT -> {
                clientId = entity.data?.toString() ?: ""
                when (type) {
                    ConnectionType.BLE -> {
                        tcpSocketClient?.let {
                            registerNewType(it)
                        }
                    }

                    ConnectionType.TCP -> {
                        bluetoothSocketClient?.let {
                            registerNewType(it)
                        }
                    }
                    else -> Unit
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

            EntityType.TYPE_ECHO -> {
                sendData(entity.copy(type = EntityType.TYPE_ECHO_RESULT))
            }

            EntityType.TYPE_ECHO_RESULT -> {

            }

            EntityType.TYPE_RTT -> {

            }

            EntityType.TYPE_RTT_RESULT -> {
                try {
                    callback.onRttResult(
                        System.currentTimeMillis() - entity.data.toString().toLong(), type
                    )
                } catch (e: Exception) {
                    callback.onDataRevException(e, type)
                }
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

    private class SocketClientCallbackImpl(
        private val manager: ConnectionManager,
        private val type: ConnectionType
    ) : SocketClientCallback {

        override fun onConnectSuccess() {
            when (type) {
                ConnectionType.BLE -> {
                    manager.bluetoothAvailable = true
                }

                ConnectionType.TCP -> {
                    manager.tcpAvailable = true
                }

                else -> Unit
            }
            manager.requestClientId()
        }

        override fun onConnectException(e: Exception) {
            when (type) {
                ConnectionType.BLE -> {
                    manager.bluetoothAvailable = false
                }

                ConnectionType.TCP -> {
                    manager.tcpAvailable = false
                }

                else -> Unit
            }
        }

        override fun onSendDataException(e: Exception, id: Int) {
            manager.callback.onSendDataException(e, id, type)
        }

        override fun onDisconnect() {
            when (type) {
                ConnectionType.BLE -> {
                    manager.bluetoothAvailable = false
                }

                ConnectionType.TCP -> {
                    manager.tcpAvailable = false
                }

                else -> Unit
            }
        }

        override fun onDataReady(data: ByteArray) {
            manager.onDataReady(data, type)
        }

        override fun onDataRevException(e: Exception) {
            manager.callback.onDataRevException(e, type)
        }

    }

}
