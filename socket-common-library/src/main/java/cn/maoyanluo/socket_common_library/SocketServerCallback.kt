package cn.maoyanluo.socket_common_library

import java.io.Closeable

interface SocketServerCallback<TSocket : Closeable> {

    fun onStartServerSuccess()
    fun onStartServerFailed(e: Exception)
    fun onStopServer()
    fun onForeverLoopException(e: Exception)
    fun createNewClientCallback(): ClientCallback
    fun onNewClientConnect(client: SocketServer.Client<TSocket>)
    fun onNewClientException(e: Exception)

    interface ClientCallback {
        fun onSendDataException(e: Exception, id: Int = -1)
        fun onDisconnect()
        fun onDataReady(data: ByteArray)
        fun onDataRevException(e: Exception)
    }

}