package cn.maoyanluo.gamecontrollersimulator2.connect

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityId
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityType
import cn.maoyanluo.log_library.LogUtils
import com.google.gson.JsonElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch


class SessionManager(
    private val coroutineManager: CoroutineManager,
    private val callback: SessionManagerCallback
) {
    companion object {
        const val TAG = "SessionManager"
    }

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
        LogUtils.i(TAG, "onConnectionSuccess type = $type")
        when (state) {
            ConnectionManagerState.READY -> {
                LogUtils.i(TAG, "not first onConnectionSuccess, prepare register new type. type = $type")
                registerNewType(type, clientId)
            }
            ConnectionManagerState.DESTROY -> {
                LogUtils.i(TAG, "first onConnectionSuccess, prepare request client id. type = $type")
                state = ConnectionManagerState.REQUEST_ID
                requestClientId(type)
            }
            else -> {
                LogUtils.i(TAG, "onConnectionSuccess request client id running. pending register. type = $type")
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
        LogUtils.i(TAG, "client id return. client id is $clientId")
        this.clientId = clientId
        state = ConnectionManagerState.READY
        pendingRegisterClientJob?.cancel()
        pendingRegisterClientJob = coroutineManager.getIOScope().launch {
            pendingRegisterClient?.consumeEach {
                LogUtils.i(TAG, "client id return. client id is $clientId. pending register running. type is $it")
                registerNewType(it, clientId)
            }
        }
    }

    fun requestRtt(type: ConnectionType) {
        LogUtils.i(TAG, "request test $type rtt.")
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
        LogUtils.w(TAG, "all connect unavailable. session destroy")
        clientId = ""
        pendingRegisterClient?.close()
        pendingRegisterClient = null
        state = ConnectionManagerState.DESTROY
    }

    fun destroy() {
        state = ConnectionManagerState.DESTROY_ING
        LogUtils.i(TAG, "destroy session")
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
                    LogUtils.i(TAG, "onDataReady TYPE_REQUEST_CLIENT_ID_RESULT")
                    setClientId(data.data?.toString() ?: "")
                }
            ),
            RouterHandler(
                id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                type = EntityType.TYPE_UNREGISTER_CLIENT_ID_RESULT,
                handler = { data, type ->
                    LogUtils.i(TAG, "onDataReady TYPE_UNREGISTER_CLIENT_ID_RESULT")
                    state = ConnectionManagerState.DESTROY_ING
                    onAllConnectionDisconnect()
                }
            ),
            RouterHandler(
                id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                type = EntityType.TYPE_ECHO,
                handler = { data, type ->
                    LogUtils.i(TAG, "onDataReady TYPE_ECHO")
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
                    LogUtils.i(TAG, "onDataReady TYPE_RTT_RESULT")
                    callback.onRttResult(
                        System.currentTimeMillis() - data.data.toString().toLong(), type
                    )
                }
            ),
        )
    }

    private fun requestClientId(type: ConnectionType) {
        LogUtils.i(TAG, "requestClientId type = $type")
        callback.sendData(
            BaseEntity<JsonElement>(
                type = EntityType.TYPE_REQUEST_CLIENT_ID,
                id = EntityId.CONNECTION_MANAGER_INTERNAL_ID,
                timestamp = System.currentTimeMillis(),
                null
            ), type)
    }
    private fun registerNewType(type: ConnectionType, clientId: String) {
        LogUtils.i(TAG, "registerNewType type = $type, clientId = $clientId")
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