package cn.maoyanluo.socket_common_library

interface IClientTransport {
    enum class SocketClientState {
        CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED
    }

    val available: Boolean
        get() = state == SocketClientState.CONNECTED

    val state: SocketClientState

    fun connect()
    fun disconnect()
    fun sendData(data: ByteArray, id: Int = -1)

}