package cn.maoyanluo.socket_common_library

interface SocketServer {

    fun startListener()
    fun stopListener()

    interface Client {
        fun sendData(data: ByteArray, id: Int = -1)
        fun isAvailable(): Boolean
        fun disconnect()
    }

}