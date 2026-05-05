package cn.maoyanluo.socket_common_library

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.socket_common_library.SocketServerCallback.ClientCallback
import cn.maoyanluo.socket_common_library.utils.IntConverter
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

abstract class SocketServer<TServerSocket: Closeable, TSocket: Closeable>(
    private val serverCallback: SocketServerCallback<TSocket>,
    private val coroutineManager: CoroutineManager
) {

    protected var serverSocket: TServerSocket? = null
    @Volatile
    protected var isStart = false

    protected abstract fun createServerSocket(): TServerSocket
    protected abstract fun acceptSocket(serverSocketSnapshot: TServerSocket?): TSocket?
    protected abstract fun createAcceptClient(socket: TSocket, callback: ClientCallback, coroutineManager: CoroutineManager): Client<TSocket>

    fun startListener() {
        coroutineManager.getIOScope().launch {
            synchronized(this@SocketServer) {
                if (isStart) {
                    return@launch
                }
                var currentServerSocket: TServerSocket? = null
                try {
                    currentServerSocket = createServerSocket()
                    serverSocket = currentServerSocket
                    isStart = true
                    coroutineManager.getIOScope().launch { serverCallback.onStartServerSuccess() }
                    startForeverLoop()
                } catch (e: Exception) {
                    try {
                        currentServerSocket?.close()
                    } catch (ignore: Exception) { }
                    coroutineManager.getIOScope().launch { serverCallback.onStartServerFailed(e) }

                }
            }
        }
    }

    private fun startForeverLoop() {
        coroutineManager.getIOScope().launch {
            val serverSocketSnapshot = serverSocket
            try {
                while (isStart && serverSocketSnapshot === serverSocket) {
                    val socket = acceptSocket(serverSocketSnapshot) ?: throw IOException("Accept socket null.")
                    onClientSocketAccept(socket)
                }
            } catch (e: Exception) {
                if (synchronized(this@SocketServer) {
                        return@synchronized isStart && serverSocketSnapshot === serverSocket
                    }) {
                    coroutineManager.getIOScope().launch { serverCallback.onForeverLoopException(e) }
                }
                synchronized(this@SocketServer) {
                    if (serverSocket === serverSocketSnapshot) {
                        stopListener()
                    }
                }
            }
        }
    }

    private fun onClientSocketAccept(socket: TSocket) {
        coroutineManager.getIOScope().launch {
            try {
                serverCallback.onNewClientConnect(
                    createAcceptClient(
                        socket,
                        serverCallback.createNewClientCallback(),
                        coroutineManager
                    )
                )
            } catch (e: Exception) {
                try {
                    socket.close()
                } catch (_: Exception) { }
                serverCallback.onNewClientException(e)
            }
        }
    }

    fun stopListener() {
        coroutineManager.getIOScope().launch {
            synchronized(this@SocketServer) {
                if (!isStart) {
                    return@synchronized
                }
                isStart = false
                try {
                    serverSocket?.close()
                    serverSocket = null
                } catch (ignore: Exception) {
                }
                coroutineManager.getIOScope().launch { serverCallback.onStopServer() }
            }
        }
    }

    abstract class Client<TSocket: Closeable>(
        private val socket: TSocket,
        private val callback: SocketServerCallback.ClientCallback,
        private val coroutineManager: CoroutineManager
    ) {
        private var isConnected = true

        init {
            dataRevLoop()
        }

        protected abstract fun getOutputStream(): OutputStream?
        protected abstract fun getInputStream(): InputStream?

        fun sendData(data: ByteArray, id: Int = -1) {
            coroutineManager.getIOScope().launch {
                synchronized(this@Client) {
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
                        coroutineManager.getIOScope().launch { callback.onSendDataException(e, id) }
                    }
                }
            }
        }

        fun isAvailable() = isConnected

        private fun dataRevLoop() {
            coroutineManager.getIOScope().launch {
                try {
                    while (isConnected) {
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
                        coroutineManager.getIOScope().launch { callback.onDataReady(buff) }
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        coroutineManager.getIOScope().launch { callback.onDataRevException(e) }
                    }
                    disconnect()
                }
            }
        }

        fun disconnect() {
            coroutineManager.getIOScope().launch {
                synchronized(this@Client) {
                    if (!isConnected) {
                        return@launch
                    }
                    isConnected = false
                    try {
                        socket.close()
                    } catch (ignore: Exception) {
                    }
                    callback.onDisconnect()
                }
            }
        }
    }

}