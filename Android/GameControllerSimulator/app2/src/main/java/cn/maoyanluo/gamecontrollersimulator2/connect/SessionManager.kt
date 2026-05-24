package cn.maoyanluo.gamecontrollersimulator2.connect

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityId
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityType
import com.google.gson.JsonElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch


class SessionManager(
    private val coroutineManager: CoroutineManager,
    private val callback: SessionManagerCallback
) {
    enum class ConnectionManagerState {
        REQUEST_ID, READY, DESTROY_ING, DESTROY
    }

    var state: ConnectionManagerState = ConnectionManagerState.DESTROY
        @Synchronized
        private set(value) {
            if (value == ConnectionManagerState.READY && field != ConnectionManagerState.READY) {
                callback.onSessionAvailableChange(true)
            }
            if (value != ConnectionManagerState.READY && field == ConnectionManagerState.READY) {
                callback.onSessionAvailableChange(false)
            }
            field = value
        }
    private var pendingRegisterClientJob: Job? = null
    private var pendingRegisterClient: Channel<ConnectionType>? = null
    private var clientId = ""

    fun onConnectionSuccess(type: ConnectionType) {
        when (state) {
            ConnectionManagerState.READY -> registerNewType(type, clientId)
            ConnectionManagerState.DESTROY -> {
                state = ConnectionManagerState.REQUEST_ID
                requestClientId(type)
            }
            else -> {
                synchronized(this) {
                    if (pendingRegisterClient == null) {
                        pendingRegisterClient = Channel()
                    }
                    coroutineManager.getIOScope().launch {
                        pendingRegisterClient?.send(type)
                    }
                }
            }
        }
    }

    private fun setClientId(clientId: String) {
        this.clientId = clientId
        state = ConnectionManagerState.READY
        pendingRegisterClientJob?.cancel()
        pendingRegisterClientJob = coroutineManager.getIOScope().launch {
            pendingRegisterClient?.consumeEach {
                registerNewType(it, clientId)
            }
        }
    }

    fun requestRtt(type: ConnectionType) {
        val currentTime = System.currentTimeMillis()
        callback.sendData(BaseEntity(
            id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
            type = EntityType.TYPE_RTT,
            timestamp = currentTime,
            data = currentTime.toString()
        ), type)
    }

    fun onAllConnectionDisconnect() {
        state = ConnectionManagerState.DESTROY_ING
        clientId = ""
        pendingRegisterClient?.close()
        pendingRegisterClient = null
        state = ConnectionManagerState.DESTROY
    }

    fun destroy() {
        state = ConnectionManagerState.DESTROY_ING
        callback.sendData(BaseEntity(
            type = EntityType.TYPE_UNREGISTER_CLIENT_ID,
            id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
            timestamp = System.currentTimeMillis(),
            clientId
        ))
        onAllConnectionDisconnect()
    }

    fun getRouterHandlers(): List<RouterHandler> {
        return listOf(
            RouterHandler(
                id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                type = EntityType.TYPE_REQUEST_CLIENT_ID_RESULT,
                handler = { data, type ->
                    setClientId(data.data?.toString() ?: "")
                }
            ),
            RouterHandler(
                id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                type = EntityType.TYPE_UNREGISTER_CLIENT_ID_RESULT,
                handler = { data, type ->
                    state = ConnectionManagerState.DESTROY_ING
                    onAllConnectionDisconnect()
                }
            ),
            RouterHandler(
                id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                type = EntityType.TYPE_ECHO,
                handler = { data, type ->
                    callback.sendData(data.copy(
                        type = EntityType.TYPE_ECHO_RESULT,
                        timestamp = System.currentTimeMillis()
                    ), type)
                }
            ),
            RouterHandler(
                id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                type = EntityType.TYPE_RTT_RESULT,
                handler = { data, type ->
                    callback.onRttResult(
                        System.currentTimeMillis() - data.data.toString().toLong(), type
                    )
                }
            ),
        )
    }

    private fun requestClientId(type: ConnectionType) {
        callback.sendData(
            BaseEntity<JsonElement>(
                type = EntityType.TYPE_REQUEST_CLIENT_ID,
                id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                timestamp = System.currentTimeMillis(),
                null
            ), type)
    }
    private fun registerNewType(type: ConnectionType, clientId: String) {
        callback.sendData(BaseEntity(
            type = EntityType.TYPE_NEW_TYPE_CONNECT,
            id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
            timestamp = System.currentTimeMillis(),
            clientId
        ), type)
    }

    interface SessionManagerCallback {

        fun sendData(data: BaseEntity<*>)
        fun sendData(data: BaseEntity<*>, type: ConnectionType)
        fun onRttResult(diff: Long, type: ConnectionType)
        fun onSessionAvailableChange(available: Boolean)

    }

}