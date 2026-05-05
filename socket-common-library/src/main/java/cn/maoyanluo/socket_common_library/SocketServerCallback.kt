package cn.maoyanluo.socket_common_library

interface SocketServerCallback {

    fun onStartServerSuccess()
    fun onStartServerFailed(e: Exception)
    fun onStopServer()
    fun onForeverLoopException(e: Exception)
    fun createNewClientCallback(): ClientCallback
    fun onNewClientConnect(client: SocketServer.Client)
    fun onNewClientException(e: Exception)

    interface ClientCallback {
        fun onSendDataException(e: Exception, id: Int = -1)
        fun onDisconnect()
        fun onDataReady(data: ByteArray)
        fun onDataRevException(e: Exception)
    }

}