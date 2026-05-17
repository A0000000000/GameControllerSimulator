package cn.maoyanluo.socket_common_library

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.socket_common_library.SocketServerCallback.ClientCallback
import cn.maoyanluo.socket_common_library.utils.IntConverter
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

abstract class SocketServer<TServerSocket: Closeable, TSocket: Closeable>(
    private val serverCallback: SocketServerCallback<TSocket>,
    private val coroutineManager: CoroutineManager
) {

    protected var serverSocket: TServerSocket? = null
    @Volatile
    protected var isStart = false

    protected abstract fun createServerSocket(): TServerSocket
    protected abstract fun acceptSocket(serverSocketSnapshot: TServerSocket?): TSocket?
    protected abstract fun createAcceptClient(socket: TSocket, callback: ClientCallback<TSocket>, coroutineManager: CoroutineManager): Client<TSocket>

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
                    } catch (_: Exception) { }
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
                coroutineManager.getIOScope().launch { serverCallback.onForeverLoopException(e) }
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
                } catch (_: Exception) {
                }
                coroutineManager.getIOScope().launch { serverCallback.onStopServer() }
            }
        }
    }

    abstract class Client<TSocket: Closeable>(
        private val socket: TSocket,
        private val callback: ClientCallback<TSocket>,
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
                        val outputStream = getOutputStream() ?: throw IOException("outputStream is null")
                        outputStream.write(IntConverter.toBigEndian(data.size), 0, 4)
                        outputStream.write(data, 0, data.size)
                        outputStream.flush()
                    } catch (e: Exception) {
                        coroutineManager.getIOScope().launch { callback.onSendDataException(this@Client, e, id) }
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
                        coroutineManager.getIOScope().launch { callback.onDataReady(this@Client, buff) }
                    }
                } catch (e: Exception) {
                    coroutineManager.getIOScope().launch { callback.onDataRevException(this@Client, e) }
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
                    } catch (_: Exception) {
                    }
                    callback.onDisconnect(this@Client)
                }
            }
        }
    }

}