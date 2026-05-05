package cn.maoyanluo.socket_common_library

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.socket_common_library.utils.IntConverter
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

abstract class SocketClient<TSocket: Closeable>(
    private val clientCallback: SocketClientCallback,
    private val coroutineManager: CoroutineManager
) {

    @Volatile
    private var isConnected = false
    protected var socket: TSocket? = null

    abstract fun createSocket(): TSocket
    abstract fun getOutputStream(): OutputStream?
    abstract fun getInputStream(): InputStream?

    fun connect() {
        coroutineManager.getIOScope().launch {
            synchronized(this@SocketClient) {
                if (isConnected) {
                    return@launch
                }
                try {
                    socket = createSocket()
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

    private fun dataRevLoop() {
        coroutineManager.getIOScope().launch {
            val socketSnapshot = socket
            try {
                while (isConnected && socketSnapshot === socket) {
                    val inputStream = getInputStream() ?: throw IOException("inputStream is null")
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
                synchronized(this@SocketClient) {
                    if (socketSnapshot === socket) {
                        disconnect()
                    }
                }
            }
        }
    }

    fun sendData(data: ByteArray, id: Int = -1) {
        coroutineManager.getIOScope().launch {
            synchronized(this@SocketClient) {
                if (!isConnected) {
                    return@launch
                }
                try {
                    if (data.size !in 0..MAX_BUFF_SIZE) {
                        throw IllegalArgumentException("package is too large: ${data.size}, max=${MAX_BUFF_SIZE}")
                    }
                    val outputStream = getOutputStream() ?: throw IOException("outputStream is null")
                    outputStream.write(IntConverter.toBigEndian(data.size), 0, 4)
                    outputStream.write(data, 0, data.size)
                    outputStream.flush()
                } catch (e: Exception) {
                    coroutineManager.getIOScope().launch { clientCallback.onSendDataException(e, id) }
                }
            }
        }

    }
    fun disconnect() {
        coroutineManager.getIOScope().launch {
            synchronized(this@SocketClient) {
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