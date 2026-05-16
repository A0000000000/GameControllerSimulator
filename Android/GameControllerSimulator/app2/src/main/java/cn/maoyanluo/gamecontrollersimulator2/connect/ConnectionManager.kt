package cn.maoyanluo.gamecontrollersimulator2.connect

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.text.TextUtils
import cn.maoyanluo.bluetooth_library.socket.BluetoothSocketClient
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityId
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityType
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
    private val device: BluetoothDevice,
    private val adapter: BluetoothAdapter,
    private val callback: ConnectionCallback,
    private val coroutineManager: CoroutineManager
): Closeable {

    enum class ConnectionType {
        UDP, TCP, BLE
    }

    companion object {
        val HOST_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")!!
        val INTERNAL_ID_ARRAY = arrayOf(EntityId.CONNECTION_MANAGER_INTERNAL_ID)
    }

    var connectionType = ConnectionType.BLE
        set(value) {
            if (value != ConnectionType.BLE) {
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

    private var bluetoothAvailable = false
        set(value) {
            val current = isAvailable
            field = value
            onAvailableChange(current, isAvailable)
        }
    private var tcpAvailable = false
        set(value) {
            val current = isAvailable
            field = value
            onAvailableChange(current, isAvailable)
        }
    private var udpAvailable = false
        set(value) {
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

        override fun onSendDataException(e: Exception, id: Int) = callback.onSendDataException(e, id, connectionType)

        override fun onDisconnect() {
            bluetoothAvailable = false
        }

        override fun onDataReady(data: ByteArray) {
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

        override fun onDataRevException(e: Exception) = callback.onDataRevException(e, connectionType)

    }

    fun init() {
        bluetoothSocketClient = BluetoothSocketClient(
            adapter,
            device,
            HOST_UUID,
            bluetoothClientCallback,
            coroutineManager
        )
        bluetoothSocketClient?.connect()
    }

    fun setConnectionType(type: ConnectionType) {
        if (type != ConnectionType.BLE) {
            throw IllegalStateException("Not support type: $type.")
        }
        connectionType = type
    }

    fun <T> sendData(data: BaseEntity<T>, id: Int = -1) {
        when {
            connectionType == ConnectionType.BLE && bluetoothAvailable -> bluetoothSocketClient?.sendData(
                gson.toJson(data).toByteArray(), id
            )

            else -> throw IllegalStateException("Not support type: $connectionType.")
        }
    }

    fun destroy() {
        coroutineManager.getIOScope().launch {
            unregisterClientId()
        }
    }

    private fun requestClientId() = sendData(
        BaseEntity<Any?>(
            type = EntityType.TYPE_REQUEST_CLIENT_ID,
            id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
            timestamp = System.currentTimeMillis(),
            null
        )
    )

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
        return when (type) {
            EntityType.TYPE_REQUEST_CLIENT_ID -> String::class.java
            EntityType.TYPE_UNREGISTER_CLIENT_ID -> String::class.java
            else -> JsonElement::class.java
        }
    }

    private fun onDataReady(entity: BaseEntity<*>) {
        when (entity.type) {
            EntityType.TYPE_REQUEST_CLIENT_ID -> {
                clientId = entity.data?.toString() ?: ""
            }

            EntityType.TYPE_UNREGISTER_CLIENT_ID -> {
                bluetoothSocketClient?.disconnect()
                bluetoothSocketClient = null
                clientId = ""
            }
        }
    }

    private fun onAvailableChange(current: Boolean, now: Boolean) {
        if (!current && now) {
            callback.onManagerAvailable()
        }
        if (current && !now) {
            callback.onManagerUnavailable()
        }
    }

    override fun close() = destroy()

}