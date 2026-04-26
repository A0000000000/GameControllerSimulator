package cn.maoyanluo.bluetooth_library.socket

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import cn.maoyanluo.bluetooth_library.socket.utils.IntConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

@Suppress("MissingPermission")
class BluetoothSocketServer(
    private val adapter: BluetoothAdapter,
    private val name: String,
    private val uuid: UUID,
    private val serverCallback: BluetoothServerCallback
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: BluetoothServerSocket? = null
    @Volatile
    private var isStart = false

    fun startListener() {
        scope.launch {
            synchronized(this@BluetoothSocketServer) {
                if (isStart) {
                    return@launch
                }
                var currentServerSocket: BluetoothServerSocket? = null
                try {
                    currentServerSocket = adapter.listenUsingRfcommWithServiceRecord(name, uuid)
                    serverSocket = currentServerSocket
                    isStart = true
                    scope.launch { serverCallback.onStartServerSuccess() }
                    startForeverLoop()
                } catch (e: Exception) {
                    try {
                        currentServerSocket?.close()
                    } catch (ignore: Exception) { }
                    scope.launch { serverCallback.onStartServerFailed(e) }
                }
            }
        }
    }

    private fun startForeverLoop() {
        scope.launch {
            val serverSocketSnapshot = serverSocket
            try {
                while (isStart && serverSocketSnapshot === serverSocket) {
                    val socket =
                        serverSocketSnapshot?.accept() ?: throw IOException("Accept socket null.")
                    onClientSocketAccept(socket)
                }
            } catch (e: Exception) {
                if (synchronized(this@BluetoothSocketServer) {
                        return@synchronized isStart && serverSocketSnapshot === serverSocket
                    }) {
                    scope.launch { serverCallback.onStopForeverException(e) }
                }
                synchronized(this@BluetoothSocketServer) {
                    if (serverSocket === serverSocketSnapshot) {
                        stopListener()
                    }
                }
            }
        }
    }

    private fun onClientSocketAccept(socket: BluetoothSocket) {
        scope.launch {
            try {
                serverCallback.onNewClientConnect(
                    Client(
                        socket,
                        serverCallback.createNewClientCallback()
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
        synchronized(this@BluetoothSocketServer) {
            if (!isStart) {
                return
            }
            isStart = false
            try {
                serverSocket?.close()
                serverSocket = null
            } catch (ignore: Exception) {
            }
            scope.launch {  serverCallback.onStopServer() }
        }
    }

    class Client(
        private val socket: BluetoothSocket,
        private val callback: BluetoothServerCallback.ClientCallback
        ) {

        init {
            dataRevLoop()
        }

        private var isConnected = true
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun sendData(data: ByteArray, id: Int = -1) {
            scope.launch {
                synchronized(this@Client) {
                    if (!isConnected) {
                        return@launch
                    }
                    try {
                        if (data.size !in 0..MAX_BUFF_SIZE) {
                            throw IllegalArgumentException("package is too large: ${data.size}, max=$MAX_BUFF_SIZE")
                        }
                        val outputStream = socket.outputStream ?: throw IOException("outputStream is null")
                        outputStream.write(IntConverter.toBigEndian(data.size), 0, 4)
                        outputStream.write(data, 0, data.size)
                        outputStream.flush()
                    } catch (e: Exception) {
                        scope.launch { callback.onSendDataException(e, id) }
                    }
                }
            }
        }

        fun isAvailable() = isConnected

        private fun dataRevLoop() {
            scope.launch {
                try {
                    while (isConnected) {
                        val inputStream = socket.inputStream ?: throw IOException("inputStream is null")
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
                        scope.launch { callback.onDataReady(buff) }
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        scope.launch { callback.onDataRevException(e) }
                    }
                    disconnect()
                }
            }
        }

        fun disconnect() {
            var needCallback = false
            synchronized(this@Client) {
                if (!isConnected) {
                    return
                }
                isConnected = false
                try {
                    socket.close()
                } catch (ignore: Exception) { }
                scope.cancel()
                needCallback = true
            }
            if (needCallback) {
                callback.onDisconnect()
            }
        }

    }

}