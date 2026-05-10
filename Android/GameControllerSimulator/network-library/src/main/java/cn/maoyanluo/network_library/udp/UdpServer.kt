package cn.maoyanluo.network_library.udp

import cn.maoyanluo.coroutine_library.CoroutineManager
import java.net.DatagramSocket
import java.net.InetAddress


class UdpServer(
    private val port: Int,
    callback: UdpBaseCallback,
    coroutineManager: CoroutineManager,
    ): UdpBase(callback, coroutineManager) {

    override fun createDatagramSocket(): DatagramSocket = DatagramSocket(port)

    override fun filterReceiveData(
        address: InetAddress,
        port: Int
    ): Boolean = true

}