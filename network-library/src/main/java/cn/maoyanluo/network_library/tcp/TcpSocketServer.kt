package cn.maoyanluo.network_library.tcp

import cn.maoyanluo.socket_common_library.SocketServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.net.ServerSocket

class TcpSocketServer(port: UInt): SocketServer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isStart = false

    override fun startListener() {
        TODO("Not yet implemented")
    }

    override fun stopListener() {
        TODO("Not yet implemented")
    }


    class Client: SocketServer.Client {
        override fun sendData(data: ByteArray, id: Int) {
            TODO("Not yet implemented")
        }

        override fun isAvailable(): Boolean {
            TODO("Not yet implemented")
        }

        override fun disconnect() {
            TODO("Not yet implemented")
        }

    }

}