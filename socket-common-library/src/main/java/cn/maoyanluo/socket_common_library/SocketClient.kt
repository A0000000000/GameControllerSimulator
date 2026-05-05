package cn.maoyanluo.socket_common_library

interface SocketClient {

    fun connect()
    fun sendData(data: ByteArray, id: Int = -1)
    fun disconnect()

}