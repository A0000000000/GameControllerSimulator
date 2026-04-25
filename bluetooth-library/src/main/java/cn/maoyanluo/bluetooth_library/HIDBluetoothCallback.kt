package cn.maoyanluo.bluetooth_library

import android.bluetooth.BluetoothDevice

interface HIDBluetoothCallback {
    fun initResult(result: Boolean)
    fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean)
    fun onConnectionStateChanged(device: BluetoothDevice?, state: Int)
    fun release()

}