package cn.maoyanluo.bluetooth_library.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import cn.maoyanluo.coroutine_library.CoroutineManager
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothGATTManager(
    private val ctx: Context,
    val device: BluetoothDevice,
    private val coroutineManager: CoroutineManager,
    private val callback: BluetoothGATTManagerCallback
) {

    private var bluetoothGatt: BluetoothGatt? = null
    @Volatile
    private var isInit = false
    private var isCallDestroy = false
    var isAvailable = false
        private set
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt?,
            status: Int,
            newState: Int
        ) {
            when (newState)
            {
                BluetoothProfile.STATE_CONNECTED -> gatt?.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (!isCallDestroy) {
                        coroutineManager.getIOScope().launch {
                            isInit = false
                            isAvailable = false
                            callback.onFault(device)
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            coroutineManager.getIOScope().launch {
                callback.onAvailable(device)
                isAvailable = true
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            coroutineManager.getIOScope().launch {
                callback.onCharacteristicRead(value, characteristic.service.uuid, characteristic.uuid, device)
            }
        }

    }

    fun init() {
        coroutineManager.getIOScope().launch {
            synchronized(this@BluetoothGATTManager) {
                if (isInit) {
                    return@launch
                }
                isInit = true
            }
            isCallDestroy = false
            bluetoothGatt = device.connectGatt(ctx, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }
    }

    fun readCharacteristic(svcUuid: UUID, dataUuid: UUID) {
        coroutineManager.getIOScope().launch {
            val service = bluetoothGatt?.getService(svcUuid)
            val characteristic = service?.getCharacteristic(dataUuid)
            characteristic?.let {
                bluetoothGatt?.readCharacteristic(characteristic)
            }
        }
    }

    fun destroy() {
        coroutineManager.getIOScope().launch {
            synchronized(this@BluetoothGATTManager) {
                if (!isInit) {
                    return@launch
                }
                isInit = false
            }
            isCallDestroy = true
            isAvailable = false
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            callback.onDestroy()
        }
    }


    interface BluetoothGATTManagerCallback {
        fun onAvailable(device: BluetoothDevice)
        fun onCharacteristicRead(data: ByteArray, svcUuid: UUID, dataUuid: UUID, device: BluetoothDevice)
        fun onFault(device: BluetoothDevice)
        fun onDestroy()

    }

}