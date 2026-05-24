package cn.maoyanluo.socket_common_library

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.socket_common_library.utils.IntConverter
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

abstract class SocketClient<TSocket: Closeable>(
    private val clientCallback: SocketClientCallback,
    private val coroutineManager: CoroutineManager
) : IClientTransport {

    @Volatile
    private var _state = IClientTransport.SocketClientState.DISCONNECTED

    override val state: IClientTransport.SocketClientState
        get() = _state

    @Volatile
    protected var socket: TSocket? = null

    private val _receiveData = MutableSharedFlow<ByteArray>()
    override val receiveData = _receiveData.asSharedFlow()
    private var sendDataQueue: Channel<Pair<ByteArray, Int>>? = null
    private var receiveJob: Job? = null
    private var sendJob: Job? = null

    protected abstract fun createSocket(): TSocket
    protected abstract fun getOutputStream(): OutputStream?
    protected abstract fun getInputStream(): InputStream?

    override fun connect() {
        coroutineManager.getIOScope().launch {
            synchronized(this@SocketClient) {
                if (_state != IClientTransport.SocketClientState.DISCONNECTED) {
                    return@launch
                }
                _state = IClientTransport.SocketClientState.CONNECTING
            }
            try {
                socket = createSocket()
                sendDataQueue = Channel()
                synchronized(this@SocketClient) {
                    _state = IClientTransport.SocketClientState.CONNECTED
                }
                coroutineManager.getIOScope().launch {
                    clientCallback.onConnectSuccess()
                }
                receiveJob = dataRevLoop()
                sendJob = dataSendLoop()
            } catch (e: Exception) {
                synchronized(this@SocketClient) {
                    try {
                        socket?.close()
                    } catch (ignore: Exception) {
                    }
                    socket = null
                    _state = IClientTransport.SocketClientState.DISCONNECTED
                }
                coroutineManager.getIOScope().launch { clientCallback.onConnectException(e) }
                return@launch
            }
        }
    }

    private fun dataRevLoop(): Job {
        return coroutineManager.getIOScope().launch {
            try {
                while (_state == IClientTransport.SocketClientState.CONNECTED) {
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
                    _receiveData.emit(buff)
                }
            } catch (e: Exception) {
                if (_state != IClientTransport.SocketClientState.DISCONNECTING) {
                    coroutineManager.getIOScope().launch { clientCallback.onDataRevException(e) }
                    disconnect()
                }
            }
        }
    }

    private fun dataSendLoop(): Job {
        return coroutineManager.getIOScope().launch {
            while (_state == IClientTransport.SocketClientState.CONNECTED) {
                var id = -2
                try {
                    val data = sendDataQueue?.receive() ?: continue
                    id = data.second
                    val outputStream =
                        getOutputStream() ?: throw IOException("outputStream is null")
                    outputStream.write(IntConverter.toBigEndian(data.first.size), 0, 4)
                    outputStream.write(data.first, 0, data.first.size)
                    outputStream.flush()
                } catch (e: Exception) {
                    coroutineManager.getIOScope().launch { clientCallback.onSendDataException(e, id) }
                }
            }
        }
    }

    override fun sendData(data: ByteArray, id: Int) {
        coroutineManager.getIOScope().launch {
            if (_state != IClientTransport.SocketClientState.CONNECTED) {
                return@launch
            }
            try {
                sendDataQueue?.send(Pair(data, id))
            } catch (e: Exception) {
                clientCallback.onSendDataException(e, id)
            }
        }
    }

    override fun disconnect() {
        coroutineManager.getIOScope().launch {
            synchronized(this@SocketClient) {
                if (_state != IClientTransport.SocketClientState.CONNECTED) {
                    return@launch
                }
                _state = IClientTransport.SocketClientState.DISCONNECTING
            }
            try {
                sendDataQueue?.close()
                sendDataQueue = null
            } catch (_: Exception) {
            }
            try {
                socket?.close()
                socket = null
            } catch (_: Exception) {
            }
            try {
                sendJob?.cancel()
                sendJob = null
            } catch (_: Exception) {
            }
            try {
                receiveJob?.cancel()
                receiveJob = null
            } catch (_: Exception) {
            }
            synchronized(this@SocketClient) {
                _state = IClientTransport.SocketClientState.DISCONNECTED
            }
            clientCallback.onDisconnect()
        }
    }

}