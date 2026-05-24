package cn.maoyanluo.bluetooth_library.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.log_library.LogUtils
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothGATTManager(
    private val ctx: Context,
    val device: BluetoothDevice,
    private val coroutineManager: CoroutineManager,
    private val callback: BluetoothGATTManagerCallback
) {

    companion object {
        const val TAG = "BluetoothGATTManager"
        const val RETRY_COUNT = 3
        const val RETRY_DIFF_TIME = 500L
    }
    private var bluetoothGatt: BluetoothGatt? = null
    @Volatile
    private var isInit = false
    private var isCallDestroy = false
    var isAvailable = false
        private set
    var initRetryCount = 0
    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt?,
            status: Int,
            newState: Int
        ) {
            LogUtils.i(TAG, "BluetoothGattCallback onConnectionStateChange status = $status, newState = $newState")
            when (newState)
            {
                BluetoothProfile.STATE_CONNECTED -> {
                    gatt?.discoverServices()
                    initRetryCount = 0
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (!isCallDestroy) {
                        clear()
                        isInit = false
                        LogUtils.d(TAG, "BluetoothGattCallback onConnectionStateChange onFault")
                        if (initRetryCount > RETRY_COUNT) {
                            initRetryCount = 0
                            coroutineManager.getIOScope().launch {
                                handler?.removeCallbacksAndMessages(null)
                                handler = null
                                handlerThread?.quitSafely()
                                handlerThread = null
                                callback.onFault(device)
                            }
                        } else {
                            init()
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            LogUtils.d(TAG, "BluetoothGattCallback onServicesDiscovered status = $status")
            isAvailable = true
            callback.onAvailable(device)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            LogUtils.d(TAG, "BluetoothGattCallback onCharacteristicRead status = $status, svc.uuid = ${characteristic.service.uuid}, data.uuid = ${characteristic.uuid}")
            onReadCharacteristic(status, value, characteristic)
        }

    }
    private val cache: HashMap<UUID, HashMap<UUID, ByteArray>> = HashMap()
    private val count: HashMap<UUID, HashMap<UUID, Int>> = HashMap()
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private val requestQueue: Queue<Pair<UUID, UUID>> = LinkedList()
    private var isRequest = false

    fun init() {
        LogUtils.i(TAG, "BluetoothGATTManager init count = $initRetryCount")
        if (handlerThread == null || handlerThread?.isAlive != true || handler == null) {
            handlerThread = HandlerThread(TAG).also {
                it.start()
                handler = Handler(it.looper)
            }
        }
        handler?.post {
            synchronized(this@BluetoothGATTManager) {
                if (isInit) {
                    LogUtils.i(TAG, "BluetoothGATTManager init. svc has already init")
                    return@post
                }
                isInit = true
            }
            initRetryCount++
            isCallDestroy = false
            LogUtils.i(TAG, "BluetoothGATTManager connect GATT")
            bluetoothGatt = device.connectGatt(ctx, false, gattCallback, BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M_MASK, handler)
        }
    }

    fun readCharacteristic(svcUuid: UUID, dataUuid: UUID) {
        LogUtils.i(
            TAG,
            "BluetoothGATTManager readCharacteristic svc.uuid = $svcUuid, data.uuid = $dataUuid"
        )
        handler?.post {
            if (cache.contains(svcUuid) && cache[svcUuid] != null && cache[svcUuid]!!.contains(
                    dataUuid
                ) && cache[svcUuid]!![dataUuid] != null
            ) {
                callback.onCharacteristicRead(
                    cache[svcUuid]!![dataUuid]!!,
                    svcUuid,
                    dataUuid,
                    BluetoothGatt.GATT_SUCCESS,
                    device
                )
                LogUtils.i(
                    TAG,
                    "BluetoothGATTManager readCharacteristic shot cache svcUuid = $svcUuid, dataUuid = $dataUuid"
                )
            } else {
                requestQueue.add(Pair(svcUuid, dataUuid))
                processNextRequest()
            }
        }
    }

    fun readCharacteristicInner(svcUuid: UUID, dataUuid: UUID) {
        if (!count.contains(svcUuid) || count[svcUuid] == null) {
            count[svcUuid] = HashMap()
        }
        if (!increaseCount(svcUuid, dataUuid)) {
            LogUtils.e(
                TAG,
                "BluetoothGATTManager readCharacteristic read count gt max count. return"
            )
            isRequest = false
            clearCount(svcUuid, dataUuid)
            callback.onCharacteristicRead(
                byteArrayOf(0),
                svcUuid,
                dataUuid,
                BluetoothGatt.GATT_FAILURE,
                device
            )
            processNextRequest()
            return
        }
        LogUtils.i(
            TAG,
            "BluetoothGATTManager readCharacteristic request data svcUuid = $svcUuid, dataUuid = $dataUuid, time = ${count[svcUuid]!![dataUuid]}"
        )
        val service = bluetoothGatt?.getService(svcUuid)
        val characteristic = service?.getCharacteristic(dataUuid)
        if (characteristic == null || bluetoothGatt?.readCharacteristic(characteristic) != true) {
            LogUtils.w(
                TAG,
                "bluetoothGatt?.readCharacteristic not return true, retry. svcUuid = $svcUuid, dataUuid = $dataUuid"
            )
            isRequest = false
            requestQueue.add(Pair(svcUuid, dataUuid))
            handler?.postDelayed({ processNextRequest() }, RETRY_DIFF_TIME)
        }
    }

    private fun processNextRequest() {
        LogUtils.i(TAG, "processNextRequest isRequest = $isRequest")
        if (!isRequest && requestQueue.isNotEmpty()) {
            val request = requestQueue.poll()
            if (request != null) {
                isRequest = true
                readCharacteristicInner(request.first, request.second)
            }
        }
    }

    private fun increaseCount(svcUuid: UUID, dataUuid: UUID): Boolean {
        if (count[svcUuid]!!.contains(dataUuid)) {
            count[svcUuid]!![dataUuid] = count[svcUuid]!![dataUuid]!! + 1
        } else {
            count[svcUuid]!![dataUuid] = 1
        }
        return count[svcUuid]!![dataUuid]!! <= RETRY_COUNT
    }

    private fun clearCount(svcUuid: UUID, dataUuid: UUID) {
        if (count.contains(svcUuid) && count[svcUuid] != null && count[svcUuid]!!.contains(dataUuid)) {
            count[svcUuid]!!.remove(dataUuid)
            if (count[svcUuid]!!.isEmpty()) {
                count.remove(svcUuid)
            }
        }
    }

    private fun onReadCharacteristic(status: Int, data: ByteArray, characteristic: BluetoothGattCharacteristic) {
        LogUtils.i(TAG, "onReadCharacteristic status = $status, svc uuid = ${characteristic.service.uuid}, data uuid = ${characteristic.uuid}")
        val svcUuid = characteristic.service.uuid
        val dataUuid = characteristic.uuid
        var delayTime = 0L
        if (status == BluetoothGatt.GATT_SUCCESS) {
            LogUtils.i(TAG, "BluetoothGATTManager onReadCharacteristic Success.")
            callback.onCharacteristicRead(data, svcUuid, dataUuid, status, device)
            if (!cache.contains(svcUuid) || cache[svcUuid] == null) {
                cache[svcUuid] = HashMap()
            }
            cache[svcUuid]!![dataUuid] = data
            clearCount(svcUuid, dataUuid)
        } else {
            requestQueue.add(Pair(svcUuid, dataUuid))
            delayTime = RETRY_DIFF_TIME
        }
        isRequest = false
        handler?.postDelayed( { processNextRequest() }, delayTime)
    }

    private fun clear() {
        LogUtils.i(TAG, "clear")
        isAvailable = false
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        count.clear()
        cache.clear()
        requestQueue.clear()
        isRequest = false
    }

    fun destroy() {
        LogUtils.i(TAG, "destroy")
        handler?.post {
            synchronized(this@BluetoothGATTManager) {
                if (!isInit) {
                    return@post
                }
                isInit = false
            }
            isCallDestroy = true
            clear()
            callback.onDestroy()
            coroutineManager.getIOScope().launch {
                handler?.removeCallbacksAndMessages(null)
                handler = null
                handlerThread?.quitSafely()
                handlerThread = null
            }
        }
    }


    interface BluetoothGATTManagerCallback {
        fun onAvailable(device: BluetoothDevice)
        fun onCharacteristicRead(data: ByteArray, svcUuid: UUID, dataUuid: UUID, status: Int, device: BluetoothDevice)
        fun onFault(device: BluetoothDevice)
        fun onDestroy()

    }

}