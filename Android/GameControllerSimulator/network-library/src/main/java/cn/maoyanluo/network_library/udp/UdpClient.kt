package cn.maoyanluo.network_library.udp

import cn.maoyanluo.coroutine_library.CoroutineManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpClient(
    private val targetPort: Int,
    private val targetAddress: InetAddress,
    callback: UdpBaseCallback,
    coroutineManager: CoroutineManager,
): UdpBase(callback, coroutineManager) {

    override fun createDatagramSocket(): DatagramSocket = DatagramSocket()

    override fun filterReceiveData(
        address: InetAddress,
        port: Int
    ): Boolean = (targetAddress == address && targetPort == port)

    fun sendData(data: ByteArray) {
        val targetPacket = DatagramPacket(data, data.size, targetAddress, targetPort)
        sendPacket(targetPacket)
    }

}