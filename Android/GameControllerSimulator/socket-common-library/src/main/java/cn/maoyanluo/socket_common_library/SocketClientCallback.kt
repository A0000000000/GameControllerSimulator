package cn.maoyanluo.socket_common_library

interface SocketClientCallback {

    fun onConnectSuccess()
    fun onConnectException(e: Exception)
    fun onSendDataException(e: Exception, id: Int = -1)
    fun onDisconnect()
    fun onDataReady(data: ByteArray)
    fun onDataRevException(e: Exception)

}