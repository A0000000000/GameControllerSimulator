package cn.maoyanluo.socket_common_library

import java.io.Closeable

interface SocketServerCallback<TSocket: Closeable> {

    fun onStartServerSuccess()
    fun onStartServerFailed(e: Exception)
    fun onStopServer()
    fun onForeverLoopException(e: Exception)
    fun createNewClientCallback(): ClientCallback<TSocket>
    fun onNewClientConnect(client: SocketServer.Client<TSocket>)
    fun onNewClientException(e: Exception)

    interface ClientCallback<TSocket: Closeable> {
        fun onSendDataException(client: SocketServer.Client<TSocket>, e: Exception, id: Int = -1)
        fun onDisconnect(client: SocketServer.Client<TSocket>)
        fun onDataReady(client: SocketServer.Client<TSocket>, data: ByteArray)
        fun onDataRevException(client: SocketServer.Client<TSocket>, e: Exception)
    }

}