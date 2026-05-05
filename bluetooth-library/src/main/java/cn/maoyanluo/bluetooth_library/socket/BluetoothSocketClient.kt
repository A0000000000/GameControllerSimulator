package cn.maoyanluo.bluetooth_library.socket

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.socket_common_library.MAX_BUFF_SIZE
import cn.maoyanluo.socket_common_library.SocketClient
import cn.maoyanluo.socket_common_library.SocketClientCallback
import cn.maoyanluo.socket_common_library.utils.IntConverter
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

@Suppress("MissingPermission")
class BluetoothSocketClient(
    private val adapter: BluetoothAdapter,
    private val targetDevice: BluetoothDevice,
    private val uuid: UUID,
    private val clientCallback: SocketClientCallback,
    private val coroutineManager: CoroutineManager
): SocketClient {

    @Volatile
    private var isConnected = false

    private var socket: BluetoothSocket? = null

    override fun connect() {
        coroutineManager.getIOScope().launch {
            synchronized(this@BluetoothSocketClient) {
                if (isConnected) {
                    return@launch
                }
                try {
                    adapter.cancelDiscovery()
                } catch (ignore: Exception) { }
                socket = targetDevice.createRfcommSocketToServiceRecord(uuid)
                try {
                    socket?.connect()
                    isConnected = true
                    coroutineManager.getIOScope().launch { clientCallback.onConnectSuccess() }
                    dataRevLoop()
                } catch (e: Exception) {
                    try {
                        socket?.close()
                    } catch (ignore: Exception) { }
                    socket = null
                    isConnected = false
                    coroutineManager.getIOScope().launch { clientCallback.onConnectException(e) }
                    return@launch
                }
            }
        }
    }

    override fun sendData(data: ByteArray, id: Int) {
        coroutineManager.getIOScope().launch {
            synchronized(this@BluetoothSocketClient) {
                if (!isConnected) {
                    return@launch
                }
                try {
                    if (data.size !in 0..MAX_BUFF_SIZE) {
                        throw IllegalArgumentException("package is too large: ${data.size}, max=${MAX_BUFF_SIZE}")
                    }
                    val outputStream = socket?.outputStream ?: throw IOException("outputStream is null")
                    outputStream.write(IntConverter.toBigEndian(data.size), 0, 4)
                    outputStream.write(data, 0, data.size)
                    outputStream.flush()
                } catch (e: Exception) {
                    coroutineManager.getIOScope().launch { clientCallback.onSendDataException(e, id) }
                }
            }
        }
    }

    private fun dataRevLoop() {
        coroutineManager.getIOScope().launch {
            val socketSnapshot = socket
            try {
                while (isConnected && socketSnapshot === socket) {
                    val inputStream = socketSnapshot?.inputStream ?: throw IOException("inputStream is null")
                    val sizeBuff = ByteArray(4)
                    var totalSize = 0
                    while (totalSize < 4) {
                        val read = inputStream.read(sizeBuff, totalSize, 4 - totalSize)
                        if (read == -1) {
                            disconnect()
                            return@launch
                        }
                        totalSize += read
                    }
                    val size = IntConverter.fromBigEndian(sizeBuff)
                    if (size !in 0..MAX_BUFF_SIZE) {
                        throw IOException("data size is exception. size = $size")
                    }
                    val buff = ByteArray(size)
                    totalSize = 0
                    while (totalSize < size) {
                        val read = inputStream.read(buff, totalSize, size - totalSize)
                        if (read == -1) {
                            disconnect()
                            return@launch
                        }
                        totalSize += read
                    }
                    coroutineManager.getIOScope().launch { clientCallback.onDataReady(buff) }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    coroutineManager.getIOScope().launch { clientCallback.onDataRevException(e) }
                }
                synchronized(this@BluetoothSocketClient) {
                    if (socketSnapshot === socket) {
                        disconnect()
                    }
                }
            }
        }
    }

    override fun disconnect() {
        coroutineManager.getIOScope().launch {
            synchronized(this@BluetoothSocketClient) {
                if (!isConnected) {
                    return@launch
                }
                isConnected = false
                try {
                    socket?.close()
                    socket = null
                } catch (ignore: Exception) {
                }
                clientCallback.onDisconnect()
            }
        }
    }

}