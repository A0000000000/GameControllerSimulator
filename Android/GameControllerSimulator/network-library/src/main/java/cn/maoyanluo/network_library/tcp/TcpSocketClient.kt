package cn.maoyanluo.network_library.tcp

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.socket_common_library.SocketClient
import cn.maoyanluo.socket_common_library.SocketClientCallback
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class TcpSocketClient(
    private val host: String,
    private val port: Int,
    clientCallback: SocketClientCallback,
    coroutineManager: CoroutineManager
): SocketClient<Socket>(clientCallback, coroutineManager) {

    override fun createSocket(): Socket = Socket(host, port)

    override fun getOutputStream(): OutputStream? = socket?.getOutputStream()

    override fun getInputStream(): InputStream? = socket?.getInputStream()

}