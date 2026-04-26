package cn.maoyanluo.bluetooth_library

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context

@SuppressLint("MissingPermission")
class BluetoothSelectManager(private val ctx: Context) {

    private val bluetoothManager = ctx.getSystemService(BluetoothManager::class.java)
    private val adapter = bluetoothManager.adapter

    fun getBondedDevice(): List<BluetoothDevice> {
        return adapter.bondedDevices.toList()
    }

    fun getAdapter(): BluetoothAdapter {
        return adapter
    }

}