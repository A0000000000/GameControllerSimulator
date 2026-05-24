package cn.maoyanluo.network_library.udp

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.socket_common_library.IClientTransport
import cn.maoyanluo.socket_common_library.MAX_BUFF_SIZE
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

abstract class UdpBase(
    private val callback: UdpCallback,
    private val coroutineManager: CoroutineManager,
) : IClientTransport {

    private var socket: DatagramSocket? = null

    @Volatile
    private var isStart = false

    protected abstract fun createDatagramSocket(): DatagramSocket
    protected abstract fun filterReceiveData(address: InetAddress, port: Int): Boolean

    fun startReceiveData() {
        coroutineManager.getIOScope().launch {
            synchronized(this@UdpBase) {
                if (isStart) return@launch
                try {
                    socket = createDatagramSocket()
                    isStart = true
                    coroutineManager.getIOScope().launch {
                        callback.onStart()
                    }
                } catch (e: Exception) {
                    try {
                        socket?.close()
                    } catch (_: Exception) {
                    }
                    socket = null
                    coroutineManager.getIOScope().launch {
                        callback.onStartException(e)
                    }
                }
            }
            val socketSnapshot = socket
            val buff = ByteArray(MAX_BUFF_SIZE)
            while (isStart && socketSnapshot != null) {
                try {
                    val packet = DatagramPacket(buff, buff.size)
                    socketSnapshot.receive(packet)
                    val data = packet.data.copyOfRange(packet.offset, packet.offset + packet.length)
                    val targetAddress = packet.address
                    val targetPort = packet.port
                    if (filterReceiveData(targetAddress, targetPort)) {
                        coroutineManager.getIOScope().launch {
                            callback.onDataReceive(data) { d, id ->
                                try {
                                    sendData(d, id)
                                } catch (e: Exception) {
                                    callback.onDataSendException(e, id)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    callback.onDataReceiveException(e)
                }
            }
        }
    }

    fun stopReceiveData() {
        coroutineManager.getIOScope().launch {
            synchronized(this@UdpBase) {
                if (!isStart) return@launch
                isStart = false
                socket?.close()
                socket = null
            }
            coroutineManager.getIOScope().launch {
                callback.onStop()
            }
        }
    }

    protected fun sendPacket(packet: DatagramPacket) {
        socket?.send(packet)
    }

    override val state: IClientTransport.SocketClientState
        get() = IClientTransport.SocketClientState.CONNECTED

    override fun connect() {
    }

    override fun disconnect() {
    }


}