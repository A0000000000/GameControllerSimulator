package cn.maoyanluo.bluetooth_library.socket

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.socket_common_library.SocketClient
import cn.maoyanluo.socket_common_library.SocketClientCallback
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@Suppress("MissingPermission")
class BluetoothSocketClient(
    private val adapter: BluetoothAdapter,
    private val targetDevice: BluetoothDevice,
    private val uuid: UUID,
    clientCallback: SocketClientCallback,
    coroutineManager: CoroutineManager
): SocketClient<BluetoothSocket>(clientCallback, coroutineManager) {

    override fun createSocket(): BluetoothSocket {
        try {
            adapter.cancelDiscovery()
        } catch (ignore: Exception) { }
        val currentSocket = targetDevice.createRfcommSocketToServiceRecord(uuid)
        currentSocket.connect()
        return currentSocket
    }

    override fun getOutputStream(): OutputStream? = socket?.outputStream
    override fun getInputStream(): InputStream? = socket?.inputStream

}