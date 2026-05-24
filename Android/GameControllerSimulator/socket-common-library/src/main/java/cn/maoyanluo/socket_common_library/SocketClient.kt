package cn.maoyanluo.socket_common_library

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.log_library.LogUtils
import cn.maoyanluo.socket_common_library.utils.IntConverter
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

abstract class SocketClient<TSocket: Closeable>(
    private val clientCallback: SocketClientCallback,
    private val coroutineManager: CoroutineManager
) : IClientTransport {

    companion object {
        const val TAG = "SocketClient"
    }

    @Volatile
    private var _state = IClientTransport.SocketClientState.DISCONNECTED

    override val state: IClientTransport.SocketClientState
        get() = _state

    @Volatile
    protected var socket: TSocket? = null
    private var sendDataQueue: Channel<Pair<ByteArray, Int>>? = null
    private var receiveJob: Job? = null
    private var sendJob: Job? = null

    protected abstract fun createSocket(): TSocket
    protected abstract fun getOutputStream(): OutputStream?
    protected abstract fun getInputStream(): InputStream?

    override fun connect() {
        LogUtils.i(TAG, "connect")
        coroutineManager.getIOScope().launch {
            LogUtils.d(TAG, "connect async")
            synchronized(this@SocketClient) {
                if (_state != IClientTransport.SocketClientState.DISCONNECTED) {
                    LogUtils.w(TAG, "connect async return current state = $_state")
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
                LogUtils.d(TAG, "create socket success.")
                notifyCallback { clientCallback.onConnectSuccess() }
                receiveJob = dataRevLoop()
                sendJob = dataSendLoop()
                LogUtils.d(TAG, "create rev job、send job success.")
            } catch (e: Exception) {
                LogUtils.e(TAG, "create socket failed exception. msg = ${e.message}", e)
                synchronized(this@SocketClient) {
                    try {
                        socket?.close()
                    } catch (ignore: Exception) {
                    }
                    socket = null
                    _state = IClientTransport.SocketClientState.DISCONNECTED
                }
                notifyCallback { clientCallback.onConnectException(e) }
                return@launch
            }
        }
    }

    private fun dataRevLoop(): Job {
        return coroutineManager.getIOScope().launch {
            try {
                LogUtils.d(TAG, "begin data rev loop")
                while (_state == IClientTransport.SocketClientState.CONNECTED) {
                    val inputStream = getInputStream() ?: throw IOException("inputStream is null")
                    val sizeBuff = ByteArray(4)
                    var totalSize = 0
                    while (totalSize < 4) {
                        val read = inputStream.read(sizeBuff, totalSize, 4 - totalSize)
                        if (read == -1) {
                            LogUtils.w(TAG, "host disconnect intercept rev loop.")
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
                            LogUtils.w(TAG, "host disconnect intercept rev loop.")
                            disconnect()
                            return@launch
                        }
                        totalSize += read
                    }
                    notifyCallback { clientCallback.onDataReady(buff) }
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "rev loop exception. msg = ${e.message}", e)
                if (_state != IClientTransport.SocketClientState.DISCONNECTING) {
                    notifyCallback { clientCallback.onDataRevException(e) }
                    disconnect()
                }
            }
        }
    }

    private fun dataSendLoop(): Job {
        return coroutineManager.getIOScope().launch {
            LogUtils.d(TAG, "begin data send loop")
            while (_state == IClientTransport.SocketClientState.CONNECTED) {
                var id = -2
                var sendData: ByteArray? = null
                try {
                    val data = sendDataQueue?.receive() ?: continue
                    sendData = data.first
                    id = data.second
                    val outputStream =
                        getOutputStream() ?: throw IOException("outputStream is null")
                    outputStream.write(IntConverter.toBigEndian(data.first.size), 0, 4)
                    outputStream.write(data.first, 0, data.first.size)
                    outputStream.flush()
                } catch (e: Exception) {
                    LogUtils.e(TAG, "send data exception. msg = ${e.message}, sendData = ${String(sendData ?: byteArrayOf())}, id = $id", e)
                    notifyCallback { clientCallback.onSendDataException(e, id)  }
                }
            }
        }
    }

    override fun sendData(data: ByteArray, id: Int) {
        coroutineManager.getIOScope().launch {
            LogUtils.d(TAG, "sendData id = $id")
            if (_state != IClientTransport.SocketClientState.CONNECTED) {
                return@launch
            }
            try {
                sendDataQueue?.send(Pair(data, id))
            } catch (e: Exception) {
                LogUtils.d(TAG, "sendData exception id = $id, data = ${String(data)}, msg = ${e.message}", e)
                notifyCallback { clientCallback.onSendDataException(e, id) }
            }
        }
    }

    override fun disconnect() {
        LogUtils.i(TAG, "disconnect")
        coroutineManager.getIOScope().launch {
            LogUtils.i(TAG, "disconnect async")
            synchronized(this@SocketClient) {
                if (_state != IClientTransport.SocketClientState.CONNECTED) {
                    LogUtils.i(TAG, "disconnect async return")
                    return@launch
                }
                _state = IClientTransport.SocketClientState.DISCONNECTING
            }
            try {
                sendDataQueue?.close()
                sendDataQueue = null
            } catch (e: Exception) {
                LogUtils.w(TAG, "close send data queue exception. msg = ${e.message}", e)
            }
            try {
                socket?.close()
                socket = null
            } catch (e: Exception) {
                LogUtils.w(TAG, "close socket exception. msg = ${e.message}", e)
            }
            try {
                sendJob?.cancel()
                sendJob = null
            } catch (e: Exception) {
                LogUtils.w(TAG, "close send job exception. msg = ${e.message}", e)
            }
            try {
                receiveJob?.cancel()
                receiveJob = null
            } catch (e: Exception) {
                LogUtils.w(TAG, "close rev job exception. msg = ${e.message}", e)
            }
            synchronized(this@SocketClient) {
                _state = IClientTransport.SocketClientState.DISCONNECTED
            }
            LogUtils.i(TAG, "disconnect end")
            notifyCallback {  clientCallback.onDisconnect() }
        }
    }

    private fun notifyCallback(block: SocketClientCallback.() -> Unit) {
        coroutineManager.getIOScope().launch {
            try {
                clientCallback.block()
            } catch (e: Exception) {
                LogUtils.w(TAG, "callback exception!", e)
            }
        }
    }

}