package cn.maoyanluo.network_library.tcp

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.socket_common_library.SocketServer
import cn.maoyanluo.socket_common_library.SocketServerCallback
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class TcpSocketServer(
    private val port: Int,
    serverCallback: SocketServerCallback<Socket>,
    coroutineManager: CoroutineManager
): SocketServer<ServerSocket, Socket>(serverCallback, coroutineManager) {

    override fun createServerSocket(): ServerSocket = ServerSocket(port)

    override fun acceptSocket(serverSocketSnapshot: ServerSocket?): Socket? = serverSocketSnapshot?.accept()

    override fun createAcceptClient(
        socket: Socket,
        callback: SocketServerCallback.ClientCallback,
        coroutineManager: CoroutineManager
    ): SocketServer.Client<Socket> = Client(
        socket,
        callback,
        coroutineManager
    )

    class Client(
        private val socket: Socket,
        callback: SocketServerCallback.ClientCallback,
        coroutineManager: CoroutineManager
    ) : SocketServer.Client<Socket>(socket, callback, coroutineManager) {

        override fun getOutputStream(): OutputStream? = socket.getOutputStream()
        override fun getInputStream(): InputStream? = socket.getInputStream()

    }

}