package cn.maoyanluo.network_library.udp

import cn.maoyanluo.socket_common_library.SocketServer

class UdpSocketServer: SocketServer {

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