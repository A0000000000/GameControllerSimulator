package cn.maoyanluo.bluetooth_library.socket

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.socket_common_library.SocketServer
import cn.maoyanluo.socket_common_library.SocketServerCallback
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@Suppress("MissingPermission")
class BluetoothSocketServer(
    private val adapter: BluetoothAdapter,
    private val name: String,
    private val uuid: UUID,
    serverCallback: SocketServerCallback<BluetoothSocket>,
    coroutineManager: CoroutineManager
): SocketServer<BluetoothServerSocket, BluetoothSocket>(serverCallback, coroutineManager) {

    override fun createServerSocket(): BluetoothServerSocket = adapter.listenUsingRfcommWithServiceRecord(name, uuid)

    override fun acceptSocket(serverSocketSnapshot: BluetoothServerSocket?): BluetoothSocket? = serverSocketSnapshot?.accept()

    override fun createAcceptClient(
        socket: BluetoothSocket,
        callback: SocketServerCallback.ClientCallback,
        coroutineManager: CoroutineManager
    ): SocketServer.Client<BluetoothSocket> = Client(
        socket,
        callback,
        coroutineManager
    )

    class Client(
        private val socket: BluetoothSocket,
        callback: SocketServerCallback.ClientCallback,
        coroutineManager: CoroutineManager
        ): SocketServer.Client<BluetoothSocket>(socket, callback, coroutineManager) {

        override fun getOutputStream(): OutputStream? = socket.outputStream
        override fun getInputStream(): InputStream? = socket.inputStream

    }

}